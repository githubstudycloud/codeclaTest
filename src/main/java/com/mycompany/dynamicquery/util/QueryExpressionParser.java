package com.mycompany.dynamicquery.util;

import com.mycompany.dynamicquery.dto.FilterExpression;
import com.mycompany.dynamicquery.enums.FilterOperator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用查询表达式解析器
 * 支持单值、区间、空值等多种表达式格式
 */
@Component
public class QueryExpressionParser {
    
    // 单值表达式：字段名 操作符 值 或 操作符 值 (默认field)
    private static final String SINGLE_VALUE_PATTERN = "^(?:([\\w\\u4e00-\\u9fa5]+)\\s*)?(>=|<=|!=|>|<|=)\\s*([\\w\\u4e00-\\u9fa5.-]+)$";
    
    // 区间表达式：值1 操作符1 字段名 操作符2 值2 (四种组合)
    private static final String RANGE_CLOSED_PATTERN = "^([\\w\\u4e00-\\u9fa5.-]+)\\s*<=\\s*([\\w\\u4e00-\\u9fa5]+)\\s*<=\\s*([\\w\\u4e00-\\u9fa5.-]+)$";
    private static final String RANGE_LEFT_OPEN_PATTERN = "^([\\w\\u4e00-\\u9fa5.-]+)\\s*<\\s*([\\w\\u4e00-\\u9fa5]+)\\s*<=\\s*([\\w\\u4e00-\\u9fa5.-]+)$";
    private static final String RANGE_RIGHT_OPEN_PATTERN = "^([\\w\\u4e00-\\u9fa5.-]+)\\s*<=\\s*([\\w\\u4e00-\\u9fa5]+)\\s*<\\s*([\\w\\u4e00-\\u9fa5.-]+)$";
    private static final String RANGE_OPEN_PATTERN = "^([\\w\\u4e00-\\u9fa5.-]+)\\s*<\\s*([\\w\\u4e00-\\u9fa5]+)\\s*<\\s*([\\w\\u4e00-\\u9fa5.-]+)$";
    
    // 空值表达式：字段名 NA 或 NA (默认field)
    private static final String NA_PATTERN = "^(?:([\\w\\u4e00-\\u9fa5]+)\\s+)?(NA)$";
    
    private final Pattern singleValuePattern = Pattern.compile(SINGLE_VALUE_PATTERN);
    private final Pattern rangeClosedPattern = Pattern.compile(RANGE_CLOSED_PATTERN);
    private final Pattern rangeLeftOpenPattern = Pattern.compile(RANGE_LEFT_OPEN_PATTERN);
    private final Pattern rangeRightOpenPattern = Pattern.compile(RANGE_RIGHT_OPEN_PATTERN);
    private final Pattern rangeOpenPattern = Pattern.compile(RANGE_OPEN_PATTERN);
    private final Pattern naPattern = Pattern.compile(NA_PATTERN);
    
    /**
     * 解析查询表达式
     */
    public FilterExpression parseExpression(String expression) {
        FilterExpression result = new FilterExpression();
        
        if (StringUtils.isBlank(expression)) {
            result.setValid(false);
            result.setErrorMessage("表达式不能为空");
            return result;
        }
        
        expression = expression.trim();
        
        // 空值表达式
        Matcher naMatcher = naPattern.matcher(expression);
        if (naMatcher.matches()) {
            result.setFieldName(naMatcher.group(1) != null ? naMatcher.group(1) : "field");
            result.setOperator(FilterOperator.NA);
            result.setValid(true);
            return result;
        }
        
        // 闭区间表达式：值1 <= 字段名 <= 值2
        Matcher rangeClosedMatcher = rangeClosedPattern.matcher(expression);
        if (rangeClosedMatcher.matches()) {
            result.setFieldName(rangeClosedMatcher.group(2));
            result.setOperator(FilterOperator.RANGE_CLOSED);
            result.setValue1(rangeClosedMatcher.group(1));
            result.setValue2(rangeClosedMatcher.group(3));
            result.setValid(true);
            return result;
        }
        
        // 左开右闭区间表达式：值1 < 字段名 <= 值2
        Matcher rangeLeftOpenMatcher = rangeLeftOpenPattern.matcher(expression);
        if (rangeLeftOpenMatcher.matches()) {
            result.setFieldName(rangeLeftOpenMatcher.group(2));
            result.setOperator(FilterOperator.RANGE_LEFT_OPEN);
            result.setValue1(rangeLeftOpenMatcher.group(1));
            result.setValue2(rangeLeftOpenMatcher.group(3));
            result.setValid(true);
            return result;
        }
        
        // 左闭右开区间表达式：值1 <= 字段名 < 值2
        Matcher rangeRightOpenMatcher = rangeRightOpenPattern.matcher(expression);
        if (rangeRightOpenMatcher.matches()) {
            result.setFieldName(rangeRightOpenMatcher.group(2));
            result.setOperator(FilterOperator.RANGE_RIGHT_OPEN);
            result.setValue1(rangeRightOpenMatcher.group(1));
            result.setValue2(rangeRightOpenMatcher.group(3));
            result.setValid(true);
            return result;
        }
        
        // 开区间表达式：值1 < 字段名 < 值2
        Matcher rangeOpenMatcher = rangeOpenPattern.matcher(expression);
        if (rangeOpenMatcher.matches()) {
            result.setFieldName(rangeOpenMatcher.group(2));
            result.setOperator(FilterOperator.RANGE_OPEN);
            result.setValue1(rangeOpenMatcher.group(1));
            result.setValue2(rangeOpenMatcher.group(3));
            result.setValid(true);
            return result;
        }
        
        // 单值表达式
        Matcher singleMatcher = singleValuePattern.matcher(expression);
        if (singleMatcher.matches()) {
            try {
                result.setFieldName(singleMatcher.group(1) != null ? singleMatcher.group(1) : "field");
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
    
    /**
     * 验证表达式是否有效
     */
    public boolean validateExpression(String expression) {
        FilterExpression parsed = parseExpression(expression);
        return parsed.isValid();
    }
    
    /**
     * 获取表达式错误信息
     */
    public String getExpressionErrorMessage(String expression) {
        FilterExpression parsed = parseExpression(expression);
        return parsed.getErrorMessage();
    }
    
    /**
     * 获取有效表达式示例
     */
    public List<String> getValidExpressionExamples() {
        List<String> examples = new ArrayList<>();
        examples.add("age > 18 - 年龄大于18");
        examples.add("> 18 - 默认字段大于18");
        examples.add("name = 张三 - 姓名等于张三");
        examples.add("score >= 90 - 分数大于等于90");
        examples.add("status != active - 状态不等于active");
        examples.add("5000 <= salary <= 10000 - 工资在5000到10000之间（闭区间）");
        examples.add("18 < age <= 65 - 年龄在18到65之间（左开右闭）");
        examples.add("60 <= score < 100 - 分数在60到100之间（左闭右开）");
        examples.add("1 < experience < 5 - 经验在1到5之间（开区间）");
        examples.add("phone NA - 手机号为空或null");
        examples.add("NA - 默认字段为空");
        return examples;
    }
    
    /**
     * 将表达式转换为SQL条件
     */
    public String convertToSqlCondition(FilterExpression expression, String parameterPrefix) {
        String fieldName = expression.getFieldName();
        FilterOperator operator = expression.getOperator();
        String prefix = StringUtils.isBlank(parameterPrefix) ? "" : parameterPrefix + ".";
        
        switch (operator) {
            case EQ:
                return fieldName + " = #{" + prefix + fieldName + "}";
            case NE:
                return fieldName + " != #{" + prefix + fieldName + "}";
            case GT:
                return fieldName + " > #{" + prefix + fieldName + "}";
            case GTE:
                return fieldName + " >= #{" + prefix + fieldName + "}";
            case LT:
                return fieldName + " < #{" + prefix + fieldName + "}";
            case LTE:
                return fieldName + " <= #{" + prefix + fieldName + "}";
            case RANGE_CLOSED:
                return fieldName + " >= #{" + prefix + fieldName + "Min} AND " + fieldName + " <= #{" + prefix + fieldName + "Max}";
            case RANGE_LEFT_OPEN:
                return fieldName + " > #{" + prefix + fieldName + "Min} AND " + fieldName + " <= #{" + prefix + fieldName + "Max}";
            case RANGE_RIGHT_OPEN:
                return fieldName + " >= #{" + prefix + fieldName + "Min} AND " + fieldName + " < #{" + prefix + fieldName + "Max}";
            case RANGE_OPEN:
                return fieldName + " > #{" + prefix + fieldName + "Min} AND " + fieldName + " < #{" + prefix + fieldName + "Max}";
            case NA:
                return "(" + fieldName + " IS NULL OR " + fieldName + " = '')";
            default:
                return "1=1";
        }
    }
    
    /**
     * 批量验证表达式
     */
    public List<String> validateExpressions(List<String> expressions) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            String expression = expressions.get(i);
            if (!validateExpression(expression)) {
                errors.add("第" + (i + 1) + "个表达式错误: " + getExpressionErrorMessage(expression));
            }
        }
        return errors;
    }
}