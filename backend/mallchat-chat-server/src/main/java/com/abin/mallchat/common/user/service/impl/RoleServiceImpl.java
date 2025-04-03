package com.abin.mallchat.common.user.service.impl;

import com.abin.mallchat.common.user.domain.enums.RoleEnum;
import com.abin.mallchat.common.user.service.IRoleService;
import com.abin.mallchat.common.user.service.cache.UserCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/**
 * Description: 角色管理
 * Author: <a href="https://github.com/zongzibinbin">abin</a>
 * Date: 2023-06-04
 */
@Service
public class RoleServiceImpl implements IRoleService {
    @Autowired
    private UserCache userCache;

    @Override
    public boolean hasPower(Long uid, RoleEnum roleEnum) {//超级管理员无敌的好吧，后期做成权限=》资源模式
        Set<Long> roleSet = userCache.getRoleSet(uid);
        return isAdmin(roleSet) || roleSet.contains(roleEnum.getId());
    }

    @Override
    public RoleEnum getHighestRole(Long uid) {
        // 获取用户的角色ID
        Set<Long> roleSet = userCache.getRoleSet(uid);
        if (roleSet == null || roleSet.isEmpty()) {
            return null;
        }

        // 优先判断是否有超级管理员角色
        if (roleSet.contains(RoleEnum.ADMIN.getId())) {
            return RoleEnum.ADMIN; // 返回超级管理员角色
        }

        // 其次判断是否有群聊管理员角色
        if (roleSet.contains(RoleEnum.CHAT_MANAGER.getId())) {
            return RoleEnum.CHAT_MANAGER; // 返回群聊管理员角色
        }

        // 如果没有特殊权限，返回null
        return null;
    }

    private boolean isAdmin(Set<Long> roleSet) {
        return Objects.requireNonNull(roleSet).contains(RoleEnum.ADMIN.getId());
    }
}