package com.abin.mallchat.common.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.abin.mallchat.common.common.constant.RedisKey;
import com.abin.mallchat.common.common.exception.BusinessException;
import com.abin.mallchat.common.common.utils.JwtUtils;
import com.abin.mallchat.common.common.utils.PasswordEncoder;
import com.abin.mallchat.common.common.utils.RedisUtils;
import com.abin.mallchat.common.user.dao.UserDao;
import com.abin.mallchat.common.user.domain.entity.User;
import com.abin.mallchat.common.user.domain.vo.request.user.PwdLoginReq;
import com.abin.mallchat.common.user.domain.vo.response.user.UserInfoResp;
import com.abin.mallchat.common.user.service.LoginService;
import com.abin.mallchat.common.user.service.adapter.UserAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Description: 登录相关处理类
 * Author: <a href="https://github.com/zongzibinbin">abin</a>
 * Date: 2023-03-19
 */
@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDao userDao;

    //token过期时间
    private static final Integer TOKEN_EXPIRE_DAYS = 5;
    //token续期时间
    private static final Integer TOKEN_RENEWAL_DAYS = 2;

    @Override
    public UserInfoResp loginByPassword(PwdLoginReq req) {
        log.info("用户密码登录，用户名：{}", req.getUsername());
        // 1. 查询用户
        User user = userDao.getByUsername(req.getUsername());
        if (user == null) {
            log.info("用户名不存在：{}", req.getUsername());
            throw new BusinessException("用户名或密码错误");
        }

        // 2. 校验密码
        if (!PasswordEncoder.matches(req.getPassword(), user.getPassword())) {
            log.info("密码校验失败，用户名：{}", req.getUsername());
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 生成token
        String token = login(user.getId());
        log.info("用户登录成功，用户ID：{}", user.getId());

        // 4. 封装返回
        UserInfoResp resp = UserAdapter.buildUserInfoResp(user, 0); //0表示登录成功
        resp.setId(user.getId());
        resp.setName(user.getName());
        resp.setAvatar(user.getAvatar());
        resp.setSex(user.getSex());
        resp.setToken(token);
        return resp;
    }

    /**
     * 校验token是不是有效
     *
     * @param token
     * @return
     */
    @Override
    public boolean verify(String token) {
        Long uid = jwtUtils.getUidOrNull(token);
        if (Objects.isNull(uid)) {
            return false;
        }
        String key = RedisKey.getKey(RedisKey.USER_TOKEN_STRING, uid);
        String realToken = RedisUtils.getStr(key);
        return Objects.equals(token, realToken);//有可能token失效了，需要校验是不是和最新token一致
    }

    @Async
    @Override
    public void renewalTokenIfNecessary(String token) {
        Long uid = jwtUtils.getUidOrNull(token);
        if (Objects.isNull(uid)) {
            return;
        }
        String key = RedisKey.getKey(RedisKey.USER_TOKEN_STRING, uid);
        long expireDays = RedisUtils.getExpire(key, TimeUnit.DAYS);
        if (expireDays == -2) {//不存在的key
            return;
        }
        if (expireDays < TOKEN_RENEWAL_DAYS) {//小于一天的token帮忙续期
            RedisUtils.expire(key, TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);
        }
    }

    @Override
    public String login(Long uid) {
        String key = RedisKey.getKey(RedisKey.USER_TOKEN_STRING, uid);
        String token = RedisUtils.getStr(key);
        if (StrUtil.isNotBlank(token)) {
            return token;
        }
        //获取用户token
        token = jwtUtils.createToken(uid);
        RedisUtils.set(key, token, TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);//token过期用redis中心化控制，初期采用5天过期，剩1天自动续期的方案。后续可以用双token实���
        return token;
    }

    @Override
    public Long getValidUid(String token) {
        boolean verify = verify(token);
        return verify ? jwtUtils.getUidOrNull(token) : null;
    }

    public static void main(String[] args) {
        System.out.println();
    }
}
