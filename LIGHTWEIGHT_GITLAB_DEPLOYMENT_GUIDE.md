# 轻量级 GitLab 部署指南 - 小团队专用

## 概述

本指南提供一个专门为 **1-5 人小团队** 设计的轻量级 GitLab 部署方案，针对 **内存限制环境** 进行了深度优化，最低可在 **2GB 内存** 的设备上运行，推荐 **4GB+ 内存** 以获得更好体验。

## 目录

- [系统要求](#系统要求)
- [快速部署](#快速部署)
- [Docker Compose 配置](#docker-compose-配置)
- [GitLab 优化配置](#gitlab-优化配置)
- [备份和维护](#备份和维护)
- [使用指南](#使用指南)
- [故障排除](#故障排除)
- [扩展选项](#扩展选项)

## 系统要求

### 最低配置
- **CPU**: 1 核心
- **内存**: 2GB (GitLab CE 精简配置)
- **存储**: 10GB 可用空间
- **网络**: 支持 HTTP/HTTPS

### 推荐配置  
- **CPU**: 2+ 核心
- **内存**: 4GB+
- **存储**: 20GB+ SSD
- **网络**: 千兆网络

### 软件要求
- Docker 20.10+
- Docker Compose 2.0+
- Git 客户端

## 快速部署

### 1. 下载部署文件

```bash
# 创建项目目录
mkdir lightweight-gitlab && cd lightweight-gitlab

# 下载配置文件
curl -O https://raw.githubusercontent.com/githubstudycloud/codeclaTest/master/lightweight-gitlab/docker-compose.yml
curl -O https://raw.githubusercontent.com/githubstudycloud/codeclaTest/master/lightweight-gitlab/.env
```

### 2. 配置环境变量

```bash
# 复制环境变量文件
cp .env.example .env

# 编辑配置（重要！）
nano .env
```

### 3. 一键启动

```bash
# 创建目录结构
./scripts/setup.sh

# 启动 GitLab
docker-compose up -d

# 查看启动状态
docker-compose logs -f gitlab
```

### 4. 访问 GitLab

```bash
# 等待服务启动（约2-3分钟）
echo "GitLab 地址: http://localhost:8080"
echo "初始用户名: root"

# 获取初始密码
docker-compose exec gitlab cat /etc/gitlab/initial_root_password
```

## Docker Compose 配置

### 主配置文件

```yaml
# docker-compose.yml
version: '3.8'

services:
  # ================================
  # GitLab CE 轻量级配置
  # ================================
  gitlab:
    image: gitlab/gitlab-ce:16.7.0-ce.0
    container_name: gitlab-ce-light
    restart: unless-stopped
    hostname: 'gitlab.local'
    environment:
      GITLAB_OMNIBUS_CONFIG: |
        # 基础配置
        external_url '${GITLAB_EXTERNAL_URL}'
        
        # 内存优化配置
        postgresql['shared_buffers'] = "128MB"
        postgresql['max_worker_processes'] = 4
        
        unicorn['worker_processes'] = 2
        unicorn['worker_memory_limit_min'] = "300 * 1 << 20"
        unicorn['worker_memory_limit_max'] = "350 * 1 << 20"
        
        sidekiq['max_concurrency'] = 10
        sidekiq['min_concurrency'] = 5
        
        # 禁用不必要的服务
        prometheus_monitoring['enable'] = false
        alertmanager['enable'] = false
        node_exporter['enable'] = false
        redis_exporter['enable'] = false
        postgres_exporter['enable'] = false
        gitlab_exporter['enable'] = false
        grafana['enable'] = false
        
        # 减少 Gitaly 内存使用
        gitaly['configuration'] = {
          concurrency: [
            {
              'rpc' => "/gitaly.SmartHTTPService/PostReceivePack",
              'max_per_repo' => 3
            }, {
              'rpc' => "/gitaly.SSHService/SSHUploadPack",
              'max_per_repo' => 3
            }
          ]
        }
        
        # 减少 GitLab Pages 内存（如果不使用可完全禁用）
        pages_nginx['enable'] = false
        gitlab_pages['enable'] = false
        
        # 优化 Redis 配置
        redis['maxmemory'] = "100mb"
        redis['maxmemory_policy'] = "allkeys-lru"
        
        # 邮件配置（可选）
        gitlab_rails['smtp_enable'] = ${SMTP_ENABLE}
        gitlab_rails['smtp_address'] = "${SMTP_SERVER}"
        gitlab_rails['smtp_port'] = ${SMTP_PORT}
        gitlab_rails['smtp_user_name'] = "${SMTP_USER}"
        gitlab_rails['smtp_password'] = "${SMTP_PASSWORD}"
        gitlab_rails['smtp_domain'] = "${SMTP_DOMAIN}"
        gitlab_rails['smtp_authentication'] = "login"
        gitlab_rails['smtp_enable_starttls_auto'] = true
        gitlab_rails['smtp_tls'] = false
        
        # GitLab 设置
        gitlab_rails['gitlab_email_from'] = '${GITLAB_EMAIL_FROM}'
        gitlab_rails['gitlab_email_display_name'] = 'GitLab'
        
        # 时区设置
        gitlab_rails['time_zone'] = 'Asia/Shanghai'
        
        # 备份设置
        gitlab_rails['backup_keep_time'] = 604800  # 7天
        gitlab_rails['backup_path'] = "/var/opt/gitlab/backups"
        
        # 限制并发克隆数量
        gitlab_rails['gitlab_shell_git_timeout'] = 800
        
    ports:
      - "8080:80"    # HTTP
      - "8443:443"   # HTTPS (可选)
      - "2222:22"    # SSH
    volumes:
      - gitlab_config:/etc/gitlab
      - gitlab_logs:/var/log/gitlab
      - gitlab_data:/var/opt/gitlab
      - ./backups:/var/opt/gitlab/backups
    shm_size: '256m'
    deploy:
      resources:
        limits:
          memory: 3G        # 内存限制
        reservations:
          memory: 1.5G      # 内存预留
    healthcheck:
      test: ["CMD", "/opt/gitlab/bin/gitlab-healthcheck", "--fail"]
      interval: 60s
      timeout: 30s
      retries: 5
      start_period: 200s
    networks:
      - gitlab_network

  # ================================
  # GitLab Runner (可选)
  # ================================
  gitlab-runner:
    image: gitlab/gitlab-runner:alpine3.18-v16.7.0
    container_name: gitlab-runner-light
    restart: unless-stopped
    profiles: ["runner"]  # 可选服务
    volumes:
      - runner_config:/etc/gitlab-runner
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - DOCKER_HOST=unix:///var/run/docker.sock
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 128M
    networks:
      - gitlab_network
    depends_on:
      gitlab:
        condition: service_healthy

  # ================================
  # Redis (外部缓存 - 可选优化)
  # ================================
  redis:
    image: redis:7.0-alpine
    container_name: gitlab-redis
    restart: unless-stopped
    profiles: ["external-redis"]  # 可选服务
    command: >
      redis-server
      --maxmemory 128mb
      --maxmemory-policy allkeys-lru
      --save 900 1
      --save 300 10
      --save 60 10000
    volumes:
      - redis_data:/data
    deploy:
      resources:
        limits:
          memory: 150M
        reservations:
          memory: 64M
    networks:
      - gitlab_network

# ================================
# 网络配置
# ================================
networks:
  gitlab_network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.21.0.0/16

# ================================
# 存储卷配置  
# ================================
volumes:
  gitlab_config:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./config

  gitlab_logs:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./logs

  gitlab_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data

  runner_config:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./runner

  redis_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./redis
```

### 环境变量配置

```bash
# .env 文件
# ================================
# GitLab 基本配置
# ================================
GITLAB_EXTERNAL_URL=http://localhost:8080
GITLAB_EMAIL_FROM=noreply@gitlab.local

# ================================
# 邮件配置 (可选)
# ================================
SMTP_ENABLE=false
SMTP_SERVER=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_DOMAIN=gmail.com

# ================================
# GitLab Runner 配置
# ================================
GITLAB_RUNNER_TOKEN=
GITLAB_RUNNER_URL=http://gitlab:80

# ================================
# 时区配置
# ================================
TZ=Asia/Shanghai

# ================================
# 备份配置
# ================================
BACKUP_RETENTION_DAYS=7
BACKUP_SCHEDULE=0 2 * * *

# ================================
# 安全配置
# ================================
GITLAB_ROOT_PASSWORD=SecureRootPassword123!
GITLAB_SIGNUP_ENABLED=false
```

### 初始化脚本

```bash
#!/bin/bash
# scripts/setup.sh - 初始化部署环境

set -e

echo "🚀 初始化轻量级 GitLab 部署环境..."

# 创建必要的目录
echo "📁 创建目录结构..."
mkdir -p {config,logs,data,backups,runner,redis,scripts}

# 设置正确的权限
echo "🔐 设置目录权限..."
sudo chown -R 998:998 config logs data
chmod -R 755 config logs data backups

# 创建环境变量文件
if [[ ! -f ".env" ]]; then
    echo "📝 创建环境变量文件..."
    cat > .env << 'EOF'
# GitLab 基本配置
GITLAB_EXTERNAL_URL=http://localhost:8080
GITLAB_EMAIL_FROM=noreply@gitlab.local

# 邮件配置 (可选)
SMTP_ENABLE=false
SMTP_SERVER=
SMTP_PORT=587
SMTP_USER=
SMTP_PASSWORD=
SMTP_DOMAIN=

# 时区设置
TZ=Asia/Shanghai

# 备份设置
BACKUP_RETENTION_DAYS=7
EOF
    echo "✅ 请编辑 .env 文件配置您的设置"
fi

# 检查系统资源
echo "🔍 检查系统资源..."
total_mem=$(free -m | awk 'NR==2{printf "%.0f", $2}')
available_mem=$(free -m | awk 'NR==2{printf "%.0f", $7}')

echo "总内存: ${total_mem}MB"
echo "可用内存: ${available_mem}MB"

if [[ $available_mem -lt 1500 ]]; then
    echo "⚠️ 警告: 可用内存不足 1.5GB，GitLab 可能运行缓慢"
    echo "💡 建议: 关闭其他应用程序或增加内存"
elif [[ $available_mem -lt 2000 ]]; then
    echo "⚠️ 注意: 可用内存较少，建议监控内存使用情况"
else
    echo "✅ 内存充足，可以正常运行"
fi

# 检查磁盘空间
echo "💾 检查磁盘空间..."
available_disk=$(df -BG . | tail -1 | awk '{print $4}' | sed 's/G//')

if [[ $available_disk -lt 10 ]]; then
    echo "⚠️ 警告: 可用磁盘空间不足 10GB"
    echo "💡 建议: 清理磁盘空间或使用外部存储"
else
    echo "✅ 磁盘空间充足: ${available_disk}GB 可用"
fi

# 创建便捷脚本
echo "🔧 创建管理脚本..."

# 启动脚本
cat > scripts/start.sh << 'EOF'
#!/bin/bash
echo "🚀 启动 GitLab..."
docker-compose up -d
echo "⏳ 等待 GitLab 启动 (约2-3分钟)..."
echo "📝 访问地址: http://localhost:8080"
echo "👤 默认用户: root"
echo "🔑 获取初始密码: docker-compose exec gitlab cat /etc/gitlab/initial_root_password"
EOF

# 停止脚本
cat > scripts/stop.sh << 'EOF'
#!/bin/bash
echo "⏸️ 停止 GitLab..."
docker-compose down
echo "✅ GitLab 已停止"
EOF

# 状态检查脚本
cat > scripts/status.sh << 'EOF'
#!/bin/bash
echo "📊 GitLab 状态检查"
echo "=================="
docker-compose ps
echo ""
echo "💾 磁盘使用:"
du -sh data config logs backups
echo ""
echo "🧠 内存使用:"
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" | head -2
EOF

# 日志查看脚本
cat > scripts/logs.sh << 'EOF'
#!/bin/bash
service=${1:-gitlab}
echo "📝 查看 $service 日志..."
docker-compose logs -f $service
EOF

# 设置脚本权限
chmod +x scripts/*.sh

echo "✅ 环境初始化完成！"
echo ""
echo "🎯 下一步操作:"
echo "1. 编辑 .env 文件: nano .env"  
echo "2. 启动 GitLab: ./scripts/start.sh"
echo "3. 查看状态: ./scripts/status.sh"
echo "4. 查看日志: ./scripts/logs.sh"
```

## GitLab 优化配置

### 1. 内存优化配置

```ruby
# config/gitlab.rb - GitLab 配置优化

# ================================
# 核心服务优化
# ================================

# Unicorn/Puma 工作进程 (减少内存占用)
puma['worker_processes'] = 2
puma['min_threads'] = 1
puma['max_threads'] = 4
puma['worker_timeout'] = 60
puma['worker_max_memory'] = 350_000_000  # 350MB

# Sidekiq 后台任务 (减少并发)
sidekiq['max_concurrency'] = 8
sidekiq['min_concurrency'] = 4
sidekiq['queue_groups'] = [
  "urgent,high",
  "default,low"
]

# PostgreSQL 数据库优化
postgresql['shared_buffers'] = "128MB"
postgresql['effective_cache_size'] = "256MB"
postgresql['work_mem'] = "4MB"
postgresql['maintenance_work_mem'] = "32MB"
postgresql['max_worker_processes'] = 2
postgresql['max_parallel_workers_per_gather'] = 1
postgresql['max_parallel_workers'] = 2
postgresql['wal_buffers'] = "8MB"

# Redis 缓存优化
redis['maxmemory'] = "100mb"
redis['maxmemory_policy'] = "allkeys-lru"
redis['save'] = "900 1 300 10 60 10000"

# ================================
# 禁用非必需服务
# ================================

# 监控服务 (节省约300-500MB内存)
prometheus_monitoring['enable'] = false
alertmanager['enable'] = false
node_exporter['enable'] = false
redis_exporter['enable'] = false
postgres_exporter['enable'] = false
gitlab_exporter['enable'] = false
grafana['enable'] = false

# 容器扫描 (小团队通常不需要)
gitlab_rails['gitlab_default_projects_features_container_registry'] = false

# GitLab Pages (如果不使用静态站点)
pages_nginx['enable'] = false
gitlab_pages['enable'] = false

# GitLab KAS (Kubernetes代理，小团队通常不需要)
gitlab_kas['enable'] = false

# ================================
# Git 和存储优化
# ================================

# Gitaly Git 服务优化
gitaly['configuration'] = {
  concurrency: [
    {
      'rpc' => "/gitaly.SmartHTTPService/PostReceivePack",
      'max_per_repo' => 2
    },
    {
      'rpc' => "/gitaly.SSHService/SSHUploadPack", 
      'max_per_repo' => 2
    }
  ],
  git: {
    catfile_cache_size: 50,
    use_bundled_git: true
  }
}

# Git 优化
gitlab_shell['git_timeout'] = 600
gitlab_rails['gitlab_shell_git_timeout'] = 600

# ================================
# Web 服务器优化
# ================================

# Nginx 优化
nginx['worker_processes'] = 2
nginx['worker_connections'] = 512
nginx['keepalive_timeout'] = 5
nginx['client_max_body_size'] = '100m'  # 限制上传大小

# ================================
# 应用设置优化
# ================================

# 限制并发操作
gitlab_rails['rack_timeout_service_timeout'] = 30
gitlab_rails['gitlab_default_projects_limit'] = 10
gitlab_rails['gitlab_default_can_create_group'] = false

# 邮件队列优化
gitlab_rails['incoming_email_enabled'] = false
gitlab_rails['service_desk_email_enabled'] = false

# 自动清理
gitlab_rails['expire_build_artifacts_worker_cron'] = "50 * * * *"
gitlab_rails['repository_cleanup_worker_cron'] = "0 4 * * 0"

# ================================
# 备份优化
# ================================

# 备份设置
gitlab_rails['backup_keep_time'] = 604800  # 7天
gitlab_rails['backup_pg_schema'] = 'public'
gitlab_rails['backup_path'] = "/var/opt/gitlab/backups"
gitlab_rails['backup_gitaly_backup_path'] = "/opt/gitlab/embedded/bin/gitaly-backup"

# ================================
# 日志优化
# ================================

# 减少日志级别和保留时间
logging['logrotate_frequency'] = "weekly"
logging['logrotate_rotate'] = 4
logging['logrotate_compress'] = "compress"
logging['logrotate_delaycompress'] = "delaycompress"

# GitLab 应用日志
gitlab_rails['log_level'] = 'WARN'
```

### 2. Docker Compose 资源限制

```yaml
# docker-compose.override.yml - 资源限制配置
version: '3.8'

services:
  gitlab:
    deploy:
      resources:
        limits:
          memory: 3G          # 最大内存限制
          cpus: '2.0'         # CPU 限制
        reservations:
          memory: 1.5G        # 内存预留
          cpus: '1.0'         # CPU 预留
    # 添加内存交换限制
    memswap_limit: 3G
    # 设置内存交换度
    sysctls:
      - vm.swappiness=10
    # 优化共享内存
    shm_size: '256m'

  gitlab-runner:
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '1.0'
        reservations:
          memory: 128M
          cpus: '0.5'
    # 限制并发作业
    environment:
      - DOCKER_MEMORY=256m
      - CONCURRENT=2
```

## 备份和维护

### 1. 自动备份脚本

```bash
#!/bin/bash
# scripts/backup.sh - GitLab 备份脚本

set -e

BACKUP_DIR="./backups"
DATE=$(date +"%Y%m%d_%H%M%S")
RETENTION_DAYS=7

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a backup.log
}

log "🗄️ 开始 GitLab 备份..."

# 检查 GitLab 状态
if ! docker-compose ps gitlab | grep -q "Up"; then
    log "❌ GitLab 服务未运行"
    exit 1
fi

# 创建 GitLab 备份
log "📦 创建 GitLab 数据备份..."
docker-compose exec -T gitlab gitlab-backup create BACKUP=${DATE}

# 备份配置文件
log "⚙️ 备份配置文件..."
tar -czf "${BACKUP_DIR}/config_backup_${DATE}.tar.gz" config/

# 计算备份大小
backup_file="${BACKUP_DIR}/${DATE}_gitlab_backup.tar"
if [[ -f "$backup_file" ]]; then
    backup_size=$(du -h "$backup_file" | cut -f1)
    log "✅ 备份完成: $backup_file ($backup_size)"
else
    log "❌ 备份文件未找到"
    exit 1
fi

# 清理旧备份
log "🧹 清理 $RETENTION_DAYS 天前的备份..."
find "$BACKUP_DIR" -name "*gitlab_backup.tar" -mtime +$RETENTION_DAYS -delete
find "$BACKUP_DIR" -name "config_backup_*.tar.gz" -mtime +$RETENTION_DAYS -delete

log "✅ 备份任务完成"

# 发送通知 (可选)
if [[ -n "$WEBHOOK_URL" ]]; then
    curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"GitLab 备份完成: $backup_file ($backup_size)\"}" \
        "$WEBHOOK_URL" || true
fi
```

### 2. 恢复脚本

```bash
#!/bin/bash
# scripts/restore.sh - GitLab 恢复脚本

set -e

BACKUP_FILE=${1}
CONFIG_BACKUP=${2}

if [[ -z "$BACKUP_FILE" ]]; then
    echo "用法: $0 <备份文件> [配置备份文件]"
    echo "可用备份:"
    ls -la backups/*gitlab_backup.tar 2>/dev/null || echo "无备份文件"
    exit 1
fi

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "🔄 开始 GitLab 恢复..."

# 停止 GitLab
log "⏸️ 停止 GitLab 服务..."
docker-compose stop gitlab

# 复制备份文件到容器内
log "📋 复制备份文件..."
docker-compose start gitlab
sleep 30

backup_name=$(basename "$BACKUP_FILE" .tar | sed 's/_gitlab_backup$//')
docker-compose cp "$BACKUP_FILE" gitlab:/var/opt/gitlab/backups/

# 执行恢复
log "🔄 执行数据恢复..."
docker-compose exec gitlab gitlab-backup restore BACKUP="$backup_name" force=yes

# 恢复配置 (可选)
if [[ -n "$CONFIG_BACKUP" && -f "$CONFIG_BACKUP" ]]; then
    log "⚙️ 恢复配置文件..."
    tar -xzf "$CONFIG_BACKUP" -C ./
fi

# 重启 GitLab
log "🚀 重启 GitLab..."
docker-compose restart gitlab

log "✅ 恢复完成！请等待 GitLab 启动..."
log "🌐 访问地址: http://localhost:8080"
```

### 3. 维护脚本

```bash
#!/bin/bash
# scripts/maintenance.sh - 日常维护任务

set -e

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "🔧 开始日常维护..."

# 清理 Docker 资源
log "🧹 清理 Docker 资源..."
docker system prune -f
docker volume prune -f

# 检查磁盘使用
log "💾 检查磁盘使用..."
df -h
echo ""
du -sh data config logs backups

# 检查内存使用
log "🧠 检查内存使用..."
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"

# 清理 GitLab 缓存
log "🗑️ 清理 GitLab 缓存..."
docker-compose exec gitlab gitlab-rails runner "Rails.cache.clear"

# 优化数据库
log "⚡ 优化数据库..."
docker-compose exec gitlab gitlab-rake db:reindex

# 检查服务健康状态
log "🏥 检查服务健康状态..."
if docker-compose exec gitlab gitlab-ctl status; then
    log "✅ GitLab 服务正常"
else
    log "❌ GitLab 服务异常"
fi

log "✅ 维护任务完成"
```

### 4. 定时任务设置

```bash
#!/bin/bash
# scripts/setup-cron.sh - 设置定时任务

echo "⏰ 设置定时任务..."

# 添加到 crontab
(crontab -l 2>/dev/null || echo "") | grep -v "gitlab" | {
    cat
    echo "# GitLab 自动备份 - 每天凌晨2点"
    echo "0 2 * * * cd $(pwd) && ./scripts/backup.sh"
    echo "# GitLab 维护任务 - 每周日凌晨3点"
    echo "0 3 * * 0 cd $(pwd) && ./scripts/maintenance.sh"
} | crontab -

echo "✅ 定时任务设置完成"
crontab -l
```

## 使用指南

### 1. 首次配置

```bash
# 1. 访问 GitLab
open http://localhost:8080

# 2. 使用 root 账户登录
# 用户名: root
# 获取初始密码:
docker-compose exec gitlab cat /etc/gitlab/initial_root_password

# 3. 修改密码和配置
# - 登录后立即修改 root 密码
# - 在 Admin Area > Settings > Sign-up restrictions 关闭公开注册
# - 在 Admin Area > Settings > Account and Limit 设置项目限制
```

### 2. 用户管理

```bash
# 创建新用户 (通过 Web 界面)
# 1. Admin Area > Users > New User
# 2. 填写用户信息，设置为 Regular user
# 3. 发送密码重置邮件或手动设置密码

# 创建组织
# 1. Groups > Create group
# 2. 设置组织名称和可见性
# 3. 添加成员并设置权限 (Developer, Maintainer 等)
```

### 3. 项目管理

```bash
# 创建项目
# 1. New project > Create blank project
# 2. 设置项目名称、描述和可见性
# 3. 初始化 README 和 .gitignore

# 项目设置建议
# - Repository > Protected branches: 保护 main/master 分支
# - CI/CD > General pipelines: 限制 pipeline 超时时间
# - Settings > General > Visibility: 设置合适的可见性级别
```

### 4. Git 操作

```bash
# 克隆项目
git clone http://localhost:8080/group/project.git

# 添加远程仓库
git remote add origin http://localhost:8080/group/project.git

# SSH 密钥配置 (端口 2222)
ssh-keygen -t ed25519 -C "your_email@example.com"
# 在 GitLab Profile > SSH Keys 添加公钥

# SSH 克隆 (注意端口)
git clone ssh://git@localhost:2222/group/project.git

# 配置 SSH 客户端 ~/.ssh/config
echo "Host gitlab.local
  HostName localhost
  Port 2222
  User git" >> ~/.ssh/config
```

## 故障排除

### 1. 常见问题解决

#### 内存不足

```bash
# 症状: GitLab 启动慢或崩溃
# 解决方案:

# 1. 检查内存使用
free -h
docker stats --no-stream

# 2. 调整 GitLab 配置
# 编辑 docker-compose.yml 减少服务数量:
GITLAB_OMNIBUS_CONFIG: |
  unicorn['worker_processes'] = 1  # 减少到 1 个工作进程
  sidekiq['max_concurrency'] = 5   # 减少并发任务

# 3. 重启 GitLab
docker-compose restart gitlab
```

#### 磁盘空间不足

```bash
# 检查磁盘使用
df -h
du -sh data config logs backups

# 清理方案:
# 1. 清理旧备份
find backups -name "*.tar" -mtime +3 -delete

# 2. 清理 Docker 缓存
docker system prune -f
docker volume prune -f

# 3. 清理 GitLab 日志
docker-compose exec gitlab find /var/log/gitlab -name "*.log" -mtime +7 -delete

# 4. 清理构建产物
docker-compose exec gitlab gitlab-rake gitlab:cleanup:build_artifacts
```

#### 服务无法启动

```bash
# 查看详细日志
docker-compose logs gitlab

# 常见错误及解决:

# 1. 端口冲突
ss -tulpn | grep :8080
# 修改 docker-compose.yml 中的端口映射

# 2. 权限问题
sudo chown -R 998:998 data config logs
chmod -R 755 data config logs

# 3. 配置错误
# 检查 .env 文件语法
# 重置配置: rm -rf config/* && docker-compose restart gitlab
```

### 2. 性能优化

```bash
# 监控资源使用
./scripts/monitoring.sh

# 内容:
#!/bin/bash
# scripts/monitoring.sh

echo "📊 GitLab 性能监控"
echo "=================="

echo "🧠 内存使用:"
docker stats --no-stream --format "table {{.Container}}\t{{.MemUsage}}\t{{.MemPerc}}" gitlab

echo -e "\n💾 磁盘使用:"
df -h | grep -E "(Size|gitlab|/$)"

echo -e "\n🔄 CPU 负载:"
uptime

echo -e "\n📈 GitLab 内部状态:"
docker-compose exec gitlab gitlab-ctl status

echo -e "\n🚀 响应时间测试:"
time curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/users/sign_in
```

### 3. 诊断工具

```bash
#!/bin/bash
# scripts/diagnostic.sh - GitLab 诊断工具

echo "🔍 GitLab 诊断工具"
echo "=================="

# 检查配置
echo "⚙️ 配置检查:"
docker-compose config --quiet && echo "✅ Docker Compose 配置正确" || echo "❌ Docker Compose 配置错误"

# 检查网络连通性
echo -e "\n🌐 网络检查:"
curl -s -o /dev/null -w "HTTP Status: %{http_code}, Time: %{time_total}s\n" http://localhost:8080 || echo "❌ 无法访问 GitLab"

# 检查服务健康
echo -e "\n🏥 健康检查:"
docker-compose ps

# 检查日志错误
echo -e "\n📝 最近错误日志:"
docker-compose logs --tail=50 gitlab | grep -i error | tail -10 || echo "未发现明显错误"

# 生成诊断报告
echo -e "\n📋 诊断报告生成..."
cat > diagnostic_report.txt << EOF
GitLab 诊断报告
生成时间: $(date)

系统信息:
$(uname -a)

内存信息:
$(free -h)

磁盘信息:
$(df -h)

Docker 版本:
$(docker --version)
$(docker-compose --version)

GitLab 状态:
$(docker-compose ps)

GitLab 配置:
$(docker-compose exec gitlab cat /etc/gitlab/gitlab.rb | grep -v "^#" | head -20)
EOF

echo "✅ 诊断报告已生成: diagnostic_report.txt"
```

## 扩展选项

### 1. GitLab Runner 配置

```bash
# 启用 GitLab Runner
docker-compose --profile runner up -d gitlab-runner

# 注册 Runner
docker-compose exec gitlab-runner gitlab-runner register \
  --url http://gitlab:80 \
  --registration-token YOUR_REGISTRATION_TOKEN \
  --executor docker \
  --description "Lightweight Docker Runner" \
  --docker-image docker:latest \
  --docker-privileged false \
  --docker-memory 256m \
  --docker-cpus 1
```

### 2. SSL/HTTPS 配置

```yaml
# docker-compose.ssl.yml - SSL 配置覆盖
version: '3.8'

services:
  gitlab:
    environment:
      GITLAB_OMNIBUS_CONFIG: |
        external_url 'https://your-domain.com'
        nginx['redirect_http_to_https'] = true
        nginx['ssl_certificate'] = "/etc/gitlab/ssl/gitlab.crt"
        nginx['ssl_certificate_key'] = "/etc/gitlab/ssl/gitlab.key"
    volumes:
      - ./ssl:/etc/gitlab/ssl:ro
    ports:
      - "443:443"
      - "80:80"

# 使用 Let's Encrypt
# certbot certonly --standalone -d your-domain.com
# cp /etc/letsencrypt/live/your-domain.com/*.pem ./ssl/
```

### 3. 外部数据库配置

```yaml
# docker-compose.external-db.yml - 外部数据库
version: '3.8'

services:
  postgres:
    image: postgres:13-alpine
    environment:
      POSTGRES_DB: gitlabhq_production
      POSTGRES_USER: gitlab
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    deploy:
      resources:
        limits:
          memory: 512M

  gitlab:
    environment:
      GITLAB_OMNIBUS_CONFIG: |
        postgresql['enable'] = false
        gitlab_rails['db_adapter'] = 'postgresql'
        gitlab_rails['db_encoding'] = 'utf8'
        gitlab_rails['db_host'] = 'postgres'
        gitlab_rails['db_port'] = 5432
        gitlab_rails['db_database'] = 'gitlabhq_production'
        gitlab_rails['db_username'] = 'gitlab'
        gitlab_rails['db_password'] = 'secure_password'
```

### 4. 监控集成

```yaml
# docker-compose.monitoring.yml - 监控服务
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    deploy:
      resources:
        limits:
          memory: 256M

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    deploy:
      resources:
        limits:
          memory: 256M
```

---

**📚 文档信息**
- **文档版本**: v1.0
- **创建日期**: 2025年8月12日
- **适用版本**: GitLab CE 16.7+, Docker Compose 2.0+
- **维护者**: 轻量级 GitLab 团队

**🎯 特性总结**
- ✅ 最低 2GB 内存运行
- ✅ 完整的 Git 仓库管理
- ✅ CI/CD Pipeline 支持
- ✅ 用户和权限管理
- ✅ 自动备份和恢复
- ✅ 资源使用监控
- ✅ 故障诊断工具
- ✅ 一键部署脚本

**💡 优化重点**
1. **内存优化**: 禁用非必需服务，减少工作进程
2. **性能调优**: PostgreSQL 和 Redis 参数优化
3. **资源限制**: Docker 资源约束和监控
4. **自动化**: 备份、维护和监控脚本
5. **易用性**: 便捷的管理脚本和文档

适合 1-5 人小团队的完整 Git 代码管理解决方案！🚀