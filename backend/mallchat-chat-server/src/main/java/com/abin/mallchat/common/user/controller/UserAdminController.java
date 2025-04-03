package com.abin.mallchat.common.user.controller;

import com.abin.mallchat.common.common.domain.vo.response.ApiResult;
import com.abin.mallchat.common.common.exception.BusinessErrorEnum;
import com.abin.mallchat.common.common.utils.RequestHolder;
import com.abin.mallchat.common.user.domain.enums.RoleEnum;
import com.abin.mallchat.common.user.domain.vo.request.user.AddUserReq;
import com.abin.mallchat.common.user.domain.vo.request.user.DeleteUserReq;
import com.abin.mallchat.common.user.domain.vo.request.user.UpdateUserReq;
import com.abin.mallchat.common.user.domain.vo.response.user.UserInfoResp;
import com.abin.mallchat.common.user.service.IRoleService;
import com.abin.mallchat.common.user.service.UserAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/capi/user/admin")
@Api(tags = "用户管理相关接口")
@Slf4j
public class UserAdminController {

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private IRoleService roleService;

    @GetMapping("/list")
    @ApiOperation("获取用户列表")
    public ApiResult<List<UserInfoResp>> listUsers() {
        Long uid = RequestHolder.get().getUid();
        // 检查是否为超级管理员
        boolean isAdmin = roleService.hasPower(uid, RoleEnum.ADMIN);
        if (!isAdmin) {
            return ApiResult.fail(BusinessErrorEnum.BUSINESS_ERROR.getErrorCode(), "权限不足");
        }
        return ApiResult.success(userAdminService.listUsers());
    }

    @PostMapping("/add")
    @ApiOperation("添加用户")
    public ApiResult<Void> addUser(@Valid @RequestBody AddUserReq req) {
        Long uid = RequestHolder.get().getUid();
        // 检查是否为超级管理员
        boolean isAdmin = roleService.hasPower(uid, RoleEnum.ADMIN);
        if (!isAdmin) {
            return ApiResult.fail(BusinessErrorEnum.BUSINESS_ERROR.getErrorCode(), "权限不足");
        }
        userAdminService.addUser(req);
        return ApiResult.success();
    }

    @PostMapping("/update")
    @ApiOperation("更新用户")
    public ApiResult<Void> updateUser(@Valid @RequestBody UpdateUserReq req) {
        Long uid = RequestHolder.get().getUid();
        // 检查是否为超级管理员
        boolean isAdmin = roleService.hasPower(uid, RoleEnum.ADMIN);
        if (!isAdmin) {
            return ApiResult.fail(BusinessErrorEnum.BUSINESS_ERROR.getErrorCode(), "权限不足");
        }
        userAdminService.updateUser(req);
        return ApiResult.success();
    }

    @PostMapping("/delete")
    @ApiOperation("删除用户")
    public ApiResult<Void> deleteUser(@Valid @RequestBody DeleteUserReq req) {
        Long uid = RequestHolder.get().getUid();
        // 检查是否为超级管理员
        boolean isAdmin = roleService.hasPower(uid, RoleEnum.ADMIN);
        if (!isAdmin) {
            return ApiResult.fail(BusinessErrorEnum.BUSINESS_ERROR.getErrorCode(), "权限不足");
        }
        userAdminService.deleteUser(req);
        return ApiResult.success();
    }
}