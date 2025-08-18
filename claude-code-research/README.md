# Claude Code 使用研究

本目录包含对 Claude Code CLI 工具的深入研究和使用指南。

## 目录结构

- `claude-code-comprehensive-guide.md` - Claude Code 完整使用指南
- `command-examples.md` - 命令测试示例和实际使用经验
- `README.md` - 本文件，项目概述

## 研究内容

### 1. 完整使用指南
详细的Claude Code使用文档，包括：
- 安装和更新
- 基本命令和高级功能
- 交互模式和斜杠命令
- 配置管理和最佳实践
- 故障排除和参考资源

### 2. 实际测试记录
在Windows MINGW64环境中的实际测试结果：
- 命令功能验证
- 性能测试数据
- 环境限制分析
- 使用技巧总结

## 测试环境

- **系统**: Windows 10 (MINGW64)
- **Claude Code版本**: 1.0.83
- **测试日期**: 2025-01-18
- **Node.js版本**: 需要16+

## 主要发现

### ✅ 正常工作的功能
- 基本命令查询 (`claude -p`)
- 管道输入输出
- 版本和帮助信息
- 配置和MCP管理命令结构
- 中文查询支持

### ❌ 受限的功能
- 交互式命令 (如 `claude doctor`)
- 需要终端原始模式的功能
- 某些高级交互特性

### 💡 使用建议
1. 优先使用非交互模式进行查询
2. 利用管道输入处理文件内容
3. 合理使用会话管理功能
4. 避免在MINGW64环境中使用交互式命令

## 快速开始

```bash
# 基本查询
claude -p "你的问题"

# 管道输入
echo "问题内容" | claude -p

# 文件分析
claude -p "分析这个代码" < your-file.js

# 查看帮助
claude --help
```

## 更多资源

- [Claude Code 官方文档](https://docs.anthropic.com/en/docs/claude-code)
- [GitHub 仓库](https://github.com/anthropics/claude-code)
- [问题反馈](https://github.com/anthropics/claude-code/issues)

---

*研究开始日期: 2025-01-18*  
*最后更新: 2025-01-18*