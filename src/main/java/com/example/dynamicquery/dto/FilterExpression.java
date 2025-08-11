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
    
    public boolean isRangeOperator() {
        return operator != null && operator.isRangeOperator();
    }
    
    public boolean isNaOperator() {
        return operator == FilterOperator.NA;
    }
    
    public boolean isSingleValueOperator() {
        return !isRangeOperator() && !isNaOperator();
    }
}