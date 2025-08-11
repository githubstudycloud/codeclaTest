# Claude Code 使用手册

Claude Code 是 Anthropic 的官方终端编程工具，可以直接在命令行中与 Claude AI 进行编程协作。

## 目录

1. [安装和设置](#安装和设置)
2. [基本使用](#基本使用)
3. [CLI 命令参考](#cli-命令参考)
4. [交互模式](#交互模式)
5. [Slash 命令](#slash-命令)
6. [配置和设置](#配置和设置)
7. [常用工作流](#常用工作流)
8. [故障排除](#故障排除)

## 安装和设置

### 系统要求
- Node.js 18 或更高版本
- 支持的操作系统：Windows、macOS、Linux

### 安装步骤

1. **全局安装**
```bash
npm install -g @anthropic-ai/claude-code
```

2. **验证安装**
```bash
claude --version
```

3. **启动Claude Code**
```bash
claude
```

### 首次设置
- 需要 Anthropic API 密钥
- 会自动引导完成初始配置

## 基本使用

### 启动Claude Code
```bash
# 在当前目录启动
claude

# 在指定目录启动
claude /path/to/project

# 带参数启动
claude --verbose
```

### 退出Claude Code
- 在交互模式中输入 `exit` 或按 `Ctrl+C`
- 使用 `/exit` 命令

## CLI 命令参考

### 基本命令

| 命令 | 说明 | 示例 |
|------|------|------|
| `claude` | 启动交互模式 | `claude` |
| `claude --help` | 显示帮助信息 | `claude -h` |
| `claude --version` | 显示版本信息 | `claude -v` |

### 命令行选项

| 选项 | 简写 | 说明 | 示例 |
|------|------|------|------|
| `--verbose` | `-v` | 详细输出模式 | `claude --verbose` |
| `--quiet` | `-q` | 静默模式 | `claude --quiet` |
| `--no-color` | | 禁用彩色输出 | `claude --no-color` |
| `--resume` | | 恢复之前的会话 | `claude --resume` |
| `--model` | `-m` | 指定模型 | `claude -m sonnet-3.5` |

### 配置命令

```bash
# 列出所有配置
claude config list

# 设置配置项
claude config set <key> <value>

# 添加配置项
claude config add <key> <value>

# 删除配置项
claude config remove <key>

# 显示配置帮助
claude config --help
```

## 交互模式

### 快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl+C` | 中断当前操作或退出 |
| `Ctrl+D` | 退出Claude Code |
| `↑` / `↓` | 浏览历史命令 |
| `Tab` | 自动补全 |
| `Ctrl+L` | 清屏 |
| `Ctrl+R` | 搜索历史命令 |

### 多行输入
- 使用反引号 ``` 包围多行代码
- 或者以 `"""` 开始，`"""` 结束

### 粘贴图像
- 直接拖拽图片到终端
- 或使用路径引用图片文件

## Slash 命令

Claude Code 支持多种 slash 命令来控制会话和获取帮助：

### 基本命令

| 命令 | 说明 | 示例 |
|------|------|------|
| `/help` | 显示帮助信息 | `/help` |
| `/exit` | 退出Claude Code | `/exit` |
| `/clear` | 清除对话历史 | `/clear` |
| `/reset` | 重置会话状态 | `/reset` |

### 文件操作

| 命令 | 说明 | 示例 |
|------|------|------|
| `/read <file>` | 读取文件内容 | `/read src/app.js` |
| `/write <file>` | 写入文件 | `/write package.json` |
| `/edit <file>` | 编辑文件 | `/edit README.md` |

### 项目管理

| 命令 | 说明 | 示例 |
|------|------|------|
| `/ls [path]` | 列出目录内容 | `/ls src/` |
| `/pwd` | 显示当前目录 | `/pwd` |
| `/cd <path>` | 改变目录 | `/cd src/` |

### Git 操作

| 命令 | 说明 | 示例 |
|------|------|------|
| `/git status` | 显示Git状态 | `/git status` |
| `/git diff` | 显示更改 | `/git diff` |
| `/git commit` | 提交更改 | `/git commit -m "message"` |

### 会话管理

| 命令 | 说明 | 示例 |
|------|------|------|
| `/save` | 保存会话 | `/save session-name` |
| `/load` | 加载会话 | `/load session-name` |
| `/history` | 显示历史记录 | `/history` |

## 配置和设置

### 配置文件位置

Claude Code 使用分层配置系统：

1. **用户配置**: `~/.claude/settings.json`
2. **项目配置**: `.claude/settings.json`
3. **企业配置**: 由管理员设置

### 常用配置项

```json
{
  "verbosity": "normal",
  "color": true,
  "autoSave": true,
  "model": "claude-3-sonnet",
  "maxTokens": 4096,
  "temperature": 0.7,
  "tools": {
    "bash": true,
    "edit": true,
    "read": true,
    "write": true
  }
}
```

### 权限设置

```json
{
  "permissions": {
    "fileAccess": {
      "read": ["*.js", "*.ts", "*.json"],
      "write": ["src/**", "docs/**"],
      "exclude": [".env", "*.key", "secrets/**"]
    },
    "networkAccess": {
      "allowed": ["api.example.com"],
      "blocked": ["*.internal.com"]
    }
  }
}
```

### 环境变量

```bash
# API 密钥
export ANTHROPIC_API_KEY="your-api-key"

# 配置文件路径
export CLAUDE_CONFIG_PATH="/custom/path/settings.json"

# 日志级别
export CLAUDE_LOG_LEVEL="debug"
```

## 常用工作流

### 1. 项目初始化
```bash
# 进入项目目录
cd my-project

# 启动 Claude Code
claude

# 让 Claude 了解项目结构
"Please analyze this project structure and help me understand the codebase"
```

### 2. 代码审查
```bash
# 查看 Git 更改
/git diff

# 请求代码审查
"Please review these changes and suggest improvements"
```

### 3. 功能开发
```bash
# 描述需求
"I need to add user authentication to this React app"

# 让 Claude 生成代码
"Create the login component and auth service"
```

### 4. 调试问题
```bash
# 描述问题
"I'm getting a 404 error when accessing /api/users"

# 查看相关文件
/read src/routes/users.js

# 请求调试帮助
"Help me debug this API endpoint"
```

### 5. 测试和部署
```bash
# 运行测试
"Run the test suite and fix any failing tests"

# 部署前检查
"Review the code for deployment readiness"
```

## 高级功能

### 1. 自定义工具
创建自定义脚本来扩展 Claude Code 功能：

```javascript
// .claude/tools/deploy.js
module.exports = {
  name: 'deploy',
  description: 'Deploy to production',
  execute: async (args) => {
    // 部署逻辑
  }
};
```

### 2. 钩子 (Hooks)
在特定事件时执行自定义脚本：

```json
{
  "hooks": {
    "pre-commit": "npm run lint",
    "post-deploy": "npm run notify-team"
  }
}
```

### 3. 模板系统
```bash
# 创建项目模板
claude template create my-template

# 使用模板
claude template use my-template new-project
```

## 故障排除

### 常见问题

#### 1. 认证问题
```bash
# 检查 API 密钥
claude config get api-key

# 重新设置 API 密钥
claude config set api-key "your-new-key"
```

#### 2. 网络问题
```bash
# 检查网络连接
claude --debug

# 设置代理
claude config set proxy "http://proxy.company.com:8080"
```

#### 3. 权限问题
```bash
# 检查文件权限
ls -la ~/.claude/

# 修复权限
chmod 755 ~/.claude/
chmod 644 ~/.claude/settings.json
```

#### 4. 性能问题
```bash
# 清除缓存
claude cache clear

# 减少上下文长度
claude config set maxTokens 2048
```

### 调试技巧

1. **启用详细日志**
```bash
claude --verbose
```

2. **检查配置**
```bash
claude config list --verbose
```

3. **查看日志文件**
```bash
tail -f ~/.claude/logs/claude.log
```

## 最佳实践

### 1. 项目组织
- 在项目根目录创建 `.claude/` 文件夹
- 添加项目特定的配置和脚本
- 使用 `.claudeignore` 排除敏感文件

### 2. 安全考虑
- 不要在配置中硬编码敏感信息
- 使用环境变量管理 API 密钥
- 定期审查文件访问权限

### 3. 团队协作
- 将 `.claude/settings.json` 提交到版本控制
- 建立统一的编码规范和工作流
- 使用共享模板和工具

### 4. 性能优化
- 适当设置 `maxTokens` 限制
- 使用 `.claudeignore` 排除大型文件
- 定期清理会话历史

## 更新和维护

### 更新 Claude Code
```bash
# 检查更新
npm update -g @anthropic-ai/claude-code

# 查看更新日志
claude --changelog
```

### 备份配置
```bash
# 备份配置
cp ~/.claude/settings.json ~/.claude/settings.backup.json

# 导出配置
claude config export > claude-config-backup.json
```

## 获取帮助

- 官方文档: https://docs.anthropic.com/claude-code
- GitHub 仓库: https://github.com/anthropics/claude-code
- 社区论坛: https://community.anthropic.com
- 问题报告: https://github.com/anthropics/claude-code/issues

---

**注意**: 本手册基于 Claude Code 的当前版本。具体功能和命令可能因版本而异，请参考官方文档获取最新信息。