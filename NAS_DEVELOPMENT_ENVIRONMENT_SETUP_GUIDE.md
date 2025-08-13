# NAS开发环境设置指南

## 硬件配置
- **处理器**: AMD Ryzen 5 5825 (8核16线程)
- **内存**: 64GB RAM
- **支持技术**: Docker, Docker Compose, 虚拟机

## 核心开发工具安装

### 1. 容器化开发环境

#### Docker基础服务栈
```yaml
# docker-compose.yml - 基础开发服务
version: '3.8'
services:
  # 代码仓库
  gitlab:
    image: gitlab/gitlab-ce:latest
    container_name: gitlab-dev
    ports:
      - "8080:80"
      - "8443:443"
      - "2222:22"
    volumes:
      - gitlab_config:/etc/gitlab
      - gitlab_logs:/var/log/gitlab
      - gitlab_data:/var/opt/gitlab
    environment:
      GITLAB_OMNIBUS_CONFIG: |
        external_url 'http://nas-ip:8080'
        gitlab_rails['gitlab_shell_ssh_port'] = 2222
    restart: unless-stopped
    
  # 数据库服务
  postgres:
    image: postgres:15
    container_name: postgres-dev
    environment:
      POSTGRES_DB: devdb
      POSTGRES_USER: developer
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped
    
  mysql:
    image: mysql:8.0
    container_name: mysql-dev
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: devdb
      MYSQL_USER: developer
      MYSQL_PASSWORD: dev_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    restart: unless-stopped
    
  # Redis缓存
  redis:
    image: redis:7-alpine
    container_name: redis-dev
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    restart: unless-stopped
    
  # MongoDB
  mongodb:
    image: mongo:6
    container_name: mongodb-dev
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin_password
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
    restart: unless-stopped

volumes:
  gitlab_config:
  gitlab_logs:
  gitlab_data:
  postgres_data:
  mysql_data:
  redis_data:
  mongodb_data:
```

#### 开发工具容器
```yaml
# docker-compose.tools.yml - 开发工具
version: '3.8'
services:
  # Jenkins CI/CD
  jenkins:
    image: jenkins/jenkins:lts
    container_name: jenkins-dev
    ports:
      - "8081:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    restart: unless-stopped
    
  # SonarQube代码质量
  sonarqube:
    image: sonarqube:community
    container_name: sonarqube-dev
    ports:
      - "9000:9000"
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://postgres:5432/sonarqube
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonar_password
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_logs:/opt/sonarqube/logs
      - sonarqube_extensions:/opt/sonarqube/extensions
    restart: unless-stopped
    
  # Nexus仓库管理
  nexus:
    image: sonatype/nexus3
    container_name: nexus-dev
    ports:
      - "8082:8081"
    volumes:
      - nexus_data:/nexus-data
    restart: unless-stopped
    
  # Portainer容器管理
  portainer:
    image: portainer/portainer-ce
    container_name: portainer-dev
    ports:
      - "9443:9443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - portainer_data:/data
    restart: unless-stopped

volumes:
  jenkins_home:
  sonarqube_data:
  sonarqube_logs:
  sonarqube_extensions:
  nexus_data:
  portainer_data:
```

### 2. 虚拟机开发环境

#### 推荐虚拟机配置
1. **主开发虚拟机** (Ubuntu 22.04 LTS)
   - 内存: 16GB
   - 存储: 200GB SSD
   - 用途: 主要开发环境

2. **Windows开发虚拟机** (Windows 11)
   - 内存: 12GB
   - 存储: 150GB SSD
   - 用途: .NET开发、Windows特定应用

3. **测试环境虚拟机** (CentOS 8)
   - 内存: 8GB
   - 存储: 100GB
   - 用途: 生产环境模拟

### 3. 编程语言环境

#### Docker方式安装开发环境
```dockerfile
# Dockerfile.dev-stack
FROM ubuntu:22.04

# 基础工具
RUN apt-get update && apt-get install -y \
    curl wget git vim nano \
    build-essential \
    software-properties-common \
    apt-transport-https \
    ca-certificates \
    gnupg \
    lsb-release

# Node.js (多版本)
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

# Python (多版本)
RUN add-apt-repository ppa:deadsnakes/ppa && \
    apt-get update && \
    apt-get install -y python3.9 python3.10 python3.11 \
    python3-pip python3-venv

# Java
RUN apt-get install -y openjdk-8-jdk openjdk-11-jdk openjdk-17-jdk

# Go
RUN wget https://go.dev/dl/go1.21.0.linux-amd64.tar.gz && \
    tar -C /usr/local -xzf go1.21.0.linux-amd64.tar.gz

# Rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y

# Docker CLI
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce-cli

# Kubernetes工具
RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

WORKDIR /workspace
```

### 4. 监控和日志

#### 监控栈
```yaml
# docker-compose.monitoring.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus
    container_name: prometheus-dev
    ports:
      - "9090:9090"
    volumes:
      - prometheus_data:/prometheus
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    restart: unless-stopped
    
  grafana:
    image: grafana/grafana
    container_name: grafana-dev
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin_password
    volumes:
      - grafana_data:/var/lib/grafana
    restart: unless-stopped
    
  elasticsearch:
    image: elasticsearch:8.8.0
    container_name: elasticsearch-dev
    environment:
      discovery.type: single-node
      ES_JAVA_OPTS: "-Xms2g -Xmx2g"
      xpack.security.enabled: false
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    restart: unless-stopped
    
  kibana:
    image: kibana:8.8.0
    container_name: kibana-dev
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
  elasticsearch_data:
```

### 5. 网络和安全配置

#### 反向代理配置
```yaml
# docker-compose.proxy.yml
version: '3.8'
services:
  nginx:
    image: nginx:alpine
    container_name: nginx-proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    restart: unless-stopped
    
  # 可选: 使用Traefik作为更现代的反向代理
  traefik:
    image: traefik:v3.0
    container_name: traefik-proxy
    command:
      - "--api.insecure=true"
      - "--providers.docker=true"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
    ports:
      - "80:80"
      - "443:443"
      - "8080:8080"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    restart: unless-stopped
```

### 6. 备份策略

#### 自动备份脚本
```bash
#!/bin/bash
# backup-dev-env.sh

BACKUP_DIR="/nas/backups/dev-env"
DATE=$(date +%Y%m%d_%H%M%S)

# 备份Docker卷
docker run --rm -v gitlab_data:/data -v $BACKUP_DIR:/backup alpine tar czf /backup/gitlab_$DATE.tar.gz -C /data .
docker run --rm -v postgres_data:/data -v $BACKUP_DIR:/backup alpine tar czf /backup/postgres_$DATE.tar.gz -C /data .

# 备份配置文件
tar czf $BACKUP_DIR/configs_$DATE.tar.gz docker-compose*.yml nginx.conf

# 清理老备份 (保留30天)
find $BACKUP_DIR -type f -mtime +30 -delete

echo "Backup completed: $DATE"
```

### 7. 资源分配建议

#### 内存分配 (64GB总内存)
- **宿主机系统**: 8GB
- **Docker服务**: 32GB
  - GitLab: 4GB
  - 数据库服务: 8GB
  - Jenkins: 2GB
  - SonarQube: 4GB
  - 监控栈: 6GB
  - 其他服务: 8GB
- **虚拟机**: 20GB
- **系统缓存**: 4GB

#### 存储规划
- **系统盘**: SSD 500GB+
- **数据盘**: HDD 2TB+ (代码仓库、数据库)
- **备份盘**: HDD 1TB+ (自动备份)

### 8. 安全最佳实践

1. **网络隔离**: 使用Docker网络分离不同服务
2. **访问控制**: 配置防火墙规则，只开放必要端口
3. **SSL/TLS**: 为所有Web服务配置HTTPS
4. **定期更新**: 自动更新容器镜像和系统补丁
5. **备份验证**: 定期测试备份恢复流程

### 9. 启动脚本

```bash
#!/bin/bash
# start-dev-env.sh

echo "启动NAS开发环境..."

# 启动基础服务
docker-compose -f docker-compose.yml up -d

# 启动开发工具
docker-compose -f docker-compose.tools.yml up -d

# 启动监控
docker-compose -f docker-compose.monitoring.yml up -d

# 启动代理
docker-compose -f docker-compose.proxy.yml up -d

echo "开发环境启动完成!"
echo "访问地址:"
echo "- GitLab: http://nas-ip:8080"
echo "- Jenkins: http://nas-ip:8081"
echo "- SonarQube: http://nas-ip:9000"
echo "- Nexus: http://nas-ip:8082"
echo "- Grafana: http://nas-ip:3000"
echo "- Portainer: https://nas-ip:9443"
```

### 10. 性能优化建议

1. **SSD加速**: 将频繁访问的数据存储在SSD上
2. **内存优化**: 合理分配JVM堆内存
3. **并发控制**: 根据CPU核心数配置并发任务
4. **缓存策略**: 使用Redis缓存常用数据
5. **定期清理**: 自动清理旧的构建产物和日志

这个设置为您的NAS提供了完整的开发环境，充分利用了64GB内存和AMD R5 5825的性能。所有服务都通过Docker容器化部署，便于管理和扩展。