package com.abin.mallchat.common.user.service;

import com.abin.mallchat.common.user.domain.enums.RoleEnum;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author <a href="https://github.com/zongzibinbin">abin</a>
 * @since 2023-06-04
 */
public interface IRoleService {

    /**
     * 是否有某个权限
     *
     * @return
     */
    boolean hasPower(Long uid, RoleEnum roleEnum);

    /**
     * 获取用户最高级别权限角色
     *
     * @param uid 用户ID
     * @return 角色枚举，如果没有特殊权限返回null
     */
    RoleEnum getHighestRole(Long uid);
}