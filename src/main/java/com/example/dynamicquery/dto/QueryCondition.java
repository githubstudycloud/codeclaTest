package com.example.dynamicquery.dto;

import com.example.dynamicquery.enums.FilterOperator;
import lombok.Data;

@Data
public class QueryCondition {
    
    private String fieldName;
    
    private FilterOperator operator;
    
    private Object value1;
    
    private Object value2;
    
    private String sqlCondition;
    
    public QueryCondition(String fieldName, FilterOperator operator, Object value1) {
        this.fieldName = fieldName;
        this.operator = operator;
        this.value1 = value1;
    }
    
    public QueryCondition(String fieldName, FilterOperator operator, Object value1, Object value2) {
        this.fieldName = fieldName;
        this.operator = operator;
        this.value1 = value1;
        this.value2 = value2;
    }
}