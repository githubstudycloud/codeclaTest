# High Performance Cache Middleware

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3%2B-green.svg)](https://spring.io/projects/spring-boot)

企业级高性能分布式缓存中间件，基于Spring Boot 3.x构建，支持多版本兼容和多语言客户端。

## 快速开始

### 服务端启动
```bash
# 克隆项目
git clone https://github.com/your-org/high-performance-cache-middleware.git
cd high-performance-cache-middleware

# 构建项目
./mvnw clean package

# 启动服务
java -jar cache-server/target/cache-server.jar
```

### Java客户端使用
```xml
<dependency>
    <groupId>com.cache.middleware</groupId>
    <artifactId>cache-client-spring-boot-starter-3x</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
@Autowired
private CacheTemplate cacheTemplate;

// 设置缓存
cacheTemplate.set("user:1001", user, Duration.ofMinutes(30));

// 获取缓存
User user = cacheTemplate.get("user:1001", User.class);
```

## 项目结构

```
high-performance-cache-middleware/
├── cache-server/                           # 服务端
│   ├── cache-server-core/                  # 核心服务
│   ├── cache-server-api/                   # API层
│   ├── cache-server-storage/               # 存储层
│   ├── cache-server-cluster/               # 集群管理
│   ├── cache-server-monitor/               # 监控模块
│   └── cache-server-admin/                 # 管理控制台
├── cache-client-java/                      # Java客户端
│   ├── cache-client-core/                  # 核心客户端
│   ├── cache-client-spring-boot-starter-1x/ # Spring Boot 1.x支持
│   ├── cache-client-spring-boot-starter-2x/ # Spring Boot 2.x支持
│   ├── cache-client-spring-boot-starter-3x/ # Spring Boot 3.x支持
│   └── cache-client-annotations/           # 注解支持
├── cache-clients/                          # 多语言客户端
│   ├── cache-client-python/               # Python客户端
│   ├── cache-client-nodejs/               # Node.js客户端
│   ├── cache-client-go/                   # Go客户端
│   ├── cache-client-dotnet/               # .NET客户端
│   └── cache-client-php/                  # PHP客户端
├── cache-common/                           # 公共模块
│   ├── cache-common-api/                   # API定义
│   ├── cache-common-protocol/              # 协议定义
│   └── cache-common-utils/                 # 工具类
├── cache-examples/                         # 示例项目
├── cache-tests/                            # 测试套件
├── deployment/                             # 部署配置
├── docs/                                   # 文档
└── scripts/                                # 脚本工具
```

## 核心特性

- 🚀 **高性能**: P99延迟<1ms，单节点>100万QPS
- 🔧 **多版本兼容**: 支持Spring Boot 1.x/2.x/3.x
- 🌍 **多语言支持**: Java/Python/Node.js/Go/.NET/PHP
- 📡 **多协议**: HTTP/HTTPS/gRPC/WebSocket/TCP
- 💾 **多存储**: SQLite/MySQL/Redis/MongoDB
- 📨 **消息队列**: RabbitMQ/Kafka/RocketMQ
- 🔄 **分布式**: 集群模式、自动故障转移
- 📊 **可观测**: 实时监控、智能告警
- 🛡️ **高可用**: 99.99% SLA保证

## 文档

- [快速开始](docs/quickstart.md)
- [架构设计](docs/architecture.md)
- [API文档](docs/api-reference.md)
- [配置指南](docs/configuration.md)
- [部署指南](docs/deployment.md)
- [性能调优](docs/performance-tuning.md)

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。