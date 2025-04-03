package com.abin.mallchat.common.user.service.impl;

import com.abin.mallchat.common.common.algorithm.sensitiveWord.SensitiveWordBs;
import com.abin.mallchat.common.common.exception.BusinessException;
import com.abin.mallchat.common.common.utils.AssertUtil;
import com.abin.mallchat.common.user.dao.UserDao;
import com.abin.mallchat.common.user.domain.entity.User;
import com.abin.mallchat.common.user.domain.vo.request.user.AddUserReq;
import com.abin.mallchat.common.user.domain.vo.request.user.DeleteUserReq;
import com.abin.mallchat.common.user.domain.vo.request.user.UpdateUserReq;
import com.abin.mallchat.common.user.domain.vo.response.user.UserInfoResp;
import com.abin.mallchat.common.user.service.UserAdminService;
import com.abin.mallchat.common.user.service.adapter.UserAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.abin.mallchat.common.common.utils.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserAdminServiceImpl implements UserAdminService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private SensitiveWordBs sensitiveWordBs;



    @Override
    public List<UserInfoResp> listUsers() {
        List<User> users = userDao.listUsers();
        return users.stream()
                .map(user -> UserAdapter.buildUserInfoResp(user, 0))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addUser(AddUserReq req) {
        // 验证用户名是否重复
        String username = req.getUsername();
        String name = req.getName();

        // 敏感词检查
        AssertUtil.isFalse(sensitiveWordBs.hasSensitiveWord(name), "昵称中包含敏感词");

        User existUser = userDao.getByName(name);
        AssertUtil.isEmpty(existUser, "昵称已被使用");

        existUser = userDao.getByUsername(username);
        AssertUtil.isEmpty(existUser, "用户名已被使用");

        // 创建新用户
        User user = User.builder()
                .username(username)
                .password(PasswordEncoder.encode(req.getPassword()))
                .name(name)
                .avatar(req.getAvatar())
                .sex(req.getSex())
                .activeStatus(0) // 默认不活跃状态
                .status(0) // 默认正常状态
                .createTime(new Date())
                .updateTime(new Date())
                .openId("admin_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .build();

        userDao.save(user);
    }

    @Override
    @Transactional
    public void updateUser(UpdateUserReq req) {
        User user = userDao.getById(req.getId());
        AssertUtil.isNotEmpty(user, "用户不存在");

        // 敏感词检查
        AssertUtil.isFalse(sensitiveWordBs.hasSensitiveWord(req.getName()), "昵称中包含敏感词");

        // 检查昵称是否被其他用户使用
        User existUser = userDao.getByName(req.getName());
        if (existUser != null && !existUser.getId().equals(req.getId())) {
            throw new BusinessException("昵称已被使用");
        }

        // 更新用户信息
        user.setName(req.getName());
        user.setAvatar(req.getAvatar());
        user.setSex(req.getSex());
        user.setUpdateTime(new Date());

        userDao.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(DeleteUserReq req) {
        User user = userDao.getById(req.getId());
        AssertUtil.isNotEmpty(user, "用户不存在");

        // 不允许删除系统用户
        AssertUtil.isFalse(User.UID_SYSTEM.equals(user.getId()), "系统用户不允许删除");

        // 物理删除用户
        userDao.removeById(req.getId());
    }
}