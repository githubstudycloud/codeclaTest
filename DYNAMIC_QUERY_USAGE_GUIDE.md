# 动态查询系统使用指南

## 概述

本动态查询系统支持灵活的表达式配置和多种查询条件组合，具有以下特点：
- **字段名位置灵活**：支持字段名在表达式中间、左边或使用默认值
- **区间查询直观**：使用数学区间表示法，支持四种半开半闭组合
- **复合字段处理**：统一的条件处理逻辑，避免重复代码
- **小数处理支持**：自动处理需要保留小数的数值字段

## 表达式格式说明

### 1. 基本表达式类型

#### 1.1 单值比较表达式
```
格式：[字段名] 操作符 值
示例：
  age > 18        # 年龄大于18
  > 18           # 默认字段大于18（字段名为field）
  name = 张三     # 姓名等于张三
  score >= 90    # 分数大于等于90
  status != active # 状态不等于active
```

#### 1.2 区间表达式（字段名在中间）
```
格式：值1 操作符1 字段名 操作符2 值2
示例：
  5000 <= salary <= 10000    # 薪资闭区间：5000 ≤ salary ≤ 10000
  18 < age <= 65             # 年龄左开右闭：18 < age ≤ 65  
  60 <= score < 100          # 分数左闭右开：60 ≤ score < 100
  1 < experience < 5         # 经验开区间：1 < experience < 5
```

#### 1.3 空值表达式
```
格式：[字段名] NA
示例：
  phone NA       # 手机号为空或null
  NA            # 默认字段为空
```

### 2. 操作符支持

| 操作符 | 说明 | 适用场景 | 示例 |
|--------|------|----------|------|
| = | 等于 | 精确匹配 | `name = 张三` |
| != | 不等于 | 排除条件 | `status != inactive` |
| > | 大于 | 数值比较 | `age > 18` |
| >= | 大于等于 | 数值比较 | `score >= 60` |
| < | 小于 | 数值比较 | `age < 65` |
| <= | 小于等于 | 数值比较 | `score <= 100` |
| <= ... <= | 闭区间 | 范围查询 | `5000 <= salary <= 10000` |
| < ... <= | 左开右闭 | 范围查询 | `18 < age <= 65` |
| <= ... < | 左闭右开 | 范围查询 | `60 <= score < 100` |
| < ... < | 开区间 | 范围查询 | `1 < experience < 5` |
| NA | 空值 | 空值检查 | `phone NA` |

### 3. 字段名规则

#### 3.1 字段名位置
- **明确指定**：`age > 18`
- **区间中间**：`18 < age <= 65`
- **默认字段**：`> 18`（当配置中字段名为"field"时）

#### 3.2 字段名映射
系统支持三种字段名映射方式：
1. **数据库字段名**：`age`, `salary`, `department`
2. **字段别名**：`userAge`, `sal`, `dept`
3. **中文字段名**：`年龄`, `薪资`, `部门`

## 配置表使用

### 1. 配置表结构
```sql
CREATE TABLE filter_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_name VARCHAR(100) NOT NULL,           -- 实际数据库字段名
    field_alias VARCHAR(100),                   -- 字段别名  
    field_chinese_name VARCHAR(100),           -- 字段中文名
    filter_expression TEXT NOT NULL,           -- 筛选表达式
    field_type VARCHAR(50),                    -- 字段类型
    description TEXT,                          -- 描述
    is_active BOOLEAN DEFAULT TRUE             -- 是否启用
);
```

### 2. 配置示例
```sql
INSERT INTO filter_config (field_name, field_alias, field_chinese_name, filter_expression, field_type, description) VALUES
-- 基本比较
('age', 'userAge', '年龄', 'age > 18', 'INTEGER', '年龄大于18'),
('name', 'userName', '姓名', 'name = 张三', 'STRING', '姓名等值查询'),

-- 区间查询
('salary', 'sal', '薪资', '5000 <= salary <= 10000', 'DOUBLE', '薪资闭区间查询'),
('age', 'ageRange', '年龄区间', '18 < age <= 65', 'INTEGER', '年龄左开右闭查询'),

-- 空值查询
('phone', 'mobile', '手机', 'phone NA', 'STRING', '手机号空值查询'),

-- 默认字段
('field', 'default', '默认字段', '> 0', 'INTEGER', '默认字段大于0');
```

## API 使用示例

### 1. 查询接口调用

#### 1.1 基本查询
```bash
POST /api/data/query
Content-Type: application/json

{
  "age": ["25"],              # 年龄等于25
  "department": ["IT"]        # 部门等于IT
}
```

#### 1.2 区间查询
```bash
POST /api/data/query
Content-Type: application/json

{
  "salary": ["7000,12000"],   # 薪资在7000-12000区间（根据配置表达式）
  "age": ["25,35"]           # 年龄在25-35区间
}
```

#### 1.3 使用别名查询
```bash
POST /api/data/query
Content-Type: application/json

{
  "userAge": ["25"],          # 使用年龄别名
  "sal": ["8000,15000"],     # 使用薪资别名
  "dept": ["IT", "HR"]       # 使用部门别名，多值OR查询
}
```

#### 1.4 使用中文字段名
```bash
POST /api/data/query
Content-Type: application/json

{
  "年龄": ["25"],
  "薪资": ["8000,15000"], 
  "部门": ["IT"]
}
```

#### 1.5 空值查询
```bash
POST /api/data/query
Content-Type: application/json

{
  "email": ["NA"],           # 邮箱为空
  "phone": ["13800138001"]   # 手机号精确匹配
}
```

### 2. 配置管理接口

#### 2.1 创建配置
```bash
POST /api/filter-config
Content-Type: application/json

{
  "fieldName": "experience",
  "fieldAlias": "workExp",
  "fieldChineseName": "工作经验", 
  "filterExpression": "1 < experience < 10",
  "fieldType": "INTEGER",
  "description": "工作经验开区间查询"
}
```

#### 2.2 验证表达式
```bash
POST /api/filter-config/validate
Content-Type: application/json

"5000 <= salary <= 10000"
```

#### 2.3 获取表达式示例
```bash
GET /api/filter-config/examples
```

## 高级特性

### 1. 复合字段处理

#### 1.1 小数字段自动处理
系统自动识别需要保留小数的字段（如salary、score），在SQL中使用`ROUND`函数：
```sql
-- score字段会自动保留2位小数
ROUND(score, 2) >= 90.5
ROUND(salary, 2) BETWEEN 5000.00 AND 10000.00
```

#### 1.2 字符串字段空值处理
字符串类型字段的空值查询会同时检查NULL和空字符串：
```sql
-- 字符串字段空值查询
(email IS NULL OR email = '')
-- 数值字段空值查询  
age IS NULL
```

### 2. 查询条件组合规则

#### 2.1 字段内条件（OR）
同一字段的多个条件使用OR连接：
```json
{
  "department": ["IT", "HR", "Finance"]
}
```
生成SQL：`(department = 'IT' OR department = 'HR' OR department = 'Finance')`

#### 2.2 字段间条件（AND）
不同字段的条件使用AND连接：
```json
{
  "age": ["25"],
  "department": ["IT"],
  "status": ["active"]
}
```
生成SQL：`age = 25 AND department = 'IT' AND status = 'active'`

#### 2.3 复杂组合示例
```json
{
  "age": ["25", "30"],           # age = 25 OR age = 30
  "department": ["IT", "HR"],    # department = 'IT' OR department = 'HR'  
  "salary": ["8000,15000"]       # 根据配置表达式处理区间
}
```
生成SQL：`(age = 25 OR age = 30) AND (department = 'IT' OR department = 'HR') AND (salary条件)`

### 3. 错误处理和校验

#### 3.1 表达式校验错误示例
```json
{
  "error": "表达式格式不正确: 不支持的操作符",
  "validExamples": [
    "age > 18 - 年龄大于18",
    "> 18 - 默认字段大于18", 
    "5000 <= salary <= 10000 - 薪资闭区间",
    "18 < age <= 65 - 年龄左开右闭",
    "phone NA - 手机号为空"
  ]
}
```

#### 3.2 查询参数校验
```bash
# 区间查询值格式错误
POST /api/data/query
{
  "salary": ["8000"]  # 错误：区间查询需要两个值
}

# 正确格式
{
  "salary": ["8000,12000"]  # 正确：逗号分隔的两个值
}
```

## 开发扩展指南

### 1. 添加新的操作符

#### 1.1 扩展FilterOperator枚举
```java
public enum FilterOperator {
    // 现有操作符...
    
    LIKE("LIKE", "模糊匹配"),
    IN("IN", "在集合中");
    
    // ...
}
```

#### 1.2 更新表达式解析器
```java
// 在QueryExpressionParser中添加新的正则表达式
private static final String LIKE_PATTERN = "^([\\w\\u4e00-\\u9fa5]+)\\s+LIKE\\s+([\\w\\u4e00-\\u9fa5%]+)$";

// 在parseExpression方法中添加新的匹配逻辑
```

#### 1.3 更新XML映射
```xml
<when test="condition.operator.name() == 'LIKE'">
    ${fieldName} LIKE CONCAT('%', #{condition.value1}, '%')
</when>
```

### 2. 添加新的字段类型

#### 2.1 扩展字段类型识别
```java
// 在DynamicXmlConditionGenerator中扩展getFieldType方法
private String getFieldType(String fieldName) {
    if (fieldName.contains("date") || fieldName.contains("time")) {
        return "Date";
    }
    if (fieldName.contains("json")) {
        return "Json";
    }
    // ...
}
```

#### 2.2 添加特殊处理逻辑
```xml
<when test="fieldType == 'Date'">
    DATE(${fieldName}) = #{condition.value1}
</when>
<when test="fieldType == 'Json'">
    JSON_EXTRACT(${fieldName}, '$.field') = #{condition.value1}
</when>
```

### 3. 工具类复用

#### 3.1 在其他查询场景中使用
```java
@Service
public class ProductQueryService {
    
    @Autowired
    private QueryConditionBuilder conditionBuilder;
    
    public List<Product> queryProducts(Map<String, List<String>> params) {
        // 复用查询条件构建器
        Map<String, Object> conditions = conditionBuilder.buildQueryConditions(params, "product");
        
        // 执行查询...
    }
}
```

#### 3.2 自定义表达式解析
```java
@Service  
public class CustomQueryService {
    
    @Autowired
    private QueryExpressionParser expressionParser;
    
    public void validateCustomExpression(String expression) {
        FilterExpression parsed = expressionParser.parseExpression(expression);
        if (!parsed.isValid()) {
            throw new IllegalArgumentException(parsed.getErrorMessage());
        }
    }
}
```

## 性能优化建议

### 1. 数据库优化
- 为常用查询字段创建索引
- 使用复合索引优化多字段查询
- 对大数据量表考虑分区策略

### 2. 查询优化
- 避免过多的OR条件组合
- 合理使用LIMIT分页
- 缓存常用的配置信息

### 3. 应用优化
- 配置表数据缓存
- 表达式解析结果缓存
- 使用连接池优化数据库连接

## 常见问题解答

### Q1: 如何处理默认字段？
A: 当表达式中没有指定字段名时（如`> 18`），系统会使用默认字段名"field"。需要在配置表中创建field_name为"field"的配置记录。

### Q2: 区间查询的值如何传递？
A: 区间查询的值使用逗号分隔，如`"salary": ["5000,10000"]`，系统会自动解析为两个边界值。

### Q3: 如何处理小数精度？
A: 系统自动识别数值字段（如score、salary）并使用`ROUND(field, 2)`保留2位小数进行比较。

### Q4: 支持哪些字段类型的空值查询？
A: 支持所有字段类型。字符串类型会检查NULL和空字符串，其他类型只检查NULL。

### Q5: 如何扩展新的操作符？
A: 需要在FilterOperator枚举、QueryExpressionParser解析器、XML映射文件三处同时添加支持。

---

此使用指南涵盖了动态查询系统的所有核心功能和扩展方法，帮助开发者快速上手和深度定制。