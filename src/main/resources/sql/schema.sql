-- 创建数据库
CREATE DATABASE IF NOT EXISTS dynamic_query_db 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE dynamic_query_db;

-- 创建配置表
DROP TABLE IF EXISTS filter_config;
CREATE TABLE filter_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_name VARCHAR(100) NOT NULL COMMENT '字段名',
    field_alias VARCHAR(100) COMMENT '字段别名',
    field_chinese_name VARCHAR(100) COMMENT '字段中文名',
    filter_expression TEXT NOT NULL COMMENT '筛选表达式',
    field_type VARCHAR(50) COMMENT '字段类型',
    description TEXT COMMENT '描述',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_field_name (field_name),
    INDEX idx_field_alias (field_alias),
    INDEX idx_field_chinese_name (field_chinese_name),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='筛选配置表';

-- 创建数据表
DROP TABLE IF EXISTS data_record;
CREATE TABLE data_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) COMMENT '姓名',
    age INT COMMENT '年龄',
    status VARCHAR(50) COMMENT '状态',
    score DOUBLE COMMENT '分数',
    department VARCHAR(100) COMMENT '部门',
    email VARCHAR(200) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    salary DOUBLE COMMENT '薪资',
    hire_date TIMESTAMP COMMENT '入职日期',
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_name (name),
    INDEX idx_age (age),
    INDEX idx_status (status),
    INDEX idx_score (score),
    INDEX idx_department (department),
    INDEX idx_salary (salary),
    INDEX idx_hire_date (hire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据记录表';