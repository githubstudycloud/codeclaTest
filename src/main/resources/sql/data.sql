USE dynamic_query_db;

-- 插入配置数据
INSERT INTO filter_config (field_name, field_alias, field_chinese_name, filter_expression, field_type, description) VALUES
('name', 'userName', '姓名', 'name = 张三', 'STRING', '按姓名查询的配置'),
('age', 'userAge', '年龄', 'age > 18', 'INTEGER', '按年龄查询的配置'),
('status', 'userStatus', '状态', 'status = active', 'STRING', '按状态查询的配置'),
('score', 'userScore', '分数', 'score >= 60', 'DOUBLE', '按分数查询的配置'),
('department', 'dept', '部门', 'department = IT', 'STRING', '按部门查询的配置'),
('salary', 'sal', '薪资', 'salary >= 5000 且 <= 10000', 'DOUBLE', '按薪资范围查询的配置（闭区间）'),
('email', 'mail', '邮箱', 'email NA', 'STRING', '查询邮箱为空的配置'),
('phone', 'mobile', '手机', 'phone != 空', 'STRING', '查询手机不为空的配置');

-- 插入测试数据
INSERT INTO data_record (name, age, status, score, department, email, phone, salary, hire_date) VALUES
('张三', 25, 'active', 85.5, 'IT', 'zhangsan@example.com', '13800138001', 8000.00, '2023-01-15'),
('李四', 30, 'active', 92.0, 'HR', 'lisi@example.com', '13800138002', 7500.00, '2022-08-20'),
('王五', 28, 'inactive', 78.5, 'IT', NULL, '13800138003', 9000.00, '2023-03-10'),
('赵六', 35, 'active', 88.0, 'Finance', 'zhaoliu@example.com', NULL, 12000.00, '2021-11-05'),
('孙七', 22, 'active', 95.5, 'IT', 'sunqi@example.com', '13800138005', 6500.00, '2023-06-01'),
('周八', 40, 'inactive', 72.0, 'Sales', NULL, '13800138006', 5500.00, '2020-12-15'),
('吴九', 27, 'active', 90.5, 'IT', 'wujiu@example.com', '13800138007', 8500.00, '2022-09-30'),
('郑十', 33, 'active', 86.0, 'HR', 'zhengshi@example.com', '13800138008', 7800.00, '2023-02-14'),
('刘一', 29, 'active', 83.5, 'Finance', NULL, NULL, 9500.00, '2022-05-20'),
('陈二', 24, 'inactive', 76.5, 'Sales', 'chener@example.com', '13800138010', 6000.00, '2023-04-25');

-- 添加更多配置示例，展示各种操作符
INSERT INTO filter_config (field_name, field_alias, field_chinese_name, filter_expression, field_type, description) VALUES
('age', 'ageRange', '年龄区间', 'age > 20 且 <= 30', 'INTEGER', '年龄左开右闭区间查询'),
('score', 'scoreRange', '分数区间', 'score >= 80 且 < 100', 'DOUBLE', '分数左闭右开区间查询'),
('salary', 'salaryOpen', '薪资开区间', 'salary > 6000 且 < 12000', 'DOUBLE', '薪资开区间查询'),
('hire_date', 'hireYear', '入职年份', 'hire_date >= 2023-01-01', 'DATE', '按入职年份查询'),
('phone', 'phoneNull', '手机为空', 'phone NA', 'STRING', '查询手机为空的记录');