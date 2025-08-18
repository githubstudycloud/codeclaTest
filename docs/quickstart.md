# 快速开始指南

本指南将帮助您快速上手 High Performance Cache Middleware。

## 系统要求

- **JDK**: 17 或更高版本
- **Maven**: 3.6+ 或 **Gradle**: 7.0+
- **Spring Boot**: 1.x/2.x/3.x（根据需要选择）
- **内存**: 最少 2GB RAM
- **磁盘**: 最少 1GB 可用空间

## 服务端部署

### 方式一：直接运行

```bash
# 克隆项目
git clone https://github.com/your-org/high-performance-cache-middleware.git
cd high-performance-cache-middleware

# 构建项目
./scripts/build.sh

# 启动服务端
java -jar cache-server/cache-server-core/target/cache-server-core-1.0.0-SNAPSHOT.jar
```

### 方式二：Docker 部署

```bash
# 使用 Docker Compose 启动完整环境
cd deployment/docker-compose
docker-compose up -d

# 检查服务状态
docker-compose ps
```

### 方式三：Kubernetes 部署

```bash
# 应用 Kubernetes 配置
kubectl apply -f deployment/kubernetes/

# 检查 Pod 状态
kubectl get pods -l app=cache-server
```

## 客户端集成

### Spring Boot 3.x

1. **添加依赖**

```xml
<dependency>
    <groupId>com.cache.middleware</groupId>
    <artifactId>cache-client-spring-boot-starter-3x</artifactId>
    <version>1.0.0</version>
</dependency>
```

2. **配置文件** (`application.yml`)

```yaml
cache:
  middleware:
    servers:
      - host: localhost
        port: 8080
        protocol: http
      - host: localhost
        port: 50051
        protocol: grpc
    client:
      connection-pool-size: 10
      connect-timeout: 5000
      read-timeout: 3000
      local-cache:
        enabled: true
        max-size: 1000
        ttl: 300s
    retry:
      max-attempts: 3
      backoff-delay: 1000
```

3. **使用示例**

```java
@RestController
public class UserController {
    
    @Autowired
    private CacheTemplate cacheTemplate;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return cacheTemplate.get("user:" + id, User.class)
            .orElseGet(() -> {
                User user = userService.findById(id);
                cacheTemplate.set("user:" + id, user, Duration.ofMinutes(30));
                return user;
            });
    }
    
    @PostMapping("/user")
    public User createUser(@RequestBody User user) {
        User savedUser = userService.save(user);
        cacheTemplate.set("user:" + savedUser.getId(), savedUser, Duration.ofMinutes(30));
        return savedUser;
    }
    
    @DeleteMapping("/user/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        cacheTemplate.delete("user:" + id);
    }
}
```

### 使用注解方式

```java
@Service
public class UserService {
    
    @Cacheable(namespace = "user", key = "#id", ttl = "30m")
    public User findById(Long id) {
        // 从数据库查询用户
        return userRepository.findById(id);
    }
    
    @CacheEvict(namespace = "user", key = "#user.id")
    public User updateUser(User user) {
        // 更新用户信息
        return userRepository.save(user);
    }
    
    @CachePut(namespace = "user", key = "#result.id", ttl = "30m")
    public User createUser(User user) {
        // 创建新用户
        return userRepository.save(user);
    }
}
```

### Spring Boot 2.x

```xml
<dependency>
    <groupId>com.cache.middleware</groupId>
    <artifactId>cache-client-spring-boot-starter-2x</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Spring Boot 1.x

```xml
<dependency>
    <groupId>com.cache.middleware</groupId>
    <artifactId>cache-client-spring-boot-starter-1x</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 多语言客户端

### Python 客户端

```bash
pip install cache-middleware-python
```

```python
from cache_middleware import CacheClient

client = CacheClient(
    servers=[
        {"host": "localhost", "port": 8080, "protocol": "http"},
        {"host": "localhost", "port": 50051, "protocol": "grpc"}
    ]
)

# 设置缓存
client.set("user:1001", {"name": "John", "age": 30}, ttl=1800)

# 获取缓存
user = client.get("user:1001")

# 删除缓存
client.delete("user:1001")
```

### Node.js 客户端

```bash
npm install cache-middleware-nodejs
```

```javascript
const { CacheClient } = require('cache-middleware-nodejs');

const client = new CacheClient({
    servers: [
        { host: 'localhost', port: 8080, protocol: 'http' },
        { host: 'localhost', port: 50051, protocol: 'grpc' }
    ]
});

// 设置缓存
await client.set('user:1001', { name: 'John', age: 30 }, { ttl: 1800 });

// 获取缓存
const user = await client.get('user:1001');

// 删除缓存
await client.delete('user:1001');
```

## 验证安装

### 检查服务状态

```bash
# HTTP 健康检查
curl http://localhost:8080/actuator/health

# gRPC 健康检查
grpc-health-probe -addr=localhost:50051

# 查看指标
curl http://localhost:9090/actuator/prometheus
```

### 基本功能测试

```bash
# 设置缓存
curl -X PUT http://localhost:8080/api/v1/cache/test/key1 \
  -H "Content-Type: application/json" \
  -d '{"value": "Hello World", "ttl": 3600}'

# 获取缓存
curl http://localhost:8080/api/v1/cache/test/key1

# 删除缓存
curl -X DELETE http://localhost:8080/api/v1/cache/test/key1
```

## 监控和管理

### Grafana 监控面板

访问 `http://localhost:3000`（用户名/密码：admin/admin）

预配置的监控面板包括：
- 系统性能指标
- 缓存命中率
- 请求延迟分布
- 连接池状态
- 错误率统计

### 管理控制台

访问 `http://localhost:8080/admin`

功能包括：
- 实时监控
- 缓存数据浏览
- 配置管理
- 集群状态
- 告警管理

## 性能调优

### JVM 参数优化

```bash
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication \
  -XX:G1HeapRegionSize=16m -XX:+UnlockExperimentalVMOptions \
  -XX:+UseCGroupMemoryLimitForHeap"
```

### 缓存配置优化

```yaml
cache:
  middleware:
    storage:
      local-cache:
        max-size: 10000
        expire-after-write: 300s
        expire-after-access: 60s
      redis:
        max-connections: 20
        connect-timeout: 2000
        socket-timeout: 1000
    performance:
      batch-size: 100
      async-write: true
      compression: true
```

## 故障排除

### 常见问题

1. **连接超时**
   - 检查网络连接
   - 验证服务端口是否开放
   - 调整超时配置

2. **内存不足**
   - 增加 JVM 堆内存
   - 调整本地缓存大小
   - 启用缓存压缩

3. **性能问题**
   - 检查 GC 配置
   - 优化缓存策略
   - 调整连接池大小

### 日志配置

```yaml
logging:
  level:
    com.cache.middleware: DEBUG
    root: INFO
  pattern:
    file: "%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/cache-middleware.log
    max-size: 100MB
    max-history: 30
```

## 下一步

- 阅读 [架构设计文档](architecture.md)
- 查看 [API 参考文档](api-reference.md)
- 了解 [部署最佳实践](deployment.md)
- 学习 [性能调优指南](performance-tuning.md)