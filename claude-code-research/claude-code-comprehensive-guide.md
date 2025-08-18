# Claude Code 完整使用指南

## 版本信息

- **当前版本**: 1.0.83 (Claude Code)
- **更新日期**: 2025-01-18
- **官方文档**: https://docs.anthropic.com/en/docs/claude-code

## 目录

1. [概述](#概述)
2. [安装与更新](#安装与更新)
3. [基本命令](#基本命令)
4. [交互模式](#交互模式)
5. [斜杠命令](#斜杠命令)
6. [命令行选项](#命令行选项)
7. [配置管理](#配置管理)
8. [高级功能](#高级功能)
9. [最佳实践](#最佳实践)
10. [故障排除](#故障排除)

## 概述

Claude Code 是 Anthropic 开发的智能编程助手工具，主要特性包括：

- 🚀 **快速开发**: 从自然语言描述直接生成代码
- 🐛 **调试修复**: 自动识别和修复代码问题  
- 🗺️ **代码导航**: 智能理解和分析代码库
- ⚡ **任务自动化**: 自动化繁琐的开发任务
- 🔧 **直接操作**: 可以直接编辑文件、创建提交等
- 🔒 **企业级安全**: 内置安全和隐私保护

## 安装与更新

### 全局安装
```bash
npm install -g @anthropic-ai/claude-code
```

### 使用
```bash
cd your-project
claude
```

### 更新到最新版本
```bash
claude update
```

### 检查健康状态
```bash
claude doctor
```

### 版本信息
```bash
claude --version
# 输出: 1.0.83 (Claude Code)
```

## 基本命令

### 1. 启动交互模式
```bash
# 启动交互式REPL
claude

# 带初始提示启动
claude "帮我分析这个项目的结构"
```

### 2. 非交互模式（一次性查询）
```bash
# 打印响应后退出
claude -p "解释这段代码的功能"

# 管道输入
echo "检查这个函数的性能" | claude -p
```

### 3. 会话管理
```bash
# 继续最近的对话
claude -c
claude --continue

# 恢复特定会话
claude -r
claude --resume [sessionId]
```

### 4. 设置和配置
```bash
# 设置认证令牌
claude setup-token

# 配置管理
claude config

# MCP服务器管理
claude mcp
```

## 交互模式

### 启动交互模式
进入项目目录后运行 `claude` 即可启动交互模式。

### 键盘快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl+C` | 取消当前输入 |
| `Ctrl+D` | 退出会话 |
| `Ctrl+L` | 清屏 |
| `↑/↓` | 浏览命令历史 |
| `Tab` | 自动补全 |

### Vim模式
```bash
# 启用Vim模式
/vim

# Vim导航命令
h/j/k/l  # 移动光标
i        # 进入INSERT模式
Esc      # 返回NORMAL模式
```

### 多行输入
- 支持多行代码输入
- 自动检测代码块结束

## 斜杠命令

斜杠命令用于控制AI行为和会话设置。

### 内置命令

| 命令 | 功能 | 示例 |
|------|------|------|
| `/help` | 显示帮助信息 | `/help` |
| `/clear` | 清除对话历史 | `/clear` |
| `/model` | 选择或更改AI模型 | `/model sonnet` |
| `/review` | 请求代码审查 | `/review` |
| `/bug` | 报告bug给Anthropic | `/bug` |
| `/vim` | 切换Vim编辑模式 | `/vim` |

### 自定义斜杠命令

#### 项目级命令
在项目根目录创建 `.claude/commands/` 目录：

```bash
mkdir -p .claude/commands
```

创建自定义命令文件（Markdown格式）：

```markdown
---
name: test
description: 运行项目测试
---

请运行项目的单元测试并分析结果：

!npm test

如果有测试失败，请帮我分析失败原因并提供修复建议。
```

#### 全局命令
在用户主目录创建 `~/.claude/commands/` 目录：

```bash
mkdir -p ~/.claude/commands
```

#### 命令语法
- `$ARGUMENTS`: 命令参数占位符
- `!command`: 执行bash命令
- `@filename`: 引用文件内容

### MCP命令
格式：`/mcp__<server-name>__<prompt-name>`

## 命令行选项

### 基本选项

| 选项 | 简写 | 说明 | 示例 |
|------|------|------|------|
| `--help` | `-h` | 显示帮助 | `claude -h` |
| `--version` | `-v` | 显示版本 | `claude -v` |
| `--print` | `-p` | 打印响应后退出 | `claude -p "代码审查"` |
| `--continue` | `-c` | 继续最近对话 | `claude -c` |
| `--resume` | `-r` | 恢复会话 | `claude -r` |

### 高级选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `--model <model>` | 指定模型 | `claude --model opus` |
| `--fallback-model <model>` | 备用模型 | `claude --fallback-model sonnet` |
| `--output-format <format>` | 输出格式 | `claude -p --output-format json` |
| `--input-format <format>` | 输入格式 | `claude --input-format stream-json` |
| `--verbose` | 详细日志 | `claude --verbose` |
| `--debug` | 调试模式 | `claude --debug` |

### 权限和安全

| 选项 | 说明 | 示例 |
|------|------|------|
| `--permission-mode <mode>` | 权限模式 | `claude --permission-mode plan` |
| `--allowedTools <tools>` | 允许的工具 | `claude --allowedTools "Bash Edit"` |
| `--disallowedTools <tools>` | 禁用的工具 | `claude --disallowedTools "WebFetch"` |
| `--dangerously-skip-permissions` | 跳过权限检查 | `claude --dangerously-skip-permissions` |

### 配置选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `--settings <file-or-json>` | 加载设置 | `claude --settings config.json` |
| `--add-dir <directories>` | 额外目录权限 | `claude --add-dir /path/to/dir` |
| `--mcp-config <configs>` | MCP配置 | `claude --mcp-config server.json` |
| `--session-id <uuid>` | 指定会话ID | `claude --session-id uuid` |

## 配置管理

### 查看配置
```bash
claude config
```

### 设置配置
```bash
# 设置全局主题
claude config set -g theme dark

# 设置本地配置
claude config set editor vim
```

### 常用配置项
- `theme`: 主题设置（light/dark）
- `editor`: 编辑器偏好
- `model`: 默认模型
- `auto-update`: 自动更新设置

## 高级功能

### 1. 模型选择
```bash
# 使用特定模型
claude --model sonnet
claude --model opus
claude --model claude-sonnet-4-20250514

# 设置备用模型
claude --fallback-model sonnet
```

### 2. 输出格式
```bash
# JSON格式输出
claude -p --output-format json "分析代码"

# 流式JSON输出
claude -p --output-format stream-json "长任务"
```

### 3. IDE集成
```bash
# 自动连接IDE
claude --ide
```

### 4. MCP服务器管理
```bash
# 查看MCP服务器
claude mcp list

# 添加MCP服务器
claude mcp add server-name

# 配置MCP服务器
claude --mcp-config server-config.json
```

### 5. 会话管理
```bash
# 使用特定会话ID
claude --session-id 12345678-1234-1234-1234-123456789abc

# 交互式选择会话恢复
claude -r
```

## 最佳实践

### 1. 项目初始化
```bash
# 进入项目目录
cd your-project

# 首次使用
claude "分析项目结构并提供优化建议"
```

### 2. 代码开发工作流
```bash
# 功能开发
claude "实现用户登录功能，使用JWT认证"

# 代码审查
claude "/review"

# 调试
claude "这个函数报错了，帮我找出问题"
```

### 3. 文档和测试
```bash
# 生成文档
claude "为这个模块生成详细的API文档"

# 编写测试
claude "为这个函数编写单元测试"
```

### 4. 性能优化
```bash
claude "分析代码性能并提供优化建议"
```

### 5. 安全检查
```bash
claude "检查代码中的安全漏洞"
```

## 故障排除

### 常见问题

#### 1. 安装问题
```bash
# 检查Node.js版本
node --version  # 需要Node.js 16+

# 清除npm缓存
npm cache clean --force

# 重新安装
npm uninstall -g @anthropic-ai/claude-code
npm install -g @anthropic-ai/claude-code
```

#### 2. 认证问题
```bash
# 重新设置令牌
claude setup-token

# 检查认证状态
claude doctor
```

#### 3. 权限问题
```bash
# 使用更宽松的权限模式
claude --permission-mode acceptEdits

# 跳过权限检查（仅测试环境）
claude --dangerously-skip-permissions
```

#### 4. 性能问题
```bash
# 启用调试模式
claude --debug

# 使用更快的模型
claude --model sonnet
```

### 日志和调试
```bash
# 启用详细日志
claude --verbose

# 启用调试模式
claude --debug

# 启用MCP调试
claude --mcp-debug
```

### 获取帮助
```bash
# 命令行帮助
claude --help

# 交互模式帮助
/help

# 健康检查
claude doctor

# 报告问题
/bug
```

## 参考资源

- **官方文档**: https://docs.anthropic.com/en/docs/claude-code
- **GitHub仓库**: https://github.com/anthropics/claude-code
- **问题反馈**: https://github.com/anthropics/claude-code/issues
- **快速开始**: https://docs.anthropic.com/en/docs/claude-code/quickstart
- **CLI参考**: https://docs.anthropic.com/en/docs/claude-code/cli-reference

---

*最后更新: 2025-01-18*  
*版本: Claude Code 1.0.83*