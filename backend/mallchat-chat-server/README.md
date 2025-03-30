# MallChat系统架构文档

## 1. 项目概述

MallChat是一个基于Spring Boot和Netty开发的实时聊天应用，集成了多种现代化功能，包括用户管理、消息通信、群组聊天、AI对话、微信集成等。系统采用分层架构设计，模块化组织代码，便于维护和扩展。

## 2. 系统架构

项目采用微服务架构思想，按功能领域划分为多个核心模块：

### 2.1 核心模块

- **chat**: 聊天核心功能模块
- **chatai**: AI对话集成模块
- **common**: 通用工具和配置模块
- **sensitive**: 敏感词过滤模块
- **user**: 用户管理模块
- **websocket**: WebSocket通信模块

## 3. 模块详解

### 3.1 Chat模块

负责聊天相关的核心业务逻辑，包括消息发送、接收、房间管理等。

```
chat/
├── constant      # 常量定义
├── consumer      # 消息队列消费者
├── controller    # REST API控制器
├── dao           # 数据访问层
├── domain        # 领域模型
├── mapper        # MyBatis映射器
└── service       # 业务逻辑层
```

**核心功能**:
- 一对一聊天
- 群组聊天
- 消息管理
- 联系人管理

### 3.2 ChatAI模块

整合AI对话能力，支持GPT和ChatGLM等多种AI模型。

```
chatai/
├── domain        # AI对话领域模型
├── dto           # 数据传输对象
├── enums         # 枚举定义
├── handler       # AI处理器
├── properties    # 配置属性
├── service       # 服务层
└── utils         # 工具类
```

**核心功能**:
- OpenAI GPT集成
- ChatGLM2集成
- AI模型管理

### 3.3 Common模块

提供全局共享的工具、配置和基础设施代码。

```
common/
├── algorithm     # 算法实现
├── annotation    # 自定义注解
├── aspect        # AOP切面
├── config        # 配置类
├── constant      # 常量定义
├── domain        # 通用领域模型
├── event         # 事件机制
├── exception     # 异常处理
├── factory       # 工厂类
├── handler       # 处理器
├── intecepter    # 拦截器
├── service       # 通用服务
└── utils         # 工具类
```

**核心功能**:
- JWT认证
- 缓存配置
- 事件总线
- 异常处理
- 拦截器
- Redis工具
- 密码加密

### 3.4 Sensitive模块

处理敏感词过滤和内容安全。

```
sensitive/
├── dao           # 数据访问层
├── domain        # 敏感词领域模型
├── mapper        # MyBatis映射器
└── MyWordFactory # 敏感词工厂
```

**核心功能**:
- 敏感词过滤
- 内容安全检查

### 3.5 User模块

管理用户相关功能，包括登录、注册、好友关系等。

```
user/
├── consumer      # 消息队列消费者
├── controller    # REST API控制器
├── dao           # 数据访问层
├── domain        # 用户领域模型
├── mapper        # MyBatis映射器
└── service       # 用户服务层
```

**核心功能**:
- 用户认证
- 微信扫码登录
- 好友管理
- 用户资源管理
- 角色权限管理

### 3.6 WebSocket模块

基于Netty实现的WebSocket服务，处理实时通信。

```
websocket/
├── HttpHeadersHandler.java        # HTTP头处理器
├── NettyUtil.java                 # Netty工具类
├── NettyWebSocketServerHandler.java # WebSocket处理器
└── NettyWebSocketServer.java      # WebSocket服务器
```

**核心功能**:
- 建立WebSocket连接
- 消息实时推送
- 连接状态管理

## 4. 技术栈

### 4.1 后端
- **语言**: Java
- **框架**: Spring Boot, Spring MVC, Spring Security
- **ORM**: MyBatis-Plus
- **通信**: Netty WebSocket
- **缓存**: Redis
- **消息队列**: RabbitMQ 
- **安全**: JWT认证

### 4.2 前端
- **框架**: Vue.js
- **构建工具**: npm
- **语言**: TypeScript, JavaScript

## 5. 核心功能流程

### 5.1 登录流程

目前系统使用微信扫码登录机制：
1. 前端通过WebSocket请求登录二维码
2. 后端生成二维码返回给前端
3. 用户通过微信扫描二维码
4. 微信服务器回调系统，通知扫码结果
5. 系统生成JWT token并通过WebSocket通知前端登录成功

### 5.2 消息处理流程

1. 用户发送消息通过WebSocket发送到服务端
2. 服务端接收消息并进行处理（敏感词过滤、格式验证等）
3. 消息存储到数据库
4. 消息通过WebSocket实时推送给目标用户

### 5.3 AI对话流程

1. 用户发送消息到AI聊天机器人
2. 系统识别为AI对话请求
3. 根据配置选择合适的AI模型处理器
4. 调用AI服务获取回复
5. 将AI回复推送给用户

## 6. 未来改进

### 6.1 密码登录功能

计划添加基于用户名密码的登录功能，实现步骤：
1. 用户表添加username和password字段
2. 实现密码加密存储（使用BCrypt）
3. 创建密码登录API接口
4. 复用现有JWT认证机制

### 6.2 其他可能的改进方向

- 性能优化和缓存策略
- 消息加密传输
- 更多AI模型的集成
- 多端同步机制完善

## 7. 总结

MallChat是一个功能完善的实时聊天应用，技术架构现代，组织结构清晰。系统以用户为中心，提供了完善的聊天、社交和AI交互能力，并具备良好的可扩展性。当前系统使用微信扫码登录，但计划添加用户名密码登录功能以增强系统的多样性和可用性。