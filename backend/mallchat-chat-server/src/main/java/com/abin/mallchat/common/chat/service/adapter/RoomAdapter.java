package com.abin.mallchat.common.chat.service.adapter;

import cn.hutool.core.bean.BeanUtil;
import com.abin.mallchat.common.chat.domain.entity.Contact;
import com.abin.mallchat.common.chat.domain.entity.GroupMember;
import com.abin.mallchat.common.chat.domain.entity.Room;
import com.abin.mallchat.common.chat.domain.entity.RoomGroup;
import com.abin.mallchat.common.chat.domain.enums.GroupRoleEnum;
import com.abin.mallchat.common.chat.domain.enums.MessageTypeEnum;
import com.abin.mallchat.common.chat.domain.vo.request.ChatMessageReq;
import com.abin.mallchat.common.chat.domain.vo.response.ChatMessageReadResp;
import com.abin.mallchat.common.chat.domain.vo.response.ChatRoomResp;
import com.abin.mallchat.common.user.domain.entity.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 房间适配器
 * 负责聊天室相关的数据转换、消息构建和成员管理
 * 处理UI展示层面的转换和业务逻辑相关的操作
 */
public class RoomAdapter {

    /**
     * 构建房间响应对象列表
     * 将房间实体转换为前端展示需要的响应对象
     *
     * @param list 房间实体列表
     * @return 房间响应对象列表
     */
    public static List<ChatRoomResp> buildResp(List<Room> list) {
        return list.stream()
                .map(a -> {
                    ChatRoomResp resp = new ChatRoomResp();
                    BeanUtil.copyProperties(a, resp);
                    resp.setActiveTime(a.getActiveTime());
                    return resp;
                }).collect(Collectors.toList());
    }

    /**
     * 构建消息已读响应对象列表
     * 从联系人实体中提取已读信息
     *
     * @param list 联系人实体列表
     * @return 消息已读响应对象列表
     */
    public static List<ChatMessageReadResp> buildReadResp(List<Contact> list) {
        return list.stream().map(contact -> {
            ChatMessageReadResp resp = new ChatMessageReadResp();
            resp.setUid(contact.getUid());
            return resp;
        }).collect(Collectors.toList());
    }

    /**
     * 批量构建群成员对象
     * 为新加入群组的用户创建成员记录
     *
     * @param uidList 用户ID列表
     * @param groupId 群组ID
     * @return 群成员对象列表
     */
    public static List<GroupMember> buildGroupMemberBatch(List<Long> uidList, Long groupId) {
        return uidList.stream()
                .distinct()
                .map(uid -> {
                    GroupMember member = new GroupMember();
                    member.setRole(GroupRoleEnum.MEMBER.getType());
                    member.setUid(uid);
                    member.setGroupId(groupId);
                    return member;
                }).collect(Collectors.toList());
    }

    /**
     * 构建群成员添加的系统消息
     * 当新成员被邀请加入群聊时，创建一条系统通知消息
     *
     * @param groupRoom 群聊房间信息
     * @param inviter 邀请人用户信息
     * @param member 被邀请成员的用户信息映射
     * @return 聊天消息请求对象
     */
    public static ChatMessageReq buildGroupAddMessage(RoomGroup groupRoom, User inviter, Map<Long, User> member) {
        ChatMessageReq chatMessageReq = new ChatMessageReq();
        chatMessageReq.setRoomId(groupRoom.getRoomId());
        chatMessageReq.setMsgType(MessageTypeEnum.SYSTEM.getType());
        StringBuilder sb = new StringBuilder();
        sb.append("\"")
                .append(inviter.getName())
                .append("\"")
                .append("邀请")
                .append(member.values().stream().map(u -> "\"" + u.getName() + "\"").collect(Collectors.joining(",")))
                .append("加入群聊");
        chatMessageReq.setBody(sb.toString());
        return chatMessageReq;
    }
}