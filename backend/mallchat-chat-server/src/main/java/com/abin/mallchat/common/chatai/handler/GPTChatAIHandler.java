package com.abin.mallchat.common.chatai.handler;

import com.abin.mallchat.common.chat.domain.entity.Message;
import com.abin.mallchat.common.chat.domain.entity.msg.MessageExtra;
import com.abin.mallchat.common.chatai.domain.ChatGPTContext;
import com.abin.mallchat.common.chatai.domain.ChatGPTMsg;
import com.abin.mallchat.common.chatai.domain.builder.ChatGPTContextBuilder;
import com.abin.mallchat.common.chatai.domain.builder.ChatGPTMsgBuilder;
import com.abin.mallchat.common.chatai.properties.ChatGPTProperties;
import com.abin.mallchat.common.chatai.utils.ChatGPTUtils;
import com.abin.mallchat.common.common.constant.RedisKey;
import com.abin.mallchat.common.common.domain.dto.FrequencyControlDTO;
import com.abin.mallchat.common.common.exception.FrequencyControlException;
import com.abin.mallchat.common.common.service.frequencycontrol.FrequencyControlUtil;
import com.abin.mallchat.common.common.utils.DateUtils;
import com.abin.mallchat.common.common.utils.RedisUtils;
import com.abin.mallchat.common.user.domain.vo.response.user.UserInfoResp;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.abin.mallchat.common.common.constant.RedisKey.USER_CHAT_CONTEXT;
import static com.abin.mallchat.common.common.service.frequencycontrol.FrequencyControlStrategyFactory.TOTAL_COUNT_WITH_IN_FIX_TIME_FREQUENCY_CONTROLLER;

@Slf4j
@Component
public class GPTChatAIHandler extends AbstractChatAIHandler {
    /**
     * GPTChatAIHandler限流前缀
     */
    private static final String CHAT_FREQUENCY_PREFIX = "GPTChatAIHandler";

    @Autowired
    private ChatGPTProperties chatGPTProperties;

    private static String AI_NAME;

    @Override
    protected void init() {
        super.init();
        if (isUse()) {
            UserInfoResp userInfo = userService.getUserInfo(chatGPTProperties.getAIUserId());
            if (userInfo == null) {
                log.error("根据AIUserId:{} 找不到用户信息", chatGPTProperties.getAIUserId());
                throw new RuntimeException("根据AIUserId: " + chatGPTProperties.getAIUserId() + " 找不到用户信息");
            }
            if (StringUtils.isBlank(userInfo.getName())) {
                log.warn("根据AIUserId:{} 找到的用户信息没有name", chatGPTProperties.getAIUserId());
                throw new RuntimeException("根据AIUserId: " + chatGPTProperties.getAIUserId() + " 找到的用户没有名字");
            }
            AI_NAME = userInfo.getName();
        }
    }

    @Override
    protected boolean isUse() {
        return chatGPTProperties.isUse();
    }

    @Override
    public Long getChatAIUserId() {
        return chatGPTProperties.getAIUserId();
    }

    @Override
    protected String doChat(Message message) {
        Long uid = message.getFromUid();
        try {
            FrequencyControlDTO frequencyControlDTO = new FrequencyControlDTO();
            frequencyControlDTO.setKey(RedisKey.getKey(CHAT_FREQUENCY_PREFIX) + ":" + uid);
            frequencyControlDTO.setUnit(TimeUnit.HOURS);
            frequencyControlDTO.setCount(chatGPTProperties.getLimit());
            frequencyControlDTO.setTime(1);
            return FrequencyControlUtil.executeWithFrequencyControl(TOTAL_COUNT_WITH_IN_FIX_TIME_FREQUENCY_CONTROLLER,
                    frequencyControlDTO, // 限流参数
                    () -> sendRequestToGPT(message));
        } catch (FrequencyControlException e) {
            return "亲爱的,你今天找我聊了" + chatGPTProperties.getLimit() + "次了~人家累了~明天见";
        } catch (Throwable e) {
            return "系统开小差啦~~";
        }
    }


    private String sendRequestToGPT(Message message) {
        ChatGPTContext context = buildContext(message);// 构建上下文
        context = tailorContext(context);// 裁剪上下文
        log.info("context = {}", context);
        String text;
        try {
            Response response = ChatGPTUtils.create(chatGPTProperties.getKey())
                    .proxyUrl(chatGPTProperties.getProxyUrl())
                    .model(chatGPTProperties.getModelName())
                    .timeout(chatGPTProperties.getTimeout())
                    .maxTokens(chatGPTProperties.getMaxTokens())
                    .message(context.getMsg())
                    .send();
            text = ChatGPTUtils.parseText(response);
            ChatGPTMsg chatGPTMsg = ChatGPTMsgBuilder.assistantMsg(text);
            context.addMsg(chatGPTMsg);
            saveContext(context);
        } catch (Exception e) {
            log.warn("gpt doChat warn:", e);
            text = "我累了，明天再聊吧";
        }
        return text;
    }

    private ChatGPTContext tailorContext(ChatGPTContext context) {
        List<ChatGPTMsg> msg = context.getMsg();
        Integer integer = ChatGPTUtils.countTokens(msg);
        if (integer < (chatGPTProperties.getMaxTokens() - 500)) { // 用户的输入+ChatGPT的回答内容都会计算token 留500个token给ChatGPT回答
            return context;
        }
        msg.remove(1);
        return tailorContext(context);
    }

    private ChatGPTContext buildContext(Message message) {
        String prompt = message.getContent().replace("@" + AI_NAME, "").trim();
        Long uid = message.getFromUid();
        Long roomId = message.getRoomId();
        ChatGPTContext chatGPTContext = RedisUtils.get(RedisKey.getKey(USER_CHAT_CONTEXT, uid, roomId), ChatGPTContext.class);
        if (chatGPTContext == null) {
            chatGPTContext = ChatGPTContextBuilder.initContext(uid, roomId);
        }
        saveContext(chatGPTContext);
        chatGPTContext.addMsg(ChatGPTMsgBuilder.userMsg(prompt));
        return chatGPTContext;
    }

    private void saveContext(ChatGPTContext chatGPTContext) {
        RedisUtils.set(RedisKey.getKey(USER_CHAT_CONTEXT, chatGPTContext.getUid(), chatGPTContext.getRoomId()), chatGPTContext, 5L, TimeUnit.MINUTES);
    }


    private Long userChatNumInrc(Long uid) {
        return RedisUtils.inc(RedisKey.getKey(RedisKey.USER_CHAT_NUM, uid), DateUtils.getEndTimeByToday().intValue(), TimeUnit.MILLISECONDS);
    }

    private Long getUserChatNum(Long uid) {
        Long num = RedisUtils.get(RedisKey.getKey(RedisKey.USER_CHAT_NUM, uid), Long.class);
        return num == null ? 0 : num;

    }


    @Override
    protected boolean supports(Message message) {
        // 检查消息扩展信息
        MessageExtra extra = message.getExtra();
        if (extra == null || CollectionUtils.isEmpty(extra.getAtUidList())) {
            log.debug("消息不包含@用户列表，跳过处理，消息ID={}", message.getId());
            return false;
        }

        // 检查是否@了AI用户
        if (!extra.getAtUidList().contains(chatGPTProperties.getAIUserId())) {
            log.debug("消息没有@AI用户，跳过处理，消息ID={}", message.getId());
            return false;
        }

        // 检查消息内容
        if (StringUtils.isBlank(message.getContent())) {
            log.debug("消息内容为空，跳过处理，消息ID={}", message.getId());
            return false;
        }

        // 处理消息内容中的特殊字符，并替换@AI_NAME
        String content = message.getContent()
            .replace('\u00a0', ' ')  // 替换不间断空格为普通空格
            .replace('\u3000', ' ')  // 替换全角空格
            .replace("@" + AI_NAME, "") // 替换@AI_NAME
            .trim();

        boolean result = StringUtils.contains(message.getContent(), "@" + AI_NAME)
            && StringUtils.isNotBlank(content);

        log.debug("消息ID={}，内容={}，Unicode={}，是否支持AI回复={}",
            message.getId(),
            message.getContent(),
            message.getContent().codePoints().mapToObj(c -> String.format("\\u%04x", c)).collect(java.util.stream.Collectors.joining()),
            result);

        return result;
    }
}
