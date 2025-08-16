package com.mycompany.dynamicquery.util;

import com.mycompany.dynamicquery.dto.FilterExpression;
import com.mycompany.dynamicquery.dto.QueryCondition;
import com.mycompany.dynamicquery.entity.FilterConfig;
import com.mycompany.dynamicquery.enums.FilterOperator;
import com.mycompany.dynamicquery.service.FilterConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用查询条件构建器
 * 可用于任何需要动态查询的场景
 */
@Component
public class QueryConditionBuilder {

    @Autowired
    private FilterConfigService filterConfigService;

    @Autowired
    private QueryExpressionParser expressionParser;

    /**
     * 构建查询条件
     * @param queryParams 查询参数 Map<字段名, 值列表>
     * @param tableName 表名（可选，用于多表查询）
     * @return 查询条件映射
     */
    public Map<String, Object> buildQueryConditions(Map<String, List<String>> queryParams, String tableName) {
        Map<String, Object> result = new HashMap<>();
        List<QueryCondition> conditions = new ArrayList<>();
        
        // 获取配置信息
        List<FilterConfig> configs = filterConfigService.getAllActiveConfigs();
        Map<String, FilterConfig> configMaps = buildConfigMaps(configs);
        
        // 处理每个查询参数
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            String fieldKey = entry.getKey();
            List<String> values = entry.getValue();
            
            if (values == null || values.isEmpty()) {
                continue;
            }
            
            FilterConfig config = findConfigByKey(fieldKey, configMaps);
            if (config == null) {
                continue;
            }
            
            FilterExpression expression = expressionParser.parseExpression(config.getFilterExpression());
            if (!expression.isValid()) {
                continue;
            }
            
            List<QueryCondition> fieldConditions = buildFieldConditions(config.getFieldName(), expression, values);
            conditions.addAll(fieldConditions);
        }
        
        result.put("conditions", conditions);
        result.put("tableName", StringUtils.defaultIfBlank(tableName, ""));
        result.put("conditionsMap", groupConditionsByField(conditions));
        
        return result;
    }
    
    /**
     * 重载方法，不指定表名
     */
    public Map<String, Object> buildQueryConditions(Map<String, List<String>> queryParams) {
        return buildQueryConditions(queryParams, null);
    }
    
    /**
     * 构建配置映射表
     */
    private Map<String, FilterConfig> buildConfigMaps(List<FilterConfig> configs) {
        Map<String, FilterConfig> result = new HashMap<>();
        
        for (FilterConfig config : configs) {
            // 字段名映射
            result.put(config.getFieldName(), config);
            
            // 别名映射
            if (StringUtils.isNotBlank(config.getFieldAlias())) {
                result.put(config.getFieldAlias(), config);
            }
            
            // 中文名映射
            if (StringUtils.isNotBlank(config.getFieldChineseName())) {
                result.put(config.getFieldChineseName(), config);
            }
        }
        
        return result;
    }
    
    /**
     * 根据字段键查找配置
     */
    private FilterConfig findConfigByKey(String fieldKey, Map<String, FilterConfig> configMaps) {
        return configMaps.get(fieldKey);
    }
    
    /**
     * 为单个字段构建查询条件
     */
    private List<QueryCondition> buildFieldConditions(String actualFieldName, FilterExpression expression, List<String> values) {
        List<QueryCondition> conditions = new ArrayList<>();
        
        for (String value : values) {
            QueryCondition condition = buildSingleCondition(actualFieldName, expression, value);
            if (condition != null) {
                conditions.add(condition);
            }
        }
        
        return conditions;
    }
    
    /**
     * 构建单个查询条件
     */
    private QueryCondition buildSingleCondition(String actualFieldName, FilterExpression expression, String value) {
        if ("NA".equals(value)) {
            return new QueryCondition(actualFieldName, FilterOperator.NA, null);
        }
        
        FilterOperator operator = expression.getOperator();
        
        if (operator.isRangeOperator()) {
            // 区间查询：值格式为 "value1,value2"
            String[] parts = value.split(",");
            if (parts.length != 2) {
                return null;
            }
            return new QueryCondition(actualFieldName, operator, parts[0].trim(), parts[1].trim());
        } else if (operator == FilterOperator.NA) {
            return new QueryCondition(actualFieldName, FilterOperator.NA, null);
        } else {
            return new QueryCondition(actualFieldName, operator, value);
        }
    }
    
    /**
     * 按字段分组查询条件
     */
    private Map<String, List<QueryCondition>> groupConditionsByField(List<QueryCondition> conditions) {
        return conditions.stream()
                .collect(Collectors.groupingBy(QueryCondition::getFieldName));
    }
    
    /**
     * 生成SQL WHERE子句
     * @param conditions 查询条件列表
     * @param parameterMap 参数映射（输出参数）
     * @return SQL WHERE子句
     */
    public String generateWhereClause(List<QueryCondition> conditions, Map<String, Object> parameterMap) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }
        
        Map<String, List<QueryCondition>> groupedConditions = groupConditionsByField(conditions);
        List<String> fieldClauses = new ArrayList<>();
        
        for (Map.Entry<String, List<QueryCondition>> entry : groupedConditions.entrySet()) {
            String fieldName = entry.getKey();
            List<QueryCondition> fieldConditions = entry.getValue();
            
            List<String> conditionClauses = new ArrayList<>();
            for (int i = 0; i < fieldConditions.size(); i++) {
                QueryCondition condition = fieldConditions.get(i);
                String conditionSql = generateSingleConditionSql(condition, fieldName + "_" + i, parameterMap);
                conditionClauses.add(conditionSql);
            }
            
            if (!conditionClauses.isEmpty()) {
                // 同字段多条件用OR连接
                fieldClauses.add("(" + String.join(" OR ", conditionClauses) + ")");
            }
        }
        
        // 不同字段用AND连接
        return fieldClauses.isEmpty() ? "" : String.join(" AND ", fieldClauses);
    }
    
    /**
     * 生成单个条件的SQL
     */
    private String generateSingleConditionSql(QueryCondition condition, String paramKey, Map<String, Object> parameterMap) {
        String fieldName = condition.getFieldName();
        FilterOperator operator = condition.getOperator();
        
        switch (operator) {
            case EQ:
                parameterMap.put(paramKey, condition.getValue1());
                return fieldName + " = #{" + paramKey + "}";
            case NE:
                parameterMap.put(paramKey, condition.getValue1());
                return fieldName + " != #{" + paramKey + "}";
            case GT:
                parameterMap.put(paramKey, condition.getValue1());
                return fieldName + " > #{" + paramKey + "}";
            case GTE:
                parameterMap.put(paramKey, condition.getValue1());
                return fieldName + " >= #{" + paramKey + "}";
            case LT:
                parameterMap.put(paramKey, condition.getValue1());
                return fieldName + " < #{" + paramKey + "}";
            case LTE:
                parameterMap.put(paramKey, condition.getValue1());
                return fieldName + " <= #{" + paramKey + "}";
            case RANGE_CLOSED:
                parameterMap.put(paramKey + "Min", condition.getValue1());
                parameterMap.put(paramKey + "Max", condition.getValue2());
                return fieldName + " >= #{" + paramKey + "Min} AND " + fieldName + " <= #{" + paramKey + "Max}";
            case RANGE_LEFT_OPEN:
                parameterMap.put(paramKey + "Min", condition.getValue1());
                parameterMap.put(paramKey + "Max", condition.getValue2());
                return fieldName + " > #{" + paramKey + "Min} AND " + fieldName + " <= #{" + paramKey + "Max}";
            case RANGE_RIGHT_OPEN:
                parameterMap.put(paramKey + "Min", condition.getValue1());
                parameterMap.put(paramKey + "Max", condition.getValue2());
                return fieldName + " >= #{" + paramKey + "Min} AND " + fieldName + " < #{" + paramKey + "Max}";
            case RANGE_OPEN:
                parameterMap.put(paramKey + "Min", condition.getValue1());
                parameterMap.put(paramKey + "Max", condition.getValue2());
                return fieldName + " > #{" + paramKey + "Min} AND " + fieldName + " < #{" + paramKey + "Max}";
            case NA:
                return "(" + fieldName + " IS NULL OR " + fieldName + " = '')";
            default:
                return "1=1";
        }
    }
    
    /**
     * 验证查询参数
     */
    public List<String> validateQueryParameters(Map<String, List<String>> queryParams) {
        List<String> errors = new ArrayList<>();
        List<FilterConfig> configs = filterConfigService.getAllActiveConfigs();
        Map<String, FilterConfig> configMaps = buildConfigMaps(configs);
        
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            String fieldKey = entry.getKey();
            List<String> values = entry.getValue();
            
            FilterConfig config = findConfigByKey(fieldKey, configMaps);
            if (config == null) {
                errors.add("未找到字段配置: " + fieldKey);
                continue;
            }
            
            FilterExpression expression = expressionParser.parseExpression(config.getFilterExpression());
            if (!expression.isValid()) {
                errors.add("字段 " + fieldKey + " 的配置表达式无效: " + expression.getErrorMessage());
                continue;
            }
            
            // 验证值格式
            if (values != null) {
                for (String value : values) {
                    if (expression.getOperator().isRangeOperator() && !"NA".equals(value)) {
                        String[] parts = value.split(",");
                        if (parts.length != 2) {
                            errors.add("字段 " + fieldKey + " 的区间值格式错误，应为: 值1,值2");
                        }
                    }
                }
            }
        }
        
        return errors;
    }
}