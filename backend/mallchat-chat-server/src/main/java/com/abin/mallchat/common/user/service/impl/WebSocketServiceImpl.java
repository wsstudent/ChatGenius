package com.abin.mallchat.common.user.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.abin.mallchat.common.common.config.ThreadPoolConfig;
import com.abin.mallchat.common.common.constant.RedisKey;
import com.abin.mallchat.common.common.event.UserOfflineEvent;
import com.abin.mallchat.common.common.event.UserOnlineEvent;
import com.abin.mallchat.common.common.exception.BusinessException;
import com.abin.mallchat.common.common.utils.RedisUtils;
import com.abin.mallchat.common.user.dao.UserDao;
import com.abin.mallchat.common.user.domain.dto.WSChannelExtraDTO;
import com.abin.mallchat.common.user.domain.entity.User;
import com.abin.mallchat.common.user.domain.enums.RoleEnum;
import com.abin.mallchat.common.user.domain.enums.WSBaseResp;
import com.abin.mallchat.common.user.domain.vo.request.user.PwdLoginReq;
import com.abin.mallchat.common.user.domain.vo.request.ws.WSAuthorize;
import com.abin.mallchat.common.user.domain.vo.request.ws.WSPasswordLoginReq;
import com.abin.mallchat.common.user.domain.vo.response.user.UserInfoResp;
import com.abin.mallchat.common.user.service.IRoleService;
import com.abin.mallchat.common.user.service.LoginService;
import com.abin.mallchat.common.user.service.WebSocketService;
import com.abin.mallchat.common.user.service.adapter.WSAdapter;
import com.abin.mallchat.common.user.service.cache.UserCache;
import com.abin.mallchat.common.websocket.NettyUtil;
import com.abin.mallchat.transaction.service.MQProducer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Description: websocket处理类
 * Author: <a href="https://github.com/zongzibinbin">abin</a>
 * Date: 2023-03-19 16:21
 */
@Component
@Slf4j
public class WebSocketServiceImpl implements WebSocketService {

    private static final Duration EXPIRE_TIME = Duration.ofHours(1);
    private static final Long MAX_MUM_SIZE = 10000L;
    /**
     * 所有请求登录的code与channel关系
     */
    public static final Cache<Integer, Channel> WAIT_LOGIN_MAP = Caffeine.newBuilder()
            .expireAfterWrite(EXPIRE_TIME)
            .maximumSize(MAX_MUM_SIZE)
            .build();
    /**
     * 所有已连接的websocket连接列表和一些额外参数
     */
    private static final ConcurrentHashMap<Channel, WSChannelExtraDTO> ONLINE_WS_MAP = new ConcurrentHashMap<>();
    /**
     * 所有在线的用户和对应的socket
     */
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<Channel>> ONLINE_UID_MAP = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<Channel, WSChannelExtraDTO> getOnlineMap() {
        return ONLINE_WS_MAP;
    }

    /**
     * redis保存loginCode的key
     */
    private static final String LOGIN_CODE = "loginCode";
    @Autowired
    private WxMpService wxMpService;
    @Autowired
    private LoginService loginService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    @Qualifier(ThreadPoolConfig.WS_EXECUTOR)
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private UserCache userCache;
    @Autowired
    private IRoleService iRoleService;
    @Autowired
    private MQProducer mqProducer;

    /**
     * 处理用户登录请求，需要返回一张带code的二维码
     *
     * @param channel
     */
    @SneakyThrows
    @Override
    public void handleLoginReq(Channel channel) {
        //生成随机不重复的登录码,并将channel存在本地cache中
        Integer code = generateLoginCode(channel);
        //请求微信接口，获取登录码地址
        WxMpQrCodeTicket wxMpQrCodeTicket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket(code, (int) EXPIRE_TIME.getSeconds());
        //返回给前端（channel必在本地）
        sendMsg(channel, WSAdapter.buildLoginResp(wxMpQrCodeTicket));
    }

    /**
     * 处理用户扫码登录请求
     *
     * @param channel
     * @param wsPasswordLoginReq
     */
   @Override
   public void passwordLogin(Channel channel, WSPasswordLoginReq wsPasswordLoginReq) {
       String username = wsPasswordLoginReq.getUsername();
       String clientIp = NettyUtil.getAttr(channel, NettyUtil.IP);

       log.info("处理WebSocket密码登录请求");

       try {
           // 创建登录请求对象
           PwdLoginReq loginReq = new PwdLoginReq();
           loginReq.setUsername(username);
           loginReq.setPassword(wsPasswordLoginReq.getPassword());

           try {
               // 调用登录服务验证用户名密码
               UserInfoResp userInfoResp = loginService.loginByPassword(loginReq);
               // 直接使用loginService返回的用户信息，避免重复查询

               // 登录成功，更新状态
               User user = new User();
               user.setId(userInfoResp.getId());
               user.setName(userInfoResp.getName());
               user.setAvatar(userInfoResp.getAvatar());

               loginSuccess(channel, user, userInfoResp.getToken());
               log.info("WebSocket密码登录成功，用户ID：{}", user.getId());

           } catch (BusinessException e) {
               log.warn("WebSocket密码登录失败：{}, IP: {}", e.getMessage(), clientIp);
               sendMsg(channel, WSBaseResp.build(1000, "用户名或密码错误"));
           }
       } catch (Exception e) {
           String errorId = generateErrorId();
           log.error("WebSocket密码登录异常，错误ID：{}, IP: {}", errorId, clientIp, e);
           sendMsg(channel, WSBaseResp.build(1000, "系统异常，请稍后再试 (ErrorID: " + errorId + ")"));
       }
   }

   /**
    * 生成错误ID用于日志跟踪
    */
   private String generateErrorId() {
       return Long.toHexString(System.currentTimeMillis()) +
              Integer.toHexString(ThreadLocalRandom.current().nextInt(0xFFFF));
   }

    /**
     * 获取不重复的登录的code，微信要求最大不超过int的存储极限
     * 防止并发，可以给方法加上synchronize，也可以使用cas乐观锁
     *
     * @return
     */
    private Integer generateLoginCode(Channel channel) {
        int inc;
        do {
            //本地cache时间必须比redis key过期时间短，否则会出现并发问题
            inc = RedisUtils.integerInc(RedisKey.getKey(LOGIN_CODE), (int) EXPIRE_TIME.toMinutes(), TimeUnit.MINUTES);
        } while (WAIT_LOGIN_MAP.asMap().containsKey(inc));
        //储存一份在本地
        WAIT_LOGIN_MAP.put(inc, channel);
        return inc;
    }

    /**
     * 处理所有ws连接的事件
     *
     * @param channel
     */
    @Override
    public void connect(Channel channel) {
        ONLINE_WS_MAP.put(channel, new WSChannelExtraDTO());
    }

    @Override
    public void removed(Channel channel) {
        WSChannelExtraDTO wsChannelExtraDTO = ONLINE_WS_MAP.get(channel);
        Optional<Long> uidOptional = Optional.ofNullable(wsChannelExtraDTO)
                .map(WSChannelExtraDTO::getUid);
        boolean offlineAll = offline(channel, uidOptional);
        if (uidOptional.isPresent() && offlineAll) {//已登录用户断连,并且全下线成功
            User user = new User();
            user.setId(uidOptional.get());
            user.setLastOptTime(new Date());
            applicationEventPublisher.publishEvent(new UserOfflineEvent(this, user));
        }
    }

   // 在WebSocketServiceImpl的authorize方法中添加日志
   @Override
   public void authorize(Channel channel, WSAuthorize wsAuthorize) {
       log.info("处理认证请求，token: {}", wsAuthorize.getToken());
       //校验token
       boolean verifySuccess = loginService.verify(wsAuthorize.getToken());
       log.info("Token验证结果: {}", verifySuccess);

       if (verifySuccess) {
           //用户校验成功给用户登录
           Long uid = loginService.getValidUid(wsAuthorize.getToken());
           User user = userDao.getById(uid);
           log.info("用户认证成功，用户ID: {}", uid);

           // 续期token
           loginService.renewalTokenIfNecessary(wsAuthorize.getToken());
           NettyUtil.setAttr(channel, NettyUtil.TOKEN, wsAuthorize.getToken());
           loginSuccess(channel, user, wsAuthorize.getToken());
       } else { //让前端的token失效
           log.warn("认证失败，无效token");
           sendMsg(channel, WSAdapter.buildInvalidateTokenResp());
       }
   }

    /**
     * 密码登录成功，并更新状态
     */
    private void loginSuccess(Channel channel, User user, String token) {
        //更新上线列表
        online(channel, user.getId());

        //获取用户最高权限角色
        RoleEnum roleEnum = iRoleService.getHighestRole(user.getId());

        //返回给用户登录成功消息
        sendMsg(channel, WSAdapter.buildLoginSuccessResp(user, token, roleEnum));

        //发送用户上线事件
        boolean online = userCache.isOnline(user.getId());
        if (!online) {
            user.setLastOptTime(new Date());
            user.refreshIp(NettyUtil.getAttr(channel, NettyUtil.IP));
            applicationEventPublisher.publishEvent(new UserOnlineEvent(this, user));
        }

        log.info("登录成功，用户ID：{}，角色：{}，已发送登录成功消息", user.getId(), roleEnum);
    }

    /**
     * 用户上线
     */
    private void online(Channel channel, Long uid) {
        getOrInitChannelExt(channel).setUid(uid);
        ONLINE_UID_MAP.putIfAbsent(uid, new CopyOnWriteArrayList<>());
        ONLINE_UID_MAP.get(uid).add(channel);
        NettyUtil.setAttr(channel, NettyUtil.UID, uid);
    }

    /**
     * 用户下线
     * return 是否全下线成功
     */
    private boolean offline(Channel channel, Optional<Long> uidOptional) {
        ONLINE_WS_MAP.remove(channel);
        if (uidOptional.isPresent()) {
            CopyOnWriteArrayList<Channel> channels = ONLINE_UID_MAP.get(uidOptional.get());
            if (CollectionUtil.isNotEmpty(channels)) {
                channels.removeIf(ch -> Objects.equals(ch, channel));
            }
            return CollectionUtil.isEmpty(ONLINE_UID_MAP.get(uidOptional.get()));
        }
        return true;
    }

    /**
     * 扫码登录成功
     *
     * @param loginCode
     * @param uid
     * @return
     */
    @Override
    public Boolean scanLoginSuccess(Integer loginCode, Long uid) {
        //确认连接在该机器
        Channel channel = WAIT_LOGIN_MAP.getIfPresent(loginCode);
        if (Objects.isNull(channel)) {
            return Boolean.FALSE;
        }
        User user = userDao.getById(uid);
        //移除code
        WAIT_LOGIN_MAP.invalidate(loginCode);
        //调用用户登录模块
        String token = loginService.login(uid);
        //用户登录
        loginSuccess(channel, user, token);
        return Boolean.TRUE;
    }

    @Override
    public Boolean scanSuccess(Integer loginCode) {
        Channel channel = WAIT_LOGIN_MAP.getIfPresent(loginCode);
        if (Objects.nonNull(channel)) {
            sendMsg(channel, WSAdapter.buildScanSuccessResp());
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


    /**
     * 如果在线列表不存在，就先把该channel放进在线列表
     *
     * @param channel
     * @return
     */
    private WSChannelExtraDTO getOrInitChannelExt(Channel channel) {
        WSChannelExtraDTO wsChannelExtraDTO =
                ONLINE_WS_MAP.getOrDefault(channel, new WSChannelExtraDTO());
        WSChannelExtraDTO old = ONLINE_WS_MAP.putIfAbsent(channel, wsChannelExtraDTO);
        return ObjectUtil.isNull(old) ? wsChannelExtraDTO : old;
    }

    //entrySet的值不是快照数据,但是它支持遍历，所以无所谓了，不用快照也行。
    @Override
    public void sendToAllOnline(WSBaseResp<?> wsBaseResp, Long skipUid) {
        ONLINE_WS_MAP.forEach((channel, ext) -> {
            if (Objects.nonNull(skipUid) && Objects.equals(ext.getUid(), skipUid)) {
                return;
            }
            threadPoolTaskExecutor.execute(() -> sendMsg(channel, wsBaseResp));
        });
    }

    @Override
    public void sendToAllOnline(WSBaseResp<?> wsBaseResp) {
        sendToAllOnline(wsBaseResp, null);
    }

    @Override
    public void sendToUid(WSBaseResp<?> wsBaseResp, Long uid) {
        CopyOnWriteArrayList<Channel> channels = ONLINE_UID_MAP.get(uid);
        if (CollectionUtil.isEmpty(channels)) {
            log.info("用户：{}不在线", uid);
            return;
        }
        channels.forEach(channel -> {
            threadPoolTaskExecutor.execute(() -> sendMsg(channel, wsBaseResp));
        });
    }


    /**
     * 给本地channel发送消息
     *
     * @param channel
     * @param wsBaseResp
     */
    private void sendMsg(Channel channel, WSBaseResp<?> wsBaseResp) {
        channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(wsBaseResp)));
    }

}
