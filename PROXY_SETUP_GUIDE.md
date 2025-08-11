# Claude Code 代理配置指南

本文档介绍如何在内网环境中为 Claude Code 配置网络代理，以便正常访问外部API和资源。

## 概述

在企业内网或受限网络环境中，Claude Code 可能无法直接访问外部服务。通过配置HTTP代理，可以解决网络访问问题，确保所有功能正常运行。

## 代理配置方法

### 方法一：环境变量配置（推荐）

#### 临时设置（当前会话有效）
```bash
# 设置HTTP和HTTPS代理
export HTTP_PROXY="http://proxy-server:8080"
export HTTPS_PROXY="http://proxy-server:8080"

# 设置不走代理的地址（可选）
export NO_PROXY="localhost,127.0.0.1,*.local,*.internal"

# 验证设置
echo "HTTP代理: $HTTP_PROXY"
echo "HTTPS代理: $HTTPS_PROXY"
```

#### 永久设置
根据你使用的Shell类型，将代理配置添加到相应的配置文件：

**Bash 用户：**
```bash
# 添加到 ~/.bashrc
echo 'export HTTP_PROXY="http://proxy-server:8080"' >> ~/.bashrc
echo 'export HTTPS_PROXY="http://proxy-server:8080"' >> ~/.bashrc
echo 'export NO_PROXY="localhost,127.0.0.1,*.local"' >> ~/.bashrc

# 重新加载配置
source ~/.bashrc
```

**Zsh 用户：**
```bash
# 添加到 ~/.zshrc
echo 'export HTTP_PROXY="http://proxy-server:8080"' >> ~/.zshrc
echo 'export HTTPS_PROXY="http://proxy-server:8080"' >> ~/.zshrc
echo 'export NO_PROXY="localhost,127.0.0.1,*.local"' >> ~/.zshrc

# 重新加载配置
source ~/.zshrc
```

**Fish Shell 用户：**
```bash
# 添加到 ~/.config/fish/config.fish
echo 'set -x HTTP_PROXY "http://proxy-server:8080"' >> ~/.config/fish/config.fish
echo 'set -x HTTPS_PROXY "http://proxy-server:8080"' >> ~/.config/fish/config.fish
echo 'set -x NO_PROXY "localhost,127.0.0.1,*.local"' >> ~/.config/fish/config.fish
```

### 方法二：系统级配置

#### Linux 系统级配置
```bash
# 编辑 /etc/environment （需要管理员权限）
sudo vim /etc/environment

# 添加以下内容：
HTTP_PROXY="http://proxy-server:8080"
HTTPS_PROXY="http://proxy-server:8080"
NO_PROXY="localhost,127.0.0.1,*.local"
```

#### Windows 系统配置
```cmd
# 命令提示符中设置
set HTTP_PROXY=http://proxy-server:8080
set HTTPS_PROXY=http://proxy-server:8080

# 或通过系统环境变量设置
# 控制面板 -> 系统 -> 高级系统设置 -> 环境变量
```

### 方法三：带认证的代理配置

如果代理服务器需要用户名和密码认证：

```bash
# 设置带认证的代理
export HTTP_PROXY="http://username:password@proxy-server:8080"
export HTTPS_PROXY="http://username:password@proxy-server:8080"
```

**注意：** 避免在配置文件中明文存储密码，建议使用以下方法：

```bash
# 使用变量存储敏感信息
export PROXY_USER="your-username"
export PROXY_PASS="your-password"
export HTTP_PROXY="http://${PROXY_USER}:${PROXY_PASS}@proxy-server:8080"
export HTTPS_PROXY="http://${PROXY_USER}:${PROXY_PASS}@proxy-server:8080"
```

## 常见代理配置示例

### 企业代理配置示例

```bash
# 示例 1: 基本HTTP代理
export HTTP_PROXY="http://10.0.1.100:8080"
export HTTPS_PROXY="http://10.0.1.100:8080"

# 示例 2: 不同端口的代理
export HTTP_PROXY="http://proxy.company.com:3128"
export HTTPS_PROXY="http://proxy.company.com:3128"

# 示例 3: SOCKS代理
export HTTP_PROXY="socks5://proxy.company.com:1080"
export HTTPS_PROXY="socks5://proxy.company.com:1080"
```

### 代理排除列表配置

```bash
# 常用的代理排除配置
export NO_PROXY="localhost,127.0.0.1,::1,*.local,*.internal,10.*,192.168.*,172.16.*"

# 特定域名排除
export NO_PROXY="localhost,*.company.com,*.internal,api.local"
```

## 验证代理配置

### 检查环境变量
```bash
# 查看当前代理设置
echo "HTTP_PROXY: $HTTP_PROXY"
echo "HTTPS_PROXY: $HTTPS_PROXY"
echo "NO_PROXY: $NO_PROXY"

# 查看所有环境变量中的代理设置
env | grep -i proxy
```

### 测试网络连接
```bash
# 使用curl测试HTTP代理
curl -I --proxy $HTTP_PROXY http://www.google.com

# 使用curl测试HTTPS代理
curl -I --proxy $HTTPS_PROXY https://www.google.com

# 测试特定API访问
curl -I --proxy $HTTP_PROXY https://api.anthropic.com
```

### 验证Claude Code网络访问
```bash
# 启动Claude Code并测试功能
claude --verbose

# 在Claude Code中测试网络功能
# 例如：请求访问外部API或文档
```

## Git 代理配置

如果使用Git进行版本控制，也需要配置Git代理：

```bash
# 设置Git HTTP代理
git config --global http.proxy http://proxy-server:8080
git config --global https.proxy http://proxy-server:8080

# 查看Git代理配置
git config --global --get http.proxy
git config --global --get https.proxy

# 清除Git代理配置
git config --global --unset http.proxy
git config --global --unset https.proxy
```

## 故障排除

### 常见问题及解决方案

#### 1. 代理连接超时
```bash
# 检查代理服务器是否可达
ping proxy-server
telnet proxy-server 8080

# 尝试不同的代理协议
export HTTP_PROXY="http://proxy-server:8080"
# 或
export HTTP_PROXY="socks5://proxy-server:1080"
```

#### 2. 认证失败
```bash
# 检查用户名密码是否正确
curl -I --proxy http://username:password@proxy-server:8080 http://www.google.com

# 尝试URL编码特殊字符
# 例如：密码包含@符号时，使用%40替代
export HTTP_PROXY="http://user:pass%40word@proxy-server:8080"
```

#### 3. SSL证书问题
```bash
# 跳过SSL验证（仅用于测试）
export NODE_TLS_REJECT_UNAUTHORIZED=0

# 或配置证书路径
export SSL_CERT_FILE="/path/to/certificate.pem"
```

#### 4. Claude Code仍无法访问网络
```bash
# 检查Claude Code是否识别代理设置
claude --debug

# 重启终端确保环境变量生效
# 或手动重新加载配置
source ~/.bashrc
```

### 调试技巧

#### 启用详细日志
```bash
# 启用详细模式
claude --verbose

# 查看网络请求详情
export DEBUG=*
claude
```

#### 检查代理日志
```bash
# 如果有权限访问代理服务器日志
tail -f /var/log/proxy/access.log

# 或使用系统日志
journalctl -f | grep proxy
```

## 安全考虑

### 1. 密码安全
- 避免在配置文件中明文存储密码
- 使用环境变量或密钥管理工具
- 定期更换代理认证密码

### 2. 网络安全
- 确保代理服务器的安全性
- 使用HTTPS代理减少数据泄露风险
- 定期审查代理访问日志

### 3. 权限控制
- 限制代理访问的目标域名
- 配置适当的NO_PROXY排除列表
- 监控异常的网络访问行为

## 高级配置

### 动态代理切换
创建脚本来快速切换不同的代理配置：

```bash
#!/bin/bash
# proxy-switch.sh

case $1 in
  "work")
    export HTTP_PROXY="http://work-proxy:8080"
    export HTTPS_PROXY="http://work-proxy:8080"
    echo "已切换到工作代理"
    ;;
  "home")
    unset HTTP_PROXY
    unset HTTPS_PROXY
    echo "已清除代理设置"
    ;;
  "backup")
    export HTTP_PROXY="http://backup-proxy:3128"
    export HTTPS_PROXY="http://backup-proxy:3128"
    echo "已切换到备用代理"
    ;;
  *)
    echo "用法: $0 {work|home|backup}"
    ;;
esac
```

### 自动代理检测
创建自动检测并配置最佳代理的脚本：

```bash
#!/bin/bash
# auto-proxy.sh

PROXIES=("http://proxy1:8080" "http://proxy2:3128" "http://proxy3:8080")

for proxy in "${PROXIES[@]}"; do
    if curl -I --proxy "$proxy" --connect-timeout 5 http://www.google.com >/dev/null 2>&1; then
        export HTTP_PROXY="$proxy"
        export HTTPS_PROXY="$proxy"
        echo "已自动配置代理: $proxy"
        break
    fi
done
```

## 总结

正确的代理配置对于在内网环境中使用Claude Code至关重要。本指南提供了多种配置方法，从基本的环境变量设置到高级的动态切换方案。选择适合你环境的配置方法，确保Claude Code能够正常访问外部资源。

记住定期检查和更新代理配置，确保网络连接的稳定性和安全性。

---

**注意**: 本文档中的代理地址和认证信息仅为示例。在实际使用中，请替换为你的实际代理服务器信息，并遵循你所在组织的网络安全政策。