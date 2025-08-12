# MySQL Docker Compose 部署指南

## 概述

本指南提供了一个完整的 MySQL 数据库集群部署方案，使用 Docker Compose 在 NAS 或服务器上部署 MySQL 主从复制集群，支持多数据库选择、自动备份、监控和管理工具。

## 目录

- [系统架构](#系统架构)
- [技术栈组件](#技术栈组件)
- [快速开始](#快速开始)
- [Docker Compose 配置](#docker-compose-配置)
- [主从复制配置](#主从复制配置)
- [多数据库支持](#多数据库支持)
- [管理工具](#管理工具)
- [监控系统](#监控系统)
- [备份和恢复](#备份和恢复)
- [安全配置](#安全配置)
- [性能优化](#性能优化)
- [维护和故障排除](#维护和故障排除)
- [扩展和升级](#扩展和升级)

## 系统架构

### 1. 集群架构图

```
                    ┌─────────────────────────────────────┐
                    │         Load Balancer               │
                    │      (HAProxy/ProxySQL)             │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────┴───────────────────────┐
                    │         读写分离                     │
                    └─────────────┬───────────────────────┘
                                  │
            ┌─────────────────────┼─────────────────────┐
            │                     │                     │
            ▼                     ▼                     ▼
    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
    │   Master     │────┤   Slave 1    │    │   Slave 2    │
    │   MySQL      │    │   MySQL      │    │   MySQL      │
    │   (写入)     │    │   (只读)     │    │   (只读)     │
    └──────────────┘    └──────────────┘    └──────────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
                    ┌─────────────┴───────────────────────┐
                    │            存储层                   │
                    │    - 数据持久化卷                   │
                    │    - 配置文件卷                     │
                    │    - 日志文件卷                     │
                    │    - 备份存储卷                     │
                    └─────────────────────────────────────┘

            ┌─────────────────────────────────────────┐
            │            管理和监控层                 │
            ├─────────────────────────────────────────┤
            │ • phpMyAdmin (Web管理界面)              │
            │ • Prometheus (指标收集)                 │
            │ • Grafana (监控面板)                    │
            │ • Redis (缓存/会话存储)                 │
            │ • Backup Scripts (自动备份)             │
            └─────────────────────────────────────────┘
```

### 2. 数据流向

```
应用程序 ────┐
            │
Web应用 ─────┤ ── Load Balancer ──┬── Master (写操作)
            │                    │
API服务 ─────┘                    └── Slaves (读操作)
```

### 3. 高可用特性

- **自动故障转移**: 主节点故障时自动切换
- **读写分离**: 写操作路由到主节点，读操作负载均衡到从节点
- **数据同步**: 主从实时数据同步
- **健康检查**: 自动检测节点健康状态
- **自动备份**: 定期数据备份和归档

## 技术栈组件

### 核心数据库

| 组件 | 版本 | 用途 | 配置 |
|------|------|------|------|
| **MySQL** | 8.0.35 | 主数据库 | 主从复制、InnoDB |
| **MariaDB** | 10.11 | 可选数据库 | 兼容MySQL协议 |
| **Percona** | 8.0.35 | 高性能MySQL | 增强版本 |

### 负载均衡和代理

| 组件 | 版本 | 用途 | 特性 |
|------|------|------|------|
| **ProxySQL** | 2.5.5 | 数据库代理 | 读写分离、连接池 |
| **HAProxy** | 2.8 | 负载均衡 | 高可用、健康检查 |

### 管理和监控

| 组件 | 版本 | 用途 | 端口 |
|------|------|------|------|
| **phpMyAdmin** | 5.2.1 | Web管理界面 | 8080 |
| **Adminer** | 4.8.1 | 轻量级管理工具 | 8081 |
| **Prometheus** | v2.45.0 | 指标收集 | 9090 |
| **Grafana** | 10.0.0 | 监控面板 | 3000 |
| **Redis** | 7.0-alpine | 缓存/会话 | 6379 |

### 存储和备份

| 组件 | 用途 | 配置 |
|------|------|------|
| **Docker Volumes** | 数据持久化 | 本地挂载 |
| **NFS/CIFS** | 网络存储 | 可选配置 |
| **Automated Backup** | 定期备份 | Cron + mysqldump |

## 快速开始

### 1. 环境要求

```bash
# 系统要求
- Docker Engine 20.10+
- Docker Compose 2.0+
- 可用内存: 4GB+ (推荐 8GB+)
- 可用磁盘: 50GB+ (数据存储)
- CPU: 2 核心+ (推荐 4 核心+)

# NAS 具体要求
- 群晖 DSM 7.0+ / QNAP QTS 5.0+
- 支持 Docker 容器
- 网络端口: 3306, 8080, 9090, 3000
```

### 2. 快速部署

```bash
# 1. 克隆项目
git clone https://github.com/githubstudycloud/codeclaTest.git
cd codeclaTest

# 2. 创建部署目录
mkdir -p mysql-cluster
cd mysql-cluster

# 3. 下载配置文件
curl -O https://raw.githubusercontent.com/githubstudycloud/codeclaTest/master/mysql-cluster/docker-compose.yml
curl -O https://raw.githubusercontent.com/githubstudycloud/codeclaTest/master/mysql-cluster/.env

# 4. 配置环境变量
cp .env.example .env
nano .env

# 5. 创建必要目录
./scripts/setup.sh

# 6. 启动集群
docker-compose up -d

# 7. 验证部署
./scripts/health-check.sh
```

### 3. 验证安装

```bash
# 检查容器状态
docker-compose ps

# 检查主从同步状态
docker-compose exec mysql-master mysql -uroot -p -e "SHOW MASTER STATUS;"
docker-compose exec mysql-slave1 mysql -uroot -p -e "SHOW SLAVE STATUS\G"

# 访问管理界面
echo "phpMyAdmin: http://localhost:8080"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo "Prometheus: http://localhost:9090"
```

## Docker Compose 配置

### 1. 主配置文件

```yaml
# docker-compose.yml
version: '3.8'

services:
  # ================================
  # MySQL Master 主节点
  # ================================
  mysql-master:
    image: mysql:8.0.35
    container_name: mysql-master
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    command: >
      --server-id=1
      --log-bin=mysql-bin
      --binlog-format=ROW
      --gtid-mode=ON
      --enforce-gtid-consistency=ON
      --log-slave-updates=ON
      --binlog-do-db=${MYSQL_DATABASE}
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --max-connections=500
      --innodb-buffer-pool-size=1G
      --innodb-log-file-size=256M
      --slow-query-log=1
      --slow-query-log-file=/var/log/mysql/slow.log
      --long-query-time=2
    volumes:
      - mysql_master_data:/var/lib/mysql
      - ./config/master/my.cnf:/etc/mysql/conf.d/my.cnf:ro
      - ./logs/master:/var/log/mysql
      - ./scripts:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"
    networks:
      - mysql_cluster
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      timeout: 20s
      retries: 10
      interval: 30s

  # ================================
  # MySQL Slave 1 从节点
  # ================================
  mysql-slave1:
    image: mysql:8.0.35
    container_name: mysql-slave1
    restart: unless-stopped
    depends_on:
      mysql-master:
        condition: service_healthy
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    command: >
      --server-id=2
      --log-bin=mysql-bin
      --binlog-format=ROW
      --gtid-mode=ON
      --enforce-gtid-consistency=ON
      --log-slave-updates=ON
      --read-only=1
      --relay-log=relay-log
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --max-connections=300
      --innodb-buffer-pool-size=512M
    volumes:
      - mysql_slave1_data:/var/lib/mysql
      - ./config/slave1/my.cnf:/etc/mysql/conf.d/my.cnf:ro
      - ./logs/slave1:/var/log/mysql
      - ./scripts:/docker-entrypoint-initdb.d
    ports:
      - "3307:3306"
    networks:
      - mysql_cluster
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      timeout: 20s
      retries: 10
      interval: 30s

  # ================================
  # MySQL Slave 2 从节点
  # ================================
  mysql-slave2:
    image: mysql:8.0.35
    container_name: mysql-slave2
    restart: unless-stopped
    depends_on:
      mysql-master:
        condition: service_healthy
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    command: >
      --server-id=3
      --log-bin=mysql-bin
      --binlog-format=ROW
      --gtid-mode=ON
      --enforce-gtid-consistency=ON
      --log-slave-updates=ON
      --read-only=1
      --relay-log=relay-log
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --max-connections=300
      --innodb-buffer-pool-size=512M
    volumes:
      - mysql_slave2_data:/var/lib/mysql
      - ./config/slave2/my.cnf:/etc/mysql/conf.d/my.cnf:ro
      - ./logs/slave2:/var/log/mysql
      - ./scripts:/docker-entrypoint-initdb.d
    ports:
      - "3308:3306"
    networks:
      - mysql_cluster
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      timeout: 20s
      retries: 10
      interval: 30s

  # ================================
  # ProxySQL 数据库代理
  # ================================
  proxysql:
    image: proxysql/proxysql:2.5.5
    container_name: proxysql
    restart: unless-stopped
    depends_on:
      - mysql-master
      - mysql-slave1
      - mysql-slave2
    volumes:
      - ./config/proxysql/proxysql.cnf:/etc/proxysql.cnf:ro
      - proxysql_data:/var/lib/proxysql
    ports:
      - "6033:6033"  # MySQL port
      - "6032:6032"  # Admin port
    networks:
      - mysql_cluster
    healthcheck:
      test: ["CMD", "mysql", "-h", "127.0.0.1", "-P", "6032", "-u", "admin", "-padmin", "-e", "SELECT 1"]
      timeout: 10s
      retries: 5
      interval: 30s

  # ================================
  # phpMyAdmin 管理界面
  # ================================
  phpmyadmin:
    image: phpmyadmin/phpmyadmin:5.2.1
    container_name: phpmyadmin
    restart: unless-stopped
    depends_on:
      - mysql-master
    environment:
      PMA_ARBITRARY: 1
      PMA_HOST: mysql-master
      PMA_PORT: 3306
      PMA_USER: root
      PMA_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      UPLOAD_LIMIT: 512M
    volumes:
      - ./config/phpmyadmin/config.user.inc.php:/etc/phpmyadmin/config.user.inc.php:ro
    ports:
      - "8080:80"
    networks:
      - mysql_cluster

  # ================================
  # Adminer 轻量级管理工具
  # ================================
  adminer:
    image: adminer:4.8.1
    container_name: adminer
    restart: unless-stopped
    depends_on:
      - mysql-master
    environment:
      ADMINER_DEFAULT_SERVER: mysql-master
    ports:
      - "8081:8080"
    networks:
      - mysql_cluster

  # ================================
  # Redis 缓存
  # ================================
  redis:
    image: redis:7.0-alpine
    container_name: redis
    restart: unless-stopped
    command: >
      redis-server
      --appendonly yes
      --requirepass ${REDIS_PASSWORD}
      --maxmemory 256mb
      --maxmemory-policy allkeys-lru
    volumes:
      - redis_data:/data
      - ./config/redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
    ports:
      - "6379:6379"
    networks:
      - mysql_cluster
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      timeout: 10s
      retries: 5
      interval: 30s

  # ================================
  # Prometheus 监控
  # ================================
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: prometheus
    restart: unless-stopped
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'
    volumes:
      - ./config/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    ports:
      - "9090:9090"
    networks:
      - mysql_cluster

  # ================================
  # MySQL Exporter 指标收集
  # ================================
  mysql-exporter:
    image: prom/mysqld-exporter:v0.15.1
    container_name: mysql-exporter
    restart: unless-stopped
    depends_on:
      - mysql-master
    environment:
      DATA_SOURCE_NAME: "exporter:${MYSQL_EXPORTER_PASSWORD}@(mysql-master:3306)/"
    command:
      - '--collect.info_schema.innodb_metrics'
      - '--collect.info_schema.innodb_tablespaces'
      - '--collect.info_schema.innodb_cmp'
      - '--collect.info_schema.innodb_cmpmem'
      - '--collect.info_schema.processlist'
      - '--collect.info_schema.query_response_time'
    ports:
      - "9104:9104"
    networks:
      - mysql_cluster

  # ================================
  # Grafana 监控面板
  # ================================
  grafana:
    image: grafana/grafana:10.0.0
    container_name: grafana
    restart: unless-stopped
    depends_on:
      - prometheus
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_USER}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
      GF_INSTALL_PLUGINS: grafana-clock-panel,grafana-simple-json-datasource
    volumes:
      - grafana_data:/var/lib/grafana
      - ./config/grafana/grafana.ini:/etc/grafana/grafana.ini:ro
      - ./config/grafana/dashboards:/etc/grafana/provisioning/dashboards:ro
      - ./config/grafana/datasources:/etc/grafana/provisioning/datasources:ro
    ports:
      - "3000:3000"
    networks:
      - mysql_cluster

  # ================================
  # 备份服务
  # ================================
  mysql-backup:
    image: databack/mysql-backup:latest
    container_name: mysql-backup
    restart: unless-stopped
    depends_on:
      - mysql-master
    environment:
      DB_DUMP_TARGET: /backup
      DB_USER: root
      DB_PASS: ${MYSQL_ROOT_PASSWORD}
      DB_DUMP_FREQ: 1440  # 每天备份
      DB_DUMP_BEGIN: 0300  # 凌晨3点开始
      DB_CLEANUP_TIME: 2160  # 保留15天
      DB_SERVER: mysql-master
      COMPRESSION: gzip
    volumes:
      - ./backups:/backup
      - backup_scripts:/scripts
    networks:
      - mysql_cluster

# ================================
# 网络配置
# ================================
networks:
  mysql_cluster:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16

# ================================
# 存储卷配置
# ================================
volumes:
  mysql_master_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/master
  
  mysql_slave1_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/slave1
  
  mysql_slave2_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/slave2
  
  proxysql_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/proxysql
  
  redis_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/redis
  
  prometheus_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/prometheus
  
  grafana_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/grafana
  
  backup_scripts:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./scripts/backup
```

### 2. 环境变量配置

```bash
# .env 文件
# ================================
# MySQL 配置
# ================================
MYSQL_ROOT_PASSWORD=SecureRootPassword123!
MYSQL_DATABASE=appdb
MYSQL_USER=appuser
MYSQL_PASSWORD=SecureAppPassword123!

# 复制用户配置
MYSQL_REPLICATION_USER=replicator
MYSQL_REPLICATION_PASSWORD=SecureReplicationPassword123!

# MySQL Exporter 配置
MYSQL_EXPORTER_PASSWORD=SecureExporterPassword123!

# ================================
# Redis 配置
# ================================
REDIS_PASSWORD=SecureRedisPassword123!

# ================================
# Grafana 配置
# ================================
GRAFANA_USER=admin
GRAFANA_PASSWORD=SecureGrafanaPassword123!

# ================================
# 网络配置
# ================================
MYSQL_MASTER_PORT=3306
MYSQL_SLAVE1_PORT=3307
MYSQL_SLAVE2_PORT=3308
PROXYSQL_PORT=6033
PROXYSQL_ADMIN_PORT=6032
PHPMYADMIN_PORT=8080
ADMINER_PORT=8081
GRAFANA_PORT=3000
PROMETHEUS_PORT=9090
REDIS_PORT=6379

# ================================
# 存储配置
# ================================
DATA_PATH=./data
CONFIG_PATH=./config
LOGS_PATH=./logs
BACKUP_PATH=./backups

# ================================
# 时区配置
# ================================
TZ=Asia/Shanghai

# ================================
# 备份配置
# ================================
BACKUP_FREQUENCY=1440  # 分钟，1440 = 24小时
BACKUP_RETENTION_DAYS=15
BACKUP_START_TIME=0300

# ================================
# 性能调优配置
# ================================
MYSQL_INNODB_BUFFER_POOL_SIZE=1G
MYSQL_MAX_CONNECTIONS=500
MYSQL_QUERY_CACHE_SIZE=256M

# ================================
# 安全配置
# ================================
MYSQL_SECURE_FILE_PRIV=/var/lib/mysql-files/
ENABLE_SSL=false
SSL_CERT_PATH=./ssl/server-cert.pem
SSL_KEY_PATH=./ssl/server-key.pem
SSL_CA_PATH=./ssl/ca-cert.pem
```

### 3. 目录结构创建脚本

```bash
#!/bin/bash
# scripts/setup.sh - 初始化目录结构

set -e

echo "正在创建 MySQL 集群目录结构..."

# 创建主要目录
mkdir -p {data,config,logs,backups,scripts,ssl}/{master,slave1,slave2,proxysql,redis,prometheus,grafana}

# 创建数据目录
mkdir -p data/{master,slave1,slave2,proxysql,redis,prometheus,grafana}

# 创建配置目录
mkdir -p config/{mysql/{master,slave1,slave2},proxysql,redis,prometheus,grafana/{dashboards,datasources},phpmyadmin}

# 创建日志目录
mkdir -p logs/{master,slave1,slave2,proxysql}

# 创建备份目录
mkdir -p backups/{daily,weekly,monthly}

# 创建脚本目录
mkdir -p scripts/{backup,monitoring,maintenance}

# 设置权限
chmod -R 755 data config logs backups scripts

# 创建MySQL配置文件
cat > config/mysql/master/my.cnf << 'EOF'
[mysqld]
# 服务器ID，主从复制必需
server-id = 1

# 二进制日志配置
log-bin = mysql-bin
binlog-format = ROW
binlog-do-db = appdb

# GTID配置
gtid-mode = ON
enforce-gtid-consistency = ON
log-slave-updates = ON

# 字符集配置
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 连接配置
max-connections = 500
max-connect-errors = 1000

# InnoDB配置
innodb-buffer-pool-size = 1G
innodb-log-file-size = 256M
innodb-flush-log-at-trx-commit = 1
innodb-file-per-table = 1

# 查询缓存
query-cache-type = 1
query-cache-size = 256M

# 慢查询日志
slow-query-log = 1
slow-query-log-file = /var/log/mysql/slow.log
long-query-time = 2

# 错误日志
log-error = /var/log/mysql/error.log

# 临时表配置
tmp-table-size = 64M
max-heap-table-size = 64M

# 安全配置
local-infile = 0
secure-file-priv = /var/lib/mysql-files/
EOF

# 复制从节点配置
cp config/mysql/master/my.cnf config/mysql/slave1/my.cnf
cp config/mysql/master/my.cnf config/mysql/slave2/my.cnf

# 修改从节点配置
sed -i 's/server-id = 1/server-id = 2/' config/mysql/slave1/my.cnf
sed -i 's/server-id = 1/server-id = 3/' config/mysql/slave2/my.cnf

# 添加从节点特有配置
echo "" >> config/mysql/slave1/my.cnf
echo "# 从节点配置" >> config/mysql/slave1/my.cnf
echo "read-only = 1" >> config/mysql/slave1/my.cnf
echo "relay-log = relay-log" >> config/mysql/slave1/my.cnf

echo "" >> config/mysql/slave2/my.cnf
echo "# 从节点配置" >> config/mysql/slave2/my.cnf
echo "read-only = 1" >> config/mysql/slave2/my.cnf
echo "relay-log = relay-log" >> config/mysql/slave2/my.cnf

echo "✅ 目录结构创建完成！"
echo "📁 数据目录: $(pwd)/data"
echo "⚙️ 配置目录: $(pwd)/config"
echo "📝 日志目录: $(pwd)/logs"
echo "💾 备份目录: $(pwd)/backups"
echo "🔧 脚本目录: $(pwd)/scripts"
```

## 主从复制配置

### 1. 自动配置脚本

```bash
#!/bin/bash
# scripts/setup-replication.sh - 配置主从复制

set -e

echo "🔧 开始配置 MySQL 主从复制..."

# 等待MySQL启动
echo "⏳ 等待 MySQL 服务启动..."
sleep 30

# 在主节点创建复制用户
echo "👤 创建复制用户..."
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
CREATE USER IF NOT EXISTS 'replicator'@'%' IDENTIFIED BY '${MYSQL_REPLICATION_PASSWORD}';
GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';
GRANT REPLICATION CLIENT ON *.* TO 'replicator'@'%';
FLUSH PRIVILEGES;
"

# 创建监控用户
echo "📊 创建监控用户..."
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY '${MYSQL_EXPORTER_PASSWORD}';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
FLUSH PRIVILEGES;
"

# 获取主节点状态
echo "📋 获取主节点状态..."
MASTER_STATUS=$(docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW MASTER STATUS\G")
echo "$MASTER_STATUS"

# 配置从节点1
echo "🔗 配置从节点 1..."
docker-compose exec mysql-slave1 mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_USER='replicator',
    MASTER_PASSWORD='${MYSQL_REPLICATION_PASSWORD}',
    MASTER_AUTO_POSITION=1;
START SLAVE;
"

# 配置从节点2
echo "🔗 配置从节点 2..."
docker-compose exec mysql-slave2 mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_USER='replicator',
    MASTER_PASSWORD='${MYSQL_REPLICATION_PASSWORD}',
    MASTER_AUTO_POSITION=1;
START SLAVE;
"

# 检查从节点状态
echo "✅ 检查从节点状态..."
echo "=== 从节点 1 状态 ==="
docker-compose exec mysql-slave1 mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G" | grep -E "(Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master)"

echo "=== 从节点 2 状态 ==="
docker-compose exec mysql-slave2 mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G" | grep -E "(Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master)"

echo "🎉 主从复制配置完成！"
```

### 2. 复制状态监控脚本

```bash
#!/bin/bash
# scripts/check-replication.sh - 检查主从复制状态

echo "🔍 检查 MySQL 主从复制状态..."

# 检查主节点状态
echo "==================== 主节点状态 ===================="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT
    VARIABLE_NAME as 'Status',
    VARIABLE_VALUE as 'Value'
FROM INFORMATION_SCHEMA.SESSION_STATUS
WHERE VARIABLE_NAME IN (
    'UPTIME',
    'THREADS_CONNECTED',
    'THREADS_RUNNING',
    'INNODB_BUFFER_POOL_PAGES_DATA',
    'INNODB_BUFFER_POOL_PAGES_FREE'
);
"

echo -e "\n================== 主节点 GTID 状态 =================="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SHOW VARIABLES LIKE 'gtid%';
"

# 检查从节点状态
for slave in slave1 slave2; do
    echo -e "\n==================== 从节点 $slave 状态 ===================="
    
    # 基本复制状态
    docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    SHOW SLAVE STATUS\G" | grep -E "(Master_Host|Master_User|Master_Port|Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master|Last_Error|Retrieved_Gtid_Set|Executed_Gtid_Set)"
    
    # 检查错误日志
    echo -e "\n--- $slave 最近错误 ---"
    docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    SELECT * FROM performance_schema.replication_connection_status;
    SELECT * FROM performance_schema.replication_applier_status;
    " 2>/dev/null || echo "无法获取性能模式状态"
done

# 测试数据同步
echo -e "\n==================== 数据同步测试 ===================="
echo "在主节点创建测试表..."
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
USE ${MYSQL_DATABASE};
CREATE TABLE IF NOT EXISTS replication_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    test_data VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO replication_test (test_data) VALUES ('Replication test $(date)');
"

echo "等待同步..."
sleep 5

echo "检查从节点数据同步..."
for slave in slave1 slave2; do
    echo "--- 从节点 $slave ---"
    docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    USE ${MYSQL_DATABASE};
    SELECT COUNT(*) as record_count FROM replication_test;
    SELECT * FROM replication_test ORDER BY id DESC LIMIT 1;
    "
done

echo -e "\n✅ 主从复制状态检查完成！"
```

### 3. ProxySQL 配置

```sql
-- config/proxysql/proxysql.cnf
datadir="/var/lib/proxysql"

admin_variables=
{
    admin_credentials="admin:admin;cluster1:secret1"
    mysql_ifaces="0.0.0.0:6032"
    refresh_interval=2000
    debug=true
}

mysql_variables=
{
    threads=4
    max_connections=2048
    default_query_delay=0
    default_query_timeout=36000000
    have_compress=true
    poll_timeout=2000
    interfaces="0.0.0.0:6033"
    default_schema="information_schema"
    stacksize=1048576
    server_version="8.0.35"
    connect_timeout_server=3000
    monitor_username="monitor"
    monitor_password="monitor"
    monitor_history=600000
    monitor_connect_interval=60000
    monitor_ping_interval=10000
    monitor_read_only_interval=1500
    monitor_read_only_timeout=500
    ping_interval_server_msec=120000
    ping_timeout_server=500
    commands_stats=true
    sessions_sort=true
    connect_retries_on_failure=10
}

# 定义 MySQL 服务器
mysql_servers=
(
    {
        address="mysql-master"
        port=3306
        hostgroup=0    # 写组
        weight=1000
        status="ONLINE"
        compression=0
        max_connections=500
        max_replication_lag=0
        use_ssl=0
        max_latency_ms=0
        comment="MySQL Master - Write"
    },
    {
        address="mysql-slave1"
        port=3306
        hostgroup=1    # 读组
        weight=800
        status="ONLINE"
        compression=0
        max_connections=300
        max_replication_lag=10
        use_ssl=0
        max_latency_ms=0
        comment="MySQL Slave 1 - Read"
    },
    {
        address="mysql-slave2"
        port=3306
        hostgroup=1    # 读组
        weight=800
        status="ONLINE"
        compression=0
        max_connections=300
        max_replication_lag=10
        use_ssl=0
        max_latency_ms=0
        comment="MySQL Slave 2 - Read"
    }
)

# 定义用户
mysql_users=
(
    {
        username="appuser"
        password="SecureAppPassword123!"
        default_hostgroup=0
        max_connections=200
        default_schema="appdb"
        active=1
        comment="Application User"
    },
    {
        username="root"
        password="SecureRootPassword123!"
        default_hostgroup=0
        max_connections=50
        default_schema="information_schema"
        active=1
        comment="Root User"
    }
)

# 查询路由规则
mysql_query_rules=
(
    {
        rule_id=1
        active=1
        match_pattern="^SELECT.*FOR UPDATE$"
        destination_hostgroup=0
        apply=1
        comment="Send SELECT FOR UPDATE to master"
    },
    {
        rule_id=2
        active=1
        match_pattern="^SELECT"
        destination_hostgroup=1
        apply=1
        comment="Send SELECT to slaves"
    }
)

# 调度器
scheduler=
(
    {
        id=1
        active=1
        interval_ms=10000
        filename="/var/lib/proxysql/monitor_read_only.sh"
        arg1="0"
        arg2="1"
        comment="Monitor read-only status"
    }
)
```

## 多数据库支持

### 1. 数据库选择配置

```yaml
# docker-compose.override.yml - 多数据库配置
version: '3.8'

services:
  # ================================
  # MariaDB 主节点 (可选)
  # ================================
  mariadb-master:
    image: mariadb:10.11
    container_name: mariadb-master
    restart: unless-stopped
    profiles: ["mariadb"]
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    command: >
      --server-id=11
      --log-bin=mariadb-bin
      --binlog-format=ROW
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --max-connections=500
      --innodb-buffer-pool-size=1G
    volumes:
      - mariadb_master_data:/var/lib/mysql
      - ./config/mariadb/master/my.cnf:/etc/mysql/conf.d/my.cnf:ro
      - ./logs/mariadb-master:/var/log/mysql
    ports:
      - "3316:3306"
    networks:
      - mysql_cluster

  # ================================
  # Percona Server (可选)
  # ================================
  percona-master:
    image: percona:8.0.35
    container_name: percona-master
    restart: unless-stopped
    profiles: ["percona"]
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    command: >
      --server-id=21
      --log-bin=percona-bin
      --binlog-format=ROW
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --max-connections=500
      --innodb-buffer-pool-size=1G
    volumes:
      - percona_master_data:/var/lib/mysql
      - ./config/percona/master/my.cnf:/etc/mysql/conf.d/my.cnf:ro
      - ./logs/percona-master:/var/log/mysql
    ports:
      - "3326:3306"
    networks:
      - mysql_cluster

volumes:
  mariadb_master_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/mariadb-master
  
  percona_master_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/percona-master
```

### 2. 数据库选择脚本

```bash
#!/bin/bash
# scripts/select-database.sh - 数据库类型选择器

set -e

echo "🗄️ MySQL 集群数据库选择器"
echo "========================="
echo "请选择要部署的数据库类型："
echo "1) MySQL 8.0 (默认)"
echo "2) MariaDB 10.11"
echo "3) Percona Server 8.0"
echo "4) 混合部署 (MySQL + MariaDB)"
echo "5) 全部部署"
echo ""

read -p "请输入选择 (1-5) [默认: 1]: " choice
choice=${choice:-1}

case $choice in
    1)
        echo "✅ 选择: MySQL 8.0"
        COMPOSE_PROFILES=""
        ;;
    2)
        echo "✅ 选择: MariaDB 10.11"
        COMPOSE_PROFILES="mariadb"
        ;;
    3)
        echo "✅ 选择: Percona Server 8.0"
        COMPOSE_PROFILES="percona"
        ;;
    4)
        echo "✅ 选择: 混合部署 (MySQL + MariaDB)"
        COMPOSE_PROFILES="mariadb"
        ;;
    5)
        echo "✅ 选择: 全部部署"
        COMPOSE_PROFILES="mariadb,percona"
        ;;
    *)
        echo "❌ 无效选择，使用默认 MySQL 8.0"
        COMPOSE_PROFILES=""
        ;;
esac

# 写入环境变量
echo "COMPOSE_PROFILES=$COMPOSE_PROFILES" >> .env
echo "DATABASE_CHOICE=$choice" >> .env

echo ""
echo "📝 配置已保存到 .env 文件"
echo "🚀 现在可以运行: docker-compose up -d"

# 根据选择显示端口信息
echo ""
echo "📡 服务端口信息："
echo "MySQL Master: 3306"
echo "MySQL Slave1: 3307"
echo "MySQL Slave2: 3308"

if [[ "$COMPOSE_PROFILES" =~ "mariadb" ]]; then
    echo "MariaDB Master: 3316"
fi

if [[ "$COMPOSE_PROFILES" =~ "percona" ]]; then
    echo "Percona Master: 3326"
fi

echo "ProxySQL: 6033"
echo "phpMyAdmin: 8080"
echo "Adminer: 8081"
echo "Grafana: 3000"
echo "Prometheus: 9090"
```

### 3. 数据库连接配置

```php
<?php
// config/database-connections.php - 多数据库连接配置

return [
    'default' => 'mysql',
    
    'connections' => [
        'mysql' => [
            'driver' => 'mysql',
            'host' => 'mysql-master',
            'port' => '3306',
            'database' => 'appdb',
            'username' => 'appuser',
            'password' => 'SecureAppPassword123!',
            'unix_socket' => '',
            'charset' => 'utf8mb4',
            'collation' => 'utf8mb4_unicode_ci',
            'prefix' => '',
            'strict' => true,
            'engine' => 'InnoDB',
            'options' => [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ],
        ],
        
        'mysql_readonly' => [
            'driver' => 'mysql',
            'host' => 'proxysql',
            'port' => '6033',
            'database' => 'appdb',
            'username' => 'appuser',
            'password' => 'SecureAppPassword123!',
            'read' => [
                'host' => ['mysql-slave1', 'mysql-slave2'],
            ],
            'write' => [
                'host' => ['mysql-master'],
            ],
            'sticky' => true,
            'charset' => 'utf8mb4',
            'collation' => 'utf8mb4_unicode_ci',
        ],
        
        'mariadb' => [
            'driver' => 'mysql',
            'host' => 'mariadb-master',
            'port' => '3306',
            'database' => 'appdb',
            'username' => 'appuser',
            'password' => 'SecureAppPassword123!',
            'charset' => 'utf8mb4',
            'collation' => 'utf8mb4_unicode_ci',
        ],
        
        'percona' => [
            'driver' => 'mysql',
            'host' => 'percona-master',
            'port' => '3306',
            'database' => 'appdb',
            'username' => 'appuser',
            'password' => 'SecureAppPassword123!',
            'charset' => 'utf8mb4',
            'collation' => 'utf8mb4_unicode_ci',
        ],
    ],
    
    'redis' => [
        'client' => 'phpredis',
        'default' => [
            'scheme' => 'tcp',
            'host' => 'redis',
            'password' => 'SecureRedisPassword123!',
            'port' => 6379,
            'database' => 0,
        ],
        'cache' => [
            'scheme' => 'tcp',
            'host' => 'redis',
            'password' => 'SecureRedisPassword123!',
            'port' => 6379,
            'database' => 1,
        ],
        'session' => [
            'scheme' => 'tcp',
            'host' => 'redis',
            'password' => 'SecureRedisPassword123!',
            'port' => 6379,
            'database' => 2,
        ],
    ],
];
```

### 4. 数据库迁移脚本

```bash
#!/bin/bash
# scripts/migrate-databases.sh - 数据库迁移工具

set -e

SOURCE_DB=""
TARGET_DB=""
OPERATION=""

show_help() {
    echo "数据库迁移工具"
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -s, --source    源数据库 (mysql|mariadb|percona)"
    echo "  -t, --target    目标数据库 (mysql|mariadb|percona)"
    echo "  -o, --operation 操作类型 (migrate|sync|compare)"
    echo "  -h, --help      显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 -s mysql -t mariadb -o migrate"
    echo "  $0 -s mysql -t percona -o sync"
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--source)
            SOURCE_DB="$2"
            shift 2
            ;;
        -t|--target)
            TARGET_DB="$2"
            shift 2
            ;;
        -o|--operation)
            OPERATION="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 验证参数
if [[ -z "$SOURCE_DB" || -z "$TARGET_DB" || -z "$OPERATION" ]]; then
    echo "❌ 缺少必需参数"
    show_help
    exit 1
fi

# 数据库端口映射
get_db_port() {
    case $1 in
        mysql) echo "3306" ;;
        mariadb) echo "3316" ;;
        percona) echo "3326" ;;
        *) echo "3306" ;;
    esac
}

# 获取数据库容器名
get_container_name() {
    case $1 in
        mysql) echo "mysql-master" ;;
        mariadb) echo "mariadb-master" ;;
        percona) echo "percona-master" ;;
        *) echo "mysql-master" ;;
    esac
}

SOURCE_PORT=$(get_db_port $SOURCE_DB)
TARGET_PORT=$(get_db_port $TARGET_DB)
SOURCE_CONTAINER=$(get_container_name $SOURCE_DB)
TARGET_CONTAINER=$(get_container_name $TARGET_DB)

echo "🔄 数据库迁移操作"
echo "源数据库: $SOURCE_DB ($SOURCE_CONTAINER:$SOURCE_PORT)"
echo "目标数据库: $TARGET_DB ($TARGET_CONTAINER:$TARGET_PORT)"
echo "操作类型: $OPERATION"
echo ""

case $OPERATION in
    migrate)
        echo "🚀 开始完整迁移..."
        
        # 1. 导出源数据库
        echo "📦 导出源数据库结构和数据..."
        docker-compose exec $SOURCE_CONTAINER mysqldump \
            -uroot -p${MYSQL_ROOT_PASSWORD} \
            --single-transaction \
            --routines \
            --triggers \
            --all-databases > /tmp/migration_${SOURCE_DB}_to_${TARGET_DB}.sql
        
        # 2. 导入到目标数据库
        echo "📥 导入到目标数据库..."
        docker-compose exec -T $TARGET_CONTAINER mysql \
            -uroot -p${MYSQL_ROOT_PASSWORD} < /tmp/migration_${SOURCE_DB}_to_${TARGET_DB}.sql
        
        echo "✅ 迁移完成！"
        ;;
        
    sync)
        echo "🔄 开始数据同步..."
        
        # 仅同步数据，不包括结构
        docker-compose exec $SOURCE_CONTAINER mysqldump \
            -uroot -p${MYSQL_ROOT_PASSWORD} \
            --single-transaction \
            --no-create-info \
            --skip-triggers \
            ${MYSQL_DATABASE} > /tmp/sync_${SOURCE_DB}_to_${TARGET_DB}.sql
        
        docker-compose exec -T $TARGET_CONTAINER mysql \
            -uroot -p${MYSQL_ROOT_PASSWORD} \
            ${MYSQL_DATABASE} < /tmp/sync_${SOURCE_DB}_to_${TARGET_DB}.sql
        
        echo "✅ 同步完成！"
        ;;
        
    compare)
        echo "🔍 比较数据库差异..."
        
        # 比较表结构
        echo "=== 表结构比较 ==="
        echo "源数据库表："
        docker-compose exec $SOURCE_CONTAINER mysql \
            -uroot -p${MYSQL_ROOT_PASSWORD} \
            -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='${MYSQL_DATABASE}'" \
            ${MYSQL_DATABASE}
        
        echo "目标数据库表："
        docker-compose exec $TARGET_CONTAINER mysql \
            -uroot -p${MYSQL_ROOT_PASSWORD} \
            -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='${MYSQL_DATABASE}'" \
            ${MYSQL_DATABASE}
        
        # 比较记录数
        echo "=== 记录数比较 ==="
        # 这里可以添加更详细的比较逻辑
        
        echo "✅ 比较完成！"
        ;;
        
    *)
        echo "❌ 不支持的操作类型: $OPERATION"
        exit 1
        ;;
esac

# 清理临时文件
rm -f /tmp/migration_*.sql /tmp/sync_*.sql

echo "🎉 操作完成！"

## 管理工具

### 1. phpMyAdmin 高级配置

```php
<?php
// config/phpmyadmin/config.user.inc.php
declare(strict_types=1);

/**
 * phpMyAdmin 自定义配置
 */

/* 服务器配置 */
$cfg['Servers'][1]['host'] = 'mysql-master';
$cfg['Servers'][1]['port'] = 3306;
$cfg['Servers'][1]['auth_type'] = 'cookie';
$cfg['Servers'][1]['user'] = '';
$cfg['Servers'][1]['password'] = '';
$cfg['Servers'][1]['compress'] = false;
$cfg['Servers'][1]['AllowNoPassword'] = false;

/* 从节点配置 */
$cfg['Servers'][2]['host'] = 'mysql-slave1';
$cfg['Servers'][2]['port'] = 3306;
$cfg['Servers'][2]['auth_type'] = 'cookie';
$cfg['Servers'][2]['verbose'] = 'MySQL Slave 1 (只读)';
$cfg['Servers'][2]['only_db'] = array('appdb');

$cfg['Servers'][3]['host'] = 'mysql-slave2';
$cfg['Servers'][3]['port'] = 3306;
$cfg['Servers'][3]['auth_type'] = 'cookie';
$cfg['Servers'][3]['verbose'] = 'MySQL Slave 2 (只读)';
$cfg['Servers'][3]['only_db'] = array('appdb');

/* 通用配置 */
$cfg['DefaultLang'] = 'zh_CN';
$cfg['ServerDefault'] = 1;
$cfg['UploadDir'] = '';
$cfg['SaveDir'] = '';
$cfg['MaxSizeForInputField'] = 0;

/* 导入/导出设置 */
$cfg['MemoryLimit'] = '512M';
$cfg['ExecTimeLimit'] = 600;
$cfg['MaxInputVars'] = 10000;

/* 界面配置 */
$cfg['ThemeDefault'] = 'pmahomme';
$cfg['NavigationTreePointerEnable'] = true;
$cfg['BrowsePointerEnable'] = true;
$cfg['BrowseMarkerEnable'] = true;
$cfg['TextareaRows'] = 25;
$cfg['TextareaCols'] = 80;
$cfg['LongtextDoubleTextarea'] = true;
$cfg['TextareaAutoSelect'] = true;

/* 安全设置 */
$cfg['CookieSameSite'] = 'Strict';
$cfg['CSPAllow'] = 'none';
$cfg['DisableMultiTableMaintenance'] = false;

/* 主从复制监控 */
$cfg['ReplicationShowSlaveHosts'] = true;
$cfg['ShowServerInfo'] = true;
$cfg['ShowPhpInfo'] = true;
$cfg['ShowChgPassword'] = true;
$cfg['ShowCreateDb'] = true;

/* 查询历史 */
$cfg['QueryHistoryDB'] = true;
$cfg['QueryHistoryMax'] = 100;

/* 用户偏好存储 */
$cfg['UserprefsDisallow'] = array();

/* 设计器 */
$cfg['RelationDisableWarning'] = true;

/* 2FA 支持 */
$cfg['TwoFactorAuthentication'] = false;
?>
```

### 2. 数据库管理脚本

```bash
#!/bin/bash
# scripts/db-management.sh - 数据库管理工具

set -e

# 显示帮助信息
show_help() {
    echo "MySQL 集群管理工具"
    echo "用法: $0 [命令] [选项]"
    echo ""
    echo "命令:"
    echo "  status          显示集群状态"
    echo "  health          健康检查"
    echo "  backup          创建备份"
    echo "  restore         恢复备份"
    echo "  optimize        优化数据库"
    echo "  monitor         实时监控"
    echo "  failover        故障转移"
    echo "  maintenance     维护模式"
    echo ""
    echo "示例:"
    echo "  $0 status"
    echo "  $0 backup --full"
    echo "  $0 restore --file backup_20231201.sql"
}

# 检查集群状态
check_status() {
    echo "🔍 MySQL 集群状态检查"
    echo "======================"
    
    # 检查容器状态
    echo "📦 容器状态:"
    docker-compose ps
    
    echo -e "\n💾 存储使用情况:"
    df -h | grep -E "(mysql|data)"
    
    echo -e "\n🔗 网络连通性:"
    for service in mysql-master mysql-slave1 mysql-slave2 proxysql; do
        if docker-compose exec $service ping -c 1 mysql-master >/dev/null 2>&1; then
            echo "✅ $service 网络正常"
        else
            echo "❌ $service 网络异常"
        fi
    done
    
    echo -e "\n📊 数据库连接测试:"
    for port in 3306 3307 3308 6033; do
        if timeout 5 bash -c "</dev/tcp/localhost/$port" 2>/dev/null; then
            echo "✅ 端口 $port 可访问"
        else
            echo "❌ 端口 $port 不可访问"
        fi
    done
}

# 健康检查
health_check() {
    echo "🏥 集群健康检查"
    echo "==============="
    
    # 检查主节点
    echo "🔍 检查主节点状态..."
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    SELECT 
        'Master Status' as Check_Type,
        CASE 
            WHEN @@read_only = 0 THEN 'Read-Write (正常)'
            ELSE 'Read-Only (异常)'
        END as Status;
    
    SHOW MASTER STATUS;
    "
    
    # 检查从节点
    for slave in slave1 slave2; do
        echo -e "\n🔍 检查从节点 $slave 状态..."
        docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
        SELECT 
            'Slave $slave Status' as Check_Type,
            CASE 
                WHEN @@read_only = 1 THEN 'Read-Only (正常)'
                ELSE 'Read-Write (异常)'
            END as Status;
        
        SHOW SLAVE STATUS\G" | grep -E "(Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master|Last_Error)"
    done
    
    # 检查ProxySQL
    echo -e "\n🔍 检查 ProxySQL 状态..."
    docker-compose exec proxysql mysql -h127.0.0.1 -P6032 -uadmin -padmin -e "
    SELECT hostgroup_id, hostname, port, status, weight FROM mysql_servers;
    SELECT username, default_hostgroup, max_connections FROM mysql_users;
    "
}

# 数据库优化
optimize_database() {
    echo "⚡ 数据库优化"
    echo "============"
    
    # 获取所有表
    tables=$(docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "
    SELECT CONCAT(table_schema,'.',table_name) 
    FROM information_schema.tables 
    WHERE table_schema NOT IN ('information_schema','performance_schema','mysql','sys')
    ")
    
    echo "🔧 优化表结构..."
    for table in $tables; do
        echo "优化表: $table"
        docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "OPTIMIZE TABLE $table;"
    done
    
    echo -e "\n📊 分析表统计信息..."
    for table in $tables; do
        echo "分析表: $table"
        docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "ANALYZE TABLE $table;"
    done
    
    echo -e "\n🧹 清理二进制日志..."
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "PURGE BINARY LOGS BEFORE DATE_SUB(NOW(), INTERVAL 3 DAY);"
    
    echo "✅ 数据库优化完成"
}

# 实时监控
real_time_monitor() {
    echo "📊 实时监控模式 (按 Ctrl+C 退出)"
    echo "==============================="
    
    while true; do
        clear
        echo "📅 $(date)"
        echo "=================="
        
        # 显示连接数
        echo "🔗 当前连接数:"
        docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
        SELECT 
            VARIABLE_NAME as Metric,
            VARIABLE_VALUE as Value
        FROM INFORMATION_SCHEMA.SESSION_STATUS 
        WHERE VARIABLE_NAME IN ('Threads_connected', 'Threads_running', 'Uptime')
        " 2>/dev/null
        
        # 显示复制延迟
        echo -e "\n⏰ 复制延迟:"
        for slave in slave1 slave2; do
            delay=$(docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G" 2>/dev/null | grep "Seconds_Behind_Master" | awk '{print $2}')
            echo "$slave: ${delay:-Unknown} 秒"
        done
        
        # 显示查询统计
        echo -e "\n📈 查询统计:"
        docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
        SELECT 
            VARIABLE_NAME as Query_Type,
            VARIABLE_VALUE as Count
        FROM INFORMATION_SCHEMA.SESSION_STATUS 
        WHERE VARIABLE_NAME LIKE 'Com_%' 
        AND VARIABLE_VALUE > 0
        ORDER BY CAST(VARIABLE_VALUE AS UNSIGNED) DESC
        LIMIT 10
        " 2>/dev/null
        
        sleep 5
    done
}

# 故障转移
failover() {
    echo "⚠️  故障转移程序"
    echo "==============="
    
    read -p "确认要执行故障转移吗？这将停止主节点服务 (y/N): " confirm
    if [[ $confirm != [yY] ]]; then
        echo "取消故障转移"
        return
    fi
    
    echo "🚨 开始故障转移..."
    
    # 1. 停止主节点写入
    echo "1️⃣ 设置主节点为只读模式..."
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    SET GLOBAL read_only = ON;
    FLUSH TABLES WITH READ LOCK;
    "
    
    # 2. 等待从节点同步
    echo "2️⃣ 等待从节点同步完成..."
    sleep 10
    
    # 3. 提升从节点为主节点
    echo "3️⃣ 提升从节点1为新主节点..."
    docker-compose exec mysql-slave1 mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    STOP SLAVE;
    RESET SLAVE ALL;
    SET GLOBAL read_only = OFF;
    "
    
    # 4. 重新配置复制
    echo "4️⃣ 重新配置复制关系..."
    docker-compose exec mysql-slave2 mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    STOP SLAVE;
    CHANGE MASTER TO
        MASTER_HOST='mysql-slave1',
        MASTER_USER='replicator',
        MASTER_PASSWORD='${MYSQL_REPLICATION_PASSWORD}',
        MASTER_AUTO_POSITION=1;
    START SLAVE;
    "
    
    echo "✅ 故障转移完成！"
    echo "📝 请更新应用配置，将写入流量指向新主节点 mysql-slave1"
}

# 维护模式
maintenance_mode() {
    action=${1:-"status"}
    
    case $action in
        "on")
            echo "🔧 启用维护模式..."
            # 创建维护页面
            docker-compose exec nginx sh -c "echo '<h1>系统维护中</h1><p>预计维护时间: 30分钟</p>' > /usr/share/nginx/html/maintenance.html"
            # 设置数据库只读
            for db in mysql-master mysql-slave1 mysql-slave2; do
                docker-compose exec $db mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SET GLOBAL read_only = ON;"
            done
            echo "✅ 维护模式已启用"
            ;;
        "off")
            echo "🔧 禁用维护模式..."
            # 恢复数据库写入
            docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SET GLOBAL read_only = OFF;"
            echo "✅ 维护模式已禁用"
            ;;
        "status")
            echo "🔍 维护模式状态..."
            readonly=$(docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "SELECT @@read_only;")
            if [[ "$readonly" == "1" ]]; then
                echo "🔧 当前处于维护模式"
            else
                echo "▶️ 当前处于正常运行模式"
            fi
            ;;
    esac
}

# 主函数
main() {
    case ${1:-"help"} in
        "status")
            check_status
            ;;
        "health")
            health_check
            ;;
        "backup")
            # 将在下面的备份部分实现
            echo "备份功能请使用 ./scripts/backup.sh"
            ;;
        "restore")
            # 将在下面的恢复部分实现
            echo "恢复功能请使用 ./scripts/restore.sh"
            ;;
        "optimize")
            optimize_database
            ;;
        "monitor")
            real_time_monitor
            ;;
        "failover")
            failover
            ;;
        "maintenance")
            maintenance_mode ${2:-"status"}
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 检查环境
if [[ ! -f ".env" ]]; then
    echo "❌ 未找到 .env 文件，请先运行 setup.sh"
    exit 1
fi

# 加载环境变量
source .env

# 执行主函数
main "$@"
```

### 3. 数据库用户管理

```bash
#!/bin/bash
# scripts/user-management.sh - 用户权限管理

set -e

# 创建应用用户
create_app_user() {
    username=$1
    password=$2
    database=$3
    permissions=${4:-"SELECT,INSERT,UPDATE,DELETE"}
    
    echo "👤 创建应用用户: $username"
    
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    CREATE USER IF NOT EXISTS '$username'@'%' IDENTIFIED BY '$password';
    GRANT $permissions ON $database.* TO '$username'@'%';
    FLUSH PRIVILEGES;
    "
    
    echo "✅ 用户 $username 创建完成"
}

# 创建只读用户
create_readonly_user() {
    username=$1
    password=$2
    database=$3
    
    echo "👁️ 创建只读用户: $username"
    
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    CREATE USER IF NOT EXISTS '$username'@'%' IDENTIFIED BY '$password';
    GRANT SELECT ON $database.* TO '$username'@'%';
    FLUSH PRIVILEGES;
    "
    
    echo "✅ 只读用户 $username 创建完成"
}

# 创建备份用户
create_backup_user() {
    username="backup_user"
    password=$1
    
    echo "💾 创建备份用户: $username"
    
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    CREATE USER IF NOT EXISTS '$username'@'%' IDENTIFIED BY '$password';
    GRANT SELECT, LOCK TABLES, SHOW VIEW, EVENT, TRIGGER, RELOAD ON *.* TO '$username'@'%';
    GRANT REPLICATION CLIENT ON *.* TO '$username'@'%';
    FLUSH PRIVILEGES;
    "
    
    echo "✅ 备份用户 $username 创建完成"
}

# 列出所有用户
list_users() {
    echo "👥 数据库用户列表"
    echo "================="
    
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    SELECT 
        User as '用户名',
        Host as '主机',
        account_locked as '锁定状态',
        password_expired as '密码过期',
        password_last_changed as '密码最后修改'
    FROM mysql.user 
    WHERE User NOT IN ('mysql.sys', 'mysql.session', 'mysql.infoschema')
    ORDER BY User;
    "
}

# 显示用户权限
show_user_privileges() {
    username=$1
    
    echo "🔐 用户权限: $username"
    echo "==================="
    
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    SHOW GRANTS FOR '$username'@'%';
    "
}

# 重置用户密码
reset_password() {
    username=$1
    new_password=$2
    
    echo "🔑 重置用户密码: $username"
    
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    ALTER USER '$username'@'%' IDENTIFIED BY '$new_password';
    FLUSH PRIVILEGES;
    "
    
    echo "✅ 用户 $username 密码重置完成"
}

# 删除用户
delete_user() {
    username=$1
    
    read -p "确认要删除用户 $username 吗？ (y/N): " confirm
    if [[ $confirm != [yY] ]]; then
        echo "取消删除"
        return
    fi
    
    echo "🗑️ 删除用户: $username"
    
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    DROP USER IF EXISTS '$username'@'%';
    FLUSH PRIVILEGES;
    "
    
    echo "✅ 用户 $username 已删除"
}

# 主函数
case ${1:-"help"} in
    "create")
        create_app_user "$2" "$3" "$4" "$5"
        ;;
    "readonly")
        create_readonly_user "$2" "$3" "$4"
        ;;
    "backup")
        create_backup_user "$2"
        ;;
    "list")
        list_users
        ;;
    "show")
        show_user_privileges "$2"
        ;;
    "reset")
        reset_password "$2" "$3"
        ;;
    "delete")
        delete_user "$2"
        ;;
    *)
        echo "用户管理工具"
        echo "用法: $0 [命令] [参数]"
        echo ""
        echo "命令:"
        echo "  create <用户名> <密码> <数据库> [权限]  创建应用用户"
        echo "  readonly <用户名> <密码> <数据库>      创建只读用户"
        echo "  backup <密码>                        创建备份用户"
        echo "  list                                 列出所有用户"
        echo "  show <用户名>                        显示用户权限"
        echo "  reset <用户名> <新密码>              重置密码"
        echo "  delete <用户名>                      删除用户"
        ;;
esac
```

## 监控系统

### 1. Prometheus 配置

```yaml
# config/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'mysql-cluster'
    environment: 'production'

rule_files:
  - "/etc/prometheus/rules/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # Prometheus 自身监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
    metrics_path: /metrics
    scrape_interval: 15s

  # MySQL 主节点监控
  - job_name: 'mysql-master'
    static_configs:
      - targets: ['mysql-exporter:9104']
    metrics_path: /metrics
    scrape_interval: 30s
    params:
      target: ['mysql-master:3306']
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: mysql-exporter:9104

  # MySQL 从节点监控
  - job_name: 'mysql-slaves'
    static_configs:
      - targets: 
        - 'mysql-slave1:3306'
        - 'mysql-slave2:3306'
    metrics_path: /metrics
    scrape_interval: 30s
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'mysql_slave_.*'
        target_label: role
        replacement: 'slave'

  # ProxySQL 监控
  - job_name: 'proxysql'
    static_configs:
      - targets: ['proxysql:6032']
    metrics_path: /metrics
    scrape_interval: 30s

  # Redis 监控
  - job_name: 'redis'
    static_configs:
      - targets: ['redis:6379']
    metrics_path: /metrics
    scrape_interval: 30s

  # Node Exporter (系统监控)
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
    metrics_path: /metrics
    scrape_interval: 30s

  # cAdvisor (容器监控)
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
    metrics_path: /metrics
    scrape_interval: 30s
```

### 2. 告警规则配置

```yaml
# config/prometheus/rules/mysql_alerts.yml
groups:
  - name: mysql_alerts
    rules:
      # MySQL 服务可用性
      - alert: MySQLDown
        expr: mysql_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "MySQL instance is down"
          description: "MySQL instance {{ $labels.instance }} has been down for more than 1 minute."

      # 主从复制延迟
      - alert: MySQLReplicationLag
        expr: mysql_slave_lag_seconds > 30
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MySQL replication lag is high"
          description: "MySQL slave {{ $labels.instance }} is lagging {{ $value }} seconds behind master."

      # 主从复制停止
      - alert: MySQLReplicationStopped
        expr: mysql_slave_sql_running == 0 or mysql_slave_io_running == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "MySQL replication has stopped"
          description: "MySQL replication on {{ $labels.instance }} has stopped."

      # 连接数过高
      - alert: MySQLHighConnections
        expr: mysql_global_status_threads_connected / mysql_global_variables_max_connections > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MySQL connections usage is high"
          description: "MySQL instance {{ $labels.instance }} has {{ $value | humanizePercentage }} connections used."

      # 慢查询过多
      - alert: MySQLSlowQueries
        expr: rate(mysql_global_status_slow_queries[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MySQL slow queries rate is high"
          description: "MySQL instance {{ $labels.instance }} has {{ $value }} slow queries per second."

      # InnoDB 缓冲池命中率低
      - alert: MySQLInnoDBBufferPoolHitRate
        expr: mysql_global_status_innodb_buffer_pool_read_requests / (mysql_global_status_innodb_buffer_pool_read_requests + mysql_global_status_innodb_buffer_pool_reads) < 0.95
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MySQL InnoDB buffer pool hit rate is low"
          description: "MySQL instance {{ $labels.instance }} InnoDB buffer pool hit rate is {{ $value | humanizePercentage }}."

      # 磁盘空间不足
      - alert: MySQLDiskSpaceLow
        expr: (node_filesystem_avail_bytes{mountpoint="/var/lib/mysql"} / node_filesystem_size_bytes{mountpoint="/var/lib/mysql"}) < 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "MySQL disk space is running low"
          description: "MySQL data directory has less than 10% free space remaining."

  - name: redis_alerts
    rules:
      # Redis 服务可用性
      - alert: RedisDown
        expr: redis_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis instance is down"
          description: "Redis instance {{ $labels.instance }} has been down for more than 1 minute."

      # Redis 内存使用率高
      - alert: RedisMemoryUsageHigh
        expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis memory usage is high"
          description: "Redis instance {{ $labels.instance }} memory usage is {{ $value | humanizePercentage }}."

  - name: proxysql_alerts
    rules:
      # ProxySQL 连接失败
      - alert: ProxySQLConnectionFailures
        expr: rate(proxysql_connection_pool_conn_err[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "ProxySQL connection failures detected"
          description: "ProxySQL is experiencing {{ $value }} connection failures per second."
```

### 3. Grafana 仪表板配置

```json
# config/grafana/dashboards/mysql-overview.json
{
  "dashboard": {
    "id": null,
    "title": "MySQL Cluster Overview",
    "tags": ["mysql", "database", "cluster"],
    "timezone": "browser",
    "panels": [
      {
        "title": "MySQL 服务状态",
        "type": "stat",
        "targets": [
          {
            "expr": "mysql_up",
            "legendFormat": "{{instance}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "mappings": [
              {
                "options": {
                  "0": {
                    "text": "DOWN",
                    "color": "red"
                  },
                  "1": {
                    "text": "UP",
                    "color": "green"
                  }
                },
                "type": "value"
              }
            ]
          }
        }
      },
      {
        "title": "查询执行速率",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(mysql_global_status_queries[5m])",
            "legendFormat": "{{instance}} - Queries/sec"
          }
        ]
      },
      {
        "title": "连接数",
        "type": "graph",
        "targets": [
          {
            "expr": "mysql_global_status_threads_connected",
            "legendFormat": "{{instance}} - Connected"
          },
          {
            "expr": "mysql_global_status_threads_running",
            "legendFormat": "{{instance}} - Running"
          }
        ]
      },
      {
        "title": "主从复制延迟",
        "type": "graph",
        "targets": [
          {
            "expr": "mysql_slave_lag_seconds",
            "legendFormat": "{{instance}} - Lag (seconds)"
          }
        ]
      },
      {
        "title": "InnoDB 缓冲池",
        "type": "graph",
        "targets": [
          {
            "expr": "mysql_global_status_innodb_buffer_pool_pages_data * mysql_global_variables_innodb_page_size",
            "legendFormat": "{{instance}} - Data"
          },
          {
            "expr": "mysql_global_status_innodb_buffer_pool_pages_free * mysql_global_variables_innodb_page_size",
            "legendFormat": "{{instance}} - Free"
          }
        ]
      },
      {
        "title": "慢查询统计",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(mysql_global_status_slow_queries[5m])",
            "legendFormat": "{{instance}} - Slow Queries/sec"
          }
        ]
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
```

### 4. 自定义监控脚本

```bash
#!/bin/bash
# scripts/custom-monitoring.sh - 自定义监控指标收集

set -e

# 配置文件
METRICS_FILE="/tmp/mysql_custom_metrics.prom"
INTERVAL=${1:-60}

# 清理指标文件
> $METRICS_FILE

echo "📊 开始收集自定义监控指标..."

while true; do
    echo "# HELP mysql_custom_table_size_bytes Table size in bytes" >> $METRICS_FILE
    echo "# TYPE mysql_custom_table_size_bytes gauge" >> $METRICS_FILE
    
    # 收集表大小信息
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "
    SELECT 
        CONCAT('mysql_custom_table_size_bytes{database=\"', table_schema, '\",table=\"', table_name, '\"} ', 
               IFNULL(data_length + index_length, 0))
    FROM information_schema.tables 
    WHERE table_schema NOT IN ('information_schema','performance_schema','mysql','sys')
    AND table_type = 'BASE TABLE'
    " >> $METRICS_FILE
    
    echo "# HELP mysql_custom_database_size_bytes Database size in bytes" >> $METRICS_FILE
    echo "# TYPE mysql_custom_database_size_bytes gauge" >> $METRICS_FILE
    
    # 收集数据库大小信息
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "
    SELECT 
        CONCAT('mysql_custom_database_size_bytes{database=\"', table_schema, '\"} ', 
               SUM(IFNULL(data_length + index_length, 0)))
    FROM information_schema.tables 
    WHERE table_schema NOT IN ('information_schema','performance_schema','mysql','sys')
    GROUP BY table_schema
    " >> $METRICS_FILE
    
    echo "# HELP mysql_custom_replication_delay_seconds Replication delay in seconds" >> $METRICS_FILE
    echo "# TYPE mysql_custom_replication_delay_seconds gauge" >> $METRICS_FILE
    
    # 收集复制延迟信息
    for slave in slave1 slave2; do
        delay=$(docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "
        SELECT IFNULL(Seconds_Behind_Master, -1) 
        FROM INFORMATION_SCHEMA.REPLICA_HOST_STATUS 
        LIMIT 1
        " 2>/dev/null || echo "-1")
        
        echo "mysql_custom_replication_delay_seconds{instance=\"mysql-$slave\"} $delay" >> $METRICS_FILE
    done
    
    echo "# HELP mysql_custom_active_connections Current active connections" >> $METRICS_FILE
    echo "# TYPE mysql_custom_active_connections gauge" >> $METRICS_FILE
    
    # 收集活跃连接信息
    active_conn=$(docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "
    SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE COMMAND != 'Sleep'
    ")
    echo "mysql_custom_active_connections{instance=\"mysql-master\"} $active_conn" >> $METRICS_FILE
    
    echo "📊 指标收集完成，等待 $INTERVAL 秒..."
    sleep $INTERVAL
done
```

## 备份和恢复

### 1. 自动备份脚本

```bash
#!/bin/bash
# scripts/backup.sh - 自动备份脚本

set -e

# 配置变量
BACKUP_DIR="/backup"
DATE=$(date +"%Y%m%d_%H%M%S")
RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-15}
MYSQL_HOST="mysql-master"
MYSQL_PORT="3306"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a ${BACKUP_DIR}/backup.log
}

# 检查备份目录
check_backup_dir() {
    if [[ ! -d "$BACKUP_DIR" ]]; then
        mkdir -p "$BACKUP_DIR"
        log "✅ 创建备份目录: $BACKUP_DIR"
    fi
    
    # 检查磁盘空间
    available_space=$(df -BG "$BACKUP_DIR" | tail -1 | awk '{print $4}' | sed 's/G//')
    if [[ $available_space -lt 10 ]]; then
        log "⚠️ 警告: 备份目录可用空间不足 10GB"
    fi
}

# 全量备份
full_backup() {
    backup_file="${BACKUP_DIR}/full_backup_${DATE}.sql"
    
    log "🚀 开始全量备份..."
    log "📁 备份文件: $backup_file"
    
    # 执行备份
    docker-compose exec mysql-master mysqldump \
        --user=root \
        --password=${MYSQL_ROOT_PASSWORD} \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --hex-blob \
        --add-drop-database \
        --add-drop-table \
        --create-options \
        --disable-keys \
        --extended-insert \
        --quick \
        --lock-tables=false \
        --all-databases > "$backup_file"
    
    if [[ $? -eq 0 ]]; then
        # 压缩备份文件
        gzip "$backup_file"
        backup_file="${backup_file}.gz"
        
        # 计算文件大小和MD5
        file_size=$(du -h "$backup_file" | cut -f1)
        md5_hash=$(md5sum "$backup_file" | cut -d' ' -f1)
        
        log "✅ 全量备份完成"
        log "📊 文件大小: $file_size"
        log "🔒 MD5校验: $md5_hash"
        
        # 记录备份信息
        echo "$DATE|full|$backup_file|$file_size|$md5_hash" >> ${BACKUP_DIR}/backup_index.txt
    else
        log "❌ 全量备份失败"
        return 1
    fi
}

# 增量备份
incremental_backup() {
    backup_file="${BACKUP_DIR}/incremental_backup_${DATE}.sql"
    
    log "🔄 开始增量备份..."
    
    # 获取上次备份的 binlog 位置
    last_backup_info=$(tail -1 ${BACKUP_DIR}/backup_index.txt 2>/dev/null || echo "")
    
    if [[ -z "$last_backup_info" ]]; then
        log "⚠️ 未找到上次备份信息，执行全量备份"
        full_backup
        return
    fi
    
    # 备份 binlog 文件
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "FLUSH LOGS;"
    
    # 复制 binlog 文件
    binlog_dir="${BACKUP_DIR}/binlogs_${DATE}"
    mkdir -p "$binlog_dir"
    
    docker-compose exec mysql-master sh -c "cp /var/lib/mysql/mysql-bin.* /backup/binlogs_${DATE}/ 2>/dev/null || true"
    
    if [[ $? -eq 0 ]]; then
        tar -czf "${backup_file}.tar.gz" -C "$binlog_dir" .
        file_size=$(du -h "${backup_file}.tar.gz" | cut -f1)
        md5_hash=$(md5sum "${backup_file}.tar.gz" | cut -d' ' -f1)
        
        log "✅ 增量备份完成"
        log "📊 文件大小: $file_size"
        log "🔒 MD5校验: $md5_hash"
        
        # 记录备份信息
        echo "$DATE|incremental|${backup_file}.tar.gz|$file_size|$md5_hash" >> ${BACKUP_DIR}/backup_index.txt
        
        # 清理临时目录
        rm -rf "$binlog_dir"
    else
        log "❌ 增量备份失败"
        return 1
    fi
}

# 数据一致性备份
consistent_backup() {
    backup_file="${BACKUP_DIR}/consistent_backup_${DATE}.sql"
    
    log "🔒 开始数据一致性备份..."
    
    # 1. 停止所有写入操作
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    FLUSH TABLES WITH READ LOCK;
    SET GLOBAL read_only = ON;
    "
    
    # 2. 等待从节点同步
    log "⏳ 等待主从同步..."
    sleep 10
    
    # 3. 记录主节点状态
    master_status=$(docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW MASTER STATUS\G")
    echo "$master_status" > "${BACKUP_DIR}/master_status_${DATE}.txt"
    
    # 4. 执行备份
    docker-compose exec mysql-master mysqldump \
        --user=root \
        --password=${MYSQL_ROOT_PASSWORD} \
        --single-transaction \
        --master-data=2 \
        --flush-logs \
        --all-databases > "$backup_file"
    
    # 5. 恢复写入权限
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    SET GLOBAL read_only = OFF;
    UNLOCK TABLES;
    "
    
    if [[ $? -eq 0 ]]; then
        gzip "$backup_file"
        backup_file="${backup_file}.gz"
        
        file_size=$(du -h "$backup_file" | cut -f1)
        md5_hash=$(md5sum "$backup_file" | cut -d' ' -f1)
        
        log "✅ 数据一致性备份完成"
        log "📊 文件大小: $file_size"
        log "🔒 MD5校验: $md5_hash"
        
        echo "$DATE|consistent|$backup_file|$file_size|$md5_hash" >> ${BACKUP_DIR}/backup_index.txt
    else
        log "❌ 数据一致性备份失败"
        # 确保恢复写入权限
        docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
        SET GLOBAL read_only = OFF;
        UNLOCK TABLES;
        " 2>/dev/null || true
        return 1
    fi
}

# 清理旧备份
cleanup_old_backups() {
    log "🧹 清理 $RETENTION_DAYS 天前的备份文件..."
    
    find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
    find "$BACKUP_DIR" -name "*.tar.gz" -mtime +$RETENTION_DAYS -delete
    find "$BACKUP_DIR" -name "master_status_*.txt" -mtime +$RETENTION_DAYS -delete
    
    # 清理备份索引中的旧记录
    if [[ -f "${BACKUP_DIR}/backup_index.txt" ]]; then
        cutoff_date=$(date -d "$RETENTION_DAYS days ago" +"%Y%m%d")
        grep -v "^$cutoff_date" "${BACKUP_DIR}/backup_index.txt" > "${BACKUP_DIR}/backup_index.tmp" || true
        mv "${BACKUP_DIR}/backup_index.tmp" "${BACKUP_DIR}/backup_index.txt"
    fi
    
    log "✅ 清理完成"
}

# 验证备份
verify_backup() {
    backup_file=$1
    
    if [[ ! -f "$backup_file" ]]; then
        log "❌ 备份文件不存在: $backup_file"
        return 1
    fi
    
    log "🔍 验证备份文件: $backup_file"
    
    # 检查文件完整性
    if [[ "$backup_file" == *.gz ]]; then
        if gzip -t "$backup_file"; then
            log "✅ 备份文件压缩完整性检查通过"
        else
            log "❌ 备份文件压缩损坏"
            return 1
        fi
    fi
    
    # 检查SQL语法 (简单检查)
    if [[ "$backup_file" == *.sql.gz ]]; then
        if zcat "$backup_file" | head -100 | grep -q "CREATE\|INSERT\|DROP"; then
            log "✅ 备份文件SQL语法检查通过"
        else
            log "❌ 备份文件SQL语法异常"
            return 1
        fi
    fi
    
    log "✅ 备份验证完成"
}

# 备份到远程存储
upload_to_remote() {
    backup_file=$1
    
    # S3 上传 (需要配置 AWS CLI)
    if [[ -n "$AWS_S3_BUCKET" ]]; then
        log "☁️ 上传到 S3..."
        aws s3 cp "$backup_file" "s3://$AWS_S3_BUCKET/mysql-backups/" --storage-class STANDARD_IA
        if [[ $? -eq 0 ]]; then
            log "✅ S3 上传完成"
        else
            log "❌ S3 上传失败"
        fi
    fi
    
    # FTP 上传
    if [[ -n "$FTP_SERVER" ]]; then
        log "📤 上传到 FTP..."
        curl -T "$backup_file" "ftp://$FTP_USER:$FTP_PASSWORD@$FTP_SERVER/mysql-backups/"
        if [[ $? -eq 0 ]]; then
            log "✅ FTP 上传完成"
        else
            log "❌ FTP 上传失败"
        fi
    fi
}

# 发送通知
send_notification() {
    status=$1
    message=$2
    
    if [[ -n "$WEBHOOK_URL" ]]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"MySQL备份 $status: $message\"}" \
            "$WEBHOOK_URL"
    fi
    
    if [[ -n "$EMAIL_TO" ]]; then
        echo "$message" | mail -s "MySQL备份 $status" "$EMAIL_TO"
    fi
}

# 显示帮助
show_help() {
    echo "MySQL 备份工具"
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  full         执行全量备份"
    echo "  incremental  执行增量备份"
    echo "  consistent   执行数据一致性备份"
    echo "  cleanup      清理旧备份"
    echo "  verify FILE  验证备份文件"
    echo "  list         列出所有备份"
    echo "  help         显示帮助信息"
}

# 列出备份
list_backups() {
    log "📋 备份文件列表"
    echo "========================================"
    
    if [[ -f "${BACKUP_DIR}/backup_index.txt" ]]; then
        echo "日期时间        | 类型        | 文件大小 | MD5校验"
        echo "----------------------------------------"
        while IFS='|' read -r date type file size md5; do
            printf "%-15s | %-11s | %-8s | %s\n" "$date" "$type" "$size" "${md5:0:8}..."
        done < "${BACKUP_DIR}/backup_index.txt"
    else
        echo "暂无备份记录"
    fi
}

# 主函数
main() {
    check_backup_dir
    
    case ${1:-"help"} in
        "full")
            full_backup
            if [[ $? -eq 0 ]]; then
                verify_backup "${BACKUP_DIR}/full_backup_${DATE}.sql.gz"
                upload_to_remote "${BACKUP_DIR}/full_backup_${DATE}.sql.gz"
                send_notification "成功" "全量备份完成"
            else
                send_notification "失败" "全量备份失败"
            fi
            ;;
        "incremental")
            incremental_backup
            if [[ $? -eq 0 ]]; then
                send_notification "成功" "增量备份完成"
            else
                send_notification "失败" "增量备份失败"
            fi
            ;;
        "consistent")
            consistent_backup
            if [[ $? -eq 0 ]]; then
                verify_backup "${BACKUP_DIR}/consistent_backup_${DATE}.sql.gz"
                upload_to_remote "${BACKUP_DIR}/consistent_backup_${DATE}.sql.gz"
                send_notification "成功" "数据一致性备份完成"
            else
                send_notification "失败" "数据一致性备份失败"
            fi
            ;;
        "cleanup")
            cleanup_old_backups
            ;;
        "verify")
            if [[ -n "$2" ]]; then
                verify_backup "$2"
            else
                echo "请指定要验证的备份文件"
            fi
            ;;
        "list")
            list_backups
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 加载环境变量
if [[ -f ".env" ]]; then
    source .env
fi

# 执行主函数
main "$@"
```

### 2. 恢复脚本

```bash
#!/bin/bash
# scripts/restore.sh - 数据恢复脚本

set -e

BACKUP_DIR="/backup"
RESTORE_LOG="${BACKUP_DIR}/restore.log"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$RESTORE_LOG"
}

# 显示可用备份
list_available_backups() {
    echo "📋 可用备份列表"
    echo "==============="
    
    if [[ -f "${BACKUP_DIR}/backup_index.txt" ]]; then
        echo "编号 | 日期时间        | 类型        | 文件大小 | MD5校验"
        echo "-----+----------------+------------+----------+----------"
        
        local index=1
        while IFS='|' read -r date type file size md5; do
            printf "%-4d | %-15s | %-11s | %-8s | %s\n" "$index" "$date" "$type" "$size" "${md5:0:8}..."
            ((index++))
        done < "${BACKUP_DIR}/backup_index.txt"
    else
        echo "暂无备份记录"
        return 1
    fi
}

# 选择备份文件
select_backup() {
    list_available_backups
    echo ""
    read -p "请选择要恢复的备份编号 (或输入 'q' 退出): " choice
    
    if [[ "$choice" == "q" ]]; then
        echo "取消恢复操作"
        exit 0
    fi
    
    if ! [[ "$choice" =~ ^[0-9]+$ ]]; then
        echo "❌ 无效输入，请输入数字"
        return 1
    fi
    
    # 获取选中的备份信息
    backup_info=$(sed -n "${choice}p" "${BACKUP_DIR}/backup_index.txt")
    if [[ -z "$backup_info" ]]; then
        echo "❌ 无效的备份编号"
        return 1
    fi
    
    backup_file=$(echo "$backup_info" | cut -d'|' -f3)
    backup_type=$(echo "$backup_info" | cut -d'|' -f2)
    
    echo "✅ 选择的备份文件: $backup_file"
    echo "📝 备份类型: $backup_type"
    
    export SELECTED_BACKUP_FILE="$backup_file"
    export SELECTED_BACKUP_TYPE="$backup_type"
}

# 预检查
pre_restore_check() {
    local backup_file=$1
    
    log "🔍 执行恢复前检查..."
    
    # 检查备份文件是否存在
    if [[ ! -f "$backup_file" ]]; then
        log "❌ 备份文件不存在: $backup_file"
        return 1
    fi
    
    # 检查文件完整性
    if [[ "$backup_file" == *.gz ]]; then
        if ! gzip -t "$backup_file"; then
            log "❌ 备份文件压缩损坏"
            return 1
        fi
    fi
    
    # 检查MySQL服务状态
    if ! docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SELECT 1;" >/dev/null 2>&1; then
        log "❌ 无法连接到MySQL服务"
        return 1
    fi
    
    # 检查磁盘空间
    backup_size=$(du -b "$backup_file" | cut -f1)
    available_space=$(df -B1 /var/lib/docker | tail -1 | awk '{print $4}')
    
    if [[ $backup_size -gt $available_space ]]; then
        log "❌ 磁盘空间不足，需要 $(( backup_size / 1024 / 1024 ))MB，可用 $(( available_space / 1024 / 1024 ))MB"
        return 1
    fi
    
    log "✅ 恢复前检查通过"
}

# 创建恢复点
create_restore_point() {
    local restore_point_name="restore_point_$(date +%Y%m%d_%H%M%S)"
    
    log "📸 创建恢复点: $restore_point_name"
    
    # 备份当前数据
    docker-compose exec mysql-master mysqldump \
        --user=root \
        --password=${MYSQL_ROOT_PASSWORD} \
        --single-transaction \
        --routines \
        --triggers \
        --all-databases | gzip > "${BACKUP_DIR}/${restore_point_name}.sql.gz"
    
    if [[ $? -eq 0 ]]; then
        log "✅ 恢复点创建完成: ${restore_point_name}.sql.gz"
        echo "$restore_point_name|restore_point|${BACKUP_DIR}/${restore_point_name}.sql.gz|$(du -h "${BACKUP_DIR}/${restore_point_name}.sql.gz" | cut -f1)|$(md5sum "${BACKUP_DIR}/${restore_point_name}.sql.gz" | cut -d' ' -f1)" >> ${BACKUP_DIR}/backup_index.txt
        export RESTORE_POINT_FILE="${BACKUP_DIR}/${restore_point_name}.sql.gz"
    else
        log "❌ 恢复点创建失败"
        return 1
    fi
}

# 停止复制
stop_replication() {
    log "⏸️ 停止主从复制..."
    
    for slave in slave1 slave2; do
        docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "STOP SLAVE;" 2>/dev/null || true
        log "✅ 停止 mysql-$slave 复制"
    done
}

# 恢复数据
restore_data() {
    local backup_file=$1
    local restore_mode=${2:-"full"}
    
    log "🔄 开始数据恢复..."
    log "📁 备份文件: $backup_file"
    log "🔧 恢复模式: $restore_mode"
    
    # 设置维护模式
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SET GLOBAL read_only = ON;"
    
    case $restore_mode in
        "full")
            # 全量恢复
            if [[ "$backup_file" == *.gz ]]; then
                zcat "$backup_file" | docker-compose exec -T mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD}
            else
                docker-compose exec -T mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} < "$backup_file"
            fi
            ;;
        "database")
            # 单数据库恢复
            read -p "请输入要恢复的数据库名: " database_name
            if [[ "$backup_file" == *.gz ]]; then
                zcat "$backup_file" | docker-compose exec -T mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} "$database_name"
            else
                docker-compose exec -T mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} "$database_name" < "$backup_file"
            fi
            ;;
        "table")
            # 单表恢复
            read -p "请输入数据库名: " database_name
            read -p "请输入表名: " table_name
            # 这里需要更复杂的逻辑来提取特定表的数据
            log "⚠️ 单表恢复功能需要手动实现"
            ;;
    esac
    
    if [[ $? -eq 0 ]]; then
        log "✅ 数据恢复完成"
        
        # 恢复写入权限
        docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SET GLOBAL read_only = OFF;"
        
        return 0
    else
        log "❌ 数据恢复失败"
        
        # 确保恢复写入权限
        docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SET GLOBAL read_only = OFF;" 2>/dev/null || true
        
        return 1
    fi
}

# 重建复制
rebuild_replication() {
    log "🔗 重建主从复制..."
    
    # 等待主节点稳定
    sleep 10
    
    # 重新配置从节点
    for slave in slave1 slave2; do
        log "📡 配置 mysql-$slave 复制..."
        
        docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
        RESET SLAVE ALL;
        CHANGE MASTER TO
            MASTER_HOST='mysql-master',
            MASTER_USER='replicator',
            MASTER_PASSWORD='${MYSQL_REPLICATION_PASSWORD}',
            MASTER_AUTO_POSITION=1;
        START SLAVE;
        "
        
        # 检查复制状态
        sleep 5
        replication_status=$(docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G" | grep -E "(Slave_IO_Running|Slave_SQL_Running)")
        log "📊 mysql-$slave 复制状态: $replication_status"
    done
    
    log "✅ 主从复制重建完成"
}

# 验证恢复
verify_restore() {
    log "🔍 验证数据恢复..."
    
    # 检查MySQL服务状态
    if docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SELECT 1;" >/dev/null 2>&1; then
        log "✅ MySQL服务正常"
    else
        log "❌ MySQL服务异常"
        return 1
    fi
    
    # 检查数据库列表
    databases=$(docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "SHOW DATABASES;" | grep -v -E "^(information_schema|performance_schema|mysql|sys)$")
    log "📊 恢复的数据库: $databases"
    
    # 检查表数量
    for db in $databases; do
        table_count=$(docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$db';")
        log "📊 数据库 $db 表数量: $table_count"
    done
    
    # 检查复制状态
    for slave in slave1 slave2; do
        io_running=$(docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "SHOW SLAVE STATUS\G" | grep "Slave_IO_Running:" | awk '{print $2}')
        sql_running=$(docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "SHOW SLAVE STATUS\G" | grep "Slave_SQL_Running:" | awk '{print $2}')
        
        if [[ "$io_running" == "Yes" && "$sql_running" == "Yes" ]]; then
            log "✅ mysql-$slave 复制正常"
        else
            log "⚠️ mysql-$slave 复制异常: IO=$io_running, SQL=$sql_running"
        fi
    done
    
    log "✅ 数据恢复验证完成"
}

# 回滚到恢复点
rollback_to_restore_point() {
    if [[ -z "$RESTORE_POINT_FILE" ]]; then
        log "❌ 未找到恢复点文件"
        return 1
    fi
    
    log "🔄 回滚到恢复点: $RESTORE_POINT_FILE"
    
    restore_data "$RESTORE_POINT_FILE" "full"
    if [[ $? -eq 0 ]]; then
        rebuild_replication
        verify_restore
        log "✅ 回滚完成"
    else
        log "❌ 回滚失败"
        return 1
    fi
}

# 清理临时文件
cleanup() {
    log "🧹 清理临时文件..."
    # 这里可以添加清理逻辑
    log "✅ 清理完成"
}

# 显示帮助
show_help() {
    echo "MySQL 数据恢复工具"
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  interactive    交互式恢复"
    echo "  full FILE      完整恢复指定文件"
    echo "  database FILE  数据库级恢复"
    echo "  table FILE     表级恢复"
    echo "  point-in-time  时间点恢复"
    echo "  rollback       回滚到恢复点"
    echo "  verify         验证当前状态"
    echo "  help           显示帮助信息"
}

# 交互式恢复
interactive_restore() {
    echo "🔄 MySQL 交互式数据恢复"
    echo "======================="
    
    # 选择备份文件
    if ! select_backup; then
        return 1
    fi
    
    backup_file="$SELECTED_BACKUP_FILE"
    backup_type="$SELECTED_BACKUP_TYPE"
    
    # 选择恢复模式
    echo ""
    echo "请选择恢复模式:"
    echo "1) 完整恢复 (替换所有数据)"
    echo "2) 数据库级恢复 (恢复指定数据库)"
    echo "3) 表级恢复 (恢复指定表)"
    echo ""
    read -p "请输入选择 (1-3): " restore_choice
    
    case $restore_choice in
        1) restore_mode="full" ;;
        2) restore_mode="database" ;;
        3) restore_mode="table" ;;
        *) echo "❌ 无效选择"; return 1 ;;
    esac
    
    # 确认操作
    echo ""
    echo "⚠️ 恢复确认"
    echo "============"
    echo "备份文件: $backup_file"
    echo "恢复模式: $restore_mode"
    echo ""
    echo "⚠️ 警告: 此操作将修改或替换现有数据，建议在执行前创建当前数据的备份。"
    echo ""
    read -p "确认要继续吗？ (yes/no): " confirm
    
    if [[ "$confirm" != "yes" ]]; then
        echo "取消恢复操作"
        return 0
    fi
    
    # 执行恢复流程
    pre_restore_check "$backup_file" || return 1
    create_restore_point || return 1
    stop_replication
    
    if restore_data "$backup_file" "$restore_mode"; then
        rebuild_replication
        verify_restore
        log "🎉 恢复操作成功完成！"
    else
        log "❌ 恢复操作失败，正在回滚..."
        rollback_to_restore_point
    fi
    
    cleanup
}

# 时间点恢复
point_in_time_restore() {
    log "⏰ 时间点恢复功能"
    
    # 这里需要实现基于binlog的时间点恢复
    echo "🚧 时间点恢复功能开发中..."
    echo "请联系管理员获取支持"
}

# 主函数
main() {
    case ${1:-"help"} in
        "interactive")
            interactive_restore
            ;;
        "full")
            if [[ -n "$2" ]]; then
                pre_restore_check "$2" || exit 1
                create_restore_point || exit 1
                stop_replication
                restore_data "$2" "full" && rebuild_replication && verify_restore
                cleanup
            else
                echo "请指定备份文件"
                exit 1
            fi
            ;;
        "database")
            if [[ -n "$2" ]]; then
                pre_restore_check "$2" || exit 1
                create_restore_point || exit 1
                restore_data "$2" "database" && verify_restore
                cleanup
            else
                echo "请指定备份文件"
                exit 1
            fi
            ;;
        "table")
            if [[ -n "$2" ]]; then
                pre_restore_check "$2" || exit 1
                create_restore_point || exit 1
                restore_data "$2" "table" && verify_restore
                cleanup
            else
                echo "请指定备份文件"
                exit 1
            fi
            ;;
        "point-in-time")
            point_in_time_restore
            ;;
        "rollback")
            rollback_to_restore_point
            ;;
        "verify")
            verify_restore
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 加载环境变量
if [[ -f ".env" ]]; then
    source .env
fi

# 执行主函数
main "$@"
```

## 安全配置

### 1. SSL/TLS 加密

```bash
#!/bin/bash
# scripts/setup-ssl.sh - 配置SSL加密

set -e

SSL_DIR="./ssl"
CERTS_DIR="$SSL_DIR/certs"
PRIVATE_DIR="$SSL_DIR/private"

echo "🔐 配置 MySQL SSL 证书..."

# 创建SSL目录
mkdir -p "$CERTS_DIR" "$PRIVATE_DIR"
chmod 700 "$PRIVATE_DIR"

# 生成CA私钥
openssl genrsa 2048 > "$PRIVATE_DIR/ca-key.pem"

# 生成CA证书
openssl req -new -x509 -nodes -days 3650 \
    -key "$PRIVATE_DIR/ca-key.pem" \
    -out "$CERTS_DIR/ca-cert.pem" \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=MySQL-Cluster/OU=Database/CN=MySQL-CA"

# 生成服务器私钥
openssl req -newkey rsa:2048 -days 3650 -nodes \
    -keyout "$PRIVATE_DIR/server-key.pem" \
    -out "$SSL_DIR/server-req.pem" \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=MySQL-Cluster/OU=Database/CN=mysql-master"

# 转换服务器私钥格式
openssl rsa -in "$PRIVATE_DIR/server-key.pem" -out "$PRIVATE_DIR/server-key.pem"

# 生成服务器证书
openssl x509 -req -in "$SSL_DIR/server-req.pem" -days 3650 \
    -CA "$CERTS_DIR/ca-cert.pem" \
    -CAkey "$PRIVATE_DIR/ca-key.pem" \
    -set_serial 01 \
    -out "$CERTS_DIR/server-cert.pem"

# 生成客户端私钥
openssl req -newkey rsa:2048 -days 3650 -nodes \
    -keyout "$PRIVATE_DIR/client-key.pem" \
    -out "$SSL_DIR/client-req.pem" \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=MySQL-Cluster/OU=Database/CN=mysql-client"

# 转换客户端私钥格式
openssl rsa -in "$PRIVATE_DIR/client-key.pem" -out "$PRIVATE_DIR/client-key.pem"

# 生成客户端证书
openssl x509 -req -in "$SSL_DIR/client-req.pem" -days 3650 \
    -CA "$CERTS_DIR/ca-cert.pem" \
    -CAkey "$PRIVATE_DIR/ca-key.pem" \
    -set_serial 01 \
    -out "$CERTS_DIR/client-cert.pem"

# 验证证书
openssl verify -CAfile "$CERTS_DIR/ca-cert.pem" \
    "$CERTS_DIR/server-cert.pem" \
    "$CERTS_DIR/client-cert.pem"

# 设置权限
chmod 600 "$PRIVATE_DIR"/*
chmod 644 "$CERTS_DIR"/*

echo "✅ SSL证书生成完成"
echo "📁 证书位置: $CERTS_DIR"
echo "🔑 私钥位置: $PRIVATE_DIR"
```

### 2. 防火墙配置

```bash
#!/bin/bash
# scripts/firewall-setup.sh - 防火墙配置

set -e

echo "🔥 配置防火墙规则..."

# 检查防火墙状态
if command -v ufw >/dev/null 2>&1; then
    # Ubuntu/Debian UFW
    echo "检测到 UFW 防火墙"
    
    # 允许SSH
    ufw allow ssh
    
    # 允许MySQL端口（仅内网）
    ufw allow from 192.168.0.0/16 to any port 3306
    ufw allow from 10.0.0.0/8 to any port 3306
    ufw allow from 172.16.0.0/12 to any port 3306
    
    # 允许管理界面（仅内网）
    ufw allow from 192.168.0.0/16 to any port 8080
    ufw allow from 192.168.0.0/16 to any port 3000
    ufw allow from 192.168.0.0/16 to any port 9090
    
    # 启用防火墙
    ufw --force enable
    
elif command -v firewall-cmd >/dev/null 2>&1; then
    # CentOS/RHEL Firewalld
    echo "检测到 Firewalld 防火墙"
    
    # 添加MySQL服务
    firewall-cmd --permanent --add-service=mysql
    
    # 添加自定义端口
    firewall-cmd --permanent --add-port=8080/tcp
    firewall-cmd --permanent --add-port=3000/tcp
    firewall-cmd --permanent --add-port=9090/tcp
    
    # 重载配置
    firewall-cmd --reload
    
else
    echo "⚠️ 未检测到支持的防火墙，请手动配置"
fi

echo "✅ 防火墙配置完成"
```

### 3. 访问控制配置

```sql
-- config/mysql/security.sql - 数据库安全配置

-- 删除匿名用户
DELETE FROM mysql.user WHERE User='';

-- 删除test数据库
DROP DATABASE IF EXISTS test;
DELETE FROM mysql.db WHERE Db='test' OR Db='test\\_%';

-- 禁用远程root登录
DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');

-- 创建安全策略用户
CREATE USER IF NOT EXISTS 'security_admin'@'localhost' IDENTIFIED BY 'SecureSecurityPassword123!';
GRANT ALL PRIVILEGES ON *.* TO 'security_admin'@'localhost' WITH GRANT OPTION;

-- 设置密码策略
SET GLOBAL validate_password.policy = STRONG;
SET GLOBAL validate_password.length = 12;
SET GLOBAL validate_password.mixed_case_count = 1;
SET GLOBAL validate_password.number_count = 1;
SET GLOBAL validate_password.special_char_count = 1;

-- 设置连接限制
SET GLOBAL max_connections = 500;
SET GLOBAL max_user_connections = 100;
SET GLOBAL max_connect_errors = 10;

-- 启用慢查询日志
SET GLOBAL slow_query_log = 1;
SET GLOBAL long_query_time = 2;

-- 启用通用查询日志（谨慎使用）
-- SET GLOBAL general_log = 1;

-- 刷新权限
FLUSH PRIVILEGES;
```

### 4. 网络安全

```bash
#!/bin/bash
# scripts/network-security.sh - 网络安全配置

set -e

echo "🌐 配置网络安全..."

# 配置Docker网络隔离
cat > ./config/docker/daemon.json << 'EOF'
{
  "icc": false,
  "iptables": true,
  "ip-forward": false,
  "userland-proxy": false,
  "no-new-privileges": true,
  "seccomp-profile": "/etc/docker/seccomp.json",
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF

# 配置容器安全选项
cat > ./config/docker/security.yml << 'EOF'
# Docker Compose 安全配置覆盖
version: '3.8'

services:
  mysql-master:
    security_opt:
      - no-new-privileges:true
      - apparmor:docker-default
    cap_drop:
      - ALL
    cap_add:
      - SETUID
      - SETGID
      - DAC_OVERRIDE
    read_only: false
    tmpfs:
      - /tmp
      - /var/tmp
    
  mysql-slave1:
    security_opt:
      - no-new-privileges:true
      - apparmor:docker-default
    cap_drop:
      - ALL
    cap_add:
      - SETUID
      - SETGID
      - DAC_OVERRIDE
    
  mysql-slave2:
    security_opt:
      - no-new-privileges:true
      - apparmor:docker-default
    cap_drop:
      - ALL
    cap_add:
      - SETUID
      - SETGID
      - DAC_OVERRIDE
EOF

echo "✅ 网络安全配置完成"
```

## 性能优化

### 1. MySQL 性能调优

```sql
-- config/mysql/performance.sql - 性能优化配置

-- InnoDB 配置优化
SET GLOBAL innodb_buffer_pool_size = 1073741824;  -- 1GB
SET GLOBAL innodb_log_file_size = 268435456;      -- 256MB
SET GLOBAL innodb_log_buffer_size = 16777216;     -- 16MB
SET GLOBAL innodb_flush_log_at_trx_commit = 1;    -- 安全模式
SET GLOBAL innodb_flush_method = 'O_DIRECT';      -- 避免双重缓冲
SET GLOBAL innodb_file_per_table = 1;             -- 独立表空间
SET GLOBAL innodb_io_capacity = 2000;             -- I/O容量
SET GLOBAL innodb_read_io_threads = 4;            -- 读线程数
SET GLOBAL innodb_write_io_threads = 4;           -- 写线程数

-- 查询缓存配置
SET GLOBAL query_cache_type = 1;                  -- 启用查询缓存
SET GLOBAL query_cache_size = 268435456;          -- 256MB缓存

-- 连接配置
SET GLOBAL max_connections = 500;                 -- 最大连接数
SET GLOBAL thread_cache_size = 50;                -- 线程缓存
SET GLOBAL table_open_cache = 2000;               -- 表缓存

-- 排序和临时表
SET GLOBAL sort_buffer_size = 2097152;            -- 2MB排序缓冲
SET GLOBAL tmp_table_size = 67108864;             -- 64MB临时表
SET GLOBAL max_heap_table_size = 67108864;        -- 64MB内存表

-- 二进制日志
SET GLOBAL binlog_cache_size = 1048576;           -- 1MB binlog缓存
SET GLOBAL sync_binlog = 1;                       -- 安全模式

-- 复制优化
SET GLOBAL slave_parallel_workers = 4;           -- 并行复制线程
SET GLOBAL slave_parallel_type = 'LOGICAL_CLOCK'; -- 并行类型
```

### 2. 系统级性能优化

```bash
#!/bin/bash
# scripts/system-optimization.sh - 系统性能优化

set -e

echo "⚡ 系统性能优化..."

# 内核参数优化
cat >> /etc/sysctl.conf << 'EOF'
# MySQL 性能优化
vm.swappiness = 10
vm.dirty_ratio = 15
vm.dirty_background_ratio = 5
fs.file-max = 65536
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 65536 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216
net.ipv4.tcp_congestion_control = bbr
EOF

# 应用内核参数
sysctl -p

# 设置文件描述符限制
cat >> /etc/security/limits.conf << 'EOF'
mysql soft nofile 65536
mysql hard nofile 65536
mysql soft nproc 32768
mysql hard nproc 32768
EOF

# Docker 优化
cat > ./config/docker/docker-optimization.conf << 'EOF'
# Docker daemon 优化配置
{
  "storage-driver": "overlay2",
  "storage-opts": [
    "overlay2.override_kernel_check=true"
  ],
  "log-level": "warn",
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "default-ulimits": {
    "nofile": {
      "Name": "nofile",
      "Hard": 65536,
      "Soft": 65536
    }
  }
}
EOF

echo "✅ 系统优化完成"
```

### 3. 监控和性能分析

```bash
#!/bin/bash
# scripts/performance-analysis.sh - 性能分析工具

set -e

echo "📊 MySQL 性能分析报告"
echo "====================="

# 数据库连接统计
echo "=== 连接统计 ==="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    VARIABLE_NAME as '指标',
    VARIABLE_VALUE as '数值'
FROM INFORMATION_SCHEMA.SESSION_STATUS 
WHERE VARIABLE_NAME IN (
    'Threads_connected',
    'Threads_running',
    'Max_used_connections',
    'Aborted_connects',
    'Aborted_clients'
);
"

# 查询性能统计
echo -e "\n=== 查询性能 ==="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    VARIABLE_NAME as '查询类型',
    VARIABLE_VALUE as '总数'
FROM INFORMATION_SCHEMA.SESSION_STATUS 
WHERE VARIABLE_NAME LIKE 'Com_%' 
AND VARIABLE_VALUE > 0
ORDER BY CAST(VARIABLE_VALUE AS UNSIGNED) DESC
LIMIT 10;
"

# InnoDB 状态
echo -e "\n=== InnoDB 状态 ==="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    'Buffer Pool Hit Rate' as '指标',
    ROUND(
        (1 - (
            SELECT VARIABLE_VALUE FROM INFORMATION_SCHEMA.SESSION_STATUS WHERE VARIABLE_NAME = 'Innodb_buffer_pool_reads'
        ) / (
            SELECT VARIABLE_VALUE FROM INFORMATION_SCHEMA.SESSION_STATUS WHERE VARIABLE_NAME = 'Innodb_buffer_pool_read_requests'
        )) * 100, 2
    ) as '命中率(%)'
UNION ALL
SELECT 
    'Pages Data',
    VARIABLE_VALUE
FROM INFORMATION_SCHEMA.SESSION_STATUS 
WHERE VARIABLE_NAME = 'Innodb_buffer_pool_pages_data'
UNION ALL
SELECT 
    'Pages Free',
    VARIABLE_VALUE
FROM INFORMATION_SCHEMA.SESSION_STATUS 
WHERE VARIABLE_NAME = 'Innodb_buffer_pool_pages_free';
"

# 慢查询统计
echo -e "\n=== 慢查询分析 ==="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    'Slow Queries' as '类型',
    VARIABLE_VALUE as '数量'
FROM INFORMATION_SCHEMA.SESSION_STATUS 
WHERE VARIABLE_NAME = 'Slow_queries'
UNION ALL
SELECT 
    'Long Query Time',
    VARIABLE_VALUE
FROM INFORMATION_SCHEMA.SESSION_VARIABLES 
WHERE VARIABLE_NAME = 'long_query_time';
"

# 表锁统计
echo -e "\n=== 锁统计 ==="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    VARIABLE_NAME as '锁类型',
    VARIABLE_VALUE as '次数'
FROM INFORMATION_SCHEMA.SESSION_STATUS 
WHERE VARIABLE_NAME IN (
    'Table_locks_immediate',
    'Table_locks_waited',
    'Innodb_row_lock_waits'
);
"

# 主从复制延迟
echo -e "\n=== 复制延迟 ==="
for slave in slave1 slave2; do
    echo "--- mysql-$slave ---"
    delay=$(docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "
    SHOW SLAVE STATUS\G" 2>/dev/null | grep "Seconds_Behind_Master" | awk '{print $2}')
    echo "延迟: ${delay:-Unknown} 秒"
done

# 磁盘空间统计
echo -e "\n=== 存储统计 ==="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    table_schema as '数据库',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) as '大小(MB)',
    COUNT(*) as '表数量'
FROM information_schema.tables 
WHERE table_schema NOT IN ('information_schema','performance_schema','mysql','sys')
GROUP BY table_schema
ORDER BY SUM(data_length + index_length) DESC;
"

echo -e "\n✅ 性能分析完成"
```

## 维护和故障排除

### 1. 日常维护脚本

```bash
#!/bin/bash
# scripts/daily-maintenance.sh - 日常维护任务

set -e

MAINTENANCE_LOG="/var/log/mysql-maintenance.log"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$MAINTENANCE_LOG"
}

log "🔧 开始日常维护任务..."

# 1. 健康检查
log "📊 执行健康检查..."
./scripts/db-management.sh health >> "$MAINTENANCE_LOG" 2>&1

# 2. 清理二进制日志
log "🧹 清理过期二进制日志..."
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
PURGE BINARY LOGS BEFORE DATE_SUB(NOW(), INTERVAL 3 DAY);
" >> "$MAINTENANCE_LOG" 2>&1

# 3. 优化表
log "⚡ 优化数据库表..."
tables=$(docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "
SELECT CONCAT(table_schema,'.',table_name) 
FROM information_schema.tables 
WHERE table_schema NOT IN ('information_schema','performance_schema','mysql','sys')
AND ENGINE = 'InnoDB'
")

for table in $tables; do
    log "优化表: $table"
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "OPTIMIZE TABLE $table;" >> "$MAINTENANCE_LOG" 2>&1
done

# 4. 更新统计信息
log "📈 更新表统计信息..."
for table in $tables; do
    docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "ANALYZE TABLE $table;" >> "$MAINTENANCE_LOG" 2>&1
done

# 5. 检查复制状态
log "🔗 检查主从复制状态..."
./scripts/check-replication.sh >> "$MAINTENANCE_LOG" 2>&1

# 6. 磁盘空间检查
log "💾 检查磁盘空间..."
df -h | grep -E "(mysql|data|backup)" >> "$MAINTENANCE_LOG" 2>&1

# 7. 性能报告
log "📊 生成性能报告..."
./scripts/performance-analysis.sh >> "$MAINTENANCE_LOG" 2>&1

# 8. 清理旧日志
log "🗑️ 清理旧日志文件..."
find ./logs -name "*.log" -mtime +7 -delete

log "✅ 日常维护任务完成"

# 发送维护报告
if [[ -n "$EMAIL_TO" ]]; then
    tail -50 "$MAINTENANCE_LOG" | mail -s "MySQL集群维护报告 - $(date +%Y-%m-%d)" "$EMAIL_TO"
fi
```

### 2. 故障诊断工具

```bash
#!/bin/bash
# scripts/diagnostic.sh - 故障诊断工具

set -e

echo "🔍 MySQL 集群故障诊断"
echo "====================="

# 检查容器状态
echo "=== 容器状态检查 ==="
docker-compose ps
echo ""

# 检查网络连通性
echo "=== 网络连通性检查 ==="
services=("mysql-master" "mysql-slave1" "mysql-slave2" "proxysql" "redis")
for service in "${services[@]}"; do
    if docker-compose exec mysql-master ping -c 1 "$service" >/dev/null 2>&1; then
        echo "✅ $service 网络连通正常"
    else
        echo "❌ $service 网络连通异常"
    fi
done
echo ""

# 检查端口监听
echo "=== 端口监听检查 ==="
ports=(3306 3307 3308 6033 8080 3000 9090)
for port in "${ports[@]}"; do
    if timeout 3 bash -c "</dev/tcp/localhost/$port" 2>/dev/null; then
        echo "✅ 端口 $port 监听正常"
    else
        echo "❌ 端口 $port 无法访问"
    fi
done
echo ""

# 检查MySQL错误日志
echo "=== MySQL 错误日志 ==="
for node in master slave1 slave2; do
    echo "--- mysql-$node 最近错误 ---"
    if docker-compose exec mysql-$node test -f /var/log/mysql/error.log; then
        docker-compose exec mysql-$node tail -10 /var/log/mysql/error.log 2>/dev/null || echo "无法读取错误日志"
    else
        echo "错误日志文件不存在"
    fi
    echo ""
done

# 检查磁盘空间
echo "=== 磁盘空间检查 ==="
df -h | head -1
df -h | grep -E "(mysql|data|backup|/$)" | while read line; do
    usage=$(echo "$line" | awk '{print $5}' | sed 's/%//')
    if [[ $usage -gt 90 ]]; then
        echo "🔴 $line (空间不足)"
    elif [[ $usage -gt 80 ]]; then
        echo "🟡 $line (空间紧张)"
    else
        echo "🟢 $line"
    fi
done
echo ""

# 检查内存使用
echo "=== 内存使用检查 ==="
free -h
echo ""

# 检查MySQL进程
echo "=== MySQL 进程状态 ==="
for node in master slave1 slave2; do
    echo "--- mysql-$node 进程列表 ---"
    docker-compose exec mysql-$node mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    SELECT 
        ID,
        USER,
        HOST,
        DB,
        COMMAND,
        TIME,
        STATE,
        LEFT(INFO, 50) as QUERY_PREVIEW
    FROM INFORMATION_SCHEMA.PROCESSLIST 
    WHERE COMMAND != 'Sleep'
    ORDER BY TIME DESC
    LIMIT 10;
    " 2>/dev/null || echo "无法连接到 mysql-$node"
    echo ""
done

# 检查复制状态详情
echo "=== 复制状态详情 ==="
for slave in slave1 slave2; do
    echo "--- mysql-$slave 复制详情 ---"
    docker-compose exec mysql-$slave mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G" 2>/dev/null | grep -E "(Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master|Last_Error|Last_SQL_Error)" || echo "无法获取复制状态"
    echo ""
done

# 检查表状态
echo "=== 表状态检查 ==="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    table_schema as 'Database',
    table_name as 'Table',
    engine as 'Engine',
    table_rows as 'Rows',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) as 'Size_MB'
FROM information_schema.tables 
WHERE table_schema NOT IN ('information_schema','performance_schema','mysql','sys')
AND table_type = 'BASE TABLE'
ORDER BY (data_length + index_length) DESC
LIMIT 20;
" 2>/dev/null || echo "无法获取表状态"

echo ""
echo "🎯 诊断建议:"
echo "1. 检查红色❌标记的项目"
echo "2. 关注🔴和🟡标记的磁盘空间"
echo "3. 检查复制延迟和错误"
echo "4. 监控大表的增长情况"
echo "5. 查看错误日志了解详细问题"
```

### 3. 自动恢复脚本

```bash
#!/bin/bash
# scripts/auto-recovery.sh - 自动故障恢复

set -e

RECOVERY_LOG="/var/log/mysql-recovery.log"
MAX_RETRIES=3
RETRY_INTERVAL=30

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$RECOVERY_LOG"
}

# 检查服务健康状态
check_service_health() {
    local service=$1
    local retries=0
    
    while [[ $retries -lt $MAX_RETRIES ]]; do
        if docker-compose exec "$service" mysqladmin ping -h localhost -u root -p${MYSQL_ROOT_PASSWORD} >/dev/null 2>&1; then
            return 0
        fi
        
        ((retries++))
        log "⚠️ $service 健康检查失败，重试 $retries/$MAX_RETRIES"
        sleep $RETRY_INTERVAL
    done
    
    return 1
}

# 重启服务
restart_service() {
    local service=$1
    
    log "🔄 重启服务: $service"
    docker-compose restart "$service"
    
    sleep 30
    
    if check_service_health "$service"; then
        log "✅ $service 重启成功"
        return 0
    else
        log "❌ $service 重启失败"
        return 1
    fi
}

# 修复复制
fix_replication() {
    local slave=$1
    
    log "🔧 修复 $slave 复制..."
    
    # 停止复制
    docker-compose exec "$slave" mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "STOP SLAVE;" 2>/dev/null
    
    # 重置复制
    docker-compose exec "$slave" mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "RESET SLAVE ALL;" 2>/dev/null
    
    # 重新配置复制
    docker-compose exec "$slave" mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    CHANGE MASTER TO
        MASTER_HOST='mysql-master',
        MASTER_USER='replicator',
        MASTER_PASSWORD='${MYSQL_REPLICATION_PASSWORD}',
        MASTER_AUTO_POSITION=1;
    START SLAVE;
    " 2>/dev/null
    
    sleep 10
    
    # 检查复制状态
    local io_running=$(docker-compose exec "$slave" mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "SHOW SLAVE STATUS\G" 2>/dev/null | grep "Slave_IO_Running:" | awk '{print $2}')
    local sql_running=$(docker-compose exec "$slave" mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "SHOW SLAVE STATUS\G" 2>/dev/null | grep "Slave_SQL_Running:" | awk '{print $2}')
    
    if [[ "$io_running" == "Yes" && "$sql_running" == "Yes" ]]; then
        log "✅ $slave 复制修复成功"
        return 0
    else
        log "❌ $slave 复制修复失败"
        return 1
    fi
}

# 主函数
main() {
    log "🚨 启动自动故障恢复程序"
    
    # 检查主节点
    if ! check_service_health "mysql-master"; then
        log "🔴 主节点异常，尝试重启"
        if ! restart_service "mysql-master"; then
            log "💥 主节点重启失败，需要人工干预"
            # 发送紧急通知
            if [[ -n "$WEBHOOK_URL" ]]; then
                curl -X POST -H 'Content-type: application/json' \
                    --data '{"text":"🚨 MySQL主节点故障，需要紧急处理！"}' \
                    "$WEBHOOK_URL"
            fi
            exit 1
        fi
    fi
    
    # 检查从节点
    for slave in mysql-slave1 mysql-slave2; do
        if ! check_service_health "$slave"; then
            log "🟡 $slave 异常，尝试重启"
            if ! restart_service "$slave"; then
                log "⚠️ $slave 重启失败，尝试跳过"
                continue
            fi
        fi
        
        # 检查复制状态
        local io_running=$(docker-compose exec "$slave" mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "SHOW SLAVE STATUS\G" 2>/dev/null | grep "Slave_IO_Running:" | awk '{print $2}')
        local sql_running=$(docker-compose exec "$slave" mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "SHOW SLAVE STATUS\G" 2>/dev/null | grep "Slave_SQL_Running:" | awk '{print $2}')
        
        if [[ "$io_running" != "Yes" || "$sql_running" != "Yes" ]]; then
            log "🔧 $slave 复制异常，尝试修复"
            fix_replication "$slave"
        fi
    done
    
    # 检查ProxySQL
    if ! docker-compose exec proxysql mysql -h127.0.0.1 -P6032 -uadmin -padmin -e "SELECT 1;" >/dev/null 2>&1; then
        log "🟡 ProxySQL异常，尝试重启"
        docker-compose restart proxysql
    fi
    
    log "✅ 自动故障恢复完成"
}

# 执行恢复
main "$@"
```

## 扩展和升级

### 1. 扩展新节点

```bash
#!/bin/bash
# scripts/add-slave.sh - 添加新的从节点

set -e

NEW_SLAVE_ID=${1:-4}
NEW_SLAVE_NAME="mysql-slave${NEW_SLAVE_ID}"

echo "➕ 添加新从节点: $NEW_SLAVE_NAME"

# 创建新从节点配置
cat > "config/mysql/slave${NEW_SLAVE_ID}/my.cnf" << EOF
[mysqld]
# 服务器ID
server-id = $((NEW_SLAVE_ID + 1))

# 二进制日志配置
log-bin = mysql-bin
binlog-format = ROW
binlog-do-db = appdb

# GTID配置
gtid-mode = ON
enforce-gtid-consistency = ON
log-slave-updates = ON

# 从节点配置
read-only = 1
relay-log = relay-log

# 字符集配置
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 连接配置
max-connections = 300

# InnoDB配置
innodb-buffer-pool-size = 512M
EOF

# 添加到docker-compose.yml
cat >> docker-compose.yml << EOF

  $NEW_SLAVE_NAME:
    image: mysql:8.0.35
    container_name: $NEW_SLAVE_NAME
    restart: unless-stopped
    depends_on:
      mysql-master:
        condition: service_healthy
    environment:
      MYSQL_ROOT_PASSWORD: \${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: \${MYSQL_DATABASE}
      MYSQL_USER: \${MYSQL_USER}
      MYSQL_PASSWORD: \${MYSQL_PASSWORD}
    command: >
      --server-id=$((NEW_SLAVE_ID + 1))
      --log-bin=mysql-bin
      --binlog-format=ROW
      --gtid-mode=ON
      --enforce-gtid-consistency=ON
      --log-slave-updates=ON
      --read-only=1
      --relay-log=relay-log
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --max-connections=300
      --innodb-buffer-pool-size=512M
    volumes:
      - mysql_slave${NEW_SLAVE_ID}_data:/var/lib/mysql
      - ./config/mysql/slave${NEW_SLAVE_ID}/my.cnf:/etc/mysql/conf.d/my.cnf:ro
      - ./logs/slave${NEW_SLAVE_ID}:/var/log/mysql
    ports:
      - "$((3308 + NEW_SLAVE_ID - 2)):3306"
    networks:
      - mysql_cluster
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p\${MYSQL_ROOT_PASSWORD}"]
      timeout: 20s
      retries: 10
      interval: 30s

volumes:
  mysql_slave${NEW_SLAVE_ID}_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/slave${NEW_SLAVE_ID}
EOF

# 创建数据目录
mkdir -p "data/slave${NEW_SLAVE_ID}"
mkdir -p "logs/slave${NEW_SLAVE_ID}"

# 启动新节点
docker-compose up -d "$NEW_SLAVE_NAME"

# 等待节点启动
echo "⏳ 等待新节点启动..."
sleep 60

# 配置复制
docker-compose exec "$NEW_SLAVE_NAME" mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_USER='replicator',
    MASTER_PASSWORD='${MYSQL_REPLICATION_PASSWORD}',
    MASTER_AUTO_POSITION=1;
START SLAVE;
"

# 检查复制状态
echo "🔍 检查复制状态..."
docker-compose exec "$NEW_SLAVE_NAME" mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G" | grep -E "(Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master)"

echo "✅ 新从节点 $NEW_SLAVE_NAME 添加完成"
```

### 2. 版本升级指南

```bash
#!/bin/bash
# scripts/upgrade-mysql.sh - MySQL版本升级

set -e

OLD_VERSION=${1:-"8.0.35"}
NEW_VERSION=${2:-"8.0.40"}

echo "⬆️ MySQL版本升级: $OLD_VERSION -> $NEW_VERSION"

# 备份当前数据
echo "💾 创建升级前备份..."
./scripts/backup.sh consistent

# 停止服务
echo "⏸️ 停止MySQL服务..."
docker-compose stop mysql-master mysql-slave1 mysql-slave2

# 更新镜像版本
echo "🔄 更新Docker镜像..."
sed -i "s/mysql:$OLD_VERSION/mysql:$NEW_VERSION/g" docker-compose.yml

# 拉取新镜像
docker-compose pull mysql-master mysql-slave1 mysql-slave2

# 启动主节点
echo "🚀 启动主节点..."
docker-compose up -d mysql-master

# 等待主节点启动
sleep 60

# 运行升级脚本
echo "⚡ 执行数据库升级..."
docker-compose exec mysql-master mysql_upgrade -uroot -p${MYSQL_ROOT_PASSWORD}

# 启动从节点
echo "🚀 启动从节点..."
docker-compose up -d mysql-slave1 mysql-slave2

# 等待从节点启动
sleep 60

# 检查升级结果
echo "🔍 检查升级结果..."
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SELECT VERSION();"

# 验证复制状态
echo "🔗 验证复制状态..."
./scripts/check-replication.sh

echo "✅ MySQL升级完成"
```

### 3. 容量规划

```bash
#!/bin/bash
# scripts/capacity-planning.sh - 容量规划分析

set -e

echo "📊 MySQL集群容量规划分析"
echo "========================"

# 当前容量统计
echo "=== 当前容量使用情况 ==="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    table_schema as '数据库',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) as '当前大小(MB)',
    COUNT(*) as '表数量'
FROM information_schema.tables 
WHERE table_schema NOT IN ('information_schema','performance_schema','mysql','sys')
GROUP BY table_schema
ORDER BY SUM(data_length + index_length) DESC;
"

# 增长趋势分析
echo -e "\n=== 数据增长趋势 ==="
# 这里需要结合历史数据来分析

# 性能瓶颈分析
echo -e "\n=== 性能瓶颈分析 ==="
docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    'InnoDB Buffer Pool Hit Rate' as '指标',
    CONCAT(
        ROUND(
            (1 - (
                SELECT VARIABLE_VALUE FROM INFORMATION_SCHEMA.SESSION_STATUS WHERE VARIABLE_NAME = 'Innodb_buffer_pool_reads'
            ) / (
                SELECT VARIABLE_VALUE FROM INFORMATION_SCHEMA.SESSION_STATUS WHERE VARIABLE_NAME = 'Innodb_buffer_pool_read_requests'
            )) * 100, 2
        ), '%'
    ) as '当前值',
    '> 95%' as '建议值'
UNION ALL
SELECT 
    'Query Cache Hit Rate',
    CONCAT(
        ROUND(
            (
                SELECT VARIABLE_VALUE FROM INFORMATION_SCHEMA.SESSION_STATUS WHERE VARIABLE_NAME = 'Qcache_hits'
            ) / (
                (SELECT VARIABLE_VALUE FROM INFORMATION_SCHEMA.SESSION_STATUS WHERE VARIABLE_NAME = 'Qcache_hits') +
                (SELECT VARIABLE_VALUE FROM INFORMATION_SCHEMA.SESSION_STATUS WHERE VARIABLE_NAME = 'Com_select')
            ) * 100, 2
        ), '%'
    ),
    '> 80%'
UNION ALL
SELECT 
    'Thread Cache Hit Rate',
    CONCAT(
        ROUND(
            (1 - (
                SELECT VARIABLE_VALUE FROM INFORMATION_SCHEMA.SESSION_STATUS WHERE VARIABLE_NAME = 'Threads_created'
            ) / (
                SELECT VARIABLE_VALUE FROM INFORMATION_SCHEMA.SESSION_STATUS WHERE VARIABLE_NAME = 'Connections'
            )) * 100, 2
        ), '%'
    ),
    '> 90%';
"

# 容量预测
echo -e "\n=== 容量预测建议 ==="
echo "基于当前数据计算未来6个月容量需求："

current_size=$(docker-compose exec mysql-master mysql -uroot -p${MYSQL_ROOT_PASSWORD} -N -e "
SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024 / 1024, 2)
FROM information_schema.tables 
WHERE table_schema NOT IN ('information_schema','performance_schema','mysql','sys')
")

echo "当前数据大小: ${current_size} GB"
echo "预估6个月后: $(echo "$current_size * 1.5" | bc -l | xargs printf "%.2f") GB (假设50%增长)"
echo "预估1年后: $(echo "$current_size * 2.0" | bc -l | xargs printf "%.2f") GB (假设100%增长)"

# 硬件建议
echo -e "\n=== 硬件配置建议 ==="
echo "基于当前负载的硬件建议："
echo "• CPU: 8核心+ (当前连接数考虑)"
echo "• 内存: 16GB+ (InnoDB缓冲池 + 系统开销)"
echo "• 存储: SSD 500GB+ (考虑性能和容量)"
echo "• 网络: 千兆网络+"

echo -e "\n=== 扩容建议 ==="
echo "1. 当数据库大小达到当前容量的80%时开始规划扩容"
echo "2. 当连接数达到最大连接数的70%时考虑增加从节点"
echo "3. 当查询响应时间超过阈值时考虑读写分离优化"
echo "4. 当备份时间超过维护窗口时考虑增量备份策略"
```

## 部署清单和最佳实践

### 1. 部署前检查清单

```markdown
# MySQL 集群部署检查清单

## 环境准备 ✓
- [ ] 服务器硬件资源确认（CPU、内存、磁盘、网络）
- [ ] 操作系统环境准备（Docker、Docker Compose）
- [ ] 网络配置和防火墙设置
- [ ] 存储规划和磁盘挂载
- [ ] 用户权限和安全配置

## 配置文件准备 ✓
- [ ] .env 环境变量配置
- [ ] docker-compose.yml 主配置文件
- [ ] MySQL 配置文件（my.cnf）
- [ ] ProxySQL 配置文件
- [ ] 监控配置文件（Prometheus、Grafana）

## 安全配置 ✓
- [ ] 强密码策略设置
- [ ] SSL证书生成和配置
- [ ] 防火墙规则配置
- [ ] 网络安全策略
- [ ] 用户权限最小化原则

## 高可用配置 ✓
- [ ] 主从复制配置
- [ ] 故障转移机制
- [ ] 负载均衡配置
- [ ] 健康检查设置

## 监控和告警 ✓
- [ ] Prometheus 监控配置
- [ ] Grafana 仪表板设置
- [ ] 告警规则配置
- [ ] 通知渠道设置

## 备份策略 ✓
- [ ] 自动备份脚本配置
- [ ] 备份存储位置和策略
- [ ] 恢复流程测试
- [ ] 备份验证机制

## 性能优化 ✓
- [ ] MySQL 参数调优
- [ ] 系统级优化配置
- [ ] 索引优化策略
- [ ] 查询性能优化

## 运维工具 ✓
- [ ] 管理脚本部署
- [ ] 监控脚本配置
- [ ] 维护脚本设置
- [ ] 故障诊断工具

## 文档和培训 ✓
- [ ] 部署文档完整性
- [ ] 操作手册准备
- [ ] 故障处理手册
- [ ] 团队培训完成
```

### 2. 最佳实践总结

```markdown
# MySQL 集群最佳实践

## 🔒 安全最佳实践

1. **密码安全**
   - 使用强密码策略（大小写+数字+特殊字符）
   - 定期更新密码
   - 禁用默认账户

2. **网络安全**
   - 使用防火墙限制访问
   - 启用SSL/TLS加密
   - 网络隔离和VPN访问

3. **权限管理**
   - 最小权限原则
   - 分角色权限管理
   - 定期审核用户权限

## ⚡ 性能最佳实践

1. **硬件配置**
   - 使用SSD存储
   - 充足的内存配置
   - 多核CPU配置

2. **数据库优化**
   - 合理设置InnoDB缓冲池
   - 优化查询和索引
   - 定期维护统计信息

3. **架构设计**
   - 读写分离
   - 分库分表策略
   - 缓存层设计

## 🔄 运维最佳实践

1. **监控策略**
   - 全面的监控指标
   - 及时的告警机制
   - 定期的性能分析

2. **备份策略**
   - 多层备份策略
   - 定期备份验证
   - 灾难恢复演练

3. **变更管理**
   - 版本控制
   - 分环境部署
   - 回滚机制

## 📊 容量规划

1. **容量监控**
   - 实时容量监控
   - 增长趋势分析
   - 容量预警机制

2. **扩容策略**
   - 水平扩展优先
   - 渐进式扩容
   - 性能测试验证

## 🚨 故障处理

1. **预防措施**
   - 定期健康检查
   - 预警机制
   - 自动化运维

2. **应急响应**
   - 故障处理流程
   - 自动恢复机制
   - 人工干预程序

## 📝 文档管理

1. **技术文档**
   - 架构设计文档
   - 操作指南
   - 故障处理手册

2. **知识管理**
   - 最佳实践分享
   - 经验总结
   - 团队培训
```

---

**📚 文档信息**
- **文档版本**: v1.0
- **创建日期**: 2025年8月12日
- **适用版本**: MySQL 8.0, Docker Compose 2.0+
- **维护者**: MySQL集群管理团队

**🔗 相关链接**
- [MySQL官方文档](https://dev.mysql.com/doc/)
- [Docker Compose文档](https://docs.docker.com/compose/)
- [ProxySQL文档](https://proxysql.com/documentation/)
- [Prometheus文档](https://prometheus.io/docs/)
- [Grafana文档](https://grafana.com/docs/)

**📞 技术支持**
如有问题请联系：
- GitHub Issues: https://github.com/githubstudycloud/codeclaTest/issues
- Email: support@company.com
- 技术交流群: MySQL集群技术交流