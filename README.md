# docker

## Minio

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
## Minio桶创建
登录 http://localhost:9001/login
输入 用户名和密码  

```
admin
```
```
79orSY%Uvy|M
```
创建一个桶，命名为 `default`，并设置为custom桶。

## redis
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

## rocketmq

```bash
# 创建配置文件
touch ~/DockerData/rocketmq/broker/conf/broker.conf

``` 
编辑broker.conf，填入如下内容
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

## RocketMQ主题创建

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

