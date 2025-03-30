package com.abin.mallchat.common.user.service.impl;

import com.abin.mallchat.common.common.utils.PasswordEncoder;
import com.abin.mallchat.common.user.dao.UserDao;
import com.abin.mallchat.common.user.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class UserInitializer implements CommandLineRunner {

    @Autowired
    private UserDao userDao;

    @Override
    public void run(String... args) {
        log.info("检查管理员账户是否存在...");

        // 使用修复后的方法检查管理员是否已存在
        if (userDao.getByUsername("admin") == null) {
            log.info("开始创建管理员账户...");

            // 创建管理员用户
            User admin = User.builder()
                    .username("admin")
                    .password(PasswordEncoder.encode("123456"))
                    .name("管理员")
                    .avatar("https://mallchat.oss-cn-beijing.aliyuncs.com/avatar/admin.jpg")
                    .openId("admin_system")  // 为管理员设置唯一的openId
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
            } else {
                log.error("管理员账户创建失败");
            }
        } else {
            log.info("管理员账户已存在，跳过创建");
        }
    }
}