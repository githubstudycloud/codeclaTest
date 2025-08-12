# Claude Code 常用命令指南

## 概述

Claude Code 是 Anthropic 官方推出的 CLI 代码助手，提供强大的编程协助功能。本文档整理了 Claude Code 的常用命令、最佳实践和高级技巧。

## 目录

- [安装与配置](#安装与配置)
- [基础命令](#基础命令)
- [交互模式](#交互模式)
- [斜杠命令](#斜杠命令)
- [高级功能](#高级功能)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

## 安装与配置

### 安装

```bash
# NPM 全局安装
npm install -g @anthropic-ai/claude-code

# 检查版本
claude --version

# 更新到最新版本
claude update
```

### 系统要求

- **操作系统**: macOS, Linux, Windows (via WSL)
- **Node.js**: 18+ 版本
- **网络**: 支持 HTTPS 连接到 Anthropic API

### 初始配置

```bash
# 交互式配置向导
claude config

# 手动设置配置
claude config set model claude-sonnet-4-20250514
claude config set max-tokens 4096

# 查看所有配置
claude config list

# 登录账户
claude login

# 登出账户
claude logout
```

## 基础命令

### 1. 启动模式

#### 交互模式（REPL）
```bash
# 启动交互式会话
claude

# 带初始问题启动
claude "帮我分析这个函数的性能问题"

# 继续上一次会话
claude -c
claude --continue

# 恢复指定会话
claude -r <session-id>
claude --resume <session-id>
```

#### 一次性查询模式
```bash
# 快速查询并退出
claude -p "解释这段代码的作用"
claude --print "优化这个 SQL 查询"

# 管道输入
cat main.py | claude -p "找出潜在的 bug"
git diff | claude -p "评审这次提交"
```

### 2. 命令行选项

```bash
# 指定模型
claude --model claude-sonnet-4-20250514

# 设置输出格式
claude --output-format json
claude --output-format text
claude --output-format stream-json

# 详细日志
claude --verbose

# 添加工作目录
claude --add-dir /path/to/project

# 指定允许的工具
claude --allowedTools read,write,bash

# 权限模式
claude --permission-mode strict
```

### 3. 会话管理

```bash
# 查看会话历史
claude sessions

# 删除指定会话
claude sessions delete <session-id>

# 清理所有会话
claude sessions clear

# 导出会话
claude sessions export <session-id> --format json
```

## 交互模式

### 键盘快捷键

| 快捷键 | 功能 | 说明 |
|--------|------|------|
| `Ctrl+C` | 取消当前输入或生成 | 中断正在进行的操作 |
| `Ctrl+D` | 退出 Claude Code | 结束当前会话 |
| `Ctrl+L` | 清屏 | 清理终端显示 |
| `↑/↓` | 历史记录导航 | 浏览命令历史 |
| `Ctrl+R` | 反向搜索历史 | 搜索之前的命令 |

### 多行输入

```bash
# 方法1：反斜杠转义
这是第一行 \
这是第二行 \
这是第三行

# 方法2：macOS Option+Enter
# 按 Option+Enter 创建新行

# 方法3：Shift+Enter（需配置终端）
# 配置终端支持 Shift+Enter
```

### Vim 模式

```bash
# 启用 Vim 模式
/vim

# Vim 模式快捷键
h, j, k, l  # 光标移动
dd          # 删除行
yy          # 复制行
p           # 粘贴
i           # 插入模式
ESC         # 回到普通模式
```

### 快速命令

```bash
# 内存快捷方式（引用 CLAUDE.md）
# 项目需要遵循 React 最佳实践

# 斜杠命令
/help       # 获取帮助
/clear      # 清理对话历史
/config     # 配置设置
```

## 斜杠命令

### 核心系统命令

```bash
# 帮助和信息
/help                    # 显示帮助信息
/bug                     # 报告 bug 给 Anthropic
/version                 # 显示版本信息

# 会话管理
/clear                   # 清理对话历史
/reset                   # 重置会话状态
/save <name>             # 保存当前会话
/load <name>             # 加载保存的会话

# 配置管理
/config                  # 查看/修改配置
/model <model-name>      # 切换模型
/login                   # 登录账户
/logout                  # 登出账户
```

### 项目管理命令

```bash
# 项目初始化
/init                    # 初始化项目，创建 CLAUDE.md
/add-dir <path>          # 添加工作目录

# 代码审查
/review                  # 请求代码审查
/review --files *.py     # 审查特定文件
/review --diff           # 审查 git diff

# 测试相关
/test                    # 运行测试
/test --watch            # 监视模式运行测试
/coverage                # 生成测试覆盖率报告
```

### 智能代理命令

```bash
# 代理管理
/agents                  # 查看可用代理
/agents list             # 列出所有代理
/agents create <name>    # 创建新代理
/agents delete <name>    # 删除代理

# 常用代理类型
/planner                 # 规划代理（只读）
/coder                   # 编码代理
/tester                  # 测试代理
/reviewer                # 审查代理
/docs                    # 文档代理
```

### 版本控制命令

```bash
# Git 集成
/git status              # 查看 git 状态
/git diff                # 查看差异
/git commit              # 提交更改
/git push                # 推送到远程
/git branch              # 分支管理

# 提交辅助
/commit-msg              # 生成提交信息
/pr-description          # 生成 PR 描述
/changelog               # 生成变更日志
```

### MCP 服务器命令

```bash
# MCP 管理
/mcp                     # 配置 MCP 服务器
/mcp list                # 列出已连接的服务器
/mcp connect <server>    # 连接服务器
/mcp disconnect <server> # 断开服务器

# 动态 MCP 命令（示例）
/mcp__github__issues     # GitHub 问题管理
/mcp__jira__tickets      # Jira 票据管理
/mcp__slack__messages    # Slack 消息处理
```

## 高级功能

### 1. 自定义斜杠命令

#### 项目级命令
```bash
# 创建项目级命令目录
mkdir -p .claude/commands

# 创建自定义命令文件
# .claude/commands/deploy.md
---
name: deploy
description: 部署应用到生产环境
---

请执行以下部署步骤：
1. 运行测试：`npm test`
2. 构建项目：`npm run build`
3. 部署到服务器：`npm run deploy`

参数：$ARGUMENTS
```

#### 全局命令
```bash
# 创建全局命令目录
mkdir -p ~/.claude/commands

# 创建全局命令
# ~/.claude/commands/optimize.md
---
name: optimize
description: 代码性能优化
---

分析并优化以下代码的性能：
$ARGUMENTS

请提供：
1. 性能分析报告
2. 优化建议
3. 重构后的代码
```

### 2. Hooks 系统

```bash
# 创建 hooks 目录
mkdir -p .claude/hooks

# 创建 pre-prompt hook
# .claude/hooks/pre-prompt.js
module.exports = {
  name: 'pre-prompt',
  run: async (context) => {
    console.log('准备处理提示...');
    return context;
  }
};

# 创建 post-response hook
# .claude/hooks/post-response.js
module.exports = {
  name: 'post-response',
  run: async (context) => {
    // 保存响应到文件
    const fs = require('fs');
    fs.appendFileSync('claude-log.txt', context.response + '\n');
    return context;
  }
};
```

### 3. CLAUDE.md 配置

```markdown
# CLAUDE.md 示例

## 项目信息
- 项目类型：React + TypeScript + Node.js
- 数据库：PostgreSQL
- 部署：Docker + AWS

## 编码规范
- 使用 ESLint + Prettier
- TypeScript 严格模式
- 函数式编程优先
- 测试驱动开发（TDD）

## 目录结构
```
src/
  components/    # React 组件
  hooks/        # 自定义 hooks
  utils/        # 工具函数
  types/        # TypeScript 类型定义
  __tests__/    # 测试文件
```

## 开发工作流
1. 创建功能分支
2. 编写测试用例
3. 实现功能
4. 代码审查
5. 合并到主分支

## 常用命令
- `npm run dev` - 开发服务器
- `npm test` - 运行测试
- `npm run build` - 构建生产版本
- `npm run lint` - 代码检查
```

### 4. 上下文加载技巧

```bash
# 加载项目文档
cat README.md | claude -p "基于文档回答问题"

# 加载配置文件
cat package.json tsconfig.json | claude -p "检查配置是否合理"

# 加载测试报告
npm test 2>&1 | claude -p "分析测试失败原因"

# 加载错误日志
tail -f /var/log/app.log | claude -p "实时分析错误日志"
```

## 最佳实践

### 1. 项目设置最佳实践

```bash
# 1. 初始化项目
claude /init

# 2. 配置工作目录
claude --add-dir ./src --add-dir ./tests

# 3. 设置合适的权限
claude config set permission-mode guided

# 4. 创建 CLAUDE.md
cat > CLAUDE.md << 'EOF'
# 项目指南
- 编程语言：Python 3.11
- 框架：FastAPI + SQLAlchemy
- 代码风格：Black + isort
- 测试：pytest

## 开发约定
- 使用类型提示
- 编写文档字符串
- 测试覆盖率 > 90%
EOF
```

### 2. 代码审查最佳实践

```bash
# Git 集成审查
git diff --cached | claude -p "/review 审查这次提交"

# 文件特定审查
claude "/review --focus security,performance src/auth.py"

# PR 准备
git log --oneline -10 | claude -p "生成 PR 描述和变更摘要"
```

### 3. 测试驱动开发

```bash
# TDD 工作流
claude "实现用户注册功能，先写测试用例"

# 测试失败分析
npm test 2>&1 | claude -p "分析测试失败，提供修复建议"

# 覆盖率改进
npm run coverage | claude -p "分析覆盖率报告，建议改进测试"
```

### 4. 性能优化

```bash
# 性能分析
npm run profile | claude -p "分析性能瓶颈，提供优化方案"

# 内存使用分析
ps aux | grep node | claude -p "分析内存使用，检查是否有内存泄漏"

# 数据库查询优化
tail -f /var/log/postgresql.log | claude -p "分析慢查询，提供优化建议"
```

### 5. 智能代理使用

```bash
# 专门的规划代理
claude "/agents create planner --read-only --tools read,glob"
claude "/planner 制定新功能的开发计划"

# 专门的测试代理
claude "/agents create tester --tools read,write,bash"
claude "/tester 为这个模块编写完整的测试套件"

# 专门的文档代理
claude "/agents create docs --tools read,write"
claude "/docs 更新 API 文档"
```

## 常见问题

### Q1: 如何解决网络连接问题？

```bash
# 设置代理
export HTTP_PROXY=http://proxy:8080
export HTTPS_PROXY=http://proxy:8080

# 检查连接
claude config test-connection

# 使用企业代理
claude config set proxy http://corporate-proxy:8080
```

### Q2: 如何提高响应速度？

```bash
# 选择更快的模型
claude --model claude-haiku

# 限制响应长度
claude config set max-tokens 1024

# 使用流式输出
claude config set output-format stream-json
```

### Q3: 如何处理大项目？

```bash
# 分工作区域
claude --add-dir ./frontend
claude --add-dir ./backend --exclude node_modules

# 使用子代理
claude "/agents create frontend --focus react,typescript"
claude "/agents create backend --focus python,fastapi"

# 分阶段处理
claude "第一阶段：分析项目架构"
claude "第二阶段：识别重构目标"
```

### Q4: 如何调试 Claude Code？

```bash
# 详细日志
claude --verbose

# 检查配置
claude config debug

# 查看会话详情
claude sessions info <session-id>

# 导出调试信息
claude debug export --output debug.json
```

### Q5: 如何备份和迁移设置？

```bash
# 导出配置
claude config export --output claude-config.json

# 导入配置
claude config import claude-config.json

# 备份会话历史
claude sessions export --all --output sessions-backup.json

# 迁移到新机器
scp claude-config.json user@newmachine:~/.claude/
```

## 命令速查表

### 启动命令
```bash
claude                          # 交互模式
claude "问题"                    # 带问题启动
claude -p "问题"                 # 一次性查询
claude -c                       # 继续会话
claude --model sonnet           # 指定模型
```

### 配置命令
```bash
claude config                   # 配置向导
claude config list              # 查看配置
claude config set key value     # 设置配置
claude login/logout             # 登录/登出
```

### 斜杠命令
```bash
/help                          # 帮助
/clear                         # 清理历史
/review                        # 代码审查
/test                          # 运行测试
/init                          # 初始化项目
/agents                        # 代理管理
```

### 快捷键
```bash
Ctrl+C                         # 取消/中断
Ctrl+D                         # 退出
Ctrl+L                         # 清屏
↑/↓                           # 历史导航
\                              # 多行输入
```

## 实战案例

### 案例1：新项目快速启动

```bash
# 1. 创建项目目录并初始化
mkdir my-awesome-app && cd my-awesome-app
git init

# 2. 初始化 Claude Code
claude /init

# 3. 创建项目结构
claude "帮我创建一个 React + TypeScript + Node.js 的项目结构"

# 4. 设置开发环境
cat package.json | claude -p "检查依赖配置，建议优化"

# 5. 编写初始测试
claude "/test 为用户认证模块编写测试用例"
```

### 案例2：代码重构工作流

```bash
# 1. 分析现有代码
claude "/review --focus architecture,maintainability src/"

# 2. 制定重构计划
claude "/planner 基于代码审查结果制定重构计划"

# 3. 逐步重构
claude "重构用户服务模块，提高可测试性"

# 4. 验证重构结果
npm test | claude -p "分析重构后的测试结果"

# 5. 更新文档
claude "/docs 更新重构后的 API 文档"
```

### 案例3：Bug 修复流程

```bash
# 1. 分析错误日志
tail -100 /var/log/app.log | claude -p "分析这些错误日志，找出根本原因"

# 2. 重现问题
claude "帮我写个测试用例来重现这个登录失败的 bug"

# 3. 修复实现
claude "修复这个认证 token 过期的问题"

# 4. 验证修复
claude "/test 运行相关测试确保修复生效"

# 5. 回归测试
claude "检查修复是否影响其他功能"
```

### 案例4：性能优化项目

```bash
# 1. 性能基准测试
npm run benchmark | claude -p "分析性能基准，识别瓶颈"

# 2. 数据库查询优化
cat slow-query.log | claude -p "优化这些慢查询"

# 3. 前端性能优化
claude "分析 webpack bundle，建议代码分割策略"

# 4. 缓存策略
claude "设计Redis缓存策略提高API响应速度"

# 5. 监控设置
claude "配置性能监控和告警系统"
```

### 案例5：团队协作场景

```bash
# 代码审查
git diff main..feature-branch | claude -p "/review 重点关注安全性和性能"

# PR 准备
git log --oneline main..HEAD | claude -p "生成 PR 描述和变更摘要"

# 冲突解决
git diff --name-only --diff-filter=U | xargs cat | claude -p "帮我解决这些合并冲突"

# 发布准备
claude "生成 v2.1.0 版本的 changelog"
```

## 进阶技巧

### 1. 组合命令使用

```bash
# 管道组合
git status --porcelain | grep "^M" | cut -c4- | xargs cat | claude -p "审查修改的文件"

# 条件执行
npm test && echo "测试通过" | claude -p "生成测试报告" || echo "测试失败" | claude -p "分析失败原因"

# 批处理
find . -name "*.py" -exec python -m py_compile {} \; 2>&1 | claude -p "分析Python语法错误"
```

### 2. 环境变量配置

```bash
# 设置常用环境变量
export CLAUDE_MODEL="claude-sonnet-4-20250514"
export CLAUDE_MAX_TOKENS="4096"
export CLAUDE_OUTPUT_FORMAT="text"

# 项目特定配置
export CLAUDE_PROJECT_CONTEXT="web-development"
export CLAUDE_CODING_STYLE="google-style"
```

### 3. 自动化脚本

```bash
# 创建开发助手脚本
cat > claude-dev.sh << 'EOF'
#!/bin/bash
# Claude Code 开发助手脚本

case $1 in
  "review")
    git diff --cached | claude -p "/review 审查即将提交的代码"
    ;;
  "test")
    npm test 2>&1 | claude -p "分析测试结果"
    ;;
  "docs")
    find . -name "*.md" | xargs cat | claude -p "检查文档完整性"
    ;;
  *)
    echo "用法: $0 {review|test|docs}"
    ;;
esac
EOF

chmod +x claude-dev.sh
```

### 4. IDE 集成

```bash
# VS Code 集成（通过终端）
# 在 VS Code 中按 Ctrl+` 打开终端

# 当前文件分析
code --list-files | head -1 | xargs cat | claude -p "分析当前文件"

# 选中文本处理（需要扩展）
# 可以配合 VS Code 扩展使用
```

## 故障排除指南

### 网络问题

```bash
# 测试网络连接
claude config test-connection

# 使用代理
export HTTP_PROXY=http://proxy.company.com:8080
export HTTPS_PROXY=http://proxy.company.com:8080

# 检查防火墙设置
curl -I https://api.anthropic.com
```

### 性能问题

```bash
# 减少上下文大小
claude --max-tokens 1024

# 使用更快的模型
claude --model claude-haiku

# 启用缓存
claude config set cache-responses true
```

### 权限问题

```bash
# 检查文件权限
ls -la ~/.claude/

# 重置配置
rm -rf ~/.claude/config
claude config

# 权限修复
chmod -R 600 ~/.claude/
```

## 总结

Claude Code 是一个功能强大的 AI 编程助手，通过合理使用命令行选项、斜杠命令、智能代理和自定义配置，可以大大提高开发效率。关键是要：

1. **正确设置项目上下文**（CLAUDE.md）
2. **使用合适的代理分工**（专门的代理做专门的事）
3. **建立良好的工作流程**（测试驱动、代码审查）
4. **充分利用自动化**（hooks、自定义命令）
5. **掌握实战技巧**（组合命令、环境配置）

希望这份指南能帮助你更好地使用 Claude Code！

---

**更新日期**: 2025年8月11日  
**版本**: v1.0  
**贡献者**: Claude Code 社区

如有问题或建议，请访问：https://github.com/anthropics/claude-code/issues