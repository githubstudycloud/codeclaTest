package com.example.dynamicquery.util;

import com.example.dynamicquery.dto.FilterExpression;
import com.example.dynamicquery.enums.FilterOperator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FilterExpressionValidator {
    
    private static final String SINGLE_VALUE_PATTERN = "^([\\w\\u4e00-\\u9fa5]+)\\s*(>=|<=|!=|>|<|=)\\s*([\\w\\u4e00-\\u9fa5.-]+)$";
    private static final String BETWEEN_PATTERN = "^([\\w\\u4e00-\\u9fa5]+)\\s*(BETWEEN_LO|BETWEEN_RO|BETWEEN_O|BETWEEN)\\s*([\\w\\u4e00-\\u9fa5.-]+)\\s*,\\s*([\\w\\u4e00-\\u9fa5.-]+)$";
    private static final String NA_PATTERN = "^([\\w\\u4e00-\\u9fa5]+)\\s*(NA)$";
    
    private final Pattern singleValuePattern = Pattern.compile(SINGLE_VALUE_PATTERN);
    private final Pattern betweenPattern = Pattern.compile(BETWEEN_PATTERN);
    private final Pattern naPattern = Pattern.compile(NA_PATTERN);
    
    public FilterExpression parseExpression(String expression) {
        FilterExpression result = new FilterExpression();
        
        if (StringUtils.isBlank(expression)) {
            result.setValid(false);
            result.setErrorMessage("表达式不能为空");
            return result;
        }
        
        expression = expression.trim();
        
        Matcher naMatcher = naPattern.matcher(expression);
        if (naMatcher.matches()) {
            result.setFieldName(naMatcher.group(1));
            result.setOperator(FilterOperator.NA);
            result.setValid(true);
            return result;
        }
        
        Matcher betweenMatcher = betweenPattern.matcher(expression);
        if (betweenMatcher.matches()) {
            try {
                result.setFieldName(betweenMatcher.group(1));
                result.setOperator(FilterOperator.fromString(betweenMatcher.group(2)));
                result.setValue1(betweenMatcher.group(3));
                result.setValue2(betweenMatcher.group(4));
                result.setValid(true);
                return result;
            } catch (IllegalArgumentException e) {
                result.setValid(false);
                result.setErrorMessage("不支持的操作符: " + betweenMatcher.group(2));
                return result;
            }
        }
        
        Matcher singleMatcher = singleValuePattern.matcher(expression);
        if (singleMatcher.matches()) {
            try {
                result.setFieldName(singleMatcher.group(1));
                result.setOperator(FilterOperator.fromString(singleMatcher.group(2)));
                result.setValue1(singleMatcher.group(3));
                result.setValid(true);
                return result;
            } catch (IllegalArgumentException e) {
                result.setValid(false);
                result.setErrorMessage("不支持的操作符: " + singleMatcher.group(2));
                return result;
            }
        }
        
        result.setValid(false);
        result.setErrorMessage("表达式格式不正确");
        return result;
    }
    
    public boolean validateExpression(String expression) {
        FilterExpression parsed = parseExpression(expression);
        return parsed.isValid();
    }
    
    public String getExpressionErrorMessage(String expression) {
        FilterExpression parsed = parseExpression(expression);
        return parsed.getErrorMessage();
    }
    
    public List<String> getValidExpressionExamples() {
        List<String> examples = new ArrayList<>();
        examples.add("age > 18 - 年龄大于18");
        examples.add("name = 张三 - 姓名等于张三");
        examples.add("score >= 90 - 分数大于等于90");
        examples.add("status != active - 状态不等于active");
        examples.add("salary BETWEEN 5000,10000 - 工资在5000到10000之间（闭区间）");
        examples.add("age BETWEEN_LO 18,65 - 年龄在18到65之间（左开右闭）");
        examples.add("score BETWEEN_RO 60,100 - 分数在60到100之间（左闭右开）");
        examples.add("experience BETWEEN_O 1,5 - 经验在1到5之间（开区间）");
        examples.add("phone NA - 手机号为空或null");
        return examples;
    }
}