package com.abin.mallchat.common.user.domain.vo.request.ws;

import lombok.Data;

@Data
public class WSPasswordLoginReq {
    private String username;
    private String password;
}