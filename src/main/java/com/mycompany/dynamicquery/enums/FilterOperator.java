package com.mycompany.dynamicquery.enums;

import lombok.Getter;

@Getter
public enum FilterOperator {
    
    EQ("=", "等于"),
    NE("!=", "不等于"),
    GT(">", "大于"),
    GTE(">=", "大于等于"),
    LT("<", "小于"),
    LTE("<=", "小于等于"),
    
    RANGE_CLOSED(">=<", "闭区间（>= 值1 且 <= 值2）"),
    RANGE_LEFT_OPEN("><", "左开右闭（> 值1 且 <= 值2）"),
    RANGE_RIGHT_OPEN(">=<", "左闭右开（>= 值1 且 < 值2）"),
    RANGE_OPEN("><", "开区间（> 值1 且 < 值2）"),
    
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
    
    public boolean isRangeOperator() {
        return this == RANGE_CLOSED || this == RANGE_LEFT_OPEN || 
               this == RANGE_RIGHT_OPEN || this == RANGE_OPEN;
    }
}