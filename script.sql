create table black
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    type        int                                      not null comment '拉黑目标类型 1.ip 2uid',
    target      varchar(32)                              not null comment '拉黑目标',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间',
    constraint idx_type_target
        unique (type, target)
)
    comment '黑名单' row_format = DYNAMIC;

create table contact
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    uid         bigint                                   not null comment 'uid',
    room_id     bigint                                   not null comment '房间id',
    read_time   datetime(3) default CURRENT_TIMESTAMP(3) not null comment '阅读到的时间',
    active_time datetime(3)                              null comment '会话内消息最后更新的时间(只有普通会话需要维护，全员会话不需要维护)',
    last_msg_id bigint                                   null comment '会话最新消息id',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间',
    constraint uniq_uid_room_id
        unique (uid, room_id)
)
    comment '会话列表';

create index idx_create_time
    on contact (create_time);

create index idx_room_id_read_time
    on contact (room_id, read_time);

create index idx_update_time
    on contact (update_time);

create table group_member
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    group_id    bigint                                   not null comment '群组id',
    uid         bigint                                   not null comment '成员uid',
    role        int                                      not null comment '成员角色 1群主 2管理员 3普通成员',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '群成员表';

create index idx_create_time
    on group_member (create_time);

create index idx_group_id_role
    on group_member (group_id, role);

create index idx_update_time
    on group_member (update_time);

create table item_config
(
    id          bigint unsigned                          not null comment 'id'
        primary key,
    type        int                                      not null comment '物品类型 1改名卡 2徽章',
    img         varchar(255)                             null comment '物品图片',
    `describe`  varchar(255)                             null comment '物品功能描述',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '功能物品配置表' row_format = DYNAMIC;

create index idx_create_time
    on item_config (create_time);

create index idx_update_time
    on item_config (update_time);

create table message
(
    id           bigint unsigned auto_increment comment 'id'
        primary key,
    room_id      bigint                                   not null comment '会话表id',
    from_uid     bigint                                   not null comment '消息发送者uid',
    content      varchar(1024)                            null comment '消息内容',
    reply_msg_id bigint                                   null comment '回复的消息内容',
    status       int                                      not null comment '消息状态 0正常 1删除',
    gap_count    int                                      null comment '与回复的消息间隔多少条',
    type         int         default 1                    null comment '消息类型 1正常文本 2.撤回消息',
    extra        json                                     null comment '扩展信息',
    create_time  datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time  datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '消息表' row_format = DYNAMIC;

create index idx_create_time
    on message (create_time);

create index idx_from_uid
    on message (from_uid);

create index idx_room_id
    on message (room_id);

create index idx_update_time
    on message (update_time);

create table message_mark
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    msg_id      bigint                                   not null comment '消息表id',
    uid         bigint                                   not null comment '标记人uid',
    type        int                                      not null comment '标记类型 1点赞 2举报',
    status      int                                      not null comment '消息状态 0正常 1取消',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '消息标记表' row_format = DYNAMIC;

create index idx_create_time
    on message_mark (create_time);

create index idx_msg_id
    on message_mark (msg_id);

create index idx_uid
    on message_mark (uid);

create index idx_update_time
    on message_mark (update_time);

create table role
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    name        varchar(64)                              not null comment '角色名称',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '角色表';

create index idx_create_time
    on role (create_time);

create index idx_update_time
    on role (update_time);

create table room
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    type        int                                      not null comment '房间类型 1群聊 2单聊',
    hot_flag    int         default 0                    null comment '是否全员展示 0否 1是',
    active_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '群最后消息的更新时间（热点群不需要写扩散，只更新这里）',
    last_msg_id bigint                                   null comment '会话中的最后一条消息id',
    ext_json    json                                     null comment '额外信息（根据不同类型房间有不同存储的东西）',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '房间表';

create index idx_create_time
    on room (create_time);

create index idx_update_time
    on room (update_time);

create table room_friend
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    room_id     bigint                                   not null comment '房间id',
    uid1        bigint                                   not null comment 'uid1（更小的uid）',
    uid2        bigint                                   not null comment 'uid2（更大的uid）',
    room_key    varchar(64)                              not null comment '房间key由两个uid拼接，先做排序uid1_uid2',
    status      int                                      not null comment '房间状态 0正常 1禁用(删好友了禁用)',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间',
    constraint room_key
        unique (room_key)
)
    comment '单聊房间表';

create index idx_create_time
    on room_friend (create_time);

create index idx_room_id
    on room_friend (room_id);

create index idx_update_time
    on room_friend (update_time);

create table room_group
(
    id            bigint unsigned auto_increment comment 'id'
        primary key,
    room_id       bigint                                   not null comment '房间id',
    name          varchar(16)                              not null comment '群名称',
    avatar        varchar(256)                             not null comment '群头像',
    ext_json      json                                     null comment '额外信息（根据不同类型房间有不同存储的东西）',
    delete_status int         default 0                    not null comment '逻辑删除(0-正常,1-删除)',
    create_time   datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time   datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '群聊房间表';

create index idx_create_time
    on room_group (create_time);

create index idx_room_id
    on room_group (room_id);

create index idx_update_time
    on room_group (update_time);

create table secure_invoke_record
(
    id                 bigint unsigned auto_increment comment 'id'
        primary key,
    secure_invoke_json json                                     not null comment '请求快照参数json',
    status             tinyint                                  not null comment '状态 1待执行 2已失败',
    next_retry_time    datetime(3)                              not null comment '下一次重试的时间',
    retry_times        int                                      not null comment '已经重试的次数',
    max_retry_times    int                                      not null comment '最大重试次数',
    fail_reason        text                                     null comment '执行失败的堆栈',
    create_time        datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time        datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '本地消息表' charset = utf8mb4;

create index idx_next_retry_time
    on secure_invoke_record (next_retry_time);

create table sensitive_word
(
    word varchar(255) not null comment '敏感词'
)
    comment '敏感词库';

create table user
(
    id            bigint unsigned auto_increment comment '用户id'
        primary key,
    name          varchar(20)                              null comment '用户昵称',
    avatar        varchar(255)                             null comment '用户头像',
    sex           int                                      null comment '性别 1为男性，2为女性',
    active_status int         default 2                    null comment '在线状态 1在线 2离线',
    last_opt_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '最后上下线时间',
    ip_info       json                                     null comment 'ip信息',
    item_id       bigint                                   null comment '佩戴的徽章id',
    status        int         default 0                    null comment '使用状态 0.正常 1拉黑',
    create_time   datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time   datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间',
    username      varchar(50)                              null comment '用户名',
    password      varchar(128)                             null comment '密码',
    open_id       varchar(32)                              null comment '微信openid用户标识',
    constraint uniq_name
        unique (name),
    constraint username
        unique (username)
)
    comment '用户表' row_format = DYNAMIC;

create index idx_active_status_last_opt_time
    on user (active_status, last_opt_time);

create index idx_create_time
    on user (create_time);

create index idx_update_time
    on user (update_time);

create table user_apply
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    uid         bigint                                   not null comment '申请人uid',
    type        int                                      not null comment '申请类型 1加好友',
    target_id   bigint                                   not null comment '接收人uid',
    msg         varchar(64)                              not null comment '申请信息',
    status      int                                      not null comment '申请状态 1待审批 2同意',
    read_status int                                      not null comment '阅读状态 1未读 2已读',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '用户申请表';

create index idx_create_time
    on user_apply (create_time);

create index idx_target_id
    on user_apply (target_id);

create index idx_target_id_read_status
    on user_apply (target_id, read_status);

create index idx_uid_target_id
    on user_apply (uid, target_id);

create index idx_update_time
    on user_apply (update_time);

create table user_backpack
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    uid         bigint                                   not null comment 'uid',
    item_id     int                                      not null comment '物品id',
    status      int                                      not null comment '使用状态 0.待使用 1已使用',
    idempotent  varchar(64)                              not null comment '幂等号',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间',
    constraint uniq_idempotent
        unique (idempotent)
)
    comment '用户背包表' row_format = DYNAMIC;

create index idx_create_time
    on user_backpack (create_time);

create index idx_uid
    on user_backpack (uid);

create index idx_update_time
    on user_backpack (update_time);

create table user_emoji
(
    id             bigint unsigned auto_increment comment 'id'
        primary key,
    uid            bigint                                   not null comment '用户表ID',
    expression_url varchar(255)                             not null comment '表情地址',
    delete_status  int         default 0                    not null comment '逻辑删除(0-正常,1-删除)',
    create_time    datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time    datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '用户表情包' row_format = DYNAMIC;

create index IDX_USER_EMOJIS_UID
    on user_emoji (uid);

create table user_friend
(
    id            bigint unsigned auto_increment comment 'id'
        primary key,
    uid           bigint                                   not null comment 'uid',
    friend_uid    bigint                                   not null comment '好友uid',
    delete_status int         default 0                    not null comment '逻辑删除(0-正常,1-删除)',
    create_time   datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time   datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '用户联系人表';

create index idx_create_time
    on user_friend (create_time);

create index idx_uid_friend_uid
    on user_friend (uid, friend_uid);

create index idx_update_time
    on user_friend (update_time);

create table user_role
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    uid         bigint                                   not null comment 'uid',
    role_id     bigint                                   not null comment '角色id',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '用户角色关系表';

create index idx_create_time
    on user_role (create_time);

create index idx_role_id
    on user_role (role_id);

create index idx_uid
    on user_role (uid);

create index idx_update_time
    on user_role (update_time);

create table wx_msg
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    open_id     char(32) collate utf8mb4_unicode_ci      not null comment '微信openid用户标识',
    msg         text                                     not null comment '用户消息',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '修改时间'
)
    comment '微信消息表' collate = utf8mb4_general_ci
                         row_format = DYNAMIC;

create index idx_create_time
    on wx_msg (create_time);

create index idx_open_id
    on wx_msg (open_id);

create index idx_update_time
    on wx_msg (update_time);


