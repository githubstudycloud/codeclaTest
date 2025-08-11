package com.example.dynamicquery.util;

import com.example.dynamicquery.dto.FilterExpression;
import com.example.dynamicquery.dto.QueryCondition;
import com.example.dynamicquery.entity.FilterConfig;
import com.example.dynamicquery.enums.FilterOperator;
import com.example.dynamicquery.service.FilterConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DynamicQueryBuilder {

    @Autowired
    private FilterConfigService filterConfigService;

    @Autowired
    private FilterExpressionValidator validator;

    public Map<String, Object> buildQueryConditions(Map<String, List<String>> queryParams) {
        Map<String, Object> result = new HashMap<>();
        List<QueryCondition> conditions = new ArrayList<>();
        
        List<FilterConfig> configs = filterConfigService.getAllActiveConfigs();
        Map<String, FilterConfig> configMap = configs.stream()
                .collect(Collectors.toMap(FilterConfig::getFieldName, config -> config, (existing, replacement) -> existing));
        
        Map<String, FilterConfig> aliasMap = configs.stream()
                .filter(config -> StringUtils.isNotBlank(config.getFieldAlias()))
                .collect(Collectors.toMap(FilterConfig::getFieldAlias, config -> config, (existing, replacement) -> existing));
        
        Map<String, FilterConfig> chineseNameMap = configs.stream()
                .filter(config -> StringUtils.isNotBlank(config.getFieldChineseName()))
                .collect(Collectors.toMap(FilterConfig::getFieldChineseName, config -> config, (existing, replacement) -> existing));

        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            String fieldKey = entry.getKey();
            List<String> values = entry.getValue();
            
            if (values == null || values.isEmpty()) {
                continue;
            }
            
            FilterConfig config = findConfig(fieldKey, configMap, aliasMap, chineseNameMap);
            if (config == null) {
                continue;
            }
            
            FilterExpression expression = validator.parseExpression(config.getFilterExpression());
            if (!expression.isValid()) {
                continue;
            }
            
            List<QueryCondition> fieldConditions = new ArrayList<>();
            for (String value : values) {
                QueryCondition condition = buildCondition(config.getFieldName(), expression, value);
                if (condition != null) {
                    fieldConditions.add(condition);
                }
            }
            
            if (!fieldConditions.isEmpty()) {
                conditions.addAll(fieldConditions);
            }
        }
        
        result.put("conditions", conditions);
        return result;
    }

    private FilterConfig findConfig(String fieldKey, Map<String, FilterConfig> configMap, 
                                   Map<String, FilterConfig> aliasMap, 
                                   Map<String, FilterConfig> chineseNameMap) {
        FilterConfig config = configMap.get(fieldKey);
        if (config == null) {
            config = aliasMap.get(fieldKey);
        }
        if (config == null) {
            config = chineseNameMap.get(fieldKey);
        }
        return config;
    }

    private QueryCondition buildCondition(String actualFieldName, FilterExpression expression, String value) {
        if ("NA".equals(value)) {
            return new QueryCondition(actualFieldName, FilterOperator.NA, null);
        }
        
        FilterOperator operator = expression.getOperator();
        
        if (operator.isBetweenOperator()) {
            String[] parts = value.split(",");
            if (parts.length != 2) {
                return null;
            }
            return new QueryCondition(actualFieldName, operator, parts[0].trim(), parts[1].trim());
        } else if (operator.isNaOperator()) {
            return new QueryCondition(actualFieldName, FilterOperator.NA, null);
        } else {
            return new QueryCondition(actualFieldName, operator, value);
        }
    }

    public String buildSqlCondition(QueryCondition condition) {
        String fieldName = condition.getFieldName();
        FilterOperator operator = condition.getOperator();
        
        switch (operator) {
            case EQ:
                return fieldName + " = #{" + fieldName + "}";
            case NE:
                return fieldName + " != #{" + fieldName + "}";
            case GT:
                return fieldName + " > #{" + fieldName + "}";
            case GTE:
                return fieldName + " >= #{" + fieldName + "}";
            case LT:
                return fieldName + " < #{" + fieldName + "}";
            case LTE:
                return fieldName + " <= #{" + fieldName + "}";
            case BETWEEN:
                return fieldName + " >= #{" + fieldName + "Min} AND " + fieldName + " <= #{" + fieldName + "Max}";
            case BETWEEN_LEFT_OPEN:
                return fieldName + " > #{" + fieldName + "Min} AND " + fieldName + " <= #{" + fieldName + "Max}";
            case BETWEEN_RIGHT_OPEN:
                return fieldName + " >= #{" + fieldName + "Min} AND " + fieldName + " < #{" + fieldName + "Max}";
            case BETWEEN_OPEN:
                return fieldName + " > #{" + fieldName + "Min} AND " + fieldName + " < #{" + fieldName + "Max}";
            case NA:
                return "(" + fieldName + " IS NULL OR " + fieldName + " = '')";
            default:
                return "1=1";
        }
    }
}