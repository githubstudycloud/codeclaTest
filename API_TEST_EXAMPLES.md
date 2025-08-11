# Dynamic Query System API 测试示例

## 系统概述

这是一个基于 SpringBoot 2.7.18 + MyBatis + MySQL 的动态查询系统。支持通过配置表定义字段筛选规则，然后通过动态参数进行数据查询。

## 功能特性

- ✅ 支持多种比较操作符：`=`, `!=`, `>`, `>=`, `<`, `<=`
- ✅ 支持区间查询：`BETWEEN`（闭区间）、`BETWEEN_LO`（左开右闭）、`BETWEEN_RO`（左闭右开）、`BETWEEN_O`（开区间）
- ✅ 支持空值查询：`NA`（null和空字符串）
- ✅ 支持字段名、别名、中文名多种方式查询
- ✅ 不同字段使用 AND 连接，同字段不同条件使用 OR 连接
- ✅ 配置表达式格式校验和错误提示

## 启动项目

1. 确保MySQL运行，执行SQL初始化脚本：
   ```sql
   -- 执行 src/main/resources/sql/schema.sql
   -- 执行 src/main/resources/sql/data.sql
   ```

2. 启动SpringBoot应用：
   ```bash
   mvn spring-boot:run
   ```

## API 测试示例

### 1. 配置表管理 API

#### 1.1 获取所有配置
```bash
GET http://localhost:8080/api/filter-config
```

#### 1.2 获取表达式示例
```bash
GET http://localhost:8080/api/filter-config/examples
```

#### 1.3 创建新配置
```bash
POST http://localhost:8080/api/filter-config
Content-Type: application/json

{
    "fieldName": "level",
    "fieldAlias": "userLevel", 
    "fieldChineseName": "用户等级",
    "filterExpression": "level > 1",
    "fieldType": "INTEGER",
    "description": "用户等级筛选"
}
```

#### 1.4 验证表达式
```bash
POST http://localhost:8080/api/filter-config/validate
Content-Type: application/json

"age BETWEEN 18,65"
```

### 2. 动态查询 API

#### 2.1 基础查询示例

**查询年龄大于25的用户：**
```bash
POST http://localhost:8080/api/data/query
Content-Type: application/json

{
    "age": ["26", "30", "35"]
}
```

**查询状态为active的用户：**
```bash
POST http://localhost:8080/api/data/query
Content-Type: application/json

{
    "status": ["active"]
}
```

#### 2.2 使用别名查询

**使用字段别名查询：**
```bash
POST http://localhost:8080/api/data/query
Content-Type: application/json

{
    "userAge": ["25"],
    "dept": ["IT"]
}
```

#### 2.3 使用中文字段名查询

**使用中文字段名查询：**
```bash
POST http://localhost:8080/api/data/query
Content-Type: application/json

{
    "年龄": ["25"],
    "部门": ["IT"]
}
```

#### 2.4 区间查询示例

**薪资区间查询（根据配置的BETWEEN表达式）：**
```bash
POST http://localhost:8080/api/data/query
Content-Type: application/json

{
    "salary": ["7000,12000"]
}
```

#### 2.5 空值查询

**查询邮箱为空的记录：**
```bash
POST http://localhost:8080/api/data/query
Content-Type: application/json

{
    "email": ["NA"]
}
```

#### 2.6 复合条件查询

**多字段AND查询（年龄>25 AND 部门=IT）：**
```bash
POST http://localhost:8080/api/data/query
Content-Type: application/json

{
    "age": ["26"],
    "department": ["IT"]
}
```

**同字段OR查询（部门=IT OR 部门=HR）：**
```bash
POST http://localhost:8080/api/data/query
Content-Type: application/json

{
    "department": ["IT", "HR"]
}
```

**复杂组合查询：**
```bash
POST http://localhost:8080/api/data/query
Content-Type: application/json

{
    "age": ["25", "30"],
    "department": ["IT", "HR"],
    "status": ["active"]
}
```
*解释：(age=25 OR age=30) AND (department=IT OR department=HR) AND status=active*

#### 2.7 统计查询

**统计满足条件的记录数：**
```bash
POST http://localhost:8080/api/data/count
Content-Type: application/json

{
    "status": ["active"],
    "department": ["IT"]
}
```

### 3. 数据管理 API

#### 3.1 获取所有数据
```bash
GET http://localhost:8080/api/data
```

#### 3.2 创建新记录
```bash
POST http://localhost:8080/api/data
Content-Type: application/json

{
    "name": "测试用户",
    "age": 28,
    "status": "active",
    "score": 88.5,
    "department": "IT",
    "email": "test@example.com",
    "phone": "13800138000",
    "salary": 9000.00
}
```

## 表达式格式说明

### 支持的操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| = | 等于 | `name = 张三` |
| != | 不等于 | `status != inactive` |
| > | 大于 | `age > 18` |
| >= | 大于等于 | `score >= 60` |
| < | 小于 | `age < 65` |
| <= | 小于等于 | `score <= 100` |
| BETWEEN | 闭区间 | `salary BETWEEN 5000,10000` |
| BETWEEN_LO | 左开右闭 | `age BETWEEN_LO 18,65` |
| BETWEEN_RO | 左闭右开 | `score BETWEEN_RO 60,100` |
| BETWEEN_O | 开区间 | `experience BETWEEN_O 1,5` |
| NA | 空值 | `phone NA` |

### 表达式格式规则

1. **单值表达式**：`字段名 操作符 值`
   - 示例：`age > 18`、`name = 张三`

2. **区间表达式**：`字段名 区间操作符 值1,值2`
   - 示例：`salary BETWEEN 5000,10000`

3. **空值表达式**：`字段名 NA`
   - 示例：`phone NA`

4. **字段名支持**：
   - 英文字段名：`age`、`name`
   - 字段别名：`userAge`、`userName`
   - 中文字段名：`年龄`、`姓名`

## 错误处理

系统会对无效的表达式进行校验并返回错误信息：

```json
{
    "timestamp": "2023-12-01T10:30:00.000+00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "表达式格式不正确: 不支持的操作符\n\n正确的表达式格式示例:\nage > 18 - 年龄大于18\nname = 张三 - 姓名等于张三\nsalary BETWEEN 5000,10000 - 工资在5000到10000之间"
}
```

## 注意事项

1. 确保MySQL数据库正确配置和启动
2. 修改 `application.yml` 中的数据库连接信息
3. 所有API支持跨域请求
4. 查询参数的值需要符合配置表中定义的表达式格式
5. 区间查询时，两个值用英文逗号分隔
6. 空值查询使用字符串 "NA"