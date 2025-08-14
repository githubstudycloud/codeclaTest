# Redis Docker Compose 简易搭建指南

## 概述

本文档提供了使用Docker Compose快速搭建Redis服务的完整指南，包括单机模式、主从模式、集群模式和哨兵模式的配置方案，适合开发、测试和生产环境使用。

## 目录
1. [基础环境要求](#基础环境要求)
2. [单机Redis部署](#单机redis部署)
3. [Redis主从复制](#redis主从复制)
4. [Redis Sentinel哨兵模式](#redis-sentinel哨兵模式)
5. [Redis集群模式](#redis集群模式)
6. [Redis监控方案](#redis监控方案)
7. [安全配置](#安全配置)
8. [性能优化](#性能优化)
9. [备份与恢复](#备份与恢复)
10. [故障排除](#故障排除)

## 基础环境要求

### 系统要求
- **Docker版本**: 20.10+
- **Docker Compose版本**: 2.0+
- **系统内存**: 最低2GB，推荐4GB+
- **磁盘空间**: 至少10GB可用空间

### 预备知识
- Docker基础命令
- Redis基本概念
- YAML文件格式

## 单机Redis部署

### 1. 基础单机配置

#### docker-compose.yml
```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    container_name: redis-single
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
      - ./config/redis.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      - redis-net
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  redis_data:
    driver: local

networks:
  redis-net:
    driver: bridge
```

#### Redis配置文件 (config/redis.conf)
```conf
# Redis基础配置
bind 0.0.0.0
port 6379
protected-mode no

# 持久化配置
save 900 1
save 300 10
save 60 10000

# RDB文件配置
dbfilename dump.rdb
dir /data

# AOF配置
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec

# 内存配置
maxmemory 256mb
maxmemory-policy allkeys-lru

# 日志配置
loglevel notice
logfile /data/redis.log

# 客户端配置
timeout 300
tcp-keepalive 60

# 性能配置
tcp-backlog 511
databases 16
```

#### 启动脚本 (start.sh)
```bash
#!/bin/bash

# Redis单机版启动脚本

echo "🚀 启动Redis单机服务..."

# 创建配置目录
mkdir -p config

# 检查配置文件
if [ ! -f config/redis.conf ]; then
    echo "创建Redis配置文件..."
    cat > config/redis.conf << 'EOF'
bind 0.0.0.0
port 6379
protected-mode no
save 900 1
save 300 10
save 60 10000
dbfilename dump.rdb
dir /data
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
maxmemory 256mb
maxmemory-policy allkeys-lru
loglevel notice
logfile /data/redis.log
timeout 300
tcp-keepalive 60
EOF
fi

# 启动服务
docker-compose up -d

# 等待服务启动
echo "等待Redis服务启动..."
sleep 10

# 检查服务状态
if docker-compose ps | grep -q "Up"; then
    echo "✅ Redis服务启动成功"
    echo "连接信息:"
    echo "  地址: localhost:6379"
    echo "  测试连接: docker-compose exec redis redis-cli ping"
else
    echo "❌ Redis服务启动失败"
    docker-compose logs redis
fi
```

### 2. 带认证的Redis配置

#### docker-compose-auth.yml
```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    container_name: redis-auth
    restart: unless-stopped
    ports:
      - "6379:6379"
    environment:
      - REDIS_PASSWORD=your_secure_password
    volumes:
      - redis_data:/data
      - ./config/redis-auth.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf --requirepass your_secure_password
    networks:
      - redis-net
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "your_secure_password", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  redis_data:

networks:
  redis-net:
    driver: bridge
```

#### 认证配置文件 (config/redis-auth.conf)
```conf
# Redis认证配置
bind 0.0.0.0
port 6379
protected-mode yes
requirepass your_secure_password

# 用户管理 (Redis 6.0+)
user default on >your_secure_password ~* &* +@all

# 创建只读用户
user readonly on >readonly_password ~* +@read -@dangerous

# 持久化配置
save 900 1
save 300 10
save 60 10000
dbfilename dump.rdb
dir /data

# AOF配置
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec

# 安全配置
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command KEYS ""
rename-command DEBUG ""
rename-command CONFIG "CONFIG_9a85be9a4c13f8a5e8e5e7c3a6a2b1d4"

# 网络安全
tcp-backlog 511
timeout 300
tcp-keepalive 60
```

## Redis主从复制

### 1. 一主两从配置

#### docker-compose-master-slave.yml
```yaml
version: '3.8'

services:
  redis-master:
    image: redis:7-alpine
    container_name: redis-master
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_master_data:/data
      - ./config/redis-master.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster-net
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

  redis-slave1:
    image: redis:7-alpine
    container_name: redis-slave1
    restart: unless-stopped
    ports:
      - "6380:6379"
    volumes:
      - redis_slave1_data:/data
      - ./config/redis-slave.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf --replicaof redis-master 6379
    depends_on:
      - redis-master
    networks:
      - redis-cluster-net
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

  redis-slave2:
    image: redis:7-alpine
    container_name: redis-slave2
    restart: unless-stopped
    ports:
      - "6381:6379"
    volumes:
      - redis_slave2_data:/data
      - ./config/redis-slave.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf --replicaof redis-master 6379
    depends_on:
      - redis-master
    networks:
      - redis-cluster-net
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

volumes:
  redis_master_data:
  redis_slave1_data:
  redis_slave2_data:

networks:
  redis-cluster-net:
    driver: bridge
```

#### 主节点配置 (config/redis-master.conf)
```conf
# Redis Master 配置
bind 0.0.0.0
port 6379
protected-mode no

# 复制配置
replica-serve-stale-data yes
replica-read-only yes
repl-diskless-sync no
repl-diskless-sync-delay 5
repl-ping-replica-period 10
repl-timeout 60

# 持久化配置
save 900 1
save 300 10
save 60 10000
dbfilename dump.rdb
dir /data

# AOF配置
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec

# 内存配置
maxmemory 512mb
maxmemory-policy allkeys-lru

# 日志配置
loglevel notice
logfile /data/redis-master.log
```

#### 从节点配置 (config/redis-slave.conf)
```conf
# Redis Slave 配置
bind 0.0.0.0
port 6379
protected-mode no

# 复制配置
replica-serve-stale-data yes
replica-read-only yes
replica-priority 100

# 持久化配置 (从节点通常关闭持久化)
save ""
# dbfilename dump.rdb
# dir /data

# AOF配置 (从节点通常关闭AOF)
appendonly no

# 内存配置
maxmemory 512mb
maxmemory-policy allkeys-lru

# 日志配置
loglevel notice
logfile /data/redis-slave.log
```

### 2. 主从管理脚本

#### manage-master-slave.sh
```bash
#!/bin/bash

# Redis主从管理脚本

COMPOSE_FILE="docker-compose-master-slave.yml"

# 函数：显示帮助信息
show_help() {
    echo "Redis主从管理脚本"
    echo "用法: $0 {start|stop|status|logs|test|failover|promote}"
    echo ""
    echo "命令说明:"
    echo "  start    - 启动主从服务"
    echo "  stop     - 停止主从服务" 
    echo "  status   - 查看服务状态"
    echo "  logs     - 查看服务日志"
    echo "  test     - 测试主从复制"
    echo "  failover - 模拟故障转移"
    echo "  promote  - 提升从节点为主节点"
}

# 函数：启动服务
start_services() {
    echo "🚀 启动Redis主从服务..."
    
    # 创建配置目录
    mkdir -p config
    
    # 检查并创建配置文件
    create_config_files
    
    # 启动服务
    docker-compose -f $COMPOSE_FILE up -d
    
    # 等待服务启动
    echo "等待服务启动..."
    sleep 15
    
    # 检查服务状态
    check_services_status
}

# 函数：创建配置文件
create_config_files() {
    # 主节点配置
    if [ ! -f config/redis-master.conf ]; then
        echo "创建主节点配置文件..."
        cat > config/redis-master.conf << 'EOF'
bind 0.0.0.0
port 6379
protected-mode no
replica-serve-stale-data yes
replica-read-only yes
save 900 1
save 300 10
save 60 10000
dbfilename dump.rdb
dir /data
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
maxmemory 512mb
maxmemory-policy allkeys-lru
loglevel notice
logfile /data/redis-master.log
EOF
    fi
    
    # 从节点配置
    if [ ! -f config/redis-slave.conf ]; then
        echo "创建从节点配置文件..."
        cat > config/redis-slave.conf << 'EOF'
bind 0.0.0.0
port 6379
protected-mode no
replica-serve-stale-data yes
replica-read-only yes
replica-priority 100
save ""
appendonly no
maxmemory 512mb
maxmemory-policy allkeys-lru
loglevel notice
logfile /data/redis-slave.log
EOF
    fi
}

# 函数：检查服务状态
check_services_status() {
    echo "📊 检查服务状态..."
    docker-compose -f $COMPOSE_FILE ps
    
    # 检查复制状态
    echo ""
    echo "🔗 检查复制状态..."
    
    echo "主节点信息:"
    docker-compose -f $COMPOSE_FILE exec redis-master redis-cli info replication
    
    echo ""
    echo "从节点1信息:"
    docker-compose -f $COMPOSE_FILE exec redis-slave1 redis-cli info replication
    
    echo ""
    echo "从节点2信息:"
    docker-compose -f $COMPOSE_FILE exec redis-slave2 redis-cli info replication
}

# 函数：测试主从复制
test_replication() {
    echo "🧪 测试主从复制..."
    
    # 在主节点写入数据
    echo "在主节点写入测试数据..."
    docker-compose -f $COMPOSE_FILE exec redis-master redis-cli set test_key "Hello Redis Master-Slave"
    docker-compose -f $COMPOSE_FILE exec redis-master redis-cli set timestamp "$(date)"
    
    # 等待复制
    sleep 2
    
    # 从从节点读取数据
    echo "从从节点1读取数据:"
    docker-compose -f $COMPOSE_FILE exec redis-slave1 redis-cli get test_key
    docker-compose -f $COMPOSE_FILE exec redis-slave1 redis-cli get timestamp
    
    echo "从从节点2读取数据:"
    docker-compose -f $COMPOSE_FILE exec redis-slave2 redis-cli get test_key
    docker-compose -f $COMPOSE_FILE exec redis-slave2 redis-cli get timestamp
    
    echo "✅ 主从复制测试完成"
}

# 函数：模拟故障转移
simulate_failover() {
    echo "⚠️  模拟主节点故障..."
    
    # 停止主节点
    docker-compose -f $COMPOSE_FILE stop redis-master
    
    echo "主节点已停止，等待5秒..."
    sleep 5
    
    # 检查从节点状态
    echo "检查从节点状态:"
    docker-compose -f $COMPOSE_FILE exec redis-slave1 redis-cli info replication
    
    echo "故障转移模拟完成，重新启动主节点:"
    docker-compose -f $COMPOSE_FILE start redis-master
}

# 函数：提升从节点为主节点
promote_slave() {
    echo "🔄 提升从节点1为主节点..."
    
    # 将从节点1设置为主节点
    docker-compose -f $COMPOSE_FILE exec redis-slave1 redis-cli slaveof no one
    
    # 将从节点2指向新主节点
    docker-compose -f $COMPOSE_FILE exec redis-slave2 redis-cli slaveof redis-slave1 6379
    
    echo "✅ 从节点1已提升为主节点"
    echo "新的复制拓扑:"
    echo "  主节点: redis-slave1"
    echo "  从节点: redis-slave2"
}

# 主逻辑
case "$1" in
    start)
        start_services
        ;;
    stop)
        echo "🛑 停止Redis主从服务..."
        docker-compose -f $COMPOSE_FILE down
        ;;
    status)
        check_services_status
        ;;
    logs)
        docker-compose -f $COMPOSE_FILE logs -f
        ;;
    test)
        test_replication
        ;;
    failover)
        simulate_failover
        ;;
    promote)
        promote_slave
        ;;
    *)
        show_help
        ;;
esac
```

## Redis Sentinel哨兵模式

### 1. Sentinel集群配置

#### docker-compose-sentinel.yml
```yaml
version: '3.8'

services:
  redis-master:
    image: redis:7-alpine
    container_name: redis-sentinel-master
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_sentinel_master_data:/data
      - ./config/sentinel/redis-master.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      redis-sentinel-net:
        ipv4_address: 172.20.0.10

  redis-slave1:
    image: redis:7-alpine
    container_name: redis-sentinel-slave1
    restart: unless-stopped
    ports:
      - "6380:6379"
    volumes:
      - redis_sentinel_slave1_data:/data
      - ./config/sentinel/redis-slave.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf --replicaof 172.20.0.10 6379
    depends_on:
      - redis-master
    networks:
      redis-sentinel-net:
        ipv4_address: 172.20.0.11

  redis-slave2:
    image: redis:7-alpine
    container_name: redis-sentinel-slave2
    restart: unless-stopped
    ports:
      - "6381:6379"
    volumes:
      - redis_sentinel_slave2_data:/data
      - ./config/sentinel/redis-slave.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf --replicaof 172.20.0.10 6379
    depends_on:
      - redis-master
    networks:
      redis-sentinel-net:
        ipv4_address: 172.20.0.12

  sentinel1:
    image: redis:7-alpine
    container_name: redis-sentinel1
    restart: unless-stopped
    ports:
      - "26379:26379"
    volumes:
      - ./config/sentinel/sentinel1.conf:/usr/local/etc/redis/sentinel.conf
    command: redis-sentinel /usr/local/etc/redis/sentinel.conf
    depends_on:
      - redis-master
    networks:
      redis-sentinel-net:
        ipv4_address: 172.20.0.20

  sentinel2:
    image: redis:7-alpine
    container_name: redis-sentinel2
    restart: unless-stopped
    ports:
      - "26380:26379"
    volumes:
      - ./config/sentinel/sentinel2.conf:/usr/local/etc/redis/sentinel.conf
    command: redis-sentinel /usr/local/etc/redis/sentinel.conf
    depends_on:
      - redis-master
    networks:
      redis-sentinel-net:
        ipv4_address: 172.20.0.21

  sentinel3:
    image: redis:7-alpine
    container_name: redis-sentinel3
    restart: unless-stopped
    ports:
      - "26381:26379"
    volumes:
      - ./config/sentinel/sentinel3.conf:/usr/local/etc/redis/sentinel.conf
    command: redis-sentinel /usr/local/etc/redis/sentinel.conf
    depends_on:
      - redis-master
    networks:
      redis-sentinel-net:
        ipv4_address: 172.20.0.22

volumes:
  redis_sentinel_master_data:
  redis_sentinel_slave1_data:
  redis_sentinel_slave2_data:

networks:
  redis-sentinel-net:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

#### Sentinel配置文件 (config/sentinel/sentinel1.conf)
```conf
# Sentinel配置
port 26379
sentinel announce-ip 172.20.0.20
sentinel announce-port 26379

# 监控主节点
sentinel monitor mymaster 172.20.0.10 6379 2

# 故障检测时间 (毫秒)
sentinel down-after-milliseconds mymaster 5000

# 故障转移超时时间
sentinel failover-timeout mymaster 10000

# 并行同步从节点数量
sentinel parallel-syncs mymaster 1

# 日志配置
logfile /tmp/sentinel1.log
loglevel notice

# 工作目录
dir /tmp
```

### 2. Sentinel管理脚本

#### sentinel-manager.sh
```bash
#!/bin/bash

# Redis Sentinel管理脚本

COMPOSE_FILE="docker-compose-sentinel.yml"

# 创建Sentinel配置文件
create_sentinel_configs() {
    mkdir -p config/sentinel
    
    # Sentinel1配置
    cat > config/sentinel/sentinel1.conf << 'EOF'
port 26379
sentinel announce-ip 172.20.0.20
sentinel announce-port 26379
sentinel monitor mymaster 172.20.0.10 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 10000
sentinel parallel-syncs mymaster 1
logfile /tmp/sentinel1.log
loglevel notice
dir /tmp
EOF

    # Sentinel2配置
    cat > config/sentinel/sentinel2.conf << 'EOF'
port 26379
sentinel announce-ip 172.20.0.21
sentinel announce-port 26379
sentinel monitor mymaster 172.20.0.10 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 10000
sentinel parallel-syncs mymaster 1
logfile /tmp/sentinel2.log
loglevel notice
dir /tmp
EOF

    # Sentinel3配置
    cat > config/sentinel/sentinel3.conf << 'EOF'
port 26379
sentinel announce-ip 172.20.0.22
sentinel announce-port 26379
sentinel monitor mymaster 172.20.0.10 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 10000
sentinel parallel-syncs mymaster 1
logfile /tmp/sentinel3.log
loglevel notice
dir /tmp
EOF

    # Redis配置文件
    cat > config/sentinel/redis-master.conf << 'EOF'
bind 0.0.0.0
port 6379
protected-mode no
save 900 1
save 300 10
save 60 10000
dbfilename dump.rdb
dir /data
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
maxmemory 512mb
maxmemory-policy allkeys-lru
EOF

    cp config/sentinel/redis-master.conf config/sentinel/redis-slave.conf
}

# 启动Sentinel集群
start_sentinel_cluster() {
    echo "🚀 启动Redis Sentinel集群..."
    
    create_sentinel_configs
    
    docker-compose -f $COMPOSE_FILE up -d
    
    echo "等待服务启动..."
    sleep 20
    
    check_sentinel_status
}

# 检查Sentinel状态
check_sentinel_status() {
    echo "📊 Sentinel集群状态:"
    
    echo "Sentinel1状态:"
    docker-compose -f $COMPOSE_FILE exec sentinel1 redis-cli -p 26379 sentinel masters
    
    echo "监控的从节点:"
    docker-compose -f $COMPOSE_FILE exec sentinel1 redis-cli -p 26379 sentinel slaves mymaster
    
    echo "其他Sentinel节点:"
    docker-compose -f $COMPOSE_FILE exec sentinel1 redis-cli -p 26379 sentinel sentinels mymaster
}

# 测试自动故障转移
test_failover() {
    echo "🧪 测试自动故障转移..."
    
    # 停止主节点
    echo "停止主节点..."
    docker-compose -f $COMPOSE_FILE stop redis-master
    
    # 等待故障转移
    echo "等待Sentinel执行故障转移..."
    sleep 15
    
    # 检查新的主节点
    echo "检查新的主节点:"
    docker-compose -f $COMPOSE_FILE exec sentinel1 redis-cli -p 26379 sentinel get-master-addr-by-name mymaster
    
    # 重新启动原主节点
    echo "重新启动原主节点（现在作为从节点）..."
    docker-compose -f $COMPOSE_FILE start redis-master
    
    sleep 10
    check_sentinel_status
}

case "$1" in
    start)
        start_sentinel_cluster
        ;;
    status)
        check_sentinel_status
        ;;
    test-failover)
        test_failover
        ;;
    stop)
        docker-compose -f $COMPOSE_FILE down
        ;;
    logs)
        docker-compose -f $COMPOSE_FILE logs -f
        ;;
    *)
        echo "用法: $0 {start|status|test-failover|stop|logs}"
        ;;
esac
```

## Redis集群模式

### 1. 6节点集群配置

#### docker-compose-cluster.yml
```yaml
version: '3.8'

services:
  redis-cluster-1:
    image: redis:7-alpine
    container_name: redis-cluster-1
    restart: unless-stopped
    ports:
      - "7001:6379"
      - "17001:16379"
    volumes:
      - redis_cluster_1_data:/data
      - ./config/cluster/redis-cluster.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      redis-cluster-net:
        ipv4_address: 172.30.0.11

  redis-cluster-2:
    image: redis:7-alpine
    container_name: redis-cluster-2
    restart: unless-stopped
    ports:
      - "7002:6379"
      - "17002:16379"
    volumes:
      - redis_cluster_2_data:/data
      - ./config/cluster/redis-cluster.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      redis-cluster-net:
        ipv4_address: 172.30.0.12

  redis-cluster-3:
    image: redis:7-alpine
    container_name: redis-cluster-3
    restart: unless-stopped
    ports:
      - "7003:6379"
      - "17003:16379"
    volumes:
      - redis_cluster_3_data:/data
      - ./config/cluster/redis-cluster.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      redis-cluster-net:
        ipv4_address: 172.30.0.13

  redis-cluster-4:
    image: redis:7-alpine
    container_name: redis-cluster-4
    restart: unless-stopped
    ports:
      - "7004:6379"
      - "17004:16379"
    volumes:
      - redis_cluster_4_data:/data
      - ./config/cluster/redis-cluster.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      redis-cluster-net:
        ipv4_address: 172.30.0.14

  redis-cluster-5:
    image: redis:7-alpine
    container_name: redis-cluster-5
    restart: unless-stopped
    ports:
      - "7005:6379"
      - "17005:16379"
    volumes:
      - redis_cluster_5_data:/data
      - ./config/cluster/redis-cluster.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      redis-cluster-net:
        ipv4_address: 172.30.0.15

  redis-cluster-6:
    image: redis:7-alpine
    container_name: redis-cluster-6
    restart: unless-stopped
    ports:
      - "7006:6379"
      - "17006:16379"
    volumes:
      - redis_cluster_6_data:/data
      - ./config/cluster/redis-cluster.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      redis-cluster-net:
        ipv4_address: 172.30.0.16

volumes:
  redis_cluster_1_data:
  redis_cluster_2_data:
  redis_cluster_3_data:
  redis_cluster_4_data:
  redis_cluster_5_data:
  redis_cluster_6_data:

networks:
  redis-cluster-net:
    driver: bridge
    ipam:
      config:
        - subnet: 172.30.0.0/16
```

#### 集群配置文件 (config/cluster/redis-cluster.conf)
```conf
# Redis集群配置
bind 0.0.0.0
port 6379
protected-mode no

# 集群模式
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
cluster-announce-ip 172.30.0.11
cluster-announce-port 6379
cluster-announce-bus-port 16379

# 持久化配置
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec

# 内存配置
maxmemory 256mb
maxmemory-policy allkeys-lru

# 日志配置
loglevel notice
logfile /data/redis-cluster.log

# 其他配置
timeout 0
tcp-keepalive 60
```

### 2. 集群管理脚本

#### cluster-manager.sh
```bash
#!/bin/bash

# Redis集群管理脚本

COMPOSE_FILE="docker-compose-cluster.yml"
CLUSTER_NODES="172.30.0.11:6379 172.30.0.12:6379 172.30.0.13:6379 172.30.0.14:6379 172.30.0.15:6379 172.30.0.16:6379"

# 创建集群配置
create_cluster_config() {
    mkdir -p config/cluster
    
    cat > config/cluster/redis-cluster.conf << 'EOF'
bind 0.0.0.0
port 6379
protected-mode no
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
maxmemory 256mb
maxmemory-policy allkeys-lru
loglevel notice
logfile /data/redis-cluster.log
timeout 0
tcp-keepalive 60
EOF
}

# 启动集群
start_cluster() {
    echo "🚀 启动Redis集群..."
    
    create_cluster_config
    
    # 启动所有节点
    docker-compose -f $COMPOSE_FILE up -d
    
    echo "等待节点启动..."
    sleep 15
    
    # 创建集群
    echo "创建Redis集群..."
    docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli --cluster create \
        $CLUSTER_NODES \
        --cluster-replicas 1 \
        --cluster-yes
    
    echo "✅ Redis集群创建完成"
    
    # 检查集群状态
    check_cluster_status
}

# 检查集群状态
check_cluster_status() {
    echo "📊 Redis集群状态:"
    
    # 集群信息
    docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli cluster info
    
    echo ""
    echo "集群节点:"
    docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli cluster nodes
    
    echo ""
    echo "槽位分配:"
    docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli cluster slots
}

# 测试集群
test_cluster() {
    echo "🧪 测试Redis集群..."
    
    # 写入测试数据
    echo "写入测试数据..."
    for i in {1..10}; do
        docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli -c set "test_key_$i" "value_$i"
    done
    
    echo ""
    echo "读取测试数据:"
    for i in {1..10}; do
        result=$(docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli -c get "test_key_$i")
        echo "test_key_$i: $result"
    done
    
    echo ""
    echo "集群键分布:"
    for node in {1..6}; do
        count=$(docker-compose -f $COMPOSE_FILE exec redis-cluster-$node redis-cli dbsize)
        echo "节点$node 键数量: $count"
    done
}

# 集群重新分片
reshard_cluster() {
    echo "🔄 集群重新分片..."
    
    docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli --cluster reshard 172.30.0.11:6379 \
        --cluster-from all \
        --cluster-to $(docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli cluster nodes | grep master | head -1 | cut -d' ' -f1) \
        --cluster-slots 100 \
        --cluster-yes
    
    echo "✅ 重新分片完成"
}

# 添加节点
add_node() {
    local new_node_ip=$1
    local new_node_port=$2
    
    if [ -z "$new_node_ip" ] || [ -z "$new_node_port" ]; then
        echo "用法: $0 add-node <new_node_ip> <new_node_port>"
        return 1
    fi
    
    echo "➕ 添加新节点: $new_node_ip:$new_node_port"
    
    docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli --cluster add-node \
        $new_node_ip:$new_node_port \
        172.30.0.11:6379
    
    echo "✅ 节点添加完成"
}

# 删除节点
remove_node() {
    local node_id=$1
    
    if [ -z "$node_id" ]; then
        echo "用法: $0 remove-node <node_id>"
        echo "获取节点ID: docker-compose exec redis-cluster-1 redis-cli cluster nodes"
        return 1
    fi
    
    echo "➖ 删除节点: $node_id"
    
    docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli --cluster del-node \
        172.30.0.11:6379 \
        $node_id
    
    echo "✅ 节点删除完成"
}

# 修复集群
fix_cluster() {
    echo "🔧 修复Redis集群..."
    
    docker-compose -f $COMPOSE_FILE exec redis-cluster-1 redis-cli --cluster fix 172.30.0.11:6379
    
    echo "✅ 集群修复完成"
}

case "$1" in
    start)
        start_cluster
        ;;
    status)
        check_cluster_status
        ;;
    test)
        test_cluster
        ;;
    reshard)
        reshard_cluster
        ;;
    add-node)
        add_node $2 $3
        ;;
    remove-node)
        remove_node $2
        ;;
    fix)
        fix_cluster
        ;;
    stop)
        docker-compose -f $COMPOSE_FILE down
        ;;
    logs)
        docker-compose -f $COMPOSE_FILE logs -f
        ;;
    *)
        echo "用法: $0 {start|status|test|reshard|add-node|remove-node|fix|stop|logs}"
        ;;
esac
```

## Redis监控方案

### 1. 使用Redis Exporter和Grafana

#### docker-compose-monitoring.yml
```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    container_name: redis-monitored
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
      - ./config/redis.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      - monitoring-net

  redis-exporter:
    image: oliver006/redis_exporter:latest
    container_name: redis-exporter
    restart: unless-stopped
    ports:
      - "9121:9121"
    environment:
      REDIS_ADDR: "redis://redis:6379"
      REDIS_EXPORTER_LOG_FORMAT: "txt"
    depends_on:
      - redis
    networks:
      - monitoring-net

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      - ./config/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    depends_on:
      - redis-exporter
    networks:
      - monitoring-net

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./config/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./config/grafana/datasources:/etc/grafana/provisioning/datasources
    depends_on:
      - prometheus
    networks:
      - monitoring-net

volumes:
  redis_data:
  prometheus_data:
  grafana_data:

networks:
  monitoring-net:
    driver: bridge
```

#### Prometheus配置 (config/prometheus.yml)
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'redis-exporter'
    static_configs:
      - targets: ['redis-exporter:9121']
    scrape_interval: 10s
    metrics_path: /metrics

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          # - alertmanager:9093
```

### 2. 监控脚本

#### monitoring-setup.sh
```bash
#!/bin/bash

# Redis监控设置脚本

setup_monitoring() {
    echo "🔧 设置Redis监控..."
    
    # 创建配置目录
    mkdir -p config/grafana/{dashboards,datasources}
    
    # 创建Prometheus配置
    create_prometheus_config
    
    # 创建Grafana数据源配置
    create_grafana_datasource
    
    # 创建Grafana仪表盘配置
    create_grafana_dashboard_config
    
    # 启动监控栈
    docker-compose -f docker-compose-monitoring.yml up -d
    
    echo "等待服务启动..."
    sleep 30
    
    echo "✅ 监控设置完成"
    echo "访问地址:"
    echo "  Grafana: http://localhost:3000 (admin/admin)"
    echo "  Prometheus: http://localhost:9090"
    echo "  Redis Exporter: http://localhost:9121/metrics"
}

create_prometheus_config() {
    cat > config/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'redis-exporter'
    static_configs:
      - targets: ['redis-exporter:9121']
    scrape_interval: 10s
    metrics_path: /metrics

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
EOF
}

create_grafana_datasource() {
    cat > config/grafana/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
EOF
}

create_grafana_dashboard_config() {
    cat > config/grafana/dashboards/dashboard.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'Redis Dashboard'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
EOF
}

case "$1" in
    setup)
        setup_monitoring
        ;;
    start)
        docker-compose -f docker-compose-monitoring.yml up -d
        ;;
    stop)
        docker-compose -f docker-compose-monitoring.yml down
        ;;
    logs)
        docker-compose -f docker-compose-monitoring.yml logs -f
        ;;
    *)
        echo "用法: $0 {setup|start|stop|logs}"
        ;;
esac
```

## 安全配置

### 1. 生产环境安全配置

#### secure-redis.conf
```conf
# Redis安全配置文件

# 网络安全
bind 127.0.0.1 172.18.0.0/16
protected-mode yes
port 6379

# 认证
requirepass your_very_secure_password_here

# 用户管理 (Redis 6.0+)
user default on >your_very_secure_password_here ~* &* +@all
user app_user on >app_user_password ~app:* +@read +@write -@dangerous
user readonly_user on >readonly_password ~* +@read -@write -@dangerous

# 命令重命名/禁用
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command KEYS ""
rename-command DEBUG ""
rename-command EVAL ""
rename-command CONFIG "CONFIG_a1b2c3d4e5f6"
rename-command SHUTDOWN SHUTDOWN_a1b2c3d4e5f6

# TLS配置 (如果需要)
# tls-port 6380
# tls-cert-file /tls/redis.crt
# tls-key-file /tls/redis.key
# tls-ca-cert-file /tls/ca.crt

# 日志和监控
logfile /var/log/redis/redis.log
loglevel warning

# 安全相关设置
tcp-backlog 128
timeout 300
tcp-keepalive 300

# 客户端限制
maxclients 10000

# 内存安全
maxmemory 1gb
maxmemory-policy allkeys-lru

# 持久化安全
stop-writes-on-bgsave-error yes
save 900 1
save 300 10
save 60 10000
```

### 2. SSL/TLS配置

#### docker-compose-ssl.yml
```yaml
version: '3.8'

services:
  redis-ssl:
    image: redis:7-alpine
    container_name: redis-ssl
    restart: unless-stopped
    ports:
      - "6379:6379"
      - "6380:6380"  # TLS端口
    volumes:
      - redis_ssl_data:/data
      - ./config/redis-ssl.conf:/usr/local/etc/redis/redis.conf
      - ./ssl:/tls:ro
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      - redis-ssl-net
    healthcheck:
      test: ["CMD", "redis-cli", "--tls", "--cert", "/tls/redis.crt", "--key", "/tls/redis.key", "--cacert", "/tls/ca.crt", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  redis_ssl_data:

networks:
  redis-ssl-net:
    driver: bridge
```

### 3. 安全检查脚本

#### security-check.sh
```bash
#!/bin/bash

# Redis安全检查脚本

REDIS_HOST="localhost"
REDIS_PORT="6379"

check_redis_security() {
    echo "🔍 Redis安全检查..."
    
    echo "1. 检查访问控制..."
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT ping 2>/dev/null; then
        echo "⚠️  警告: Redis无需认证即可访问"
    else
        echo "✅ Redis需要认证访问"
    fi
    
    echo ""
    echo "2. 检查危险命令..."
    dangerous_commands=("FLUSHALL" "FLUSHDB" "KEYS" "CONFIG" "DEBUG" "EVAL")
    
    for cmd in "${dangerous_commands[@]}"; do
        if redis-cli -h $REDIS_HOST -p $REDIS_PORT $cmd 2>&1 | grep -q "unknown command"; then
            echo "✅ $cmd 命令已禁用"
        else
            echo "⚠️  警告: $cmd 命令可用"
        fi
    done
    
    echo ""
    echo "3. 检查配置安全..."
    config_info=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT CONFIG GET "*" 2>/dev/null)
    
    if echo "$config_info" | grep -q "protected-mode.*yes"; then
        echo "✅ protected-mode 已启用"
    else
        echo "⚠️  警告: protected-mode 未启用"
    fi
    
    if echo "$config_info" | grep -q "bind.*127.0.0.1"; then
        echo "✅ 绑定到本地地址"
    else
        echo "⚠️  警告: 可能绑定到所有接口"
    fi
    
    echo ""
    echo "4. 检查用户权限..."
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT ACL LIST 2>/dev/null | grep -q "user"; then
        echo "✅ 发现用户ACL配置"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT ACL LIST
    else
        echo "⚠️  未发现用户ACL配置"
    fi
}

generate_security_report() {
    echo "📊 生成安全报告..."
    
    {
        echo "Redis Security Report"
        echo "===================="
        echo "Date: $(date)"
        echo "Host: $REDIS_HOST:$REDIS_PORT"
        echo ""
        
        check_redis_security
        
    } > security_report.txt
    
    echo "✅ 安全报告已生成: security_report.txt"
}

case "$1" in
    check)
        check_redis_security
        ;;
    report)
        generate_security_report
        ;;
    *)
        echo "用法: $0 {check|report}"
        ;;
esac
```

## 性能优化

### 1. 内存优化配置

#### redis-memory-optimized.conf
```conf
# Redis内存优化配置

# 内存限制
maxmemory 2gb
maxmemory-policy allkeys-lru

# 内存采样
maxmemory-samples 10

# 懒删除
lazyfree-lazy-eviction yes
lazyfree-lazy-expire yes
lazyfree-lazy-server-del yes
replica-lazy-flush yes

# 哈希表优化
hash-max-ziplist-entries 512
hash-max-ziplist-value 64

# 列表优化
list-max-ziplist-size -2
list-compress-depth 1

# 集合优化
set-max-intset-entries 512

# 有序集合优化
zset-max-ziplist-entries 128
zset-max-ziplist-value 64

# HyperLogLog优化
hll-sparse-max-bytes 3000

# 字符串优化
# 对于大量小字符串，考虑使用压缩
```

### 2. 性能测试脚本

#### performance-test.sh
```bash
#!/bin/bash

# Redis性能测试脚本

REDIS_HOST="localhost"
REDIS_PORT="6379"

# 基础性能测试
basic_performance_test() {
    echo "🏃 基础性能测试..."
    
    echo "SET性能测试:"
    redis-benchmark -h $REDIS_HOST -p $REDIS_PORT -t set -n 100000 -d 100 -c 50 -q
    
    echo "GET性能测试:"
    redis-benchmark -h $REDIS_HOST -p $REDIS_PORT -t get -n 100000 -d 100 -c 50 -q
    
    echo "INCR性能测试:"
    redis-benchmark -h $REDIS_HOST -p $REDIS_PORT -t incr -n 100000 -c 50 -q
    
    echo "LPUSH性能测试:"
    redis-benchmark -h $REDIS_HOST -p $REDIS_PORT -t lpush -n 100000 -d 100 -c 50 -q
    
    echo "LPOP性能测试:"
    redis-benchmark -h $REDIS_HOST -p $REDIS_PORT -t lpop -n 100000 -c 50 -q
}

# 内存使用分析
memory_analysis() {
    echo "🧠 内存使用分析..."
    
    redis-cli -h $REDIS_HOST -p $REDIS_PORT info memory
    
    echo ""
    echo "键空间信息:"
    redis-cli -h $REDIS_HOST -p $REDIS_PORT info keyspace
    
    echo ""
    echo "慢查询日志:"
    redis-cli -h $REDIS_HOST -p $REDIS_PORT slowlog get 10
}

# 连接池测试
connection_pool_test() {
    echo "🔗 连接池测试..."
    
    # 测试不同并发连接数下的性能
    for connections in 10 50 100 200; do
        echo "测试 $connections 个并发连接:"
        redis-benchmark -h $REDIS_HOST -p $REDIS_PORT -t set,get -n 50000 -c $connections -q
    done
}

# 延迟测试
latency_test() {
    echo "⏱️  延迟测试..."
    
    redis-cli -h $REDIS_HOST -p $REDIS_PORT --latency-history -i 1 &
    LATENCY_PID=$!
    
    echo "延迟监控已启动，PID: $LATENCY_PID"
    echo "按Ctrl+C停止监控"
    
    trap "kill $LATENCY_PID 2>/dev/null" EXIT
    wait $LATENCY_PID
}

case "$1" in
    basic)
        basic_performance_test
        ;;
    memory)
        memory_analysis
        ;;
    connections)
        connection_pool_test
        ;;
    latency)
        latency_test
        ;;
    all)
        basic_performance_test
        memory_analysis
        connection_pool_test
        ;;
    *)
        echo "用法: $0 {basic|memory|connections|latency|all}"
        ;;
esac
```

## 备份与恢复

### 1. 备份策略

#### backup-redis.sh
```bash
#!/bin/bash

# Redis备份脚本

BACKUP_DIR="/var/backups/redis"
REDIS_CONTAINER="redis-single"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# RDB备份
backup_rdb() {
    echo "📦 创建RDB备份..."
    
    # 触发BGSAVE
    docker exec $REDIS_CONTAINER redis-cli BGSAVE
    
    # 等待备份完成
    echo "等待RDB备份完成..."
    while [ "$(docker exec $REDIS_CONTAINER redis-cli LASTSAVE)" = "$(docker exec $REDIS_CONTAINER redis-cli LASTSAVE)" ]; do
        sleep 1
    done
    
    # 复制RDB文件
    docker cp $REDIS_CONTAINER:/data/dump.rdb $BACKUP_DIR/dump_$DATE.rdb
    
    echo "✅ RDB备份完成: $BACKUP_DIR/dump_$DATE.rdb"
}

# AOF备份
backup_aof() {
    echo "📝 创建AOF备份..."
    
    # 重写AOF
    docker exec $REDIS_CONTAINER redis-cli BGREWRITEAOF
    
    # 等待重写完成
    echo "等待AOF重写完成..."
    sleep 5
    
    # 复制AOF文件
    docker cp $REDIS_CONTAINER:/data/appendonly.aof $BACKUP_DIR/appendonly_$DATE.aof
    
    echo "✅ AOF备份完成: $BACKUP_DIR/appendonly_$DATE.aof"
}

# 配置备份
backup_config() {
    echo "⚙️  备份配置文件..."
    
    docker exec $REDIS_CONTAINER cat /usr/local/etc/redis/redis.conf > $BACKUP_DIR/redis_config_$DATE.conf
    
    echo "✅ 配置备份完成: $BACKUP_DIR/redis_config_$DATE.conf"
}

# 压缩备份
compress_backup() {
    echo "🗜️  压缩备份文件..."
    
    cd $BACKUP_DIR
    tar -czf redis_backup_$DATE.tar.gz *$DATE*
    
    # 删除原始文件
    rm -f *$DATE.rdb *$DATE.aof *$DATE.conf
    
    echo "✅ 备份压缩完成: $BACKUP_DIR/redis_backup_$DATE.tar.gz"
}

# 清理旧备份
cleanup_old_backups() {
    echo "🧹 清理旧备份..."
    
    # 删除7天前的备份
    find $BACKUP_DIR -name "redis_backup_*.tar.gz" -mtime +7 -delete
    
    echo "✅ 旧备份清理完成"
}

# 完整备份
full_backup() {
    echo "🚀 开始完整备份..."
    
    backup_rdb
    backup_aof
    backup_config
    compress_backup
    cleanup_old_backups
    
    echo "✅ 完整备份完成"
}

case "$1" in
    rdb)
        backup_rdb
        ;;
    aof)
        backup_aof
        ;;
    config)
        backup_config
        ;;
    full)
        full_backup
        ;;
    clean)
        cleanup_old_backups
        ;;
    *)
        echo "用法: $0 {rdb|aof|config|full|clean}"
        ;;
esac
```

### 2. 恢复脚本

#### restore-redis.sh
```bash
#!/bin/bash

# Redis恢复脚本

BACKUP_DIR="/var/backups/redis"
REDIS_CONTAINER="redis-single"

restore_from_rdb() {
    local backup_file=$1
    
    if [ ! -f "$backup_file" ]; then
        echo "❌ 备份文件不存在: $backup_file"
        return 1
    fi
    
    echo "🔄 从RDB备份恢复..."
    
    # 停止Redis
    docker-compose stop redis
    
    # 复制备份文件
    docker cp $backup_file $REDIS_CONTAINER:/data/dump.rdb
    
    # 启动Redis
    docker-compose start redis
    
    echo "✅ RDB恢复完成"
}

restore_from_aof() {
    local backup_file=$1
    
    if [ ! -f "$backup_file" ]; then
        echo "❌ 备份文件不存在: $backup_file"
        return 1
    fi
    
    echo "🔄 从AOF备份恢复..."
    
    # 停止Redis
    docker-compose stop redis
    
    # 复制备份文件
    docker cp $backup_file $REDIS_CONTAINER:/data/appendonly.aof
    
    # 启动Redis
    docker-compose start redis
    
    echo "✅ AOF恢复完成"
}

list_backups() {
    echo "📋 可用备份列表:"
    ls -la $BACKUP_DIR/redis_backup_*.tar.gz 2>/dev/null || echo "未找到备份文件"
}

extract_backup() {
    local backup_archive=$1
    
    if [ ! -f "$backup_archive" ]; then
        echo "❌ 备份压缩文件不存在: $backup_archive"
        return 1
    fi
    
    echo "📦 解压备份文件..."
    
    # 创建临时目录
    temp_dir=$(mktemp -d)
    
    # 解压到临时目录
    tar -xzf $backup_archive -C $temp_dir
    
    echo "备份文件解压到: $temp_dir"
    ls -la $temp_dir
}

case "$1" in
    rdb)
        restore_from_rdb $2
        ;;
    aof)
        restore_from_aof $2
        ;;
    list)
        list_backups
        ;;
    extract)
        extract_backup $2
        ;;
    *)
        echo "用法: $0 {rdb|aof|list|extract} [backup_file]"
        echo ""
        echo "示例:"
        echo "  $0 list                                    # 列出备份"
        echo "  $0 rdb /var/backups/redis/dump.rdb       # 从RDB恢复"
        echo "  $0 aof /var/backups/redis/appendonly.aof # 从AOF恢复"
        echo "  $0 extract backup.tar.gz                 # 解压备份"
        ;;
esac
```

## 故障排除

### 1. 常见问题诊断

#### troubleshoot.sh
```bash
#!/bin/bash

# Redis故障排除脚本

REDIS_HOST="localhost"
REDIS_PORT="6379"
COMPOSE_FILE="docker-compose.yml"

# 检查Redis连接
check_connection() {
    echo "🔍 检查Redis连接..."
    
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT ping > /dev/null 2>&1; then
        echo "✅ Redis连接正常"
        return 0
    else
        echo "❌ Redis连接失败"
        return 1
    fi
}

# 检查容器状态
check_container_status() {
    echo "📦 检查容器状态..."
    
    docker-compose -f $COMPOSE_FILE ps
    
    echo ""
    echo "容器详细信息:"
    docker-compose -f $COMPOSE_FILE ps --format json | jq '.'
}

# 检查Redis日志
check_redis_logs() {
    echo "📜 检查Redis日志..."
    
    echo "最近的100行日志:"
    docker-compose -f $COMPOSE_FILE logs --tail=100 redis
    
    echo ""
    echo "错误日志:"
    docker-compose -f $COMPOSE_FILE logs redis | grep -i error
}

# 检查系统资源
check_system_resources() {
    echo "💻 检查系统资源..."
    
    echo "内存使用:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
    
    echo ""
    echo "磁盘空间:"
    df -h
    
    echo ""
    echo "Redis内存信息:"
    if check_connection; then
        redis-cli -h $REDIS_HOST -p $REDIS_PORT info memory
    fi
}

# 检查Redis配置
check_redis_config() {
    echo "⚙️  检查Redis配置..."
    
    if check_connection; then
        echo "当前配置:"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT CONFIG GET "*" | head -20
        
        echo ""
        echo "重要配置项:"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT CONFIG GET "maxmemory*"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT CONFIG GET "save"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT CONFIG GET "appendonly"
    fi
}

# 性能诊断
performance_diagnosis() {
    echo "🔬 性能诊断..."
    
    if check_connection; then
        echo "慢查询日志:"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT SLOWLOG GET 10
        
        echo ""
        echo "客户端连接信息:"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT CLIENT LIST
        
        echo ""
        echo "统计信息:"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT INFO stats
    fi
}

# 网络诊断
network_diagnosis() {
    echo "🌐 网络诊断..."
    
    echo "端口监听状态:"
    netstat -tlpn | grep $REDIS_PORT
    
    echo ""
    echo "Redis网络配置:"
    if check_connection; then
        redis-cli -h $REDIS_HOST -p $REDIS_PORT CONFIG GET "bind"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT CONFIG GET "protected-mode"
        redis-cli -h $REDIS_HOST -p $REDIS_PORT CONFIG GET "tcp-*"
    fi
    
    echo ""
    echo "容器网络信息:"
    docker network ls
}

# 修复常见问题
fix_common_issues() {
    echo "🔧 修复常见问题..."
    
    echo "1. 重启Redis容器..."
    docker-compose -f $COMPOSE_FILE restart redis
    
    sleep 5
    
    if check_connection; then
        echo "✅ 重启后连接正常"
    else
        echo "❌ 重启后仍无法连接"
        
        echo "2. 重新构建并启动..."
        docker-compose -f $COMPOSE_FILE down
        docker-compose -f $COMPOSE_FILE up -d
        
        sleep 10
        
        if check_connection; then
            echo "✅ 重新构建后连接正常"
        else
            echo "❌ 问题仍然存在，需要进一步调查"
        fi
    fi
}

# 生成诊断报告
generate_diagnosis_report() {
    echo "📊 生成诊断报告..."
    
    report_file="redis_diagnosis_$(date +%Y%m%d_%H%M%S).txt"
    
    {
        echo "Redis Diagnosis Report"
        echo "======================"
        echo "Date: $(date)"
        echo "Host: $REDIS_HOST:$REDIS_PORT"
        echo ""
        
        echo "=== Connection Check ==="
        check_connection
        echo ""
        
        echo "=== Container Status ==="
        check_container_status
        echo ""
        
        echo "=== System Resources ==="
        check_system_resources
        echo ""
        
        echo "=== Redis Configuration ==="
        check_redis_config
        echo ""
        
        echo "=== Performance Diagnosis ==="
        performance_diagnosis
        echo ""
        
        echo "=== Network Diagnosis ==="
        network_diagnosis
        
    } > $report_file
    
    echo "✅ 诊断报告已生成: $report_file"
}

case "$1" in
    connection)
        check_connection
        ;;
    container)
        check_container_status
        ;;
    logs)
        check_redis_logs
        ;;
    resources)
        check_system_resources
        ;;
    config)
        check_redis_config
        ;;
    performance)
        performance_diagnosis
        ;;
    network)
        network_diagnosis
        ;;
    fix)
        fix_common_issues
        ;;
    report)
        generate_diagnosis_report
        ;;
    all)
        check_connection
        check_container_status
        check_system_resources
        check_redis_config
        performance_diagnosis
        ;;
    *)
        echo "用法: $0 {connection|container|logs|resources|config|performance|network|fix|report|all}"
        echo ""
        echo "命令说明:"
        echo "  connection   - 检查Redis连接"
        echo "  container    - 检查容器状态"
        echo "  logs         - 检查Redis日志"
        echo "  resources    - 检查系统资源"
        echo "  config       - 检查Redis配置"
        echo "  performance  - 性能诊断"
        echo "  network      - 网络诊断"
        echo "  fix          - 修复常见问题"
        echo "  report       - 生成诊断报告"
        echo "  all          - 执行所有检查"
        ;;
esac
```

### 2. 常见错误解决方案

#### 错误处理指南

**1. 内存不足错误**
```bash
# 错误信息: OOM command not allowed when used memory > 'maxmemory'
# 解决方案:
redis-cli CONFIG SET maxmemory 2gb
redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

**2. 持久化失败**
```bash
# 错误信息: Background saving error
# 解决方案:
# 检查磁盘空间
df -h
# 检查权限
docker exec redis ls -la /data
# 修复权限
docker exec redis chown redis:redis /data
```

**3. 连接被拒绝**
```bash
# 错误信息: Connection refused
# 解决方案:
# 检查端口绑定
docker port redis-container
# 检查防火墙
sudo ufw status
# 检查配置
docker exec redis cat /usr/local/etc/redis/redis.conf | grep bind
```

**4. 集群故障转移问题**
```bash
# 集群节点下线
redis-cli --cluster fix 127.0.0.1:7001
# 重新分配槽位
redis-cli --cluster reshard 127.0.0.1:7001
```

## 总结

本指南提供了Redis Docker Compose的完整部署方案，包括：

1. **单机部署**: 适合开发和测试环境
2. **主从复制**: 提供读写分离和数据冗余
3. **Sentinel哨兵**: 自动故障转移和服务发现
4. **集群模式**: 高可用和水平扩展
5. **监控方案**: Prometheus + Grafana监控
6. **安全配置**: 认证、授权和网络安全
7. **性能优化**: 内存和配置优化
8. **备份恢复**: 完整的备份和恢复策略
9. **故障排除**: 诊断和修复工具

通过这些配置和脚本，可以快速搭建适合不同场景的Redis环境，并提供完整的运维支持。

### 快速开始命令

```bash
# 单机模式
./start.sh

# 主从模式  
./manage-master-slave.sh start

# Sentinel模式
./sentinel-manager.sh start

# 集群模式
./cluster-manager.sh start

# 监控模式
./monitoring-setup.sh setup
```

选择适合你需求的部署模式，按照相应的脚本即可快速搭建Redis环境。