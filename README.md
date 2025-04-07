# ChatGenius

> 一个现代化的实时聊天应用
>
> 最后更新：2025-04-07
>
> 维护者：wsstudent

## 项目简介

ChatGenius是一个基于Java后端和Vue.js前端构建的实时聊天应用。它提供了丰富的即时通讯功能，支持个人和群组聊天，并具有文件共享、消息持久化等特性。

## 功能特点

- ✅ 使用WebSocket实现实时消息传递
- ✅ 用户认证和会话管理
- ✅ 使用Redis进行消息持久化
- ✅ 基于RocketMQ的可扩展消息队列架构
- ✅ 使用MinIO进行文件共享的对象存储
- ✅ 基于Vue.js构建的现代响应式UI

## 技术栈

- **后端**: Java (68.5%)
- **前端**: Vue.js (12.8%), TypeScript (12.8%), SCSS (4.9%)
- **数据库**: Redis
- **消息队列**: RocketMQ
- **对象存储**: MinIO

## 系统架构

ChatGenius采用微服务架构，包含以下组件：
- 提供UI界面的前端应用
- 处理业务逻辑的后端API服务
- 用于实时通信的WebSocket服务器
- 用于消息队列和事件处理的RocketMQ
- 用于缓存和会话管理的Redis
- 用于对象存储的MinIO

## 项目设置

### 前提条件

- Docker和Docker Compose
- Java 11或更高版本
- Node.js 14或更高版本
- npm或yarn

### 后端设置

1. 克隆仓库
```bash
git clone https://github.com/wsstudent/ChatGenius.git
cd ChatGenius
```

2. 使用Docker设置基础设施组件（见下文）
3. 构建并运行Java应用

### 前端设置

```bash
# 导航到前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器（热重载）
npm run serve

# 构建生产版本
npm run build
```

## 基础设施设置

### Minio

```bash
docker run \
          -d \
          --name minio \
          -p 9000:9000 -p 9001:9001 \
          -v ~/DockerData/minio/data:/data \
          -e "MINIO_ROOT_USER=admin" \
          -e "MINIO_ROOT_PASSWORD=79orSY%Uvy|M" \
          quay.io/minio/minio server /data --console-address ":9001"
```

### Minio桶创建
1. 登录 http://localhost:9001/login
2. 输入用户名和密码：
```
admin
```
```
79orSY%Uvy|M
```
3. 创建一个名为`default`的桶，并设置为custom桶。

### Redis
```bash
docker run \
          -d \
          --name myRedis \
          -p 6379:6379 \
          -e REDIS_PASSWORD=123456 \
          -v ~/DockerData/redis/data:/data \
          -v ~/DockerData/redis/conf/redis.conf:/etc/redis/redis.conf \
          redis
```

### RocketMQ

```bash
# 创建配置文件
touch ~/DockerData/rocketmq/broker/conf/broker.conf
```

编辑broker.conf，填入如下内容：
```
brokerClusterName=DefaultCluster
brokerName=broker-a
brokerId=0
deleteWhen=04
fileReservedTime=48
brokerRole=ASYNC_MASTER
flushDiskType=ASYNC_FLUSH
namesrvAddr=rocketmq-namesrv:9876
brokerIP1=127.0.0.1  # 添加此行，指定broker对外暴露的IP
```

```bash
docker network create rocketmq-net

# 启动 NameServer
docker run \
          -d \
          --name rocketmq-namesrv \
          --net rocketmq-net \
          -p 9876:9876 \
          -v ~/DockerData/rocketmq/namesrv/data:/data \
          -v ~/DockerData/rocketmq/namesrv/logs:/logs \
          apache/rocketmq:latest \
          sh mqnamesrv

# 启动 Broker
docker run -d \
  --name rocketmq-broker \
  --net rocketmq-net \
  -p 10911:10911 -p 10909:10909 -p 10912:10912 \
  -v ~/DockerData/rocketmq/broker/logs:/home/rocketmq/logs \
  -v ~/DockerData/rocketmq/broker/store:/home/rocketmq/store \
  -v ~/DockerData/rocketmq/broker/conf:/home/rocketmq/store/config \
  -e "NAMESRV_ADDR=rocketmq-namesrv:9876" \
  apache/rocketmq:latest \
  sh mqbroker -c /home/rocketmq/store/config/broker.conf
```

### RocketMQ主题创建

部署RocketMQ后，需要手动创建项目所需的主题，否则应用启动时会报错：

```bash
# 进入broker容器
docker exec -it rocketmq-broker bash

# 创建必要的主题
# 1. WebSocket推送主题
sh /home/rocketmq/rocketmq-5.3.2/bin/mqadmin updateTopic -n rocketmq-namesrv:9876 -c DefaultCluster -t websocket_push

# 2. 消息发送主题
sh /home/rocketmq/rocketmq-5.3.2/bin/mqadmin updateTopic -n rocketmq-namesrv:9876 -c DefaultCluster -t chat_send_msg

# 3. 用户扫码发送消息主题
sh /home/rocketmq/rocketmq-5.3.2/bin/mqadmin updateTopic -n rocketmq-namesrv:9876 -c DefaultCluster -t user_scan_send_msg

# 4. 用户登录发送消息主题
sh /home/rocketmq/rocketmq-5.3.2/bin/mqadmin updateTopic -n rocketmq-namesrv:9876 -c DefaultCluster -t user_login_send_msg

# 验证主题是否创建成功
sh /home/rocketmq/rocketmq-5.3.2/bin/mqadmin topicList -n rocketmq-namesrv:9876
```

## 使用说明

1. 启动所有基础设施组件（MinIO, Redis, RocketMQ）
2. 启动后端服务器
3. 启动前端应用
4. 通过 http://localhost:8080 访问应用

## API文档

运行应用时，REST API文档可在 `/api/docs` 路径下访问。

## 项目结构

```
ChatGenius/
├── backend/                # Java后端代码
│   ├── src/main/java/      # Java源代码
│   ├── src/main/resources/ # 配置文件
│   └── pom.xml             # Maven配置
├── frontend/               # Vue.js前端代码
│   ├── src/                # 源代码
│   ├── public/             # 静态资源
│   └── package.json        # npm配置
└── README.md               # 项目说明文档
```

## 开发指南

### 代码规范

- 后端代码遵循Google Java风格指南
- 前端代码遵循Vue风格指南和TypeScript推荐规范
- 提交前请运行代码格式化和lint检查

### 分支管理

- `master`: 主分支，保持稳定可发布状态
- `develop`: 开发分支，集成最新功能
- `feature/*`: 功能分支，开发新特性
- `bugfix/*`: 修复分支，修复问题

## 贡献指南

1. Fork本项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m '添加一些很棒的功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交Pull Request

## 常见问题

**Q: 如何解决RocketMQ连接超时问题？**

A: 检查broker.conf中的brokerIP1配置是否正确，确保它指向您的主机IP而不是容器内部IP。

**Q: 前端连接WebSocket失败怎么办？**

A: 确保后端WebSocket服务正在运行，并检查前端配置中的WebSocket URL是否正确。

## 许可证

本项目采用MIT许可证 - 详情请参阅LICENSE文件。

## 联系方式

如有问题或建议，请通过以下方式联系：

- GitHub Issues: [https://github.com/wsstudent/ChatGenius/issues](https://github.com/wsstudent/ChatGenius/issues)
- 邮箱: wsstudent@example.com

---

*© 2025 ChatGenius. 保留所有权利。*
