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
docker run \
          -d \
          --name rocketmq-broker \
          --net rocketmq-net \
          -p 10911:10911 -p 10909:10909 \
          -v ~/DockerData/rocketmq/broker/data:/data \
          -v ~/DockerData/rocketmq/broker/logs:/logs \
          -v ~/DockerData/rocketmq/broker/conf/broker.conf:/home/rocketmq/store/config/broker.conf \
          -e "JAVA_OPT_EXT=-Drocketmq.broker.accessKey=root -Drocketmq.broker.secretKey=123456" \
          apache/rocketmq:latest \
          sh mqbroker -c /home/rocketmq/store/config/broker.conf

```

