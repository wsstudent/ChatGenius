package com.abin.mallchat.common.chat.service.adapter;

import com.abin.mallchat.common.chat.domain.entity.Contact;
import com.abin.mallchat.common.chat.domain.entity.Room;
import com.abin.mallchat.common.chat.domain.entity.RoomFriend;
import com.abin.mallchat.common.chat.domain.entity.RoomGroup;
import com.abin.mallchat.common.chat.domain.enums.HotFlagEnum;
import com.abin.mallchat.common.chat.domain.enums.RoomTypeEnum;
import com.abin.mallchat.common.common.domain.enums.NormalOrNoEnum;
import com.abin.mallchat.common.user.domain.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天基础结构适配器
 * 主要负责基础聊天实体(房间、群组、好友关系)的创建和转换
 * 处理底层实体创建及关联关系的建立
 */
public class ChatAdapter {
    public static final String SEPARATOR = ",";

    /**
     * 生成好友聊天室的唯一标识键
     * 将用户ID按升序排列后用逗号连接，确保同一组用户生成相同的key
     *
     * @param uidList 用户ID列表
     * @return 唯一标识键
     */
    public static String generateRoomKey(List<Long> uidList) {
        return uidList.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(SEPARATOR));
    }

    /**
     * 构建基础聊天室实体
     *
     * @param typeEnum 房间类型枚举
     * @return 聊天室实体
     */
    public static Room buildRoom(RoomTypeEnum typeEnum) {
        Room room = new Room();
        room.setType(typeEnum.getType());
        room.setHotFlag(HotFlagEnum.NOT.getType());
        return room;
    }

    /**
     * 构建好友聊天关系实体
     * 将两个用户关联到同一个聊天室
     *
     * @param roomId 房间ID
     * @param uidList 两个用户的ID列表
     * @return 好友聊天关系实体
     */
    public static RoomFriend buildFriendRoom(Long roomId, List<Long> uidList) {
        List<Long> collect = uidList.stream().sorted().collect(Collectors.toList());
        RoomFriend roomFriend = new RoomFriend();
        roomFriend.setRoomId(roomId);
        roomFriend.setUid1(collect.get(0));
        roomFriend.setUid2(collect.get(1));
        roomFriend.setRoomKey(generateRoomKey(uidList));
        roomFriend.setStatus(NormalOrNoEnum.NORMAL.getStatus());
        return roomFriend;
    }

    /**
     * 构建群聊室实体
     * 设置默认的群名称和头像，基于创建者的信息
     *
     * @param user 创建者用户信息
     * @param roomId 聊天室ID
     * @return 群聊实体
     */
    public static RoomGroup buildGroupRoom(User user, Long roomId) {
        RoomGroup roomGroup = new RoomGroup();
        roomGroup.setName(user.getName() + "的群组");  // 设置默认群名为：创建者名称+"的群组"
        roomGroup.setAvatar(user.getAvatar());  // 设置默认群头像为创建者头像
        roomGroup.setRoomId(roomId);
        return roomGroup;
    }

    /**
     * 构建用户与聊天室的会话实体
     *
     * @param uid 用户ID
     * @param roomId 房间ID
     * @return 会话实体
     */
    public static Contact buildContact(Long uid, Long roomId) {
        Contact contact = new Contact();
        contact.setRoomId(roomId);
        contact.setUid(uid);
        return contact;
    }

    /**
     * 从多个好友聊天关系中获取与指定用户相关的所有好友ID
     *
     * @param values 好友聊天关系集合
     * @param uid 指定用户ID
     * @return 好友ID集合
     */
    public static Set<Long> getFriendUidSet(Collection<RoomFriend> values, Long uid) {
        return values.stream()
                .map(a -> getFriendUid(a, uid))
                .collect(Collectors.toSet());
    }

    /**
     * 从好友聊天关系中获取与指定用户对应的好友ID
     *
     * @param roomFriend 好友聊天关系
     * @param uid 指定用户ID
     * @return 好友ID
     */
    public static Long getFriendUid(RoomFriend roomFriend, Long uid) {
        return Objects.equals(uid, roomFriend.getUid1()) ? roomFriend.getUid2() : roomFriend.getUid1();
    }


}