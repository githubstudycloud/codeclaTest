# 使用Docker搭建信息收集整理工具完整指南

## 目录
1. [系统架构概览](#系统架构概览)
2. [核心工具介绍](#核心工具介绍)
3. [环境准备](#环境准备)
4. [逐步部署指南](#逐步部署指南)
5. [高级配置](#高级配置)
6. [使用方法](#使用方法)
7. [维护和备份](#维护和备份)
8. [故障排除](#故障排除)

## 系统架构概览

### 信息收集整理系统组件图
```
                    ┌─────────────────┐
                    │   反向代理      │
                    │   (Nginx)      │
                    └─────────┬───────┘
                              │
                ┌─────────────┼─────────────┐
                │             │             │
        ┌───────▼──────┐ ┌───▼────┐ ┌─────▼─────┐
        │ RSS聚合工具   │ │知识管理 │ │ 文档服务   │
        │ (FreshRSS)   │ │(Outline)│ │(BookStack) │
        └──────────────┘ └────────┘ └───────────┘
                │             │             │
                └─────────────┼─────────────┘
                              │
                    ┌─────────▼───────┐
                    │     数据库      │
                    │ (PostgreSQL)   │
                    └─────────────────┘
```

### 核心功能模块
- **信息收集层**: RSS聚合、网页剪藏、文档导入
- **存储处理层**: 数据库、文件存储、全文搜索
- **组织管理层**: 分类标签、知识图谱、版本控制
- **展示输出层**: Web界面、API接口、导出功能

## 核心工具介绍

### 1. FreshRSS - RSS订阅聚合器
**功能特点**:
- 支持RSS/Atom/JSON Feed
- 全文搜索和过滤
- 标签分类和收藏
- 移动端适配

**适用场景**:
- 新闻资讯聚合
- 博客文章追踪
- 技术更新监控

### 2. Outline - 现代化知识库
**功能特点**:
- 实时协作编辑
- Markdown支持
- 层级结构组织
- 强大的搜索功能

**适用场景**:
- 团队知识库
- 个人笔记系统
- 项目文档管理

### 3. BookStack - 自托管Wiki平台
**功能特点**:
- 书籍-章节-页面结构
- 用户权限管理
- 丰富的编辑器
- 导出多种格式

**适用场景**:
- 技术文档
- 学习笔记
- 操作手册

### 4. Wallabag - 稍后阅读服务
**功能特点**:
- 离线阅读支持
- 全文提取
- 标签和分类
- 多端同步

**适用场景**:
- 文章收藏
- 离线阅读
- 内容整理

### 5. Elasticsearch + Kibana - 搜索分析
**功能特点**:
- 全文搜索引擎
- 数据可视化
- 实时分析
- RESTful API

**适用场景**:
- 海量文档搜索
- 数据分析展示
- 内容挖掘

## 环境准备

### 系统要求
- **操作系统**: Linux/Windows/macOS
- **内存**: 最低4GB，推荐8GB+
- **存储**: 50GB+可用空间
- **Docker版本**: 20.10+
- **Docker Compose版本**: 2.0+

### 目录结构准备
```bash
mkdir -p ~/info-collection-system/{
  config,
  data/{postgres,elasticsearch,redis,uploads},
  logs,
  backups,
  docker-compose-files
}

cd ~/info-collection-system
```

## 逐步部署指南

### 第一步：基础服务部署

#### 1.1 创建网络和存储卷
```bash
# 创建Docker网络
docker network create info-net

# 创建存储卷
docker volume create postgres_data
docker volume create elasticsearch_data
docker volume create redis_data
```

#### 1.2 部署基础服务栈
创建 `docker-compose.base.yml`:
```yaml
version: '3.8'

services:
  # PostgreSQL数据库
  postgres:
    image: postgres:15-alpine
    container_name: info-postgres
    environment:
      POSTGRES_DB: info_db
      POSTGRES_USER: info_user
      POSTGRES_PASSWORD: your_secure_password
      PGDATA: /var/lib/postgresql/data/pgdata
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./config/postgres:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    networks:
      - info-net
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U info_user -d info_db"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Redis缓存
  redis:
    image: redis:7-alpine
    container_name: info-redis
    command: redis-server --appendonly yes --requirepass your_redis_password
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    networks:
      - info-net
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Elasticsearch搜索引擎
  elasticsearch:
    image: elasticsearch:8.11.0
    container_name: info-elasticsearch
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms1g -Xmx2g"
      - xpack.security.enabled=false
      - xpack.security.enrollment.enabled=false
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
      - "9300:9300"
    networks:
      - info-net
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:
  redis_data:
  elasticsearch_data:

networks:
  info-net:
    external: true
```

启动基础服务:
```bash
docker-compose -f docker-compose.base.yml up -d
```

### 第二步：信息收集工具部署

#### 2.1 FreshRSS RSS聚合器
创建 `docker-compose.freshrss.yml`:
```yaml
version: '3.8'

services:
  freshrss:
    image: freshrss/freshrss:latest
    container_name: info-freshrss
    environment:
      CRON_MIN: '*/15'  # 每15分钟更新一次
      TZ: Asia/Shanghai
    volumes:
      - ./data/freshrss:/var/www/FreshRSS/data
      - ./config/freshrss:/var/www/FreshRSS/data/users/_/config
    ports:
      - "8080:80"
    networks:
      - info-net
    depends_on:
      - postgres
    restart: unless-stopped
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.freshrss.rule=Host(`rss.localhost`)"
      - "traefik.http.services.freshrss.loadbalancer.server.port=80"

networks:
  info-net:
    external: true
```

#### 2.2 Outline知识库
创建 `docker-compose.outline.yml`:
```yaml
version: '3.8'

services:
  outline:
    image: outlinewiki/outline:latest
    container_name: info-outline
    environment:
      SECRET_KEY: your_secret_key_32_chars_long
      UTILS_SECRET: your_utils_secret_32_chars_long
      DATABASE_URL: postgres://info_user:your_secure_password@postgres:5432/outline_db
      REDIS_URL: redis://:your_redis_password@redis:6379
      URL: http://localhost:8081
      PORT: 3000
      
      # 文件存储配置
      FILE_STORAGE: local
      FILE_STORAGE_LOCAL_ROOT_DIR: /var/lib/outline/data
      FILE_STORAGE_UPLOAD_MAX_SIZE: 26214400
      
      # 搜索配置
      ENABLE_UPDATES: false
      DEBUG: cache,presenters,events
      
      # 可选：Slack集成
      # SLACK_CLIENT_ID: your_slack_client_id
      # SLACK_CLIENT_SECRET: your_slack_client_secret
    volumes:
      - ./data/outline:/var/lib/outline/data
    ports:
      - "8081:3000"
    networks:
      - info-net
    depends_on:
      - postgres
      - redis
    restart: unless-stopped

networks:
  info-net:
    external: true
```

#### 2.3 BookStack文档平台
创建 `docker-compose.bookstack.yml`:
```yaml
version: '3.8'

services:
  bookstack:
    image: lscr.io/linuxserver/bookstack:latest
    container_name: info-bookstack
    environment:
      PUID: 1000
      PGID: 1000
      APP_URL: http://localhost:8082
      DB_HOST: postgres
      DB_PORT: 5432
      DB_USER: info_user
      DB_PASS: your_secure_password
      DB_DATABASE: bookstack_db
      MAIL_DRIVER: smtp
      MAIL_HOST: smtp.gmail.com
      MAIL_PORT: 587
      MAIL_ENCRYPTION: tls
      MAIL_USERNAME: your_email@gmail.com
      MAIL_PASSWORD: your_email_password
      MAIL_FROM_NAME: BookStack
    volumes:
      - ./data/bookstack:/config
    ports:
      - "8082:80"
    networks:
      - info-net
    depends_on:
      - postgres
    restart: unless-stopped

networks:
  info-net:
    external: true
```

#### 2.4 Wallabag稍后阅读
创建 `docker-compose.wallabag.yml`:
```yaml
version: '3.8'

services:
  wallabag:
    image: wallabag/wallabag:latest
    container_name: info-wallabag
    environment:
      SYMFONY__ENV__DATABASE_DRIVER: pdo_pgsql
      SYMFONY__ENV__DATABASE_HOST: postgres
      SYMFONY__ENV__DATABASE_PORT: 5432
      SYMFONY__ENV__DATABASE_NAME: wallabag_db
      SYMFONY__ENV__DATABASE_USER: info_user
      SYMFONY__ENV__DATABASE_PASSWORD: your_secure_password
      SYMFONY__ENV__DATABASE_CHARSET: utf8
      SYMFONY__ENV__SECRET: your_wallabag_secret_key
      SYMFONY__ENV__DOMAIN_NAME: http://localhost:8083
      SYMFONY__ENV__MAILER_DSN: smtp://localhost
      SYMFONY__ENV__FROM_EMAIL: wallabag@localhost
    volumes:
      - ./data/wallabag/images:/var/www/wallabag/web/assets/images
      - ./data/wallabag/data:/var/www/wallabag/data
    ports:
      - "8083:80"
    networks:
      - info-net
    depends_on:
      - postgres
    restart: unless-stopped

networks:
  info-net:
    external: true
```

### 第三步：分析和可视化工具

#### 3.1 Kibana数据可视化
创建 `docker-compose.kibana.yml`:
```yaml
version: '3.8'

services:
  kibana:
    image: kibana:8.11.0
    container_name: info-kibana
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
      ELASTICSEARCH_USERNAME: ""
      ELASTICSEARCH_PASSWORD: ""
      XPACK_SECURITY_ENABLED: false
    ports:
      - "5601:5601"
    networks:
      - info-net
    depends_on:
      - elasticsearch
    restart: unless-stopped
    volumes:
      - ./config/kibana/kibana.yml:/usr/share/kibana/config/kibana.yml

networks:
  info-net:
    external: true
```

#### 3.2 Metabase数据分析
创建 `docker-compose.metabase.yml`:
```yaml
version: '3.8'

services:
  metabase:
    image: metabase/metabase:latest
    container_name: info-metabase
    environment:
      MB_DB_TYPE: postgres
      MB_DB_DBNAME: metabase_db
      MB_DB_PORT: 5432
      MB_DB_USER: info_user
      MB_DB_PASS: your_secure_password
      MB_DB_HOST: postgres
    ports:
      - "3001:3000"
    networks:
      - info-net
    depends_on:
      - postgres
    restart: unless-stopped
    volumes:
      - ./data/metabase:/metabase-data

networks:
  info-net:
    external: true
```

### 第四步：反向代理和SSL配置

#### 4.1 Nginx反向代理
创建 `docker-compose.nginx.yml`:
```yaml
version: '3.8'

services:
  nginx:
    image: nginx:alpine
    container_name: info-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./config/nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./config/nginx/conf.d:/etc/nginx/conf.d:ro
      - ./data/nginx/ssl:/etc/nginx/ssl:ro
      - ./logs/nginx:/var/log/nginx
    networks:
      - info-net
    depends_on:
      - freshrss
      - outline
      - bookstack
      - wallabag
      - kibana
      - metabase
    restart: unless-stopped

networks:
  info-net:
    external: true
```

创建Nginx配置文件 `config/nginx/nginx.conf`:
```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';
    
    access_log /var/log/nginx/access.log main;
    
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    
    gzip on;
    gzip_vary on;
    gzip_min_length 10240;
    gzip_proxied expired no-cache no-store private must-revalidate auth;
    gzip_types text/plain text/css text/xml text/javascript 
               application/x-javascript application/xml+rss;
    
    include /etc/nginx/conf.d/*.conf;
}
```

创建服务配置 `config/nginx/conf.d/info-collection.conf`:
```nginx
# FreshRSS
server {
    listen 80;
    server_name rss.localhost rss.yourdomain.com;
    
    location / {
        proxy_pass http://freshrss:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Outline
server {
    listen 80;
    server_name outline.localhost outline.yourdomain.com;
    
    location / {
        proxy_pass http://outline:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}

# BookStack
server {
    listen 80;
    server_name docs.localhost docs.yourdomain.com;
    
    location / {
        proxy_pass http://bookstack:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Wallabag
server {
    listen 80;
    server_name read.localhost read.yourdomain.com;
    
    location / {
        proxy_pass http://wallabag:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Kibana
server {
    listen 80;
    server_name kibana.localhost kibana.yourdomain.com;
    
    location / {
        proxy_pass http://kibana:5601;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Metabase
server {
    listen 80;
    server_name analytics.localhost analytics.yourdomain.com;
    
    location / {
        proxy_pass http://metabase:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 第五步：一键部署脚本

#### 5.1 主部署脚本
创建 `deploy.sh`:
```bash
#!/bin/bash

# 信息收集系统部署脚本
set -e

echo "🚀 开始部署信息收集整理系统..."

# 检查Docker和Docker Compose
command -v docker >/dev/null 2>&1 || { echo "❌ Docker未安装"; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "❌ Docker Compose未安装"; exit 1; }

# 创建网络
echo "📡 创建Docker网络..."
docker network create info-net 2>/dev/null || echo "网络已存在"

# 初始化数据库
echo "🗄️ 初始化数据库..."
cat > config/postgres/01-init-databases.sql << EOF
-- 创建各个应用的数据库
CREATE DATABASE outline_db;
CREATE DATABASE bookstack_db;
CREATE DATABASE wallabag_db;
CREATE DATABASE metabase_db;

-- 授权用户访问
GRANT ALL PRIVILEGES ON DATABASE outline_db TO info_user;
GRANT ALL PRIVILEGES ON DATABASE bookstack_db TO info_user;
GRANT ALL PRIVILEGES ON DATABASE wallabag_db TO info_user;
GRANT ALL PRIVILEGES ON DATABASE metabase_db TO info_user;
EOF

# 按顺序启动服务
echo "🔧 启动基础服务..."
docker-compose -f docker-compose.base.yml up -d

echo "⏳ 等待数据库就绪..."
sleep 30

echo "📚 启动信息收集工具..."
docker-compose -f docker-compose.freshrss.yml up -d
docker-compose -f docker-compose.outline.yml up -d
docker-compose -f docker-compose.bookstack.yml up -d
docker-compose -f docker-compose.wallabag.yml up -d

echo "📊 启动分析工具..."
docker-compose -f docker-compose.kibana.yml up -d
docker-compose -f docker-compose.metabase.yml up -d

echo "🌐 启动反向代理..."
docker-compose -f docker-compose.nginx.yml up -d

echo "✅ 部署完成！"
echo ""
echo "🎯 访问地址："
echo "📰 RSS聚合器: http://rss.localhost"
echo "📝 知识库: http://outline.localhost"
echo "📖 文档平台: http://docs.localhost"
echo "🔖 稍后阅读: http://read.localhost"
echo "📈 数据可视化: http://kibana.localhost"
echo "📊 数据分析: http://analytics.localhost"
echo ""
echo "📋 查看服务状态: docker-compose ps"
echo "📜 查看日志: docker-compose logs -f [service_name]"
```

#### 5.2 配置hosts文件（开发环境）
```bash
# 添加到 /etc/hosts (Linux/Mac) 或 C:\Windows\System32\drivers\etc\hosts (Windows)
127.0.0.1 rss.localhost
127.0.0.1 outline.localhost
127.0.0.1 docs.localhost
127.0.0.1 read.localhost
127.0.0.1 kibana.localhost
127.0.0.1 analytics.localhost
```

### 第六步：高级配置

#### 6.1 全文搜索集成
创建 `scripts/setup-elasticsearch-integration.sh`:
```bash
#!/bin/bash

# 配置Elasticsearch索引模板
curl -X PUT "localhost:9200/_index_template/info-collection" \
-H 'Content-Type: application/json' \
-d '{
  "index_patterns": ["info-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "analysis": {
        "analyzer": {
          "chinese_analyzer": {
            "type": "custom",
            "tokenizer": "ik_max_word",
            "filter": ["lowercase"]
          }
        }
      }
    },
    "mappings": {
      "properties": {
        "title": {
          "type": "text",
          "analyzer": "chinese_analyzer"
        },
        "content": {
          "type": "text",
          "analyzer": "chinese_analyzer"
        },
        "tags": {
          "type": "keyword"
        },
        "created_at": {
          "type": "date"
        },
        "source": {
          "type": "keyword"
        }
      }
    }
  }
}'

echo "Elasticsearch索引模板创建完成"
```

#### 6.2 数据同步脚本
创建 `scripts/sync-data.py`:
```python
#!/usr/bin/env python3
"""
信息收集系统数据同步脚本
同步各个工具的数据到Elasticsearch进行统一搜索
"""

import requests
import json
import psycopg2
from elasticsearch import Elasticsearch
from datetime import datetime
import logging

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class InfoCollectionSync:
    def __init__(self):
        self.es = Elasticsearch([{'host': 'localhost', 'port': 9200}])
        self.db_config = {
            'host': 'localhost',
            'port': 5432,
            'user': 'info_user',
            'password': 'your_secure_password'
        }
    
    def sync_freshrss_articles(self):
        """同步FreshRSS文章到Elasticsearch"""
        try:
            # 连接FreshRSS数据库
            conn = psycopg2.connect(database='freshrss', **self.db_config)
            cursor = conn.cursor()
            
            # 查询最新文章
            cursor.execute("""
                SELECT id, title, content, link, date_published, tags
                FROM articles 
                WHERE date_published > NOW() - INTERVAL '7 days'
            """)
            
            articles = cursor.fetchall()
            
            for article in articles:
                doc = {
                    'title': article[1],
                    'content': article[2],
                    'url': article[3],
                    'created_at': article[4],
                    'tags': article[5].split(',') if article[5] else [],
                    'source': 'freshrss'
                }
                
                self.es.index(
                    index=f"info-freshrss-{datetime.now().strftime('%Y-%m')}",
                    body=doc
                )
            
            logger.info(f"同步了 {len(articles)} 篇FreshRSS文章")
            
        except Exception as e:
            logger.error(f"FreshRSS同步失败: {e}")
        finally:
            if 'conn' in locals():
                conn.close()
    
    def sync_outline_documents(self):
        """同步Outline文档到Elasticsearch"""
        try:
            # 使用Outline API
            headers = {'Authorization': 'Bearer YOUR_OUTLINE_API_TOKEN'}
            response = requests.get(
                'http://localhost:8081/api/documents.list',
                headers=headers
            )
            
            if response.status_code == 200:
                documents = response.json()['data']
                
                for doc in documents:
                    es_doc = {
                        'title': doc['title'],
                        'content': doc['text'],
                        'url': doc['url'],
                        'created_at': doc['createdAt'],
                        'tags': doc.get('tags', []),
                        'source': 'outline'
                    }
                    
                    self.es.index(
                        index=f"info-outline-{datetime.now().strftime('%Y-%m')}",
                        body=es_doc
                    )
                
                logger.info(f"同步了 {len(documents)} 个Outline文档")
            
        except Exception as e:
            logger.error(f"Outline同步失败: {e}")
    
    def sync_wallabag_articles(self):
        """同步Wallabag文章到Elasticsearch"""
        try:
            conn = psycopg2.connect(database='wallabag_db', **self.db_config)
            cursor = conn.cursor()
            
            cursor.execute("""
                SELECT title, content, url, created_at, is_starred
                FROM entry 
                WHERE created_at > NOW() - INTERVAL '7 days'
            """)
            
            articles = cursor.fetchall()
            
            for article in articles:
                doc = {
                    'title': article[0],
                    'content': article[1],
                    'url': article[2],
                    'created_at': article[3],
                    'is_starred': article[4],
                    'source': 'wallabag'
                }
                
                self.es.index(
                    index=f"info-wallabag-{datetime.now().strftime('%Y-%m')}",
                    body=doc
                )
            
            logger.info(f"同步了 {len(articles)} 篇Wallabag文章")
            
        except Exception as e:
            logger.error(f"Wallabag同步失败: {e}")
        finally:
            if 'conn' in locals():
                conn.close()

if __name__ == "__main__":
    sync = InfoCollectionSync()
    sync.sync_freshrss_articles()
    sync.sync_outline_documents()
    sync.sync_wallabag_articles()
```

#### 6.3 定时任务配置
创建 `docker-compose.cron.yml`:
```yaml
version: '3.8'

services:
  cron-jobs:
    image: alpine:latest
    container_name: info-cron
    volumes:
      - ./scripts:/scripts
      - ./logs:/logs
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - info-net
    command: >
      sh -c "
        apk add --no-cache python3 py3-pip dcron &&
        pip3 install requests psycopg2-binary elasticsearch &&
        echo '0 */6 * * * cd /scripts && python3 sync-data.py >> /logs/sync.log 2>&1' | crontab - &&
        echo '0 2 * * * cd /scripts && ./backup.sh >> /logs/backup.log 2>&1' | crontab - &&
        crond -f
      "
    restart: unless-stopped

networks:
  info-net:
    external: true
```

## 高级配置

### SSL/TLS配置
使用Let's Encrypt自动获取SSL证书：

```yaml
# docker-compose.ssl.yml
version: '3.8'

services:
  certbot:
    image: certbot/certbot
    container_name: info-certbot
    volumes:
      - ./data/certbot/conf:/etc/letsencrypt
      - ./data/certbot/www:/var/www/certbot
    command: certonly --webroot --webroot-path=/var/www/certbot --email your-email@domain.com --agree-tos --no-eff-email -d yourdomain.com
    networks:
      - info-net

networks:
  info-net:
    external: true
```

### 监控和告警
集成Prometheus和Grafana：

```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: info-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./config/prometheus:/etc/prometheus
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
    networks:
      - info-net
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: info-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./config/grafana:/etc/grafana/provisioning
    networks:
      - info-net
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:

networks:
  info-net:
    external: true
```

## 使用方法

### 初始配置步骤

#### 1. FreshRSS设置
1. 访问 `http://rss.localhost`
2. 创建管理员账户
3. 添加RSS订阅源
4. 配置自动更新间隔
5. 设置分类和标签

#### 2. Outline配置
1. 访问 `http://outline.localhost`
2. 创建团队和用户账户
3. 导入已有文档或创建新文档
4. 设置文档结构和权限

#### 3. BookStack设置
1. 访问 `http://docs.localhost`
2. 使用默认账户登录 (admin@admin.com / password)
3. 创建书籍和章节结构
4. 配置用户权限和角色

#### 4. Wallabag配置
1. 访问 `http://read.localhost`
2. 创建用户账户
3. 安装浏览器扩展
4. 配置移动应用

### 工作流程示例

#### 信息收集工作流
```
1. 发现信息源
   ├── RSS订阅 → FreshRSS
   ├── 网页文章 → Wallabag
   └── 即时想法 → Outline

2. 信息处理
   ├── 阅读和标记 → Wallabag
   ├── 提取要点 → Outline
   └── 结构化整理 → BookStack

3. 知识应用
   ├── 搜索查找 → Elasticsearch
   ├── 数据分析 → Metabase
   └── 可视化展示 → Kibana
```

#### 团队协作流程
```
1. 信息共享
   ├── 团队RSS源 → FreshRSS
   ├── 共享知识库 → Outline
   └── 项目文档 → BookStack

2. 协作编辑
   ├── 实时编辑 → Outline
   ├── 版本控制 → BookStack
   └── 评论讨论 → 各平台

3. 知识管理
   ├── 分类整理 → 标签系统
   ├── 权限控制 → 用户管理
   └── 数据备份 → 自动备份
```

## 维护和备份

### 自动备份脚本
创建 `scripts/backup.sh`:
```bash
#!/bin/bash

# 信息收集系统备份脚本
BACKUP_DIR="/backups/info-collection-$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

echo "🗄️ 开始备份数据库..."

# 备份PostgreSQL数据库
docker exec info-postgres pg_dumpall -U info_user > "$BACKUP_DIR/postgres_backup.sql"

# 备份Redis数据
docker exec info-redis redis-cli --rdb /tmp/dump.rdb
docker cp info-redis:/tmp/dump.rdb "$BACKUP_DIR/redis_backup.rdb"

# 备份Elasticsearch数据
curl -X POST "localhost:9200/_snapshot/backup_repo/snapshot_$(date +%Y%m%d_%H%M%S)?wait_for_completion=true"

echo "📁 备份应用数据..."

# 备份应用数据目录
tar -czf "$BACKUP_DIR/app_data.tar.gz" ./data/

# 备份配置文件
tar -czf "$BACKUP_DIR/configs.tar.gz" ./config/

echo "🧹 清理老备份..."
find /backups -type d -mtime +30 -name "info-collection-*" -exec rm -rf {} +

echo "✅ 备份完成: $BACKUP_DIR"
```

### 恢复脚本
创建 `scripts/restore.sh`:
```bash
#!/bin/bash

if [ -z "$1" ]; then
    echo "使用方法: $0 <backup_directory>"
    exit 1
fi

BACKUP_DIR=$1

echo "🔄 开始恢复数据..."

# 停止所有服务
docker-compose down

# 恢复PostgreSQL
if [ -f "$BACKUP_DIR/postgres_backup.sql" ]; then
    docker-compose -f docker-compose.base.yml up -d postgres
    sleep 30
    docker exec -i info-postgres psql -U info_user < "$BACKUP_DIR/postgres_backup.sql"
fi

# 恢复Redis
if [ -f "$BACKUP_DIR/redis_backup.rdb" ]; then
    docker cp "$BACKUP_DIR/redis_backup.rdb" info-redis:/data/dump.rdb
fi

# 恢复应用数据
if [ -f "$BACKUP_DIR/app_data.tar.gz" ]; then
    tar -xzf "$BACKUP_DIR/app_data.tar.gz" -C ./
fi

# 恢复配置文件
if [ -f "$BACKUP_DIR/configs.tar.gz" ]; then
    tar -xzf "$BACKUP_DIR/configs.tar.gz" -C ./
fi

# 重新启动所有服务
./deploy.sh

echo "✅ 恢复完成"
```

### 系统监控

#### 健康检查脚本
创建 `scripts/health-check.sh`:
```bash
#!/bin/bash

# 系统健康检查脚本

echo "🏥 系统健康检查报告"
echo "=========================="

# 检查服务状态
echo "📊 服务状态："
services=("info-postgres" "info-redis" "info-elasticsearch" "info-freshrss" "info-outline" "info-bookstack" "info-wallabag" "info-kibana" "info-metabase" "info-nginx")

for service in "${services[@]}"; do
    if docker ps --format "table {{.Names}}" | grep -q "$service"; then
        echo "✅ $service: 运行中"
    else
        echo "❌ $service: 停止"
    fi
done

# 检查磁盘空间
echo ""
echo "💾 磁盘空间："
df -h | grep -E "(/$|/data)" | while read line; do
    usage=$(echo $line | awk '{print $5}' | sed 's/%//')
    if [ $usage -gt 80 ]; then
        echo "⚠️  $line"
    else
        echo "✅ $line"
    fi
done

# 检查内存使用
echo ""
echo "🧠 内存使用："
memory_usage=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100}')
if (( $(echo "$memory_usage > 80" | bc -l) )); then
    echo "⚠️  内存使用率: ${memory_usage}%"
else
    echo "✅ 内存使用率: ${memory_usage}%"
fi

# 检查网络连接
echo ""
echo "🌐 网络连接："
endpoints=("http://localhost:8080" "http://localhost:8081" "http://localhost:8082" "http://localhost:8083" "http://localhost:5601" "http://localhost:3001")

for endpoint in "${endpoints[@]}"; do
    if curl -s --head "$endpoint" | head -n 1 | grep -q "HTTP/1.[01] [23].."; then
        echo "✅ $endpoint: 可访问"
    else
        echo "❌ $endpoint: 不可访问"
    fi
done

echo ""
echo "📅 检查时间: $(date)"
```

## 故障排除

### 常见问题和解决方案

#### 1. 数据库连接失败
**症状**: 应用无法连接到PostgreSQL
**解决方案**:
```bash
# 检查数据库状态
docker logs info-postgres

# 重启数据库
docker restart info-postgres

# 检查网络连接
docker exec info-outline ping postgres
```

#### 2. Elasticsearch内存不足
**症状**: Elasticsearch启动失败或性能差
**解决方案**:
```bash
# 增加虚拟内存限制
sudo sysctl -w vm.max_map_count=262144

# 调整Elasticsearch内存配置
# 在docker-compose.base.yml中修改ES_JAVA_OPTS
```

#### 3. 磁盘空间不足
**症状**: 容器启动失败或数据写入错误
**解决方案**:
```bash
# 清理Docker系统
docker system prune -a

# 清理日志文件
find ./logs -name "*.log" -mtime +7 -delete

# 清理老备份
find ./backups -mtime +30 -delete
```

#### 4. 服务端口冲突
**症状**: 服务启动时端口已被占用
**解决方案**:
```bash
# 查看端口占用
netstat -tulpn | grep :8080

# 修改docker-compose文件中的端口映射
# 或停止占用端口的其他服务
```

### 性能优化建议

#### 1. 数据库优化
```sql
-- PostgreSQL性能调优
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
```

#### 2. Elasticsearch优化
```yaml
# elasticsearch.yml
cluster.name: "info-collection"
node.name: "info-node-1"
path.data: /usr/share/elasticsearch/data
path.logs: /usr/share/elasticsearch/logs
bootstrap.memory_lock: true
network.host: 0.0.0.0
http.port: 9200
discovery.type: single-node

# JVM选项优化
ES_JAVA_OPTS: "-Xms2g -Xmx2g -XX:+UseG1GC"
```

#### 3. Redis优化
```conf
# redis.conf
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

### 日志管理

#### 集中日志收集
创建 `docker-compose.logging.yml`:
```yaml
version: '3.8'

services:
  fluentd:
    image: fluent/fluentd:v1.16-debian-1
    container_name: info-fluentd
    volumes:
      - ./config/fluentd:/fluentd/etc
      - ./logs:/var/log
    ports:
      - "24224:24224"
    networks:
      - info-net
    restart: unless-stopped

networks:
  info-net:
    external: true
```

#### 日志分析配置
创建 `config/fluentd/fluent.conf`:
```xml
<source>
  @type forward
  port 24224
  bind 0.0.0.0
</source>

<match nginx.**>
  @type elasticsearch
  host elasticsearch
  port 9200
  index_name nginx-logs
  type_name _doc
</match>

<match app.**>
  @type elasticsearch
  host elasticsearch
  port 9200
  index_name app-logs
  type_name _doc
</match>

<match **>
  @type stdout
</match>
```

## 扩展和定制

### 添加新的信息收集工具

#### 示例：集成Miniflux RSS阅读器
```yaml
# docker-compose.miniflux.yml
version: '3.8'

services:
  miniflux:
    image: miniflux/miniflux:latest
    container_name: info-miniflux
    environment:
      DATABASE_URL: postgres://info_user:your_secure_password@postgres:5432/miniflux_db?sslmode=disable
      RUN_MIGRATIONS: 1
      CREATE_ADMIN: 1
      ADMIN_USERNAME: admin
      ADMIN_PASSWORD: admin123
    ports:
      - "8084:8080"
    networks:
      - info-net
    depends_on:
      - postgres
    restart: unless-stopped

networks:
  info-net:
    external: true
```

### API集成示例

#### Webhook接收器
创建 `services/webhook-receiver.py`:
```python
from flask import Flask, request, jsonify
import json
from elasticsearch import Elasticsearch

app = Flask(__name__)
es = Elasticsearch([{'host': 'localhost', 'port': 9200}])

@app.route('/webhook/freshrss', methods=['POST'])
def freshrss_webhook():
    """接收FreshRSS的新文章通知"""
    data = request.json
    
    # 处理数据并索引到Elasticsearch
    doc = {
        'title': data.get('title'),
        'content': data.get('content'),
        'url': data.get('link'),
        'source': 'freshrss-webhook',
        'timestamp': data.get('date_published')
    }
    
    es.index(index='info-realtime', body=doc)
    
    return jsonify({'status': 'success'})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

## 总结

这个基于Docker的信息收集整理系统提供了：

1. **完整的工具链**: RSS聚合、知识管理、文档平台、稍后阅读
2. **统一的搜索**: Elasticsearch全文搜索和数据分析
3. **容器化部署**: 一键部署，易于维护和扩展
4. **数据备份**: 自动备份和恢复机制
5. **监控告警**: 健康检查和性能监控
6. **可扩展性**: 易于添加新工具和功能

通过这套系统，您可以构建一个高效的个人或团队信息收集整理平台，提升知识管理效率。