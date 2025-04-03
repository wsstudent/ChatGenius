package com.abin.mallchat.common.user.service;

import com.abin.mallchat.common.user.domain.vo.request.user.AddUserReq;
import com.abin.mallchat.common.user.domain.vo.request.user.DeleteUserReq;
import com.abin.mallchat.common.user.domain.vo.request.user.UpdateUserReq;
import com.abin.mallchat.common.user.domain.vo.response.user.UserInfoResp;

import java.util.List;

public interface UserAdminService {

    /**
     * 获取用户列表
     */
    List<UserInfoResp> listUsers();

    /**
     * 添加用户
     */
    void addUser(AddUserReq req);

    /**
     * 更新用户
     */
    void updateUser(UpdateUserReq req);

    /**
     * 删除用户
     */
    void deleteUser(DeleteUserReq req);
}