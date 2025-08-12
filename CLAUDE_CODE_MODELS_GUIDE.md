# Claude Code 模型管理指南

## 概述

Claude Code 支持多种 AI 模型，用户可以根据不同的使用场景选择最适合的模型。本指南详细介绍如何查看、选择和管理 Claude Code 中的可用模型。

## 模型相关命令

### 1. 查看和设置模型

```bash
# 在交互模式中查看/设置模型
claude
/model

# 查看当前模型配置
claude config get model

# 设置默认模型
claude config set model <model-name>

# 启动时指定模型
claude --model <model-name>
```

### 2. 模型切换

```bash
# 交互模式中切换模型
/model <model-name>

# 临时使用特定模型
claude --model claude-3-5-haiku-20241022 -p "简单查询"
```

## 可用模型列表

基于最新的 Claude Code 实际测试，当前可用的模型包括：

### 主要模型

| 模型名称 | 显示名称 | 特点 | 推荐用途 |
|----------|----------|------|----------|
| **Sonnet** | Sonnet (Sonnet 4 for daily use) | 最新 Sonnet 4，日常使用推荐 | 综合编程任务、代码审查、架构设计 |
| **claude-3-5-sonnet** | Claude 3.5 Sonnet | 成熟稳定的 Sonnet 3.5 | 复杂开发任务、深度代码分析 |
| **claude-3-5-haiku** | Claude 3.5 Haiku | 快速响应，轻量级 | 简单查询、语法检查、快速分析 |
| **claude-3-opus** | Claude 3 Opus | 最强推理能力 | 复杂问题解决、算法设计 |

### 具体模型版本

```bash
# Sonnet 系列
claude-sonnet-4-20250514      # Sonnet 4 最新版本
claude-3-5-sonnet-20241022    # Sonnet 3.5 稳定版

# Haiku 系列  
claude-3-5-haiku-20241022     # Haiku 3.5 快速版

# Opus 系列
claude-3-opus-20240229        # Opus 3 强推理版
```

## 模型选择策略

### 按任务类型选择

```bash
# 日常开发 - 推荐 Sonnet 4
claude --model sonnet
/model sonnet

# 快速查询 - 使用 Haiku
claude --model claude-3-5-haiku-20241022 -p "检查这段代码语法"

# 复杂算法 - 使用 Opus
claude --model claude-3-opus-20240229 "设计高效的排序算法"

# 代码审查 - 使用 Sonnet 3.5
claude --model claude-3-5-sonnet-20241022 "/review 审查这个模块"
```

### 按性能需求选择

| 需求 | 推荐模型 | 理由 |
|------|----------|------|
| **速度优先** | claude-3-5-haiku | 响应最快，成本最低 |
| **质量优先** | claude-3-opus | 推理能力最强，输出质量最高 |
| **平衡考虑** | sonnet (Sonnet 4) | 性能与速度的最佳平衡 |
| **稳定可靠** | claude-3-5-sonnet | 成熟稳定，广泛验证 |

## 实际使用示例

### 1. 日常开发工作流

```bash
# 设置 Sonnet 4 为默认模型
claude config set model sonnet

# 启动开发会话
claude "帮我分析这个 React 组件的性能问题"

# 快速语法检查时切换到 Haiku
/model claude-3-5-haiku-20241022
```

### 2. 不同场景的模型切换

```bash
# 场景1：快速代码审查
claude --model claude-3-5-haiku-20241022 -p "/review 检查基本语法错误"

# 场景2：复杂架构设计
claude --model claude-3-opus-20240229 "设计微服务架构方案"

# 场景3：日常编程协助
claude --model sonnet "重构这个函数提高可读性"
```

### 3. 批量任务处理

```bash
# 使用脚本处理不同类型任务
#!/bin/bash

# 快速检查
find . -name "*.py" | xargs -I {} claude --model claude-3-5-haiku-20241022 -p "检查 {} 的语法"

# 深度分析
claude --model claude-3-opus-20240229 -p "分析整个项目的架构问题"

# 代码优化
claude --model sonnet -p "优化这些性能瓶颈函数"
```

## 配置管理

### 查看当前配置

```bash
# 查看所有配置
claude config list

# 查看当前模型
claude config get model

# 查看模型详细信息
/model
```

### 配置文件位置

```bash
# 配置文件位置（通常）
~/.claude/config
# 或
%USERPROFILE%\.claude\config  # Windows

# 查看配置文件内容
cat ~/.claude/config
```

### 重置模型配置

```bash
# 重置到默认模型
claude config unset model

# 重新配置
claude config
```

## 性能对比

### 响应速度对比

| 模型 | 相对速度 | 适用场景 |
|------|----------|----------|
| Haiku 3.5 | ⭐⭐⭐⭐⭐ | 实时交互、快速查询 |
| Sonnet 4 | ⭐⭐⭐⭐ | 日常开发任务 |
| Sonnet 3.5 | ⭐⭐⭐ | 中等复杂任务 |
| Opus 3 | ⭐⭐ | 复杂分析任务 |

### 输出质量对比

| 模型 | 代码质量 | 推理深度 | 创新能力 |
|------|----------|----------|----------|
| Opus 3 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Sonnet 4 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Sonnet 3.5 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| Haiku 3.5 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |

## 成本考虑

### 模型成本等级

```
Opus 3 > Sonnet 4 > Sonnet 3.5 > Haiku 3.5
（成本从高到低）
```

### 成本优化建议

```bash
# 1. 简单任务使用 Haiku
claude --model claude-3-5-haiku-20241022 -p "格式化这段代码"

# 2. 日常任务使用 Sonnet 4
claude --model sonnet "实现用户注册功能"

# 3. 只在必要时使用 Opus
claude --model claude-3-opus-20240229 "设计复杂的算法优化方案"
```

## 最佳实践

### 1. 智能模型切换

```bash
# 工作流中的模型切换策略
# 开始：使用 Haiku 快速了解需求
claude --model claude-3-5-haiku-20241022 "快速分析这个需求"

# 中期：使用 Sonnet 4 进行开发
/model sonnet

# 复杂问题：切换到 Opus
/model claude-3-opus-20240229
```

### 2. 项目级别配置

```bash
# 在项目 CLAUDE.md 中指定推荐模型
echo "# 推荐模型: sonnet (日常开发)" >> CLAUDE.md
echo "# 备选模型: claude-3-5-haiku-20241022 (快速查询)" >> CLAUDE.md
```

### 3. 团队协作

```bash
# 团队统一模型配置
# .claude/team-config.json
{
  "default_model": "sonnet",
  "quick_model": "claude-3-5-haiku-20241022",
  "complex_model": "claude-3-opus-20240229"
}
```

## 故障排除

### 模型切换失败

```bash
# 检查网络连接
claude config test-connection

# 重置配置
claude config unset model
claude config

# 查看错误日志
claude --verbose
```

### 模型性能异常

```bash
# 清除缓存
claude config clear-cache

# 重新登录
claude logout
claude login

# 检查 API 状态
curl -I https://api.anthropic.com
```

## 总结

- **默认推荐**: Sonnet 4 作为日常开发的最佳选择
- **快速任务**: Haiku 3.5 适合简单查询和语法检查
- **复杂分析**: Opus 3 提供最强的推理能力
- **稳定可靠**: Sonnet 3.5 适合关键项目开发

选择合适的模型可以显著提高开发效率和输出质量，建议根据具体任务需求灵活切换。

---

**更新日期**: 2025年8月11日  
**版本**: v1.0  
**基于**: Claude Code 实际测试结果

如有问题请参考官方文档：https://docs.anthropic.com/en/docs/claude-code