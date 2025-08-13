# AI代码工具容器化部署与Web SSH访问研究

## 研究概述

本文深入研究了Claude Code和Gemini Code等AI代码工具在Docker容器和虚拟机环境中的部署策略，重点解决Web登录认证、多账号切换、代理配置等关键技术问题。

## 目录
1. [技术背景分析](#技术背景分析)
2. [部署架构对比](#部署架构对比)
3. [Docker容器化方案](#docker容器化方案)
4. [虚拟机Web SSH方案](#虚拟机web-ssh方案)
5. [账号认证与切换机制](#账号认证与切换机制)
6. [代理配置管理](#代理配置管理)
7. [实际部署案例](#实际部署案例)
8. [性能与安全评估](#性能与安全评估)
9. [最佳实践建议](#最佳实践建议)

## 技术背景分析

### AI代码工具的认证挑战

#### Claude Code认证机制
- **OAuth 2.0流程**: 需要浏览器进行身份验证
- **API密钥方式**: 支持环境变量配置
- **会话管理**: 基于token的会话保持
- **多租户支持**: 支持组织和个人账号切换

#### Gemini Code认证机制
- **Google账号集成**: 依赖Google OAuth流程
- **服务账号**: 支持JSON密钥文件认证
- **API配额管理**: 基于项目和用户的配额限制
- **区域限制**: 需要考虑地理位置和代理需求

### 容器化部署的技术难点

#### 1. 浏览器访问限制
```
传统容器环境问题:
├── 无图形界面
├── 无浏览器支持
├── OAuth流程中断
└── 会话状态丢失
```

#### 2. 网络访问控制
```
网络配置挑战:
├── 代理服务器配置
├── DNS解析问题
├── 防火墙规则
└── 证书验证
```

#### 3. 状态持久化
```
数据持久化需求:
├── 认证token存储
├── 配置文件保存
├── 项目代码同步
└── 日志文件管理
```

## 部署架构对比

### 方案一：纯Docker容器方案

#### 架构图
```
┌─────────────────────────────────────────┐
│              Web Browser                │
│          (Host Machine)                 │
└─────────────┬───────────────────────────┘
              │ OAuth Redirect
              ▼
┌─────────────────────────────────────────┐
│           Docker Container              │
│  ┌─────────────────────────────────┐    │
│  │        VNC Server               │    │
│  │     ┌─────────────────────┐     │    │
│  │     │   Desktop Env       │     │    │
│  │     │  ┌───────────────┐  │     │    │
│  │     │  │ Claude Code   │  │     │    │
│  │     │  │ Gemini Code   │  │     │    │
│  │     │  └───────────────┘  │     │    │
│  │     └─────────────────────┘     │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

#### 优势分析
- **轻量级部署**: 资源占用相对较少
- **快速启动**: 容器启动速度快
- **易于扩展**: 可以快速创建多个实例
- **版本控制**: 镜像版本管理方便

#### 劣势分析
- **浏览器支持复杂**: 需要VNC或X11转发
- **性能开销**: 图形界面转发影响性能
- **会话管理困难**: OAuth重定向处理复杂

### 方案二：虚拟机Web SSH方案

#### 架构图
```
┌─────────────────────────────────────────┐
│              Web Browser                │
└─────────────┬───────────────────────────┘
              │ HTTPS
              ▼
┌─────────────────────────────────────────┐
│            Web SSH Gateway             │
│  ┌─────────────────────────────────┐    │
│  │      Guacamole/Shellinabox      │    │
│  └─────────────┬───────────────────┘    │
└────────────────┼────────────────────────┘
                 │ SSH
                 ▼
┌─────────────────────────────────────────┐
│           Virtual Machine               │
│  ┌─────────────────────────────────┐    │
│  │         Ubuntu Desktop          │    │
│  │  ┌───────────────────────────┐  │    │
│  │  │      Claude Code          │  │    │
│  │  │      Gemini Code          │  │    │
│  │  │      VS Code              │  │    │
│  │  │      Firefox/Chrome       │  │    │
│  │  └───────────────────────────┘  │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

#### 优势分析
- **完整桌面环境**: 支持完整的浏览器OAuth流程
- **原生体验**: 接近本地开发环境
- **灵活性强**: 可以安装任意软件
- **隔离性好**: 每个用户独立的虚拟机环境

#### 劣势分析
- **资源消耗大**: 需要更多CPU和内存
- **启动时间长**: 虚拟机启动较慢
- **管理复杂**: 需要额外的虚拟化管理

## Docker容器化方案

### 方案2.1：基于VNC的容器方案

#### Dockerfile配置
```dockerfile
# Dockerfile for AI Code Tools Container
FROM ubuntu:22.04

# 避免交互式安装
ENV DEBIAN_FRONTEND=noninteractive

# 安装基础依赖
RUN apt-get update && apt-get install -y \
    # 基础工具
    curl wget git vim nano \
    # 图形界面支持
    xfce4 xfce4-goodies \
    # VNC服务器
    tightvncserver \
    # 浏览器
    firefox \
    # 开发工具
    nodejs npm python3 python3-pip \
    # 网络工具
    openssh-client proxychains4 \
    && rm -rf /var/lib/apt/lists/*

# 安装noVNC (Web VNC客户端)
RUN git clone https://github.com/novnc/noVNC.git /opt/novnc && \
    git clone https://github.com/novnc/websockify /opt/novnc/utils/websockify

# 创建用户
RUN useradd -m -s /bin/bash developer && \
    echo 'developer:password' | chpasswd

# 安装Claude Code
RUN curl -fsSL https://claude.ai/install.sh | bash

# 安装Google Cloud CLI (for Gemini)
RUN curl https://sdk.cloud.google.com | bash

# VNC配置
USER developer
WORKDIR /home/developer

# 设置VNC密码
RUN mkdir .vnc && \
    echo "password" | vncpasswd -f > .vnc/passwd && \
    chmod 600 .vnc/passwd

# VNC启动脚本
COPY scripts/start-vnc.sh /home/developer/
RUN chmod +x start-vnc.sh

# 配置文件
COPY config/ /home/developer/.config/

EXPOSE 5901 6080

CMD ["./start-vnc.sh"]
```

#### VNC启动脚本
```bash
#!/bin/bash
# scripts/start-vnc.sh

# 启动VNC服务器
vncserver :1 -geometry 1920x1080 -depth 24

# 启动noVNC Web客户端
/opt/novnc/utils/novnc_proxy --vnc localhost:5901 --listen 6080 &

# 保持容器运行
tail -f /dev/null
```

#### Docker Compose配置
```yaml
# docker-compose.ai-tools.yml
version: '3.8'

services:
  ai-code-tools:
    build: 
      context: .
      dockerfile: Dockerfile.ai-tools
    container_name: ai-code-tools
    environment:
      - DISPLAY=:1
      - VNC_PASSWORD=secure_password
      # Claude Code配置
      - CLAUDE_API_KEY=${CLAUDE_API_KEY}
      - CLAUDE_ORG_ID=${CLAUDE_ORG_ID}
      # Gemini配置
      - GOOGLE_APPLICATION_CREDENTIALS=/home/developer/.config/gcloud/service-account.json
      - GEMINI_PROJECT_ID=${GEMINI_PROJECT_ID}
      # 代理配置
      - HTTP_PROXY=${HTTP_PROXY}
      - HTTPS_PROXY=${HTTPS_PROXY}
      - NO_PROXY=${NO_PROXY}
    ports:
      - "5901:5901"  # VNC
      - "6080:6080"  # noVNC Web
    volumes:
      - ./data/projects:/home/developer/projects
      - ./data/config:/home/developer/.config
      - ./data/claude:/home/developer/.claude
      - ./data/gcloud:/home/developer/.config/gcloud
    networks:
      - ai-tools-net
    restart: unless-stopped

networks:
  ai-tools-net:
    driver: bridge
```

### 方案2.2：基于X11转发的容器方案

#### X11转发Dockerfile
```dockerfile
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# 安装X11和开发工具
RUN apt-get update && apt-get install -y \
    x11-apps x11-utils x11-xserver-utils \
    dbus-x11 \
    firefox \
    code \
    curl wget git \
    && rm -rf /var/lib/apt/lists/*

# 安装AI工具
RUN curl -fsSL https://claude.ai/install.sh | bash && \
    npm install -g @google/generative-ai

# 创建用户
RUN useradd -m -s /bin/bash developer
USER developer
WORKDIR /home/developer

# 启动脚本
COPY scripts/start-x11.sh /home/developer/
RUN chmod +x start-x11.sh

CMD ["./start-x11.sh"]
```

#### X11启动脚本
```bash
#!/bin/bash
# scripts/start-x11.sh

# 等待X11 socket可用
while [ ! -e /tmp/.X11-unix/X${DISPLAY#:} ]; do
    sleep 1
done

# 启动桌面会话
exec dbus-launch --exit-with-session startxfce4
```

#### X11 Docker Compose
```yaml
version: '3.8'

services:
  ai-tools-x11:
    build:
      context: .
      dockerfile: Dockerfile.x11
    container_name: ai-tools-x11
    environment:
      - DISPLAY=${DISPLAY}
      - CLAUDE_API_KEY=${CLAUDE_API_KEY}
      - GOOGLE_APPLICATION_CREDENTIALS=/home/developer/.config/gcloud/key.json
    volumes:
      - /tmp/.X11-unix:/tmp/.X11-unix:rw
      - ./data:/home/developer/data
    network_mode: host
    restart: unless-stopped
```

## 虚拟机Web SSH方案

### 方案3.1：基于Apache Guacamole的方案

#### Guacamole部署架构
```yaml
# docker-compose.guacamole.yml
version: '3.8'

services:
  # Guacamole数据库
  guacamole-db:
    image: postgres:13
    container_name: guacamole-postgres
    environment:
      POSTGRES_DB: guacamole_db
      POSTGRES_USER: guacamole_user
      POSTGRES_PASSWORD: secure_password
    volumes:
      - guacamole_db:/var/lib/postgresql/data
      - ./scripts/initdb.sql:/docker-entrypoint-initdb.d/initdb.sql
    restart: unless-stopped

  # Guacamole daemon
  guacd:
    image: guacamole/guacd:latest
    container_name: guacd
    restart: unless-stopped

  # Guacamole web应用
  guacamole:
    image: guacamole/guacamole:latest
    container_name: guacamole
    environment:
      GUACD_HOSTNAME: guacd
      POSTGRES_DATABASE: guacamole_db
      POSTGRES_HOSTNAME: guacamole-db
      POSTGRES_USER: guacamole_user
      POSTGRES_PASSWORD: secure_password
    ports:
      - "8080:8080"
    depends_on:
      - guacamole-db
      - guacd
    restart: unless-stopped

  # 反向代理
  nginx:
    image: nginx:alpine
    container_name: guacamole-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./config/nginx:/etc/nginx/conf.d
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - guacamole
    restart: unless-stopped

volumes:
  guacamole_db:
```

#### 虚拟机配置脚本
```bash
#!/bin/bash
# scripts/setup-ai-dev-vm.sh

# 创建AI开发虚拟机
VM_NAME="ai-dev-vm"
VM_MEMORY="8192"
VM_DISK="100G"
VM_CPUS="4"

# 使用KVM创建虚拟机
virt-install \
    --name $VM_NAME \
    --memory $VM_MEMORY \
    --vcpus $VM_CPUS \
    --disk size=100 \
    --cdrom ubuntu-22.04-desktop-amd64.iso \
    --os-variant ubuntu22.04 \
    --network bridge=virbr0 \
    --graphics vnc,listen=0.0.0.0 \
    --noautoconsole

# 等待虚拟机安装完成
echo "虚拟机创建中，请通过VNC完成安装..."
echo "VNC地址: vnc://$(hostname):5900"

# 虚拟机配置完成后的自动化脚本
cat > vm-post-install.sh << 'EOF'
#!/bin/bash
# 在虚拟机内运行的配置脚本

# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装开发工具
sudo apt install -y \
    curl wget git vim \
    nodejs npm python3 python3-pip \
    code firefox \
    openssh-server \
    xrdp

# 安装Claude Code
curl -fsSL https://claude.ai/install.sh | bash

# 安装Google Cloud CLI
curl https://sdk.cloud.google.com | bash
source ~/.bashrc

# 配置SSH
sudo systemctl enable ssh
sudo systemctl start ssh

# 配置XRDP
sudo systemctl enable xrdp
sudo systemctl start xrdp

# 创建开发用户
sudo useradd -m -s /bin/bash aidev
sudo usermod -aG sudo aidev
echo 'aidev:dev_password' | sudo chpasswd

# 配置防火墙
sudo ufw allow ssh
sudo ufw allow 3389  # RDP
sudo ufw --force enable

echo "虚拟机配置完成!"
echo "SSH: ssh aidev@$(hostname -I | cut -d' ' -f1)"
echo "RDP: $(hostname -I | cut -d' ' -f1):3389"
EOF

chmod +x vm-post-install.sh
```

### 方案3.2：基于Shell in a Box的轻量级方案

#### Shell in a Box部署
```yaml
# docker-compose.shellinabox.yml
version: '3.8'

services:
  shellinabox:
    image: sspreitzer/shellinabox:latest
    container_name: shellinabox
    environment:
      - SIAB_USER=developer
      - SIAB_PASSWORD=secure_password
      - SIAB_SUDO=true
      - SIAB_SSH_HOST=${SSH_HOST}
      - SIAB_SSH_PORT=${SSH_PORT:-22}
    ports:
      - "4200:4200"
    volumes:
      - ./config/shellinabox:/etc/shellinabox
    restart: unless-stopped

  # SSH跳板机
  ssh-gateway:
    image: ubuntu:22.04
    container_name: ssh-gateway
    command: >
      bash -c "
        apt-get update &&
        apt-get install -y openssh-server sudo &&
        useradd -m -s /bin/bash developer &&
        echo 'developer:password' | chpasswd &&
        usermod -aG sudo developer &&
        service ssh start &&
        tail -f /dev/null
      "
    ports:
      - "2222:22"
    volumes:
      - ./data/ssh-home:/home/developer
    restart: unless-stopped
```

#### 多虚拟机管理脚本
```bash
#!/bin/bash
# scripts/vm-manager.sh

# 虚拟机管理脚本

VM_BASE_NAME="ai-dev"
VM_COUNT=3
VM_MEMORY="4096"
VM_DISK="50G"

# 创建多个AI开发虚拟机
create_vms() {
    for i in $(seq 1 $VM_COUNT); do
        VM_NAME="${VM_BASE_NAME}-${i}"
        
        echo "创建虚拟机: $VM_NAME"
        
        virt-install \
            --name $VM_NAME \
            --memory $VM_MEMORY \
            --vcpus 2 \
            --disk size=50 \
            --location http://archive.ubuntu.com/ubuntu/dists/jammy/main/installer-amd64/ \
            --os-variant ubuntu22.04 \
            --network bridge=virbr0 \
            --extra-args="console=tty0 console=ttyS0,115200n8 serial" \
            --serial pty \
            --nographics \
            --wait=-1
    done
}

# 配置虚拟机
configure_vms() {
    for i in $(seq 1 $VM_COUNT); do
        VM_NAME="${VM_BASE_NAME}-${i}"
        VM_IP=$(virsh domifaddr $VM_NAME | grep -oE "([0-9]{1,3}\.){3}[0-9]{1,3}" | head -1)
        
        if [ ! -z "$VM_IP" ]; then
            echo "配置虚拟机 $VM_NAME (IP: $VM_IP)"
            
            # 通过SSH配置虚拟机
            ssh-copy-id -i ~/.ssh/id_rsa.pub ubuntu@$VM_IP
            
            ssh ubuntu@$VM_IP << 'REMOTE_SCRIPT'
                # 安装AI开发工具
                curl -fsSL https://claude.ai/install.sh | bash
                curl https://sdk.cloud.google.com | bash
                
                # 配置用户环境
                mkdir -p ~/.config/claude ~/.config/gcloud
                
                # 创建开发目录
                mkdir -p ~/projects ~/scripts
                
                echo "虚拟机配置完成"
REMOTE_SCRIPT
        fi
    done
}

# 启动所有虚拟机
start_vms() {
    for i in $(seq 1 $VM_COUNT); do
        VM_NAME="${VM_BASE_NAME}-${i}"
        echo "启动虚拟机: $VM_NAME"
        virsh start $VM_NAME
    done
}

# 停止所有虚拟机
stop_vms() {
    for i in $(seq 1 $VM_COUNT); do
        VM_NAME="${VM_BASE_NAME}-${i}"
        echo "停止虚拟机: $VM_NAME"
        virsh shutdown $VM_NAME
    done
}

# 删除所有虚拟机
destroy_vms() {
    for i in $(seq 1 $VM_COUNT); do
        VM_NAME="${VM_BASE_NAME}-${i}"
        echo "删除虚拟机: $VM_NAME"
        virsh destroy $VM_NAME 2>/dev/null
        virsh undefine $VM_NAME --remove-all-storage
    done
}

# 列出虚拟机状态
list_vms() {
    echo "AI开发虚拟机状态:"
    for i in $(seq 1 $VM_COUNT); do
        VM_NAME="${VM_BASE_NAME}-${i}"
        STATUS=$(virsh domstate $VM_NAME 2>/dev/null || echo "不存在")
        IP=$(virsh domifaddr $VM_NAME 2>/dev/null | grep -oE "([0-9]{1,3}\.){3}[0-9]{1,3}" | head -1)
        echo "  $VM_NAME: $STATUS $([ ! -z "$IP" ] && echo "($IP)")"
    done
}

# 主菜单
case "$1" in
    create)
        create_vms
        ;;
    configure)
        configure_vms
        ;;
    start)
        start_vms
        ;;
    stop)
        stop_vms
        ;;
    destroy)
        destroy_vms
        ;;
    list)
        list_vms
        ;;
    *)
        echo "用法: $0 {create|configure|start|stop|destroy|list}"
        exit 1
        ;;
esac
```

## 账号认证与切换机制

### 多账号管理架构

#### 账号配置管理器
```python
#!/usr/bin/env python3
# scripts/account-manager.py

import json
import os
import subprocess
import sys
from pathlib import Path

class AIAccountManager:
    def __init__(self, config_dir="~/.config/ai-tools"):
        self.config_dir = Path(config_dir).expanduser()
        self.config_dir.mkdir(parents=True, exist_ok=True)
        self.accounts_file = self.config_dir / "accounts.json"
        self.current_file = self.config_dir / "current.json"
        self.load_accounts()
    
    def load_accounts(self):
        """加载账号配置"""
        if self.accounts_file.exists():
            with open(self.accounts_file) as f:
                self.accounts = json.load(f)
        else:
            self.accounts = {"claude": {}, "gemini": {}}
            self.save_accounts()
    
    def save_accounts(self):
        """保存账号配置"""
        with open(self.accounts_file, 'w') as f:
            json.dump(self.accounts, f, indent=2)
    
    def add_claude_account(self, name, api_key, org_id=None, base_url=None):
        """添加Claude账号"""
        self.accounts["claude"][name] = {
            "api_key": api_key,
            "org_id": org_id,
            "base_url": base_url or "https://api.anthropic.com"
        }
        self.save_accounts()
        print(f"Claude账号 '{name}' 添加成功")
    
    def add_gemini_account(self, name, project_id, service_account_path, region="us-central1"):
        """添加Gemini账号"""
        self.accounts["gemini"][name] = {
            "project_id": project_id,
            "service_account": service_account_path,
            "region": region
        }
        self.save_accounts()
        print(f"Gemini账号 '{name}' 添加成功")
    
    def switch_account(self, service, account_name):
        """切换账号"""
        if service not in self.accounts:
            print(f"不支持的服务: {service}")
            return False
        
        if account_name not in self.accounts[service]:
            print(f"账号 '{account_name}' 不存在于 {service}")
            return False
        
        account = self.accounts[service][account_name]
        
        if service == "claude":
            self._switch_claude_account(account_name, account)
        elif service == "gemini":
            self._switch_gemini_account(account_name, account)
        
        # 记录当前账号
        current = {"claude": None, "gemini": None}
        if self.current_file.exists():
            with open(self.current_file) as f:
                current = json.load(f)
        
        current[service] = account_name
        with open(self.current_file, 'w') as f:
            json.dump(current, f, indent=2)
        
        print(f"已切换到 {service} 账号: {account_name}")
        return True
    
    def _switch_claude_account(self, name, account):
        """切换Claude账号"""
        # 设置环境变量
        env_script = f"""
export CLAUDE_API_KEY="{account['api_key']}"
export CLAUDE_ORG_ID="{account.get('org_id', '')}"
export CLAUDE_BASE_URL="{account['base_url']}"
"""
        
        # 写入环境变量文件
        env_file = self.config_dir / "claude_env.sh"
        with open(env_file, 'w') as f:
            f.write(env_script)
        
        # 更新Claude配置文件
        claude_config = {
            "api_key": account['api_key'],
            "organization_id": account.get('org_id'),
            "base_url": account['base_url']
        }
        
        claude_config_dir = Path("~/.claude").expanduser()
        claude_config_dir.mkdir(exist_ok=True)
        
        with open(claude_config_dir / "config.json", 'w') as f:
            json.dump(claude_config, f, indent=2)
    
    def _switch_gemini_account(self, name, account):
        """切换Gemini账号"""
        # 设置环境变量
        env_script = f"""
export GOOGLE_APPLICATION_CREDENTIALS="{account['service_account']}"
export GEMINI_PROJECT_ID="{account['project_id']}"
export GEMINI_REGION="{account['region']}"
"""
        
        # 写入环境变量文件
        env_file = self.config_dir / "gemini_env.sh"
        with open(env_file, 'w') as f:
            f.write(env_script)
        
        # 激活gcloud配置
        try:
            subprocess.run([
                "gcloud", "auth", "activate-service-account",
                "--key-file", account['service_account']
            ], check=True)
            
            subprocess.run([
                "gcloud", "config", "set", "project", account['project_id']
            ], check=True)
            
        except subprocess.CalledProcessError as e:
            print(f"gcloud配置失败: {e}")
    
    def list_accounts(self):
        """列出所有账号"""
        current = {}
        if self.current_file.exists():
            with open(self.current_file) as f:
                current = json.load(f)
        
        print("\n=== Claude账号 ===")
        for name in self.accounts["claude"]:
            status = " (当前)" if current.get("claude") == name else ""
            print(f"  {name}{status}")
        
        print("\n=== Gemini账号 ===")
        for name in self.accounts["gemini"]:
            status = " (当前)" if current.get("gemini") == name else ""
            print(f"  {name}{status}")
    
    def remove_account(self, service, account_name):
        """删除账号"""
        if service in self.accounts and account_name in self.accounts[service]:
            del self.accounts[service][account_name]
            self.save_accounts()
            print(f"账号 '{account_name}' 已从 {service} 中删除")
        else:
            print(f"账号 '{account_name}' 在 {service} 中不存在")

def main():
    manager = AIAccountManager()
    
    if len(sys.argv) < 2:
        print("用法:")
        print("  python account-manager.py add-claude <name> <api_key> [org_id] [base_url]")
        print("  python account-manager.py add-gemini <name> <project_id> <service_account_path> [region]")
        print("  python account-manager.py switch <service> <account_name>")
        print("  python account-manager.py list")
        print("  python account-manager.py remove <service> <account_name>")
        sys.exit(1)
    
    command = sys.argv[1]
    
    if command == "add-claude":
        name = sys.argv[2]
        api_key = sys.argv[3]
        org_id = sys.argv[4] if len(sys.argv) > 4 else None
        base_url = sys.argv[5] if len(sys.argv) > 5 else None
        manager.add_claude_account(name, api_key, org_id, base_url)
    
    elif command == "add-gemini":
        name = sys.argv[2]
        project_id = sys.argv[3]
        service_account = sys.argv[4]
        region = sys.argv[5] if len(sys.argv) > 5 else "us-central1"
        manager.add_gemini_account(name, project_id, service_account, region)
    
    elif command == "switch":
        service = sys.argv[2]
        account_name = sys.argv[3]
        manager.switch_account(service, account_name)
    
    elif command == "list":
        manager.list_accounts()
    
    elif command == "remove":
        service = sys.argv[2]
        account_name = sys.argv[3]
        manager.remove_account(service, account_name)
    
    else:
        print(f"未知命令: {command}")

if __name__ == "__main__":
    main()
```

#### Web界面账号管理
```html
<!DOCTYPE html>
<html>
<head>
    <title>AI工具账号管理</title>
    <meta charset="utf-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .container { max-width: 800px; margin: 0 auto; }
        .account-section { margin: 20px 0; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
        .current-account { background-color: #e8f5e8; }
        .account-item { padding: 10px; margin: 5px 0; border: 1px solid #ccc; border-radius: 3px; }
        button { padding: 8px 16px; margin: 5px; cursor: pointer; }
        .switch-btn { background-color: #4CAF50; color: white; border: none; }
        .remove-btn { background-color: #f44336; color: white; border: none; }
        .add-form { background-color: #f9f9f9; padding: 15px; border-radius: 5px; }
        input, select { padding: 5px; margin: 5px; width: 200px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>AI工具账号管理</h1>
        
        <!-- Claude账号管理 -->
        <div class="account-section">
            <h2>Claude账号</h2>
            <div id="claude-accounts"></div>
            
            <div class="add-form">
                <h3>添加Claude账号</h3>
                <input type="text" id="claude-name" placeholder="账号名称">
                <input type="text" id="claude-api-key" placeholder="API Key">
                <input type="text" id="claude-org-id" placeholder="Organization ID (可选)">
                <input type="text" id="claude-base-url" placeholder="Base URL (可选)">
                <button onclick="addClaudeAccount()">添加账号</button>
            </div>
        </div>
        
        <!-- Gemini账号管理 -->
        <div class="account-section">
            <h2>Gemini账号</h2>
            <div id="gemini-accounts"></div>
            
            <div class="add-form">
                <h3>添加Gemini账号</h3>
                <input type="text" id="gemini-name" placeholder="账号名称">
                <input type="text" id="gemini-project-id" placeholder="Project ID">
                <input type="file" id="gemini-service-account" accept=".json">
                <select id="gemini-region">
                    <option value="us-central1">US Central</option>
                    <option value="us-east1">US East</option>
                    <option value="europe-west1">Europe West</option>
                    <option value="asia-east1">Asia East</option>
                </select>
                <button onclick="addGeminiAccount()">添加账号</button>
            </div>
        </div>
        
        <!-- 代理设置 -->
        <div class="account-section">
            <h2>代理设置</h2>
            <div class="add-form">
                <input type="text" id="http-proxy" placeholder="HTTP代理 (http://proxy:port)">
                <input type="text" id="https-proxy" placeholder="HTTPS代理 (https://proxy:port)">
                <input type="text" id="no-proxy" placeholder="不使用代理的域名 (逗号分隔)">
                <button onclick="updateProxy()">更新代理设置</button>
            </div>
        </div>
    </div>

    <script>
        // API基础URL
        const API_BASE = '/api';
        
        // 加载账号列表
        async function loadAccounts() {
            try {
                const response = await fetch(`${API_BASE}/accounts`);
                const data = await response.json();
                
                renderAccounts('claude', data.claude, data.current.claude);
                renderAccounts('gemini', data.gemini, data.current.gemini);
            } catch (error) {
                console.error('加载账号失败:', error);
            }
        }
        
        // 渲染账号列表
        function renderAccounts(service, accounts, currentAccount) {
            const container = document.getElementById(`${service}-accounts`);
            container.innerHTML = '';
            
            Object.keys(accounts).forEach(name => {
                const account = accounts[name];
                const isCurrent = name === currentAccount;
                
                const accountDiv = document.createElement('div');
                accountDiv.className = `account-item ${isCurrent ? 'current-account' : ''}`;
                
                accountDiv.innerHTML = `
                    <strong>${name}</strong> ${isCurrent ? '(当前)' : ''}
                    <div style="margin-top: 5px;">
                        ${service === 'claude' ? 
                            `API Key: ${account.api_key.substring(0, 10)}...` : 
                            `Project: ${account.project_id}`
                        }
                    </div>
                    <button class="switch-btn" onclick="switchAccount('${service}', '${name}')" 
                            ${isCurrent ? 'disabled' : ''}>
                        ${isCurrent ? '当前账号' : '切换'}
                    </button>
                    <button class="remove-btn" onclick="removeAccount('${service}', '${name}')">
                        删除
                    </button>
                `;
                
                container.appendChild(accountDiv);
            });
        }
        
        // 添加Claude账号
        async function addClaudeAccount() {
            const name = document.getElementById('claude-name').value;
            const apiKey = document.getElementById('claude-api-key').value;
            const orgId = document.getElementById('claude-org-id').value;
            const baseUrl = document.getElementById('claude-base-url').value;
            
            if (!name || !apiKey) {
                alert('请填写账号名称和API Key');
                return;
            }
            
            try {
                const response = await fetch(`${API_BASE}/accounts/claude`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, api_key: apiKey, org_id: orgId, base_url: baseUrl })
                });
                
                if (response.ok) {
                    alert('Claude账号添加成功');
                    loadAccounts();
                    // 清空表单
                    document.getElementById('claude-name').value = '';
                    document.getElementById('claude-api-key').value = '';
                    document.getElementById('claude-org-id').value = '';
                    document.getElementById('claude-base-url').value = '';
                } else {
                    alert('添加失败');
                }
            } catch (error) {
                alert('添加失败: ' + error.message);
            }
        }
        
        // 添加Gemini账号
        async function addGeminiAccount() {
            const name = document.getElementById('gemini-name').value;
            const projectId = document.getElementById('gemini-project-id').value;
            const serviceAccountFile = document.getElementById('gemini-service-account').files[0];
            const region = document.getElementById('gemini-region').value;
            
            if (!name || !projectId || !serviceAccountFile) {
                alert('请填写所有必需字段');
                return;
            }
            
            const formData = new FormData();
            formData.append('name', name);
            formData.append('project_id', projectId);
            formData.append('service_account', serviceAccountFile);
            formData.append('region', region);
            
            try {
                const response = await fetch(`${API_BASE}/accounts/gemini`, {
                    method: 'POST',
                    body: formData
                });
                
                if (response.ok) {
                    alert('Gemini账号添加成功');
                    loadAccounts();
                    // 清空表单
                    document.getElementById('gemini-name').value = '';
                    document.getElementById('gemini-project-id').value = '';
                    document.getElementById('gemini-service-account').value = '';
                } else {
                    alert('添加失败');
                }
            } catch (error) {
                alert('添加失败: ' + error.message);
            }
        }
        
        // 切换账号
        async function switchAccount(service, accountName) {
            try {
                const response = await fetch(`${API_BASE}/accounts/switch`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ service, account_name: accountName })
                });
                
                if (response.ok) {
                    alert(`已切换到${service}账号: ${accountName}`);
                    loadAccounts();
                } else {
                    alert('切换失败');
                }
            } catch (error) {
                alert('切换失败: ' + error.message);
            }
        }
        
        // 删除账号
        async function removeAccount(service, accountName) {
            if (!confirm(`确定要删除${service}账号"${accountName}"吗？`)) {
                return;
            }
            
            try {
                const response = await fetch(`${API_BASE}/accounts/${service}/${accountName}`, {
                    method: 'DELETE'
                });
                
                if (response.ok) {
                    alert('账号删除成功');
                    loadAccounts();
                } else {
                    alert('删除失败');
                }
            } catch (error) {
                alert('删除失败: ' + error.message);
            }
        }
        
        // 更新代理设置
        async function updateProxy() {
            const httpProxy = document.getElementById('http-proxy').value;
            const httpsProxy = document.getElementById('https-proxy').value;
            const noProxy = document.getElementById('no-proxy').value;
            
            try {
                const response = await fetch(`${API_BASE}/proxy`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        http_proxy: httpProxy, 
                        https_proxy: httpsProxy, 
                        no_proxy: noProxy 
                    })
                });
                
                if (response.ok) {
                    alert('代理设置更新成功');
                } else {
                    alert('更新失败');
                }
            } catch (error) {
                alert('更新失败: ' + error.message);
            }
        }
        
        // 页面加载时获取账号列表
        window.onload = loadAccounts;
    </script>
</body>
</html>
```

## 代理配置管理

### 动态代理配置系统

#### 代理配置管理器
```python
#!/usr/bin/env python3
# scripts/proxy-manager.py

import json
import os
import subprocess
import yaml
from pathlib import Path

class ProxyManager:
    def __init__(self, config_dir="~/.config/ai-tools"):
        self.config_dir = Path(config_dir).expanduser()
        self.config_dir.mkdir(parents=True, exist_ok=True)
        self.proxy_config_file = self.config_dir / "proxy.json"
        self.load_proxy_config()
    
    def load_proxy_config(self):
        """加载代理配置"""
        if self.proxy_config_file.exists():
            with open(self.proxy_config_file) as f:
                self.proxy_config = json.load(f)
        else:
            self.proxy_config = {
                "profiles": {},
                "current_profile": None
            }
            self.save_proxy_config()
    
    def save_proxy_config(self):
        """保存代理配置"""
        with open(self.proxy_config_file, 'w') as f:
            json.dump(self.proxy_config, f, indent=2)
    
    def add_proxy_profile(self, name, http_proxy=None, https_proxy=None, 
                         no_proxy=None, proxy_type="http"):
        """添加代理配置文件"""
        self.proxy_config["profiles"][name] = {
            "http_proxy": http_proxy,
            "https_proxy": https_proxy,
            "no_proxy": no_proxy,
            "proxy_type": proxy_type
        }
        self.save_proxy_config()
        print(f"代理配置 '{name}' 添加成功")
    
    def switch_proxy_profile(self, name):
        """切换代理配置"""
        if name not in self.proxy_config["profiles"]:
            print(f"代理配置 '{name}' 不存在")
            return False
        
        profile = self.proxy_config["profiles"][name]
        
        # 设置环境变量
        self._set_proxy_env(profile)
        
        # 更新Docker代理设置
        self._update_docker_proxy(profile)
        
        # 更新SSH代理设置
        self._update_ssh_proxy(profile)
        
        # 更新应用代理设置
        self._update_app_proxy(profile)
        
        self.proxy_config["current_profile"] = name
        self.save_proxy_config()
        
        print(f"已切换到代理配置: {name}")
        return True
    
    def _set_proxy_env(self, profile):
        """设置代理环境变量"""
        env_script = "#!/bin/bash\n"
        
        if profile["http_proxy"]:
            env_script += f'export HTTP_PROXY="{profile["http_proxy"]}"\n'
            env_script += f'export http_proxy="{profile["http_proxy"]}"\n'
        
        if profile["https_proxy"]:
            env_script += f'export HTTPS_PROXY="{profile["https_proxy"]}"\n'
            env_script += f'export https_proxy="{profile["https_proxy"]}"\n'
        
        if profile["no_proxy"]:
            env_script += f'export NO_PROXY="{profile["no_proxy"]}"\n'
            env_script += f'export no_proxy="{profile["no_proxy"]}"\n'
        
        # 写入环境变量文件
        env_file = self.config_dir / "proxy_env.sh"
        with open(env_file, 'w') as f:
            f.write(env_script)
        
        # 使环境变量生效
        os.system(f"source {env_file}")
    
    def _update_docker_proxy(self, profile):
        """更新Docker代理设置"""
        docker_config_dir = Path("~/.docker").expanduser()
        docker_config_dir.mkdir(exist_ok=True)
        
        config = {
            "proxies": {
                "default": {}
            }
        }
        
        if profile["http_proxy"]:
            config["proxies"]["default"]["httpProxy"] = profile["http_proxy"]
        
        if profile["https_proxy"]:
            config["proxies"]["default"]["httpsProxy"] = profile["https_proxy"]
        
        if profile["no_proxy"]:
            config["proxies"]["default"]["noProxy"] = profile["no_proxy"]
        
        with open(docker_config_dir / "config.json", 'w') as f:
            json.dump(config, f, indent=2)
        
        # 重启Docker服务 (需要管理员权限)
        try:
            subprocess.run(["sudo", "systemctl", "restart", "docker"], check=True)
        except subprocess.CalledProcessError:
            print("警告: 无法重启Docker服务，请手动重启")
    
    def _update_ssh_proxy(self, profile):
        """更新SSH代理设置"""
        ssh_config_dir = Path("~/.ssh").expanduser()
        ssh_config_dir.mkdir(mode=0o700, exist_ok=True)
        
        ssh_config_file = ssh_config_dir / "config"
        
        # 读取现有SSH配置
        ssh_config = ""
        if ssh_config_file.exists():
            with open(ssh_config_file) as f:
                ssh_config = f.read()
        
        # 移除旧的代理配置
        lines = ssh_config.split('\n')
        filtered_lines = [line for line in lines if not line.strip().startswith('ProxyCommand')]
        
        # 添加新的代理配置
        if profile["http_proxy"]:
            proxy_host, proxy_port = profile["http_proxy"].replace("http://", "").split(":")
            proxy_command = f"ProxyCommand nc -X connect -x {proxy_host}:{proxy_port} %h %p"
            
            # 在Host *配置中添加代理
            host_star_found = False
            for i, line in enumerate(filtered_lines):
                if line.strip() == "Host *":
                    filtered_lines.insert(i + 1, f"    {proxy_command}")
                    host_star_found = True
                    break
            
            if not host_star_found:
                filtered_lines.extend(["", "Host *", f"    {proxy_command}"])
        
        # 写回SSH配置
        with open(ssh_config_file, 'w') as f:
            f.write('\n'.join(filtered_lines))
        
        ssh_config_file.chmod(0o600)
    
    def _update_app_proxy(self, profile):
        """更新应用代理设置"""
        # 更新Claude配置
        claude_config_dir = Path("~/.claude").expanduser()
        claude_config_dir.mkdir(exist_ok=True)
        
        claude_config = {}
        claude_config_file = claude_config_dir / "config.json"
        
        if claude_config_file.exists():
            with open(claude_config_file) as f:
                claude_config = json.load(f)
        
        if profile["https_proxy"]:
            claude_config["proxy"] = profile["https_proxy"]
        else:
            claude_config.pop("proxy", None)
        
        with open(claude_config_file, 'w') as f:
            json.dump(claude_config, f, indent=2)
        
        # 更新gcloud代理设置
        gcloud_config_dir = Path("~/.config/gcloud").expanduser()
        gcloud_config_dir.mkdir(parents=True, exist_ok=True)
        
        properties_file = gcloud_config_dir / "configurations" / "config_default"
        properties_file.parent.mkdir(exist_ok=True)
        
        properties = "[core]\n"
        if profile["http_proxy"]:
            properties += f"custom_ca_certs_file = {profile['http_proxy']}\n"
        
        with open(properties_file, 'w') as f:
            f.write(properties)
    
    def disable_proxy(self):
        """禁用代理"""
        # 清除环境变量
        env_script = """#!/bin/bash
unset HTTP_PROXY
unset http_proxy
unset HTTPS_PROXY
unset https_proxy
unset NO_PROXY
unset no_proxy
"""
        
        env_file = self.config_dir / "proxy_env.sh"
        with open(env_file, 'w') as f:
            f.write(env_script)
        
        # 清除Docker代理
        docker_config_dir = Path("~/.docker").expanduser()
        docker_config_file = docker_config_dir / "config.json"
        
        if docker_config_file.exists():
            with open(docker_config_file) as f:
                config = json.load(f)
            
            config.pop("proxies", None)
            
            with open(docker_config_file, 'w') as f:
                json.dump(config, f, indent=2)
        
        self.proxy_config["current_profile"] = None
        self.save_proxy_config()
        
        print("代理已禁用")
    
    def list_proxy_profiles(self):
        """列出所有代理配置"""
        current = self.proxy_config.get("current_profile")
        
        print("代理配置列表:")
        for name, profile in self.proxy_config["profiles"].items():
            status = " (当前)" if name == current else ""
            print(f"  {name}{status}")
            print(f"    HTTP: {profile.get('http_proxy', '未设置')}")
            print(f"    HTTPS: {profile.get('https_proxy', '未设置')}")
            print(f"    No Proxy: {profile.get('no_proxy', '未设置')}")
            print()
    
    def test_proxy(self, profile_name=None):
        """测试代理连接"""
        if profile_name:
            if profile_name not in self.proxy_config["profiles"]:
                print(f"代理配置 '{profile_name}' 不存在")
                return
            profile = self.proxy_config["profiles"][profile_name]
        else:
            current = self.proxy_config.get("current_profile")
            if not current:
                print("没有活动的代理配置")
                return
            profile = self.proxy_config["profiles"][current]
        
        # 测试HTTP连接
        test_urls = [
            "https://api.anthropic.com",
            "https://generativelanguage.googleapis.com",
            "https://www.google.com",
            "https://github.com"
        ]
        
        for url in test_urls:
            try:
                if profile["https_proxy"]:
                    proxy_env = {
                        "https_proxy": profile["https_proxy"],
                        "http_proxy": profile.get("http_proxy", "")
                    }
                    result = subprocess.run([
                        "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
                        "--max-time", "10", url
                    ], env={**os.environ, **proxy_env}, capture_output=True, text=True)
                else:
                    result = subprocess.run([
                        "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
                        "--max-time", "10", url
                    ], capture_output=True, text=True)
                
                status_code = result.stdout.strip()
                if status_code in ["200", "301", "302"]:
                    print(f"✅ {url}: 连接成功 ({status_code})")
                else:
                    print(f"❌ {url}: 连接失败 ({status_code})")
                    
            except Exception as e:
                print(f"❌ {url}: 测试失败 ({e})")

def main():
    import sys
    
    manager = ProxyManager()
    
    if len(sys.argv) < 2:
        print("用法:")
        print("  python proxy-manager.py add <name> [http_proxy] [https_proxy] [no_proxy]")
        print("  python proxy-manager.py switch <name>")
        print("  python proxy-manager.py disable")
        print("  python proxy-manager.py list")
        print("  python proxy-manager.py test [profile_name]")
        sys.exit(1)
    
    command = sys.argv[1]
    
    if command == "add":
        name = sys.argv[2]
        http_proxy = sys.argv[3] if len(sys.argv) > 3 else None
        https_proxy = sys.argv[4] if len(sys.argv) > 4 else None
        no_proxy = sys.argv[5] if len(sys.argv) > 5 else None
        manager.add_proxy_profile(name, http_proxy, https_proxy, no_proxy)
    
    elif command == "switch":
        name = sys.argv[2]
        manager.switch_proxy_profile(name)
    
    elif command == "disable":
        manager.disable_proxy()
    
    elif command == "list":
        manager.list_proxy_profiles()
    
    elif command == "test":
        profile_name = sys.argv[2] if len(sys.argv) > 2 else None
        manager.test_proxy(profile_name)
    
    else:
        print(f"未知命令: {command}")

if __name__ == "__main__":
    main()
```

#### Docker Compose代理集成
```yaml
# docker-compose.ai-tools-proxy.yml
version: '3.8'

services:
  ai-tools-proxy:
    build:
      context: .
      dockerfile: Dockerfile.ai-tools
      args:
        HTTP_PROXY: ${HTTP_PROXY}
        HTTPS_PROXY: ${HTTPS_PROXY}
        NO_PROXY: ${NO_PROXY}
    container_name: ai-tools-proxy
    environment:
      # 代理环境变量
      - HTTP_PROXY=${HTTP_PROXY}
      - HTTPS_PROXY=${HTTPS_PROXY}
      - NO_PROXY=${NO_PROXY}
      - http_proxy=${HTTP_PROXY}
      - https_proxy=${HTTPS_PROXY}
      - no_proxy=${NO_PROXY}
      
      # AI工具配置
      - CLAUDE_API_KEY=${CLAUDE_API_KEY}
      - CLAUDE_BASE_URL=${CLAUDE_BASE_URL:-https://api.anthropic.com}
      - GOOGLE_APPLICATION_CREDENTIALS=/app/config/gcloud/service-account.json
      - GEMINI_PROJECT_ID=${GEMINI_PROJECT_ID}
    
    volumes:
      - ./data/projects:/app/projects
      - ./data/config:/app/config
      - ./scripts:/app/scripts
    
    ports:
      - "6080:6080"  # noVNC
      - "5901:5901"  # VNC
    
    networks:
      - ai-tools-net
    
    restart: unless-stopped

  # 代理服务器 (可选)
  squid-proxy:
    image: ubuntu/squid:latest
    container_name: squid-proxy
    ports:
      - "3128:3128"
    volumes:
      - ./config/squid:/etc/squid
    networks:
      - ai-tools-net
    restart: unless-stopped

  # Privoxy代理 (SOCKS转HTTP)
  privoxy:
    image: vimagick/privoxy
    container_name: privoxy
    ports:
      - "8118:8118"
    volumes:
      - ./config/privoxy:/etc/privoxy
    networks:
      - ai-tools-net
    restart: unless-stopped

networks:
  ai-tools-net:
    driver: bridge
```

## 实际部署案例

### 案例一：个人开发环境

#### 需求场景
- 一个开发者需要同时使用Claude Code和Gemini Code
- 需要在不同项目间切换不同的API账号
- 网络环境需要使用代理访问AI服务
- 希望通过Web界面管理所有配置

#### 部署方案
```bash
#!/bin/bash
# 个人开发环境部署脚本

# 1. 创建项目目录
mkdir -p ~/ai-dev-env/{config,data,scripts,logs}
cd ~/ai-dev-env

# 2. 下载配置文件
git clone https://github.com/your-repo/ai-tools-config.git config

# 3. 配置环境变量
cat > .env << EOF
# Claude配置
CLAUDE_API_KEY_PERSONAL=sk-ant-api03-...
CLAUDE_API_KEY_WORK=sk-ant-api03-...

# Gemini配置
GEMINI_PROJECT_ID_PERSONAL=my-personal-project
GEMINI_PROJECT_ID_WORK=company-project

# 代理配置
HTTP_PROXY=http://proxy.company.com:8080
HTTPS_PROXY=http://proxy.company.com:8080
NO_PROXY=localhost,127.0.0.1,*.local
EOF

# 4. 启动服务
docker-compose -f config/docker-compose.personal.yml up -d

# 5. 配置账号
python3 scripts/account-manager.py add-claude personal $CLAUDE_API_KEY_PERSONAL
python3 scripts/account-manager.py add-claude work $CLAUDE_API_KEY_WORK

# 6. 配置代理
python3 scripts/proxy-manager.py add company-proxy $HTTP_PROXY $HTTPS_PROXY $NO_PROXY

echo "部署完成！访问 http://localhost:6080 开始使用"
```

### 案例二：团队共享环境

#### 需求场景
- 10人开发团队需要共享AI工具
- 每个成员有独立的虚拟机环境
- 统一的代理和认证配置
- 管理员可以集中管理所有环境

#### 部署架构
```yaml
# docker-compose.team-env.yml
version: '3.8'

services:
  # 认证服务
  auth-service:
    image: keycloak/keycloak:latest
    container_name: team-auth
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin_password
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: keycloak_password
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    networks:
      - team-net

  # 管理面板
  admin-panel:
    build: ./admin-panel
    container_name: team-admin
    environment:
      DATABASE_URL: postgresql://admin:admin_password@postgres:5432/admin_db
      KEYCLOAK_URL: http://auth-service:8080
    ports:
      - "3000:3000"
    depends_on:
      - postgres
      - auth-service
    networks:
      - team-net

  # 虚拟机管理器
  vm-manager:
    build: ./vm-manager
    container_name: vm-manager
    privileged: true
    volumes:
      - /var/run/libvirt:/var/run/libvirt
      - ./vm-configs:/etc/libvirt/qemu
    ports:
      - "8081:8080"
    networks:
      - team-net

  # 代理服务器
  proxy-server:
    image: squid:latest
    container_name: team-proxy
    volumes:
      - ./config/squid:/etc/squid
    ports:
      - "3128:3128"
    networks:
      - team-net

  # 数据库
  postgres:
    image: postgres:13
    container_name: team-postgres
    environment:
      POSTGRES_DB: postgres
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-dbs.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - team-net

volumes:
  postgres_data:

networks:
  team-net:
    driver: bridge
```

#### 团队管理脚本
```bash
#!/bin/bash
# scripts/team-manager.sh

# 团队环境管理脚本

TEAM_SIZE=10
VM_PREFIX="team-ai-dev"

# 为每个团队成员创建虚拟机
create_team_vms() {
    for i in $(seq 1 $TEAM_SIZE); do
        username="dev$(printf "%02d" $i)"
        vm_name="${VM_PREFIX}-${username}"
        
        echo "创建虚拟机: $vm_name"
        
        # 创建虚拟机
        virt-install \
            --name $vm_name \
            --memory 4096 \
            --vcpus 2 \
            --disk size=50 \
            --location http://archive.ubuntu.com/ubuntu/dists/jammy/main/installer-amd64/ \
            --os-variant ubuntu22.04 \
            --network bridge=virbr0 \
            --extra-args="console=tty0 console=ttyS0,115200n8 serial" \
            --serial pty \
            --nographics \
            --wait=-1
        
        # 等待虚拟机启动
        sleep 60
        
        # 获取虚拟机IP
        vm_ip=$(virsh domifaddr $vm_name | grep -oE "([0-9]{1,3}\.){3}[0-9]{1,3}" | head -1)
        
        if [ ! -z "$vm_ip" ]; then
            echo "配置虚拟机 $vm_name (IP: $vm_ip)"
            
            # 配置虚拟机
            ssh-copy-id -i ~/.ssh/id_rsa.pub ubuntu@$vm_ip
            
            ssh ubuntu@$vm_ip << REMOTE_SCRIPT
                # 创建用户
                sudo useradd -m -s /bin/bash $username
                sudo usermod -aG sudo $username
                echo '$username:dev_password' | sudo chpasswd
                
                # 安装AI工具
                curl -fsSL https://claude.ai/install.sh | bash
                curl https://sdk.cloud.google.com | bash
                
                # 配置开发环境
                sudo -u $username mkdir -p /home/$username/{projects,scripts,.config}
                
                # 设置代理
                sudo -u $username tee /home/$username/.bashrc << 'BASHRC'
export HTTP_PROXY=http://team-proxy:3128
export HTTPS_PROXY=http://team-proxy:3128
export NO_PROXY=localhost,127.0.0.1,*.local
BASHRC
                
                echo "用户 $username 配置完成"
REMOTE_SCRIPT
            
            # 注册到管理系统
            curl -X POST http://localhost:3000/api/vms \
                -H "Content-Type: application/json" \
                -d "{
                    \"name\": \"$vm_name\",
                    \"ip\": \"$vm_ip\",
                    \"username\": \"$username\",
                    \"status\": \"active\"
                }"
        fi
    done
}

# 配置Guacamole连接
configure_guacamole() {
    for i in $(seq 1 $TEAM_SIZE); do
        username="dev$(printf "%02d" $i)"
        vm_name="${VM_PREFIX}-${username}"
        vm_ip=$(virsh domifaddr $vm_name | grep -oE "([0-9]{1,3}\.){3}[0-9]{1,3}" | head -1)
        
        if [ ! -z "$vm_ip" ]; then
            # 在Guacamole中创建连接配置
            curl -X POST http://localhost:8080/guacamole/api/session/data/postgresql/connections \
                -H "Content-Type: application/json" \
                -H "Guacamole-Token: $GUACAMOLE_TOKEN" \
                -d "{
                    \"name\": \"$vm_name\",
                    \"protocol\": \"ssh\",
                    \"parameters\": {
                        \"hostname\": \"$vm_ip\",
                        \"port\": \"22\",
                        \"username\": \"$username\",
                        \"password\": \"dev_password\"
                    }
                }"
        fi
    done
}

# 主函数
case "$1" in
    create)
        create_team_vms
        ;;
    configure-guacamole)
        configure_guacamole
        ;;
    *)
        echo "用法: $0 {create|configure-guacamole}"
        exit 1
        ;;
esac
```

### 案例三：云服务提供商环境

#### 需求场景
- 为客户提供AI工具即服务
- 支持多租户隔离
- 自动扩缩容
- 计费和监控

#### Kubernetes部署配置
```yaml
# k8s/ai-tools-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-tools-service
  namespace: ai-tools
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ai-tools
  template:
    metadata:
      labels:
        app: ai-tools
    spec:
      containers:
      - name: ai-tools
        image: ai-tools:latest
        ports:
        - containerPort: 6080
        env:
        - name: TENANT_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.labels['tenant']
        - name: HTTP_PROXY
          valueFrom:
            configMapKeyRef:
              name: proxy-config
              key: http_proxy
        - name: HTTPS_PROXY
          valueFrom:
            configMapKeyRef:
              name: proxy-config
              key: https_proxy
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        volumeMounts:
        - name: user-data
          mountPath: /app/data
        - name: config-volume
          mountPath: /app/config
      volumes:
      - name: user-data
        persistentVolumeClaim:
          claimName: user-data-pvc
      - name: config-volume
        configMap:
          name: ai-tools-config

---
apiVersion: v1
kind: Service
metadata:
  name: ai-tools-service
  namespace: ai-tools
spec:
  selector:
    app: ai-tools
  ports:
  - port: 80
    targetPort: 6080
  type: LoadBalancer

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ai-tools-ingress
  namespace: ai-tools
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - ai-tools.example.com
    secretName: ai-tools-tls
  rules:
  - host: ai-tools.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ai-tools-service
            port:
              number: 80
```

## 性能与安全评估

### 性能对比分析

#### 资源消耗对比表
| 方案 | CPU使用率 | 内存消耗 | 磁盘I/O | 网络延迟 | 启动时间 |
|------|-----------|----------|---------|----------|----------|
| Docker+VNC | 15-25% | 1-2GB | 低 | 50-100ms | 30-60s |
| Docker+X11 | 10-20% | 0.5-1GB | 低 | 20-50ms | 10-30s |
| VM+Web SSH | 25-40% | 2-4GB | 中等 | 10-30ms | 2-5min |
| K8s部署 | 20-30% | 1.5-3GB | 中等 | 30-80ms | 1-2min |

#### 并发性能测试
```bash
#!/bin/bash
# scripts/performance-test.sh

# 性能测试脚本

echo "开始性能测试..."

# 测试Docker方案
echo "测试Docker+VNC方案..."
time docker-compose -f docker-compose.vnc.yml up -d
sleep 60
docker stats --no-stream ai-tools-vnc > performance/docker-vnc-stats.txt

# 测试虚拟机方案
echo "测试虚拟机方案..."
time ./scripts/vm-manager.sh start
sleep 180
virsh list --all > performance/vm-stats.txt

# 并发连接测试
echo "测试并发连接..."
for i in {1..10}; do
    curl -s -o /dev/null -w "%{time_total}\n" http://localhost:6080 &
done
wait > performance/concurrent-response-times.txt

# 资源监控
echo "监控系统资源..."
top -bn1 | head -20 > performance/system-resources.txt
df -h > performance/disk-usage.txt
free -h > performance/memory-usage.txt

echo "性能测试完成，结果保存在performance/目录"
```

### 安全评估

#### 安全威胁分析
```markdown
| 威胁类型 | 风险级别 | 影响范围 | 缓解措施 |
|----------|----------|----------|----------|
| API密钥泄露 | 高 | 整个账号 | 密钥轮换、环境隔离 |
| 容器逃逸 | 中 | 宿主机 | 权限限制、SELinux |
| 网络嗅探 | 中 | 传输数据 | HTTPS/TLS加密 |
| 身份伪造 | 高 | 用户账号 | 多因子认证 |
| DDoS攻击 | 中 | 服务可用性 | 流量限制、负载均衡 |
```

#### 安全加固脚本
```bash
#!/bin/bash
# scripts/security-hardening.sh

echo "开始安全加固..."

# 1. 容器安全
echo "配置容器安全..."

# 创建非特权用户
docker exec ai-tools useradd -m -s /bin/bash appuser
docker exec ai-tools usermod -aG sudo appuser

# 限制容器权限
cat > docker-compose.secure.yml << 'EOF'
version: '3.8'
services:
  ai-tools:
    image: ai-tools:latest
    user: "1000:1000"
    read_only: true
    tmpfs:
      - /tmp
      - /var/tmp
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
    security_opt:
      - no-new-privileges:true
      - apparmor:docker-default
EOF

# 2. 网络安全
echo "配置网络安全..."

# 配置防火墙
ufw --force enable
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 6080/tcp  # noVNC

# 3. 密钥管理
echo "配置密钥管理..."

# 创建密钥存储目录
mkdir -p /etc/ai-tools/secrets
chmod 700 /etc/ai-tools/secrets

# 使用Docker secrets
echo "$CLAUDE_API_KEY" | docker secret create claude_api_key -
echo "$GEMINI_SERVICE_ACCOUNT" | docker secret create gemini_service_account -

# 4. 日志审计
echo "配置日志审计..."

# 配置rsyslog
cat > /etc/rsyslog.d/ai-tools.conf << 'EOF'
# AI Tools logs
local0.*    /var/log/ai-tools/access.log
local1.*    /var/log/ai-tools/error.log
local2.*    /var/log/ai-tools/auth.log
EOF

# 重启rsyslog
systemctl restart rsyslog

# 5. 访问控制
echo "配置访问控制..."

# 配置nginx认证
htpasswd -cb /etc/nginx/.htpasswd admin secure_password

cat > /etc/nginx/sites-available/ai-tools << 'EOF'
server {
    listen 80;
    server_name ai-tools.local;
    
    auth_basic "AI Tools Access";
    auth_basic_user_file /etc/nginx/.htpasswd;
    
    location / {
        proxy_pass http://localhost:6080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

ln -s /etc/nginx/sites-available/ai-tools /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx

echo "安全加固完成"
```

## 最佳实践建议

### 1. 架构选择建议

#### 选择决策树
```
开始
├── 用户数量 < 5？
│   ├── 是 → Docker+VNC方案
│   └── 否 → 继续
├── 需要完整桌面环境？
│   ├── 是 → 虚拟机+Web SSH方案
│   └── 否 → 继续
├── 需要弹性伸缩？
│   ├── 是 → Kubernetes方案
│   └── 否 → Docker+负载均衡方案
```

#### 环境配置矩阵
| 使用场景 | 推荐方案 | 配置规格 | 预期成本 |
|----------|----------|----------|----------|
| 个人开发 | Docker+VNC | 2核4GB | 低 |
| 小团队 | VM+SSH | 4核8GB | 中等 |
| 企业级 | K8s+多租户 | 8核16GB+ | 高 |
| 云服务商 | 容器化+自动扩缩 | 动态分配 | 按需 |

### 2. 运维最佳实践

#### 自动化部署流程
```bash
#!/bin/bash
# scripts/automated-deployment.sh

# 自动化部署流程

set -e

# 1. 环境检查
check_requirements() {
    echo "检查系统要求..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        echo "错误: Docker未安装"
        exit 1
    fi
    
    # 检查内存
    total_mem=$(free -m | awk 'NR==2{printf "%.1f", $2/1024}')
    if (( $(echo "$total_mem < 4.0" | bc -l) )); then
        echo "警告: 内存不足4GB，可能影响性能"
    fi
    
    # 检查磁盘空间
    available_space=$(df / | awk 'NR==2{printf "%.1f", $4/1024/1024}')
    if (( $(echo "$available_space < 20.0" | bc -l) )); then
        echo "错误: 可用磁盘空间不足20GB"
        exit 1
    fi
    
    echo "✅ 系统要求检查通过"
}

# 2. 配置验证
validate_config() {
    echo "验证配置文件..."
    
    if [ ! -f ".env" ]; then
        echo "错误: 缺少.env配置文件"
        exit 1
    fi
    
    source .env
    
    if [ -z "$CLAUDE_API_KEY" ]; then
        echo "警告: 未设置Claude API Key"
    fi
    
    if [ -z "$GEMINI_PROJECT_ID" ]; then
        echo "警告: 未设置Gemini Project ID"
    fi
    
    echo "✅ 配置验证完成"
}

# 3. 服务部署
deploy_services() {
    echo "部署服务..."
    
    # 创建网络
    docker network create ai-tools-net 2>/dev/null || true
    
    # 启动基础服务
    docker-compose -f docker-compose.base.yml up -d
    
    # 等待服务就绪
    echo "等待服务启动..."
    sleep 30
    
    # 启动AI工具服务
    docker-compose -f docker-compose.ai-tools.yml up -d
    
    # 健康检查
    max_attempts=30
    attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:6080 > /dev/null; then
            echo "✅ 服务部署成功"
            break
        fi
        
        attempt=$((attempt + 1))
        echo "等待服务就绪... ($attempt/$max_attempts)"
        sleep 10
    done
    
    if [ $attempt -eq $max_attempts ]; then
        echo "❌ 服务部署失败"
        exit 1
    fi
}

# 4. 配置初始化
initialize_config() {
    echo "初始化配置..."
    
    # 创建默认账号
    if [ ! -z "$CLAUDE_API_KEY" ]; then
        python3 scripts/account-manager.py add-claude default "$CLAUDE_API_KEY"
    fi
    
    if [ ! -z "$GEMINI_PROJECT_ID" ] && [ ! -z "$GEMINI_SERVICE_ACCOUNT" ]; then
        python3 scripts/account-manager.py add-gemini default "$GEMINI_PROJECT_ID" "$GEMINI_SERVICE_ACCOUNT"
    fi
    
    # 配置代理
    if [ ! -z "$HTTP_PROXY" ]; then
        python3 scripts/proxy-manager.py add default "$HTTP_PROXY" "$HTTPS_PROXY" "$NO_PROXY"
        python3 scripts/proxy-manager.py switch default
    fi
    
    echo "✅ 配置初始化完成"
}

# 5. 生成报告
generate_report() {
    echo "生成部署报告..."
    
    cat > deployment-report.txt << EOF
AI工具部署报告
================

部署时间: $(date)
系统信息: $(uname -a)
Docker版本: $(docker --version)

服务状态:
$(docker-compose ps)

访问地址:
- Web界面: http://localhost:6080
- 管理面板: http://localhost:3000

配置文件:
- 环境变量: .env
- 账号配置: ~/.config/ai-tools/accounts.json
- 代理配置: ~/.config/ai-tools/proxy.json

日志位置:
- 应用日志: ./logs/
- Docker日志: docker-compose logs

EOF
    
    echo "✅ 部署报告已生成: deployment-report.txt"
}

# 主流程
main() {
    echo "🚀 开始自动化部署..."
    
    check_requirements
    validate_config
    deploy_services
    initialize_config
    generate_report
    
    echo "🎉 部署完成！"
    echo "访问 http://localhost:6080 开始使用AI工具"
}

main "$@"
```

### 3. 监控和告警

#### 综合监控方案
```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
  # Prometheus监控
  prometheus:
    image: prom/prometheus:latest
    container_name: ai-tools-prometheus
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
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    networks:
      - ai-tools-net

  # Grafana可视化
  grafana:
    image: grafana/grafana:latest
    container_name: ai-tools-grafana
    ports:
      - "3001:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
      GF_USERS_ALLOW_SIGN_UP: false
    volumes:
      - grafana_data:/var/lib/grafana
      - ./config/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./config/grafana/datasources:/etc/grafana/provisioning/datasources
    networks:
      - ai-tools-net

  # AlertManager告警
  alertmanager:
    image: prom/alertmanager:latest
    container_name: ai-tools-alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./config/alertmanager:/etc/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
      - '--web.external-url=http://localhost:9093'
    networks:
      - ai-tools-net

  # Node Exporter系统监控
  node-exporter:
    image: prom/node-exporter:latest
    container_name: ai-tools-node-exporter
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    networks:
      - ai-tools-net

volumes:
  prometheus_data:
  grafana_data:

networks:
  ai-tools-net:
    external: true
```

## 总结与展望

### 技术方案总结

本研究深入分析了AI代码工具在容器化和虚拟化环境中的部署策略，主要得出以下结论：

1. **Docker VNC方案**适合个人开发者，资源消耗适中，部署简单
2. **虚拟机Web SSH方案**适合团队协作，提供完整桌面体验，但资源消耗较大
3. **Kubernetes方案**适合企业级部署，支持自动扩缩容和多租户隔离
4. **混合方案**可以结合不同技术的优势，满足复杂场景需求

### 关键技术要点

1. **认证流程**：通过VNC/RDP实现浏览器OAuth认证，或使用API密钥方式
2. **代理配置**：统一的代理管理系统，支持动态切换和环境隔离
3. **账号管理**：多账号切换机制，支持不同项目和组织的隔离
4. **安全防护**：容器安全、网络隔离、密钥管理等多层防护

### 未来发展方向

1. **AI原生架构**：专为AI工具设计的容器运行时和调度器
2. **边缘计算支持**：支持在边缘设备上部署轻量级AI工具
3. **智能调度**：基于工作负载自动选择最优的部署方案
4. **联邦学习集成**：支持多环境间的模型和知识共享

通过本研究提供的方案和工具，开发者可以根据自身需求选择合适的部署策略，实现AI代码工具的高效、安全、可扩展的使用。