package com.mycompany.dynamicquery.util;

import com.mycompany.dynamicquery.dto.FilterExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 过滤表达式验证器（兼容性保持）
 * @deprecated 请使用 QueryExpressionParser 代替
 */
@Component
@Deprecated
public class FilterExpressionValidator {
    
    @Autowired
    private QueryExpressionParser queryExpressionParser;
    
    public FilterExpression parseExpression(String expression) {
        return queryExpressionParser.parseExpression(expression);
    }
    
    public boolean validateExpression(String expression) {
        return queryExpressionParser.validateExpression(expression);
    }
    
    public String getExpressionErrorMessage(String expression) {
        return queryExpressionParser.getExpressionErrorMessage(expression);
    }
    
    public List<String> getValidExpressionExamples() {
        return queryExpressionParser.getValidExpressionExamples();
    }
}