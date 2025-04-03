package com.abin.mallchat.common.user.service.impl;

import com.abin.mallchat.common.common.utils.PasswordEncoder;
import com.abin.mallchat.common.user.dao.UserDao;
import com.abin.mallchat.common.user.dao.UserRoleDao;
import com.abin.mallchat.common.user.domain.entity.User;
import com.abin.mallchat.common.user.domain.entity.UserRole;
import com.abin.mallchat.common.user.domain.enums.RoleEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class UserInitializer implements CommandLineRunner {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserRoleDao userRoleDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        log.info("检查管理员账户是否存在...");

        // 检查管理员是否已存在
        User adminUser = userDao.getByUsername("admin");

        if (adminUser == null) {
            log.info("开始创建管理员账户...");

            // 创建管理员用户
            User admin = User.builder()
                    .username("admin")
                    .password(PasswordEncoder.encode("123456"))
                    .name("管理员")
                    .avatar("https://mallchat.oss-cn-beijing.aliyuncs.com/avatar/admin.jpg")
                    .openId("admin_system")
                    .sex(1)
                    .activeStatus(1)
                    .lastOptTime(new Date())
                    .status(0)
                    .createTime(new Date())
                    .updateTime(new Date())
                    .build();

            boolean success = userDao.save(admin);
            if (success) {
                log.info("管理员账户创建成功，ID: {}", admin.getId());

                // 为管理员分配ADMIN角色
                UserRole userRole = new UserRole();
                userRole.setUid(admin.getId());
                userRole.setRoleId(RoleEnum.ADMIN.getId());
                userRole.setCreateTime(new Date());
                userRole.setUpdateTime(new Date());

                boolean roleSuccess = userRoleDao.save(userRole);
                if (roleSuccess) {
                    log.info("管理员角色分配成功");
                } else {
                    log.error("管理员角色分配失败");
                }
            } else {
                log.error("管理员账户创建失败");
            }
        } else {
            log.info("管理员账户已存在，检查角色分配");

            // 检查管理员是否已有ADMIN角色
            List<UserRole> roles = userRoleDao.listByUid(adminUser.getId());
            boolean hasAdminRole = roles.stream()
                    .anyMatch(role -> role.getRoleId().equals(RoleEnum.ADMIN.getId()));

            if (!hasAdminRole) {
                log.info("为已存在的管理员分配ADMIN角色");
                // 分配ADMIN角色
                UserRole userRole = new UserRole();
                userRole.setUid(adminUser.getId());
                userRole.setRoleId(RoleEnum.ADMIN.getId());
                userRole.setCreateTime(new Date());
                userRole.setUpdateTime(new Date());

                boolean roleSuccess = userRoleDao.save(userRole);
                if (roleSuccess) {
                    log.info("管理员角色分配成功");
                } else {
                    log.error("管理员角色分配失败");
                }
            } else {
                log.info("管理员已有ADMIN角色，无需分配");
            }
        }
    }
}