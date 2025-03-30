package com.abin.mallchat.common.user.controller;

import com.abin.mallchat.common.common.domain.vo.response.ApiResult;
import com.abin.mallchat.common.user.domain.vo.request.user.PwdLoginReq;
import com.abin.mallchat.common.user.domain.vo.response.user.UserInfoResp;
import com.abin.mallchat.common.user.service.LoginService;
import com.abin.mallchat.common.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/capi/user")
@Api(tags = "用户登录接口")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserService userService;

    @PostMapping("/login/password")
    @ApiOperation("密码登录")
    public ApiResult<UserInfoResp> passwordLogin(@RequestBody @Valid PwdLoginReq req) {
        UserInfoResp userInfo = loginService.loginByPassword(req);
        return ApiResult.success(userInfo);
    }
}