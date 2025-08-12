# GitLab NAS 部署指南

## 概述

本指南详细介绍如何在 NAS 设备上使用 Docker Compose 部署 GitLab，专为 1-20 人的小团队优化，包含完整的 CI/CD 配置、性能优化、备份策略和监控方案。

## 目录

- [系统要求](#系统要求)
- [架构设计](#架构设计)
- [Docker Compose 配置](#docker-compose-配置)
- [GitLab 配置优化](#gitlab-配置优化)
- [CI/CD Runner 配置](#cicd-runner-配置)
- [SSL 证书配置](#ssl-证书配置)
- [备份策略](#备份策略)
- [监控和日志](#监控和日志)
- [维护和升级](#维护和升级)
- [故障排除](#故障排除)

## 系统要求

### 最低硬件要求

```yaml
CPU: 4 核心 (推荐 8 核心)
内存: 8GB (推荐 16GB+)
存储: 200GB+ SSD (推荐 500GB+)
网络: 千兆网络连接
```

### NAS 系统要求

- **支持 Docker**: Synology DSM 7.0+, QNAP QTS 5.0+, 或其他支持 Docker 的 NAS 系统
- **SSH 访问权限**: 需要 root 或 sudo 权限
- **固定 IP**: 建议为 NAS 设置静态 IP 地址

### 端口规划

```yaml
GitLab Web:     80, 443 (HTTP/HTTPS)
GitLab SSH:     2222 (Git SSH)
GitLab Registry: 5005 (Docker Registry)
Postgres:       5432 (内部)
Redis:          6379 (内部)
Prometheus:     9090 (监控)
Grafana:        3000 (监控面板)
```

## 架构设计

### 组件架构

```
┌─────────────────────────────────────────────────────────────┐
│                        NAS 设备                              │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │   GitLab    │  │ PostgreSQL  │  │    Redis    │          │
│  │   Server    │  │  Database   │  │    Cache    │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │ GitLab      │  │ Prometheus  │  │   Grafana   │          │
│  │ Runner      │  │ Monitoring  │  │ Dashboard   │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐ │
│  │               Docker Engine                             │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                NAS 操作系统                              │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 数据存储结构

```
/volume1/gitlab/
├── config/           # GitLab 配置文件
├── logs/             # 日志文件  
├── data/             # GitLab 数据
├── postgres/         # 数据库数据
├── redis/            # Redis 数据
├── runner/           # Runner 配置
├── backups/          # 备份文件
├── ssl/              # SSL 证书
└── monitoring/       # 监控数据
```

## Docker Compose 配置

### 主配置文件 docker-compose.yml

```yaml
version: '3.8'

services:
  # PostgreSQL 数据库
  gitlab-postgres:
    image: postgres:14-alpine
    container_name: gitlab-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: gitlabhq_production
      POSTGRES_USER: gitlab
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --lc-collate=C --lc-ctype=C"
    volumes:
      - ./postgres:/var/lib/postgresql/data
      - ./backups/postgres:/backups
    networks:
      - gitlab-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gitlab -d gitlabhq_production"]
      interval: 30s
      timeout: 10s
      retries: 3
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G

  # Redis 缓存
  gitlab-redis:
    image: redis:7-alpine
    container_name: gitlab-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD} --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - ./redis:/data
    networks:
      - gitlab-network
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M

  # GitLab 主服务
  gitlab:
    image: gitlab/gitlab-ce:16.11.1-ce.0
    container_name: gitlab
    restart: unless-stopped
    hostname: ${GITLAB_HOSTNAME}
    environment:
      GITLAB_OMNIBUS_CONFIG: |
        # 外部 URL 配置
        external_url 'https://${GITLAB_HOSTNAME}'
        
        # SSL 配置
        nginx['ssl_certificate'] = "/etc/gitlab/ssl/${GITLAB_HOSTNAME}.crt"
        nginx['ssl_certificate_key'] = "/etc/gitlab/ssl/${GITLAB_HOSTNAME}.key"
        nginx['ssl_protocols'] = "TLSv1.2 TLSv1.3"
        nginx['ssl_ciphers'] = "ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256"
        nginx['ssl_prefer_server_ciphers'] = "on"
        nginx['ssl_session_cache'] = "builtin:1000 shared:SSL:10m"
        nginx['ssl_session_timeout'] = "5m"
        
        # SSH 配置
        gitlab_rails['gitlab_shell_ssh_port'] = 2222
        
        # 数据库配置
        gitlab_rails['db_adapter'] = 'postgresql'
        gitlab_rails['db_encoding'] = 'unicode'
        gitlab_rails['db_host'] = 'gitlab-postgres'
        gitlab_rails['db_port'] = 5432
        gitlab_rails['db_database'] = 'gitlabhq_production'
        gitlab_rails['db_username'] = 'gitlab'
        gitlab_rails['db_password'] = '${POSTGRES_PASSWORD}'
        
        # Redis 配置
        gitlab_rails['redis_host'] = 'gitlab-redis'
        gitlab_rails['redis_port'] = 6379
        gitlab_rails['redis_password'] = '${REDIS_PASSWORD}'
        gitlab_rails['redis_database'] = 0
        
        # 邮件配置
        gitlab_rails['smtp_enable'] = true
        gitlab_rails['smtp_address'] = '${SMTP_SERVER}'
        gitlab_rails['smtp_port'] = 587
        gitlab_rails['smtp_user_name'] = '${SMTP_USERNAME}'
        gitlab_rails['smtp_password'] = '${SMTP_PASSWORD}'
        gitlab_rails['smtp_domain'] = '${SMTP_DOMAIN}'
        gitlab_rails['smtp_authentication'] = 'login'
        gitlab_rails['smtp_enable_starttls_auto'] = true
        gitlab_rails['smtp_tls'] = false
        gitlab_rails['smtp_openssl_verify_mode'] = 'peer'
        gitlab_rails['gitlab_email_from'] = '${GITLAB_EMAIL_FROM}'
        gitlab_rails['gitlab_email_display_name'] = 'GitLab'
        
        # 小团队优化配置
        puma['worker_processes'] = 2
        puma['worker_timeout'] = 60
        puma['worker_memory_limit_mb'] = 1024
        
        sidekiq['max_concurrency'] = 10
        sidekiq['min_concurrency'] = 1
        
        # 禁用不必要的服务以节省资源
        prometheus_monitoring['enable'] = false
        grafana['enable'] = false
        alertmanager['enable'] = false
        node_exporter['enable'] = false
        redis_exporter['enable'] = false
        postgres_exporter['enable'] = false
        
        # GitLab Pages 禁用
        pages_external_url "https://pages.${GITLAB_HOSTNAME}"
        gitlab_pages['enable'] = false
        
        # Container Registry 配置
        registry_external_url 'https://${GITLAB_HOSTNAME}:5005'
        gitlab_rails['registry_enabled'] = true
        registry['enable'] = true
        registry_nginx['ssl_certificate'] = "/etc/gitlab/ssl/${GITLAB_HOSTNAME}.crt"
        registry_nginx['ssl_certificate_key'] = "/etc/gitlab/ssl/${GITLAB_HOSTNAME}.key"
        
        # 备份配置
        gitlab_rails['backup_keep_time'] = 604800  # 7 天
        gitlab_rails['backup_path'] = "/var/opt/gitlab/backups"
        gitlab_rails['backup_archive_permissions'] = 0644
        gitlab_rails['backup_upload_connection'] = {
          'provider' => 'local'
        }
        
        # Git 配置优化
        gitlab_rails['git_timeout'] = 10
        
        # 日志配置
        logging['logrotate_frequency'] = "weekly"
        logging['logrotate_rotate'] = 4
        logging['logrotate_compress'] = "compress"
        logging['logrotate_delaycompress'] = "delaycompress"
        
        # 性能优化
        gitlab_rails['artifacts_enabled'] = true
        gitlab_rails['artifacts_path'] = "/var/opt/gitlab/gitlab-artifacts"
        gitlab_rails['lfs_enabled'] = true
        gitlab_rails['lfs_storage_path'] = "/var/opt/gitlab/gitlab-lfs"
        
        # 安全配置
        gitlab_rails['initial_root_password'] = '${GITLAB_ROOT_PASSWORD}'
        gitlab_rails['display_initial_root_password'] = false
        
    ports:
      - "80:80"
      - "443:443"
      - "2222:22"
      - "5005:5005"
    volumes:
      - ./config:/etc/gitlab
      - ./logs:/var/log/gitlab
      - ./data:/var/opt/gitlab
      - ./ssl:/etc/gitlab/ssl
      - ./backups/gitlab:/var/opt/gitlab/backups
    networks:
      - gitlab-network
    depends_on:
      gitlab-postgres:
        condition: service_healthy
      gitlab-redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "/opt/gitlab/bin/gitlab-healthcheck", "--fail", "--max-time", "10"]
      interval: 60s
      timeout: 30s
      retries: 5
      start_period: 180s
    deploy:
      resources:
        limits:
          memory: 6G
        reservations:
          memory: 4G

  # GitLab Runner
  gitlab-runner:
    image: gitlab/gitlab-runner:latest
    container_name: gitlab-runner
    restart: unless-stopped
    volumes:
      - ./runner:/etc/gitlab-runner
      - /var/run/docker.sock:/var/run/docker.sock
      - ./data/runner-cache:/cache
    networks:
      - gitlab-network
    depends_on:
      - gitlab
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M

  # Prometheus 监控
  prometheus:
    image: prom/prometheus:latest
    container_name: gitlab-prometheus
    restart: unless-stopped
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'
      - '--web.enable-admin-api'
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/prometheus-data:/prometheus
    networks:
      - gitlab-network
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M

  # Grafana 监控面板
  grafana:
    image: grafana/grafana:latest
    container_name: gitlab-grafana
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    volumes:
      - ./monitoring/grafana-data:/var/lib/grafana
      - ./monitoring/grafana-dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana-datasources:/etc/grafana/provisioning/datasources
    networks:
      - gitlab-network
    depends_on:
      - prometheus
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M

  # 备份服务
  gitlab-backup:
    image: alpine:latest
    container_name: gitlab-backup
    restart: "no"
    volumes:
      - ./backups:/backups
      - ./data:/var/opt/gitlab:ro
      - ./config:/etc/gitlab:ro
      - ./postgres:/var/lib/postgresql/data:ro
    networks:
      - gitlab-network
    command: |
      sh -c "
        apk add --no-cache dcron postgresql-client
        echo '0 2 * * * /backups/backup-script.sh' | crontab -
        crond -f
      "
    depends_on:
      - gitlab
      - gitlab-postgres

networks:
  gitlab-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16

volumes:
  postgres-data:
  redis-data:
  gitlab-config:
  gitlab-logs:
  gitlab-data:
```

### 环境变量配置 .env

```bash
# 基础配置
GITLAB_HOSTNAME=gitlab.yourdomain.com
GITLAB_ROOT_PASSWORD=your-super-secure-root-password
COMPOSE_PROJECT_NAME=gitlab

# 数据库配置
POSTGRES_PASSWORD=your-postgres-password

# Redis 配置
REDIS_PASSWORD=your-redis-password

# 邮件配置
SMTP_SERVER=smtp.gmail.com
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-smtp-password
SMTP_DOMAIN=gmail.com
GITLAB_EMAIL_FROM=gitlab@yourdomain.com

# 监控配置
GRAFANA_PASSWORD=your-grafana-password

# 备份配置
BACKUP_RETENTION_DAYS=30
BACKUP_ENCRYPTION_KEY=your-backup-encryption-key
```

## GitLab 配置优化

### 小团队性能优化配置

为 1-20 人团队定制的 `gitlab.rb` 优化配置：

```ruby
# /volume1/gitlab/config/gitlab.rb

# 基础配置
external_url 'https://gitlab.yourdomain.com'

# 小团队优化：减少资源消耗
puma['worker_processes'] = 2
puma['min_threads'] = 1
puma['max_threads'] = 8
puma['worker_timeout'] = 60
puma['worker_memory_limit_mb'] = 1024

# Sidekiq 队列优化
sidekiq['max_concurrency'] = 10
sidekiq['min_concurrency'] = 1
sidekiq['queue_groups'] = [
  "urgent:2",
  "default:1", 
  "low:1"
]

# PostgreSQL 连接池优化
postgresql['shared_preload_libraries'] = 'pg_stat_statements'
postgresql['max_connections'] = 100
postgresql['shared_buffers'] = "512MB"
postgresql['effective_cache_size'] = "2GB"
postgresql['maintenance_work_mem'] = "128MB"
postgresql['checkpoint_completion_target'] = 0.9
postgresql['wal_buffers'] = "16MB"
postgresql['default_statistics_target'] = 100

# Redis 优化
redis['maxmemory'] = '512mb'
redis['maxmemory_policy'] = 'allkeys-lru'
redis['save'] = '900 1 300 10 60 10000'

# Nginx 优化
nginx['worker_processes'] = 2
nginx['worker_connections'] = 1024
nginx['keepalive_timeout'] = 65
nginx['gzip'] = "on"
nginx['gzip_comp_level'] = 6
nginx['gzip_types'] = [
  "text/plain",
  "text/css", 
  "application/json",
  "application/javascript",
  "text/xml",
  "application/xml",
  "application/xml+rss",
  "text/javascript"
]

# Git 配置优化
gitlab_rails['git_timeout'] = 10
gitlab_shell['git_timeout'] = 30
gitlab_rails['git_max_size'] = 100  # 100MB

# 禁用不必要的功能
prometheus_monitoring['enable'] = false
grafana['enable'] = false
alertmanager['enable'] = false
gitlab_pages['enable'] = false
gitlab_kas['enable'] = false

# 邮件配置
gitlab_rails['smtp_enable'] = true
gitlab_rails['smtp_address'] = ENV['SMTP_SERVER']
gitlab_rails['smtp_port'] = 587
gitlab_rails['smtp_authentication'] = 'login'
gitlab_rails['smtp_enable_starttls_auto'] = true

# 备份配置
gitlab_rails['backup_keep_time'] = 604800  # 7天
gitlab_rails['backup_path'] = '/var/opt/gitlab/backups'

# 安全配置
gitlab_rails['webhook_timeout'] = 10
gitlab_rails['max_request_duration_seconds'] = 60
gitlab_rails['rate_limit_requests_per_period'] = 300
gitlab_rails['rate_limit_period'] = 60

# 存储优化
gitlab_rails['artifacts_enabled'] = true
gitlab_rails['artifacts_expire_in'] = "30 days"
gitlab_rails['lfs_enabled'] = true
gitlab_rails['packages_enabled'] = true
```

### 内存使用优化

```bash
# 系统内存分配建议（总内存 8GB）
GitLab主服务: 4GB
PostgreSQL: 2GB  
Redis: 512MB
GitLab Runner: 1GB
系统预留: 512MB
```

### 磁盘 I/O 优化

```bash
# 在 NAS 上优化磁盘性能
# 1. 使用 SSD 存储 GitLab 数据
# 2. 分离日志和数据存储
# 3. 启用 Docker 镜像层缓存

# docker-compose.override.yml
version: '3.8'
services:
  gitlab:
    volumes:
      # 将频繁写入的目录放在 SSD 上
      - /volume1/ssd/gitlab/data:/var/opt/gitlab
      - /volume1/hdd/gitlab/logs:/var/log/gitlab
      - /volume1/ssd/gitlab/config:/etc/gitlab
```

## CI/CD Runner 配置

### Runner 注册脚本

```bash
#!/bin/bash
# scripts/register-runner.sh

# 等待 GitLab 启动完成
echo "等待 GitLab 启动..."
until docker exec gitlab gitlab-ctl status | grep "run: gitaly"; do
  sleep 10
done

# 获取 Runner 注册 Token
echo "获取 Runner Token..."
RUNNER_TOKEN=$(docker exec gitlab gitlab-rails runner -e production "puts Gitlab::CurrentSettings.current_application_settings.runners_registration_token")

# 注册 Docker Runner
docker exec -it gitlab-runner gitlab-runner register \
  --non-interactive \
  --url "https://gitlab.yourdomain.com/" \
  --registration-token "$RUNNER_TOKEN" \
  --executor "docker" \
  --docker-image alpine:latest \
  --description "NAS Docker Runner" \
  --tag-list "docker,nas,general" \
  --run-untagged="true" \
  --locked="false" \
  --access-level="not_protected" \
  --docker-privileged="true" \
  --docker-volumes="/var/run/docker.sock:/var/run/docker.sock" \
  --docker-volumes="/cache" \
  --docker-network-mode="gitlab_gitlab-network"

echo "Runner 注册完成！"
```

### Runner 配置优化

```toml
# runner/config.toml
concurrent = 3
check_interval = 0

[session_server]
  session_timeout = 1800

[[runners]]
  name = "NAS Docker Runner"
  url = "https://gitlab.yourdomain.com/"
  token = "your-runner-token"
  executor = "docker"
  
  [runners.custom_build_dir]
  
  [runners.cache]
    Type = "local"
    Path = "/cache"
    
  [runners.docker]
    tls_verify = false
    image = "alpine:latest"
    privileged = true
    disable_entrypoint_overwrite = false
    oom_kill_disable = false
    disable_cache = false
    volumes = [
      "/var/run/docker.sock:/var/run/docker.sock",
      "/cache"
    ]
    shm_size = 0
    network_mode = "gitlab_gitlab-network"
    
    # 资源限制
    memory = "1g"
    memory_swap = "2g"
    memory_reservation = "512m"
    cpus = "2"
    
    # 镜像拉取策略
    pull_policy = "if-not-present"
    
    # 网络配置
    network_mode = "gitlab_gitlab-network"
    
    # 清理配置
    volumes_from = []
    links = []
    allowed_images = ["*"]
    allowed_services = ["*"]
```

### CI/CD 管道模板

创建 `.gitlab-ci-templates` 目录结构：

```yaml
# .gitlab-ci-templates/node-app.yml
# Node.js 应用 CI/CD 模板

stages:
  - test
  - build
  - deploy

variables:
  NODE_VERSION: "18"
  CACHE_KEY: "$CI_COMMIT_REF_SLUG-node"

# 缓存配置
.node_cache: &node_cache
  cache:
    key: $CACHE_KEY
    paths:
      - node_modules/
      - .npm/
    policy: pull-push

# 测试阶段
test:
  stage: test
  image: node:$NODE_VERSION-alpine
  <<: *node_cache
  before_script:
    - npm ci --cache .npm --prefer-offline
  script:
    - npm run lint
    - npm run test:unit
    - npm run test:coverage
  coverage: '/Lines\s*:\s*(\d+\.?\d*)%/'
  artifacts:
    reports:
      coverage_report:
        coverage_format: cobertura
        path: coverage/cobertura-coverage.xml
    paths:
      - coverage/
    expire_in: 1 week
  only:
    - merge_requests
    - master
    - develop

# 安全扫描
security_scan:
  stage: test
  image: node:$NODE_VERSION-alpine
  before_script:
    - npm install -g npm-audit-resolver
  script:
    - npm audit --audit-level high
    - npm audit --fix
  allow_failure: true
  only:
    - merge_requests
    - master

# 构建阶段
build:
  stage: build
  image: node:$NODE_VERSION-alpine
  <<: *node_cache
  before_script:
    - npm ci --cache .npm --prefer-offline
  script:
    - npm run build
  artifacts:
    paths:
      - dist/
    expire_in: 1 week
  only:
    - master
    - develop

# Docker 构建
build_docker:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker build -t $CI_REGISTRY_IMAGE:latest .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - docker push $CI_REGISTRY_IMAGE:latest
  only:
    - master

# 部署到开发环境
deploy_dev:
  stage: deploy
  image: alpine:latest
  before_script:
    - apk add --no-cache openssh-client
    - mkdir -p ~/.ssh
    - echo "$SSH_PRIVATE_KEY" | tr -d '\r' > ~/.ssh/id_rsa
    - chmod 600 ~/.ssh/id_rsa
    - ssh-keyscan -H $DEV_SERVER >> ~/.ssh/known_hosts
  script:
    - ssh $DEV_USER@$DEV_SERVER "docker pull $CI_REGISTRY_IMAGE:latest"
    - ssh $DEV_USER@$DEV_SERVER "docker stop app || true"
    - ssh $DEV_USER@$DEV_SERVER "docker rm app || true"
    - ssh $DEV_USER@$DEV_SERVER "docker run -d --name app -p 3000:3000 $CI_REGISTRY_IMAGE:latest"
  environment:
    name: development
    url: http://$DEV_SERVER:3000
  only:
    - develop

# 部署到生产环境
deploy_prod:
  stage: deploy
  image: alpine:latest
  before_script:
    - apk add --no-cache openssh-client
    - mkdir -p ~/.ssh
    - echo "$SSH_PRIVATE_KEY" | tr -d '\r' > ~/.ssh/id_rsa
    - chmod 600 ~/.ssh/id_rsa
    - ssh-keyscan -H $PROD_SERVER >> ~/.ssh/known_hosts
  script:
    - ssh $PROD_USER@$PROD_SERVER "docker pull $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA"
    - ssh $PROD_USER@$PROD_SERVER "docker stop app || true"
    - ssh $PROD_USER@$PROD_SERVER "docker rm app || true"
    - ssh $PROD_USER@$PROD_SERVER "docker run -d --name app -p 80:3000 $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA"
  environment:
    name: production
    url: http://$PROD_SERVER
  when: manual
  only:
    - master
```

## SSL 证书配置

### Let's Encrypt 自动证书

```bash
#!/bin/bash
# scripts/setup-ssl.sh

# 创建 SSL 目录
mkdir -p /volume1/gitlab/ssl

# 使用 Certbot 获取证书
docker run -it --rm \
  -v /volume1/gitlab/ssl:/etc/letsencrypt \
  -v /volume1/gitlab/ssl/challenge:/var/www/certbot \
  certbot/certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d gitlab.yourdomain.com

# 复制证书文件
cp /volume1/gitlab/ssl/live/gitlab.yourdomain.com/fullchain.pem /volume1/gitlab/ssl/gitlab.yourdomain.com.crt
cp /volume1/gitlab/ssl/live/gitlab.yourdomain.com/privkey.pem /volume1/gitlab/ssl/gitlab.yourdomain.com.key

# 设置正确的权限
chmod 644 /volume1/gitlab/ssl/gitlab.yourdomain.com.crt
chmod 600 /volume1/gitlab/ssl/gitlab.yourdomain.com.key

echo "SSL 证书配置完成！"
```

### SSL 证书自动续期

```bash
#!/bin/bash
# scripts/renew-ssl.sh

# 续期证书
docker run --rm \
  -v /volume1/gitlab/ssl:/etc/letsencrypt \
  -v /volume1/gitlab/ssl/challenge:/var/www/certbot \
  certbot/certbot renew

# 复制新证书
cp /volume1/gitlab/ssl/live/gitlab.yourdomain.com/fullchain.pem /volume1/gitlab/ssl/gitlab.yourdomain.com.crt
cp /volume1/gitlab/ssl/live/gitlab.yourdomain.com/privkey.pem /volume1/gitlab/ssl/gitlab.yourdomain.com.key

# 重启 GitLab 以应用新证书
docker-compose -f /volume1/gitlab/docker-compose.yml restart gitlab

echo "SSL 证书已更新！"
```

## 备份策略

### 自动备份脚本

```bash
#!/bin/bash
# backups/backup-script.sh

set -e

# 配置变量
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-30}
ENCRYPTION_KEY=${BACKUP_ENCRYPTION_KEY}

# 创建备份目录
mkdir -p "$BACKUP_DIR/gitlab" "$BACKUP_DIR/postgres" "$BACKUP_DIR/config"

echo "开始备份 GitLab ($(date))"

# 1. GitLab 应用备份
docker exec gitlab gitlab-backup create BACKUP=$DATE

# 移动 GitLab 备份文件
mv /volume1/gitlab/data/backups/${DATE}_gitlab_backup.tar "$BACKUP_DIR/gitlab/"

# 2. PostgreSQL 数据库备份
docker exec gitlab-postgres pg_dump -U gitlab gitlabhq_production | gzip > "$BACKUP_DIR/postgres/postgres_${DATE}.sql.gz"

# 3. 配置文件备份
tar -czf "$BACKUP_DIR/config/config_${DATE}.tar.gz" -C /volume1/gitlab config

# 4. SSL 证书备份
tar -czf "$BACKUP_DIR/config/ssl_${DATE}.tar.gz" -C /volume1/gitlab ssl

# 5. 加密备份文件（如果设置了加密密钥）
if [ -n "$ENCRYPTION_KEY" ]; then
    echo "加密备份文件..."
    find "$BACKUP_DIR" -name "*${DATE}*" -type f | while read file; do
        openssl enc -aes-256-cbc -salt -in "$file" -out "${file}.enc" -k "$ENCRYPTION_KEY"
        rm "$file"
    done
fi

# 6. 清理旧备份
echo "清理 ${RETENTION_DAYS} 天前的备份..."
find "$BACKUP_DIR" -type f -mtime +$RETENTION_DAYS -delete

# 7. 备份到远程存储（可选）
if [ -n "$REMOTE_BACKUP_PATH" ]; then
    echo "上传备份到远程存储..."
    rsync -av --delete "$BACKUP_DIR/" "$REMOTE_BACKUP_PATH/"
fi

# 8. 发送备份通知
BACKUP_SIZE=$(du -sh "$BACKUP_DIR" | cut -f1)
echo "GitLab 备份完成！备份大小: $BACKUP_SIZE"

# 可选：发送邮件通知
if command -v mail >/dev/null 2>&1; then
    echo "GitLab 备份于 $(date) 完成。备份大小: $BACKUP_SIZE" | \
    mail -s "GitLab 备份完成" admin@yourdomain.com
fi
```

### 恢复脚本

```bash
#!/bin/bash
# scripts/restore-gitlab.sh

set -e

BACKUP_DATE=$1
BACKUP_DIR="/backups"
ENCRYPTION_KEY=${BACKUP_ENCRYPTION_KEY}

if [ -z "$BACKUP_DATE" ]; then
    echo "用法: $0 <备份日期 YYYYMMDD_HHMMSS>"
    echo "可用备份:"
    ls -1 "$BACKUP_DIR/gitlab/" | grep "gitlab_backup.tar" | sed 's/_gitlab_backup.tar.*//'
    exit 1
fi

echo "开始恢复 GitLab 备份: $BACKUP_DATE"

# 1. 停止 GitLab 服务
docker-compose down

# 2. 解密备份文件（如果需要）
if [ -n "$ENCRYPTION_KEY" ]; then
    echo "解密备份文件..."
    find "$BACKUP_DIR" -name "*${BACKUP_DATE}*.enc" | while read file; do
        decrypted_file="${file%.enc}"
        openssl enc -d -aes-256-cbc -in "$file" -out "$decrypted_file" -k "$ENCRYPTION_KEY"
    done
fi

# 3. 恢复 PostgreSQL 数据库
echo "恢复数据库..."
docker-compose up -d gitlab-postgres
sleep 30

gunzip -c "$BACKUP_DIR/postgres/postgres_${BACKUP_DATE}.sql.gz" | \
docker exec -i gitlab-postgres psql -U gitlab -d gitlabhq_production

# 4. 恢复配置文件
echo "恢复配置文件..."
tar -xzf "$BACKUP_DIR/config/config_${BACKUP_DATE}.tar.gz" -C /volume1/gitlab/

# 5. 恢复 SSL 证书
tar -xzf "$BACKUP_DIR/config/ssl_${BACKUP_DATE}.tar.gz" -C /volume1/gitlab/

# 6. 恢复 GitLab 数据
echo "恢复 GitLab 应用数据..."
cp "$BACKUP_DIR/gitlab/${BACKUP_DATE}_gitlab_backup.tar" /volume1/gitlab/data/backups/

# 7. 启动所有服务
docker-compose up -d

# 8. 等待 GitLab 启动并恢复备份
echo "等待 GitLab 启动..."
sleep 120

docker exec gitlab gitlab-backup restore BACKUP=$BACKUP_DATE

# 9. 重新配置
docker exec gitlab gitlab-ctl reconfigure
docker exec gitlab gitlab-ctl restart

echo "GitLab 恢复完成！"
```

## 监控和日志

### Prometheus 配置

```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "gitlab_rules.yml"

scrape_configs:
  - job_name: 'gitlab'
    static_configs:
      - targets: ['gitlab:80']
    metrics_path: '/-/metrics'
    params:
      token: ['your-gitlab-prometheus-token']

  - job_name: 'gitlab-runner'
    static_configs:
      - targets: ['gitlab-runner:9252']

  - job_name: 'postgres'
    static_configs:
      - targets: ['gitlab-postgres:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['gitlab-redis:9121']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

### GitLab 监控规则

```yaml
# monitoring/gitlab_rules.yml
groups:
  - name: gitlab.rules
    rules:
      - alert: GitLabDown
        expr: up{job="gitlab"} == 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "GitLab 实例宕机"
          description: "GitLab 已经宕机超过 5 分钟"

      - alert: GitLabHighCPU
        expr: rate(process_cpu_seconds_total{job="gitlab"}[5m]) > 0.8
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "GitLab CPU 使用率过高"
          description: "GitLab CPU 使用率已超过 80% 持续 10 分钟"

      - alert: GitLabHighMemory
        expr: process_resident_memory_bytes{job="gitlab"} / 1024 / 1024 / 1024 > 6
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "GitLab 内存使用过高"
          description: "GitLab 内存使用超过 6GB"

      - alert: PostgreSQLDown
        expr: up{job="postgres"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL 数据库宕机"
          description: "PostgreSQL 数据库已宕机超过 2 分钟"

      - alert: RedisDown
        expr: up{job="redis"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Redis 缓存服务宕机"
          description: "Redis 缓存服务已宕机超过 2 分钟"

      - alert: GitLabRunnerDown
        expr: up{job="gitlab-runner"} == 0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "GitLab Runner 离线"
          description: "GitLab Runner 已离线超过 5 分钟"
```

### Grafana 仪表板配置

```json
{
  "dashboard": {
    "id": null,
    "title": "GitLab NAS 监控",
    "tags": ["gitlab", "nas"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "GitLab 状态",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"gitlab\"}",
            "legendFormat": "GitLab 状态"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "color": {
              "mode": "thresholds"
            },
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "HTTP 请求率",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total{job=\"gitlab\"}[5m])",
            "legendFormat": "HTTP 请求/秒"
          }
        ]
      },
      {
        "id": 3,
        "title": "内存使用情况",
        "type": "graph",
        "targets": [
          {
            "expr": "process_resident_memory_bytes{job=\"gitlab\"} / 1024 / 1024 / 1024",
            "legendFormat": "GitLab 内存 (GB)"
          },
          {
            "expr": "process_resident_memory_bytes{job=\"postgres\"} / 1024 / 1024 / 1024",
            "legendFormat": "PostgreSQL 内存 (GB)"
          }
        ]
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "5s"
  }
}
```

### 日志管理

```yaml
# docker-compose.logging.yml
# 日志收集和轮转配置

version: '3.8'

services:
  gitlab:
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "5"
        labels: "service=gitlab"

  gitlab-postgres:
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "3"
        labels: "service=postgres"

  gitlab-redis:
    logging:
      driver: "json-file"
      options:
        max-size: "20m"
        max-file: "3"
        labels: "service=redis"

  # Fluentd 日志收集器（可选）
  fluentd:
    image: fluent/fluentd:v1.16-debian
    container_name: gitlab-fluentd
    restart: unless-stopped
    ports:
      - "24224:24224"
    volumes:
      - ./monitoring/fluentd.conf:/fluentd/etc/fluent.conf
      - ./logs/fluentd:/var/log/fluentd
    networks:
      - gitlab-network
```

## 安装部署

### 一键安装脚本

```bash
#!/bin/bash
# install-gitlab.sh

set -e

echo "=== GitLab NAS 部署安装脚本 ==="

# 检查系统要求
check_requirements() {
    echo "检查系统要求..."
    
    # 检查 Docker
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    # 检查 Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    # 检查内存
    MEMORY_GB=$(free -g | awk 'NR==2{printf "%.0f", $2}')
    if [ $MEMORY_GB -lt 8 ]; then
        echo "⚠️  警告：系统内存少于 8GB，建议升级内存"
    fi
    
    # 检查磁盘空间
    DISK_GB=$(df -BG /volume1 2>/dev/null | awk 'NR==2{print $4}' | sed 's/G//' || echo "0")
    if [ $DISK_GB -lt 200 ]; then
        echo "❌ 磁盘空间不足 200GB，当前可用: ${DISK_GB}GB"
        exit 1
    fi
    
    echo "✅ 系统要求检查通过"
}

# 创建目录结构
setup_directories() {
    echo "创建目录结构..."
    
    GITLAB_ROOT="/volume1/gitlab"
    
    mkdir -p "$GITLAB_ROOT"/{config,logs,data,postgres,redis,runner,backups,ssl,monitoring}
    mkdir -p "$GITLAB_ROOT"/backups/{gitlab,postgres,config}
    mkdir -p "$GITLAB_ROOT"/monitoring/{prometheus-data,grafana-data,grafana-dashboards,grafana-datasources}
    mkdir -p "$GITLAB_ROOT"/scripts
    
    # 设置权限
    chmod -R 755 "$GITLAB_ROOT"
    
    echo "✅ 目录结构创建完成"
}

# 生成环境变量文件
generate_env_file() {
    echo "生成环境变量配置..."
    
    # 获取用户输入
    read -p "请输入 GitLab 域名 (例如: gitlab.yourdomain.com): " GITLAB_HOSTNAME
    read -p "请输入 GitLab root 密码: " -s GITLAB_ROOT_PASSWORD
    echo
    read -p "请输入 PostgreSQL 密码: " -s POSTGRES_PASSWORD
    echo
    read -p "请输入 Redis 密码: " -s REDIS_PASSWORD
    echo
    read -p "请输入 Grafana 密码: " -s GRAFANA_PASSWORD
    echo
    
    # 生成随机加密密钥
    BACKUP_ENCRYPTION_KEY=$(openssl rand -base64 32)
    
    cat > "/volume1/gitlab/.env" << EOF
# GitLab NAS 部署配置
GITLAB_HOSTNAME=$GITLAB_HOSTNAME
GITLAB_ROOT_PASSWORD=$GITLAB_ROOT_PASSWORD
COMPOSE_PROJECT_NAME=gitlab

# 数据库配置
POSTGRES_PASSWORD=$POSTGRES_PASSWORD

# Redis 配置
REDIS_PASSWORD=$REDIS_PASSWORD

# 邮件配置（请根据实际情况修改）
SMTP_SERVER=smtp.gmail.com
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-smtp-password
SMTP_DOMAIN=gmail.com
GITLAB_EMAIL_FROM=gitlab@$GITLAB_HOSTNAME

# 监控配置
GRAFANA_PASSWORD=$GRAFANA_PASSWORD

# 备份配置
BACKUP_RETENTION_DAYS=30
BACKUP_ENCRYPTION_KEY=$BACKUP_ENCRYPTION_KEY
EOF

    chmod 600 "/volume1/gitlab/.env"
    echo "✅ 环境变量配置完成"
}

# 下载配置文件
download_configs() {
    echo "创建配置文件..."
    
    # 这里应该包含所有必要的配置文件创建
    # 为了简化，我们创建基础的配置
    
    # Prometheus 配置
    cat > "/volume1/gitlab/monitoring/prometheus.yml" << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'gitlab'
    static_configs:
      - targets: ['gitlab:80']
    metrics_path: '/-/metrics'
EOF

    echo "✅ 配置文件创建完成"
}

# 启动服务
start_services() {
    echo "启动 GitLab 服务..."
    
    cd /volume1/gitlab
    
    # 下载镜像
    docker-compose pull
    
    # 启动数据库服务
    docker-compose up -d gitlab-postgres gitlab-redis
    
    echo "等待数据库启动..."
    sleep 30
    
    # 启动 GitLab 主服务
    docker-compose up -d gitlab
    
    echo "等待 GitLab 启动（这可能需要 5-10 分钟）..."
    
    # 等待 GitLab 健康检查通过
    attempt=0
    max_attempts=60
    while [ $attempt -lt $max_attempts ]; do
        if docker exec gitlab gitlab-ctl status >/dev/null 2>&1; then
            echo "✅ GitLab 启动成功！"
            break
        fi
        echo "等待中... ($attempt/$max_attempts)"
        sleep 30
        attempt=$((attempt + 1))
    done
    
    if [ $attempt -eq $max_attempts ]; then
        echo "❌ GitLab 启动超时，请检查日志"
        exit 1
    fi
    
    # 启动其他服务
    docker-compose up -d
    
    echo "✅ 所有服务启动完成"
}

# 配置 GitLab Runner
setup_runner() {
    echo "配置 GitLab Runner..."
    
    # 等待 GitLab 完全启动
    sleep 60
    
    # 获取 Runner 注册 token
    RUNNER_TOKEN=$(docker exec gitlab gitlab-rails runner -e production "puts Gitlab::CurrentSettings.current_application_settings.runners_registration_token" 2>/dev/null || echo "")
    
    if [ -n "$RUNNER_TOKEN" ]; then
        # 注册 Runner
        docker exec gitlab-runner gitlab-runner register \
            --non-interactive \
            --url "https://${GITLAB_HOSTNAME}/" \
            --registration-token "$RUNNER_TOKEN" \
            --executor "docker" \
            --docker-image alpine:latest \
            --description "NAS Docker Runner" \
            --tag-list "docker,nas,general" \
            --run-untagged="true" \
            --locked="false" \
            --docker-privileged="true" \
            --docker-volumes="/var/run/docker.sock:/var/run/docker.sock"
        
        echo "✅ GitLab Runner 配置完成"
    else
        echo "⚠️  Runner Token 获取失败，请稍后手动配置"
    fi
}

# 显示部署信息
show_deployment_info() {
    echo
    echo "=== GitLab 部署完成！==="
    echo
    echo "🌐 Web 访问地址: https://${GITLAB_HOSTNAME}"
    echo "👤 管理员用户名: root"
    echo "🔑 管理员密码: ${GITLAB_ROOT_PASSWORD}"
    echo "📊 监控面板: http://$(hostname -I | awk '{print $1}'):3000"
    echo "🔧 Grafana 密码: ${GRAFANA_PASSWORD}"
    echo
    echo "📁 配置文件位置: /volume1/gitlab"
    echo "💾 备份目录: /volume1/gitlab/backups"
    echo "📋 日志目录: /volume1/gitlab/logs"
    echo
    echo "🔧 常用命令:"
    echo "  查看服务状态: docker-compose -f /volume1/gitlab/docker-compose.yml ps"
    echo "  查看日志: docker-compose -f /volume1/gitlab/docker-compose.yml logs -f gitlab"
    echo "  重启服务: docker-compose -f /volume1/gitlab/docker-compose.yml restart"
    echo "  停止服务: docker-compose -f /volume1/gitlab/docker-compose.yml down"
    echo
    echo "📖 详细文档请参考: GITLAB_NAS_DEPLOYMENT_GUIDE.md"
}

# 主函数
main() {
    check_requirements
    setup_directories
    generate_env_file
    download_configs
    start_services
    setup_runner
    show_deployment_info
}

# 运行主函数
main "$@"
```

### 手动安装步骤

```bash
# 1. 创建目录结构
mkdir -p /volume1/gitlab/{config,logs,data,postgres,redis,runner,backups,ssl,monitoring}

# 2. 下载 docker-compose.yml 和 .env 文件
cd /volume1/gitlab
wget https://raw.githubusercontent.com/youraccount/gitlab-nas/main/docker-compose.yml
wget https://raw.githubusercontent.com/youraccount/gitlab-nas/main/.env.example
mv .env.example .env

# 3. 编辑环境变量
vim .env

# 4. 生成 SSL 证书（如果需要）
./scripts/setup-ssl.sh

# 5. 启动服务
docker-compose up -d

# 6. 等待启动完成
docker-compose logs -f gitlab

# 7. 配置 GitLab Runner
./scripts/register-runner.sh
```

## 维护和升级

### 日常维护任务

```bash
#!/bin/bash
# scripts/maintenance.sh

# 每日维护任务
daily_maintenance() {
    echo "执行每日维护任务..."
    
    # 1. 检查服务状态
    docker-compose ps
    
    # 2. 检查磁盘使用情况
    df -h /volume1
    
    # 3. 检查内存使用
    free -h
    
    # 4. 清理旧日志
    find /volume1/gitlab/logs -name "*.log" -mtime +7 -delete
    
    # 5. 清理 Docker 垃圾
    docker system prune -f
    
    # 6. 验证备份
    if [ -f "/volume1/gitlab/backups/gitlab/$(date +%Y%m%d)_*_gitlab_backup.tar" ]; then
        echo "✅ 今日备份存在"
    else
        echo "❌ 今日备份缺失"
    fi
    
    echo "每日维护完成"
}

# 每周维护任务
weekly_maintenance() {
    echo "执行每周维护任务..."
    
    # 1. 更新容器镜像
    docker-compose pull
    
    # 2. 重启服务以应用更新
    docker-compose restart
    
    # 3. 清理未使用的镜像
    docker image prune -a -f
    
    # 4. 检查备份完整性
    ./scripts/verify-backups.sh
    
    # 5. 更新 SSL 证书（如果需要）
    ./scripts/renew-ssl.sh
    
    echo "每周维护完成"
}

# 月度维护任务
monthly_maintenance() {
    echo "执行月度维护任务..."
    
    # 1. 升级 GitLab（如果有新版本）
    ./scripts/upgrade-gitlab.sh
    
    # 2. 性能优化
    docker exec gitlab-postgres vacuumdb -U gitlab -d gitlabhq_production -z
    
    # 3. 备份配置文件
    tar -czf "/volume1/gitlab/backups/config/monthly_config_$(date +%Y%m).tar.gz" \
        -C /volume1/gitlab config ssl
    
    # 4. 生成月度报告
    ./scripts/generate-monthly-report.sh
    
    echo "月度维护完成"
}

case ${1:-daily} in
    daily)   daily_maintenance ;;
    weekly)  weekly_maintenance ;;
    monthly) monthly_maintenance ;;
    *)       echo "用法: $0 {daily|weekly|monthly}" ;;
esac
```

### GitLab 升级脚本

```bash
#!/bin/bash
# scripts/upgrade-gitlab.sh

set -e

CURRENT_VERSION=$(docker exec gitlab gitlab-rake gitlab:env:info | grep "GitLab information" -A 20 | grep "Version:" | awk '{print $2}')
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)

echo "当前 GitLab 版本: $CURRENT_VERSION"
echo "开始升级流程..."

# 1. 创建升级前备份
echo "创建升级前备份..."
./backups/backup-script.sh

# 2. 停止服务
echo "停止 GitLab 服务..."
docker-compose stop gitlab gitlab-runner

# 3. 备份当前镜像
echo "备份当前镜像..."
docker tag gitlab/gitlab-ce:latest gitlab/gitlab-ce:backup-$BACKUP_DATE

# 4. 拉取新镜像
echo "拉取最新镜像..."
docker-compose pull gitlab

# 5. 启动新版本
echo "启动新版本..."
docker-compose up -d gitlab

# 6. 等待升级完成
echo "等待升级完成..."
attempt=0
max_attempts=30
while [ $attempt -lt $max_attempts ]; do
    if docker exec gitlab gitlab-ctl status >/dev/null 2>&1; then
        echo "✅ GitLab 升级成功！"
        break
    fi
    echo "升级中... ($attempt/$max_attempts)"
    sleep 60
    attempt=$((attempt + 1))
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ 升级超时，开始回滚..."
    
    # 回滚到之前版本
    docker-compose stop gitlab
    docker tag gitlab/gitlab-ce:backup-$BACKUP_DATE gitlab/gitlab-ce:latest
    docker-compose up -d gitlab
    
    echo "❌ 已回滚到升级前版本"
    exit 1
fi

# 7. 重启其他服务
docker-compose up -d

# 8. 验证升级
NEW_VERSION=$(docker exec gitlab gitlab-rake gitlab:env:info | grep "GitLab information" -A 20 | grep "Version:" | awk '{print $2}')
echo "升级完成！"
echo "旧版本: $CURRENT_VERSION"
echo "新版本: $NEW_VERSION"
```

### 性能监控脚本

```bash
#!/bin/bash
# scripts/performance-monitor.sh

# 性能监控和优化
monitor_performance() {
    echo "=== GitLab 性能监控 ==="
    
    # 系统资源使用
    echo "📊 系统资源使用情况:"
    echo "CPU 使用率: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)%"
    echo "内存使用: $(free | grep Mem | awk '{printf("%.1f%%\n", $3/$2 * 100.0)}')"
    echo "磁盘使用: $(df -h /volume1 | awk 'NR==2{print $5}')"
    
    # Docker 容器资源使用
    echo
    echo "🐳 容器资源使用:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
    
    # GitLab 特定指标
    echo
    echo "🦊 GitLab 指标:"
    
    # 响应时间
    RESPONSE_TIME=$(curl -o /dev/null -s -w '%{time_total}\n' http://localhost/-/health || echo "N/A")
    echo "响应时间: ${RESPONSE_TIME}s"
    
    # 数据库连接数
    DB_CONNECTIONS=$(docker exec gitlab-postgres psql -U gitlab -d gitlabhq_production -t -c "SELECT count(*) FROM pg_stat_activity;" 2>/dev/null | xargs || echo "N/A")
    echo "数据库连接数: $DB_CONNECTIONS"
    
    # Redis 内存使用
    REDIS_MEMORY=$(docker exec gitlab-redis redis-cli info memory | grep used_memory_human | cut -d: -f2 | tr -d '\r' || echo "N/A")
    echo "Redis 内存使用: $REDIS_MEMORY"
    
    # GitLab 队列状态
    SIDEKIQ_QUEUES=$(docker exec gitlab gitlab-rails runner "puts Sidekiq::Stats.new.queues" 2>/dev/null || echo "N/A")
    echo "Sidekiq 队列: $SIDEKIQ_QUEUES"
}

# 性能优化建议
performance_recommendations() {
    echo
    echo "🔧 性能优化建议:"
    
    # 检查内存使用
    MEMORY_PERCENT=$(free | grep Mem | awk '{printf("%.0f\n", $3/$2 * 100.0)}')
    if [ $MEMORY_PERCENT -gt 85 ]; then
        echo "⚠️  内存使用过高 (${MEMORY_PERCENT}%)，建议:"
        echo "   - 减少 Puma worker 进程数"
        echo "   - 调整 Sidekiq 并发数"
        echo "   - 增加系统内存"
    fi
    
    # 检查磁盘使用
    DISK_PERCENT=$(df /volume1 | awk 'NR==2{print $5}' | sed 's/%//')
    if [ $DISK_PERCENT -gt 80 ]; then
        echo "⚠️  磁盘使用过高 (${DISK_PERCENT}%)，建议:"
        echo "   - 清理旧的备份文件"
        echo "   - 启用 LFS 存储"
        echo "   - 设置 artifacts 过期时间"
    fi
    
    # 检查 CPU 负载
    LOAD_AVG=$(uptime | awk -F'load average:' '{print $2}' | awk '{print $1}' | sed 's/,//')
    CPU_COUNT=$(nproc)
    if (( $(echo "$LOAD_AVG > $CPU_COUNT" | bc -l) )); then
        echo "⚠️  CPU 负载过高 (${LOAD_AVG})，建议:"
        echo "   - 检查是否有异常进程"
        echo "   - 优化 CI/CD 并发数"
        echo "   - 考虑升级 CPU"
    fi
}

# 生成性能报告
generate_performance_report() {
    REPORT_FILE="/volume1/gitlab/logs/performance_$(date +%Y%m%d_%H%M%S).log"
    
    {
        echo "GitLab 性能报告 - $(date)"
        echo "================================"
        monitor_performance
        performance_recommendations
    } | tee "$REPORT_FILE"
    
    echo
    echo "📋 性能报告已保存到: $REPORT_FILE"
}

case ${1:-monitor} in
    monitor)     monitor_performance ;;
    recommend)   performance_recommendations ;;
    report)      generate_performance_report ;;
    *)           echo "用法: $0 {monitor|recommend|report}" ;;
esac
```

## 故障排除

### 常见问题解决

```bash
#!/bin/bash
# scripts/troubleshoot.sh

# GitLab 故障诊断
diagnose_gitlab() {
    echo "=== GitLab 故障诊断 ==="
    
    # 1. 检查容器状态
    echo "📋 容器状态:"
    docker-compose ps
    
    # 2. 检查健康状态
    echo
    echo "🏥 健康检查:"
    docker exec gitlab gitlab-ctl status || echo "❌ GitLab 服务异常"
    
    # 3. 检查端口占用
    echo
    echo "🔌 端口检查:"
    netstat -tlnp | grep -E ':(80|443|2222|5005|3000|9090)\s'
    
    # 4. 检查资源使用
    echo
    echo "💾 资源使用:"
    docker stats --no-stream
    
    # 5. 检查日志错误
    echo
    echo "📄 最近错误日志:"
    docker-compose logs --tail=50 gitlab | grep -i error || echo "无错误日志"
}

# 常见问题修复
fix_common_issues() {
    echo "=== 常见问题修复 ==="
    
    echo "🔧 执行常见修复操作..."
    
    # 1. 重新配置 GitLab
    echo "重新配置 GitLab..."
    docker exec gitlab gitlab-ctl reconfigure
    
    # 2. 重启服务
    echo "重启服务..."
    docker exec gitlab gitlab-ctl restart
    
    # 3. 清理缓存
    echo "清理缓存..."
    docker exec gitlab gitlab-rake cache:clear
    
    # 4. 检查数据库连接
    echo "检查数据库连接..."
    docker exec gitlab gitlab-rake db:check_schema
    
    # 5. 修复权限
    echo "修复文件权限..."
    docker exec gitlab chown -R git:git /var/opt/gitlab
    
    echo "✅ 常见问题修复完成"
}

# 数据库问题修复
fix_database_issues() {
    echo "=== 数据库问题修复 ==="
    
    # 1. 检查数据库连接
    if ! docker exec gitlab-postgres pg_isready -U gitlab; then
        echo "❌ 数据库连接失败，尝试重启..."
        docker-compose restart gitlab-postgres
        sleep 30
    fi
    
    # 2. 数据库完整性检查
    echo "检查数据库完整性..."
    docker exec gitlab-postgres psql -U gitlab -d gitlabhq_production -c "SELECT datname FROM pg_database WHERE datname = 'gitlabhq_production';"
    
    # 3. 重建索引
    echo "重建数据库索引..."
    docker exec gitlab-postgres psql -U gitlab -d gitlabhq_production -c "REINDEX DATABASE gitlabhq_production;"
    
    # 4. 清理连接
    echo "清理空闲连接..."
    docker exec gitlab-postgres psql -U gitlab -d gitlabhq_production -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND query_start < now() - interval '1 hour';"
}

# SSL 证书问题修复
fix_ssl_issues() {
    echo "=== SSL 证书问题修复 ==="
    
    # 1. 检查证书文件
    if [ ! -f "/volume1/gitlab/ssl/gitlab.yourdomain.com.crt" ]; then
        echo "❌ SSL 证书文件缺失，重新生成..."
        ./scripts/setup-ssl.sh
    fi
    
    # 2. 检查证书有效期
    CERT_EXPIRY=$(openssl x509 -enddate -noout -in /volume1/gitlab/ssl/gitlab.yourdomain.com.crt | cut -d= -f2)
    CERT_EXPIRY_EPOCH=$(date -d "$CERT_EXPIRY" +%s)
    CURRENT_EPOCH=$(date +%s)
    DAYS_UNTIL_EXPIRY=$(( (CERT_EXPIRY_EPOCH - CURRENT_EPOCH) / 86400 ))
    
    if [ $DAYS_UNTIL_EXPIRY -lt 30 ]; then
        echo "⚠️  证书将在 $DAYS_UNTIL_EXPIRY 天后过期，更新证书..."
        ./scripts/renew-ssl.sh
    fi
    
    # 3. 验证证书配置
    echo "验证 SSL 配置..."
    openssl x509 -text -noout -in /volume1/gitlab/ssl/gitlab.yourdomain.com.crt | grep -E "(Subject:|Issuer:|Not After)"
}

# 存储空间清理
cleanup_storage() {
    echo "=== 存储空间清理 ==="
    
    # 1. 清理 Docker 资源
    echo "清理 Docker 资源..."
    docker system prune -f
    docker volume prune -f
    
    # 2. 清理旧日志
    echo "清理旧日志文件..."
    find /volume1/gitlab/logs -name "*.log" -mtime +30 -delete
    find /volume1/gitlab/logs -name "*.log.*" -mtime +7 -delete
    
    # 3. 清理旧备份
    echo "清理旧备份文件..."
    find /volume1/gitlab/backups -name "*.tar*" -mtime +60 -delete
    
    # 4. 清理 GitLab artifacts
    echo "清理 GitLab artifacts..."
    docker exec gitlab gitlab-rake gitlab:artifacts:cleanup
    
    # 5. 清理 GitLab traces
    echo "清理 GitLab traces..."
    docker exec gitlab gitlab-rake gitlab:traces:cleanup
    
    echo "✅ 存储空间清理完成"
    df -h /volume1
}

# 性能优化
performance_tuning() {
    echo "=== 性能优化 ==="
    
    # 1. 重启服务以释放内存
    echo "重启服务释放内存..."
    docker-compose restart gitlab
    
    # 2. 数据库优化
    echo "优化数据库..."
    docker exec gitlab-postgres psql -U gitlab -d gitlabhq_production -c "VACUUM ANALYZE;"
    
    # 3. Redis 内存优化
    echo "优化 Redis 内存..."
    docker exec gitlab-redis redis-cli FLUSHDB
    
    # 4. 重建 GitLab 缓存
    echo "重建 GitLab 缓存..."
    docker exec gitlab gitlab-rake cache:clear
    docker exec gitlab gitlab-rake assets:clean
    docker exec gitlab gitlab-rake assets:precompile
    
    echo "✅ 性能优化完成"
}

# 主菜单
show_menu() {
    echo "GitLab 故障排除工具"
    echo "=================="
    echo "1. 诊断问题"
    echo "2. 修复常见问题"
    echo "3. 修复数据库问题"
    echo "4. 修复 SSL 证书问题"
    echo "5. 清理存储空间"
    echo "6. 性能优化"
    echo "7. 查看实时日志"
    echo "0. 退出"
    echo
}

# 查看实时日志
view_logs() {
    echo "选择要查看的日志："
    echo "1. GitLab 主服务日志"
    echo "2. PostgreSQL 日志"
    echo "3. Redis 日志"
    echo "4. GitLab Runner 日志"
    echo "5. 所有服务日志"
    
    read -p "请选择 (1-5): " log_choice
    
    case $log_choice in
        1) docker-compose logs -f gitlab ;;
        2) docker-compose logs -f gitlab-postgres ;;
        3) docker-compose logs -f gitlab-redis ;;
        4) docker-compose logs -f gitlab-runner ;;
        5) docker-compose logs -f ;;
        *) echo "无效选择" ;;
    esac
}

# 交互式菜单
if [ $# -eq 0 ]; then
    while true; do
        show_menu
        read -p "请选择操作 (0-7): " choice
        
        case $choice in
            1) diagnose_gitlab ;;
            2) fix_common_issues ;;
            3) fix_database_issues ;;
            4) fix_ssl_issues ;;
            5) cleanup_storage ;;
            6) performance_tuning ;;
            7) view_logs ;;
            0) echo "退出"; exit 0 ;;
            *) echo "无效选择，请重试" ;;
        esac
        
        echo
        read -p "按回车键继续..."
        clear
    done
else
    # 命令行参数
    case $1 in
        diagnose)    diagnose_gitlab ;;
        fix)         fix_common_issues ;;
        database)    fix_database_issues ;;
        ssl)         fix_ssl_issues ;;
        cleanup)     cleanup_storage ;;
        performance) performance_tuning ;;
        logs)        view_logs ;;
        *)           echo "用法: $0 {diagnose|fix|database|ssl|cleanup|performance|logs}" ;;
    esac
fi
```

### 错误代码对照表

| 错误代码 | 问题描述 | 解决方案 |
|----------|----------|----------|
| 502 Bad Gateway | GitLab 服务未启动 | `docker-compose restart gitlab` |
| 503 Service Unavailable | 服务启动中 | 等待 5-10 分钟 |
| SSL Certificate Error | 证书问题 | 运行 `./scripts/setup-ssl.sh` |
| Database Connection Error | 数据库连接失败 | 检查 PostgreSQL 服务状态 |
| Out of Memory | 内存不足 | 重启服务或增加内存 |
| Disk Full | 磁盘空间不足 | 运行清理脚本 |
| Runner Not Responding | Runner 离线 | 重新注册 Runner |

## 总结

本指南提供了在 NAS 设备上部署 GitLab 的完整解决方案，专为 1-20 人的小团队优化。主要特点：

### 🎯 核心优势

1. **完整的 Docker Compose 配置** - 包含 GitLab、PostgreSQL、Redis、Runner、监控等全套服务
2. **性能优化** - 针对小团队使用场景的资源配置和性能调优
3. **完善的 CI/CD 支持** - 内置 GitLab Runner 和完整的管道模板
4. **自动化备份和恢复** - 加密备份、自动清理、一键恢复
5. **监控和告警** - Prometheus + Grafana 监控方案
6. **故障排除** - 完整的诊断和修复工具

### 📊 资源配置

- **内存使用**: GitLab 4GB + PostgreSQL 2GB + Redis 512MB + Runner 1GB = 约 8GB
- **磁盘使用**: 初始约 50GB，建议预留 200GB+ 空间
- **性能**: 可稳定支持 1-20 人团队的日常开发需求

### 🔧 维护要点

1. **每日**: 检查服务状态、清理日志、验证备份
2. **每周**: 更新镜像、清理 Docker 资源、更新 SSL 证书
3. **每月**: 升级 GitLab、数据库优化、生成月度报告

### 🚀 后续扩展

- 可根据团队增长调整资源配置
- 支持集成外部 LDAP/AD 认证
- 可扩展多 Runner 节点支持更大并发
- 支持集成企业监控和日志系统

通过本指南，您可以在 NAS 设备上快速部署一个功能完整、性能优化的 GitLab 实例，为团队提供稳定可靠的代码管理和 CI/CD 服务。

---

**更新日期**: 2025年8月11日  
**版本**: v1.0  
**适用**: GitLab CE 16.11+, Docker 20.10+, Docker Compose 2.0+

如有问题请参考故障排除章节或联系技术支持。

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "\u8bbe\u8ba1GitLab NAS\u90e8\u7f72\u67b6\u6784\u548c\u914d\u7f6e", "status": "completed", "id": "1"}, {"content": "\u7f16\u5199Docker Compose\u914d\u7f6e\u6587\u4ef6", "status": "in_progress", "id": "2"}, {"content": "\u4f18\u5316\u5c0f\u56e2\u961f\u4f7f\u7528\u7684\u6027\u80fd\u914d\u7f6e", "status": "pending", "id": "3"}, {"content": "\u914d\u7f6eCI/CD Runner\u548c\u6d41\u6c34\u7ebf", "status": "pending", "id": "4"}, {"content": "\u6dfb\u52a0\u5907\u4efd\u548c\u76d1\u63a7\u65b9\u6848", "status": "pending", "id": "5"}, {"content": "\u521b\u5efa\u5b8c\u6574\u5b89\u88c5\u548c\u7ef4\u62a4\u6587\u6863", "status": "pending", "id": "6"}, {"content": "\u63d0\u4ea4\u6587\u6863\u5230git\u4ed3\u5e93", "status": "pending", "id": "7"}]