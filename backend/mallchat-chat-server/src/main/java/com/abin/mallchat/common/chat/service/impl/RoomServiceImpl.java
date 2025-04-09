package com.abin.mallchat.common.chat.service.impl;

import com.abin.mallchat.common.chat.dao.GroupMemberDao;
import com.abin.mallchat.common.chat.dao.RoomDao;
import com.abin.mallchat.common.chat.dao.RoomFriendDao;
import com.abin.mallchat.common.chat.dao.RoomGroupDao;
import com.abin.mallchat.common.chat.domain.entity.GroupMember;
import com.abin.mallchat.common.chat.domain.entity.Room;
import com.abin.mallchat.common.chat.domain.entity.RoomFriend;
import com.abin.mallchat.common.chat.domain.entity.RoomGroup;
import com.abin.mallchat.common.chat.domain.enums.GroupRoleEnum;
import com.abin.mallchat.common.chat.domain.enums.RoomTypeEnum;
import com.abin.mallchat.common.chat.service.RoomService;
import com.abin.mallchat.common.chat.service.adapter.ChatAdapter;
import com.abin.mallchat.common.chat.service.cache.RoomGroupCache;
import com.abin.mallchat.common.common.domain.enums.NormalOrNoEnum;
import com.abin.mallchat.common.common.utils.AssertUtil;
import com.abin.mallchat.common.user.domain.entity.User;
import com.abin.mallchat.common.user.service.cache.UserInfoCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 房间服务实现类
 * 负责聊天室基础设施的创建和管理
 * 在底层实现上依赖 ChatAdapter 创建基础聊天实体
 */
@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private RoomFriendDao roomFriendDao;
    @Autowired
    private RoomDao roomDao;
    @Autowired
    private GroupMemberDao groupMemberDao;
    @Autowired
    private UserInfoCache userInfoCache;
    @Autowired
    private RoomGroupDao roomGroupDao;
    @Autowired
    private RoomGroupCache roomGroupCache;

    /**
     * 创建好友聊天房间
     * 如果两个用户之间已经存在聊天房间则恢复，否则创建新的
     *
     * @param uidList 两个用户的ID列表
     * @return 好友聊天关系实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoomFriend createFriendRoom(List<Long> uidList) {
        AssertUtil.isNotEmpty(uidList, "房间创建失败，好友数量不对");
        AssertUtil.equal(uidList.size(), 2, "房间创建失败，好友数量不对");
        String key = ChatAdapter.generateRoomKey(uidList);

        RoomFriend roomFriend = roomFriendDao.getByKey(key);
        if (Objects.nonNull(roomFriend)) { //如果存在房间就恢复，适用于恢复好友场景
            restoreRoomIfNeed(roomFriend);
        } else {//新建房间
            Room room = createRoom(RoomTypeEnum.FRIEND);
            roomFriend = createFriendRoom(room.getId(), uidList);
        }
        return roomFriend;
    }

    /**
     * 获取两个用户之间的好友聊天房间
     *
     * @param uid1 用户1的ID
     * @param uid2 用户2的ID
     * @return 好友聊天关系实体，如不存在则返回null
     */
    @Override
    public RoomFriend getFriendRoom(Long uid1, Long uid2) {
        String key = ChatAdapter.generateRoomKey(Arrays.asList(uid1, uid2));
        return roomFriendDao.getByKey(key);
    }

    /**
     * 禁用好友聊天房间
     * 通常在用户解除好友关系时调用
     *
     * @param uidList 两个用户的ID列表
     */
    @Override
    public void disableFriendRoom(List<Long> uidList) {
        AssertUtil.isNotEmpty(uidList, "房间创建失败，好友数量不对");
        AssertUtil.equal(uidList.size(), 2, "房间创建失败，好友数量不对");
        String key = ChatAdapter.generateRoomKey(uidList);
        roomFriendDao.disableRoom(key);
    }

    /**
     * 创建群聊房间
     * 创建一个新的群组并设置创建者为群主
     * 利用 ChatAdapter.buildGroupRoom 设置默认的群名称和头像
     *
     * @param uid 创建者用户ID
     * @return 新创建的群组实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoomGroup createGroupRoom(Long uid) {
        // 检查用户是否已经创建过自己的群组
        List<GroupMember> selfGroup = groupMemberDao.getSelfGroup(uid);
        AssertUtil.isEmpty(selfGroup, "每个人只能创建一个群");

        // 获取用户信息，用于设置默认群名称和头像
        User user = userInfoCache.get(uid);

        // 创建基础房间
        Room room = createRoom(RoomTypeEnum.GROUP);

        // 使用 ChatAdapter 创建群组实体，设置默认的群名称和头像
        RoomGroup roomGroup = ChatAdapter.buildGroupRoom(user, room.getId());
        roomGroupDao.save(roomGroup);

        // 创建群主角色
        GroupMember leader = GroupMember.builder()
                .role(GroupRoleEnum.LEADER.getType())
                .groupId(roomGroup.getId())
                .uid(uid)
                .build();
        groupMemberDao.save(leader);

        return roomGroup;
    }

    /**
     * 更新群组信息
     * 可用于修改群名称、群头像等信息
     *
     * @param roomGroup 包含更新信息的群组实体
     */
    @Override
    public void updateRoomGroup(RoomGroup roomGroup) {
        roomGroupDao.updateById(roomGroup);
    }

    /**
     * 创建好友聊天关系实体
     * 通过 ChatAdapter 构建并保存到数据库
     *
     * @param roomId 房间ID
     * @param uidList 用户ID列表
     * @return 好友聊天关系实体
     */
    private RoomFriend createFriendRoom(Long roomId, List<Long> uidList) {
        // 使用 ChatAdapter 构建好友关系实体
        RoomFriend insert = ChatAdapter.buildFriendRoom(roomId, uidList);
        roomFriendDao.save(insert);
        return insert;
    }

    /**
     * 创建基础房间实体
     * 通过 ChatAdapter 构建并保存到数据库
     *
     * @param typeEnum 房间类型枚举
     * @return 房间实体
     */
    private Room createRoom(RoomTypeEnum typeEnum) {
        // 使用 ChatAdapter 构建基础房间实体
        Room insert = ChatAdapter.buildRoom(typeEnum);
        roomDao.save(insert);
        return insert;
    }

    /**
     * 如果需要，恢复已禁用的房间
     *
     * @param room 好友聊天关系实体
     */
    private void restoreRoomIfNeed(RoomFriend room) {
        if (Objects.equals(room.getStatus(), NormalOrNoEnum.NOT_NORMAL.getStatus())) {
            roomFriendDao.restoreRoom(room.getId());
        }
    }
}