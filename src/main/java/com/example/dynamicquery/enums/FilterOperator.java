package com.example.dynamicquery.enums;

import lombok.Getter;

@Getter
public enum FilterOperator {
    
    EQ("=", "等于"),
    NE("!=", "不等于"),
    GT(">", "大于"),
    GTE(">=", "大于等于"),
    LT("<", "小于"),
    LTE("<=", "小于等于"),
    
    BETWEEN("BETWEEN", "区间（闭区间）"),
    BETWEEN_LEFT_OPEN("BETWEEN_LO", "区间（左开右闭）"),
    BETWEEN_RIGHT_OPEN("BETWEEN_RO", "区间（左闭右开）"),
    BETWEEN_OPEN("BETWEEN_O", "区间（开区间）"),
    
    NA("NA", "空值或空字符串");
    
    private final String operator;
    private final String description;
    
    FilterOperator(String operator, String description) {
        this.operator = operator;
        this.description = description;
    }
    
    public static FilterOperator fromString(String operator) {
        for (FilterOperator op : FilterOperator.values()) {
            if (op.getOperator().equalsIgnoreCase(operator)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
}