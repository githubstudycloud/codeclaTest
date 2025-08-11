package com.example.dynamicquery.dto;

import com.example.dynamicquery.enums.FilterOperator;
import lombok.Data;

@Data
public class FilterExpression {
    
    private String fieldName;
    
    private String fieldAlias;
    
    private String fieldChineseName;
    
    private FilterOperator operator;
    
    private String value1;
    
    private String value2;
    
    private boolean isValid;
    
    private String errorMessage;
    
    public boolean isBetweenOperator() {
        return operator == FilterOperator.BETWEEN ||
               operator == FilterOperator.BETWEEN_LEFT_OPEN ||
               operator == FilterOperator.BETWEEN_RIGHT_OPEN ||
               operator == FilterOperator.BETWEEN_OPEN;
    }
    
    public boolean isNaOperator() {
        return operator == FilterOperator.NA;
    }
    
    public boolean isSingleValueOperator() {
        return !isBetweenOperator() && !isNaOperator();
    }
}