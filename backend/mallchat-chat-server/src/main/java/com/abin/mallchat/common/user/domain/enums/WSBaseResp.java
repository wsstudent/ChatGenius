package com.abin.mallchat.common.user.domain.enums;

import lombok.Data;

/**
 * Description: ws的基本返回信息体
 * Author: <a href="https://github.com/zongzibinbin">abin</a>
 * Date: 2023-03-19
 */
@Data
public class WSBaseResp<T> {
    /**
     * ws推送给前端的消息
     *
     * @see WSRespTypeEnum
     */
    private Integer type;
    private T data;
    private Integer code;
    private String msg;

    /**
     * 构建错误响应
     * @param code 错误码
     * @param message 错误信息
     * @return 错误响应
     */
    public static WSBaseResp<Void> build(int code, String message) {
        WSBaseResp<Void> wsBaseResp = new WSBaseResp<>();
        wsBaseResp.setCode(code);
        wsBaseResp.setMsg(message);
        return wsBaseResp;
    }
}