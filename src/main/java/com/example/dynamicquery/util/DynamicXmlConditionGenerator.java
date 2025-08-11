package com.example.dynamicquery.util;

import com.example.dynamicquery.enums.FilterOperator;
import org.springframework.stereotype.Component;

/**
 * 动态XML条件生成器
 * 用于生成MyBatis XML中的动态SQL条件
 */
@Component
public class DynamicXmlConditionGenerator {
    
    /**
     * 生成字段的所有条件XML
     * @param fieldName 字段名
     * @param conditionVariable 条件变量名（如：ageCondition）
     * @return XML条件字符串
     */
    public String generateFieldConditions(String fieldName, String conditionVariable) {
        StringBuilder xml = new StringBuilder();
        
        // 等值条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'EQ'\">\n");
        xml.append("                        ").append(fieldName).append(" = #{").append(conditionVariable).append(".value1}\n");
        xml.append("                    </if>\n");
        
        // 不等值条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'NE'\">\n");
        xml.append("                        ").append(fieldName).append(" != #{").append(conditionVariable).append(".value1}\n");
        xml.append("                    </if>\n");
        
        // 大于条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'GT'\">\n");
        xml.append("                        ").append(fieldName).append(" > #{").append(conditionVariable).append(".value1}\n");
        xml.append("                    </if>\n");
        
        // 大于等于条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'GTE'\">\n");
        xml.append("                        ").append(fieldName).append(" >= #{").append(conditionVariable).append(".value1}\n");
        xml.append("                    </if>\n");
        
        // 小于条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'LT'\">\n");
        xml.append("                        ").append(fieldName).append(" &lt; #{").append(conditionVariable).append(".value1}\n");
        xml.append("                    </if>\n");
        
        // 小于等于条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'LTE'\">\n");
        xml.append("                        ").append(fieldName).append(" &lt;= #{").append(conditionVariable).append(".value1}\n");
        xml.append("                    </if>\n");
        
        // 闭区间条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'RANGE_CLOSED'\">\n");
        xml.append("                        ").append(fieldName).append(" >= #{").append(conditionVariable).append(".value1} AND ").append(fieldName).append(" &lt;= #{").append(conditionVariable).append(".value2}\n");
        xml.append("                    </if>\n");
        
        // 左开右闭区间条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'RANGE_LEFT_OPEN'\">\n");
        xml.append("                        ").append(fieldName).append(" > #{").append(conditionVariable).append(".value1} AND ").append(fieldName).append(" &lt;= #{").append(conditionVariable).append(".value2}\n");
        xml.append("                    </if>\n");
        
        // 左闭右开区间条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'RANGE_RIGHT_OPEN'\">\n");
        xml.append("                        ").append(fieldName).append(" >= #{").append(conditionVariable).append(".value1} AND ").append(fieldName).append(" &lt; #{").append(conditionVariable).append(".value2}\n");
        xml.append("                    </if>\n");
        
        // 开区间条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'RANGE_OPEN'\">\n");
        xml.append("                        ").append(fieldName).append(" > #{").append(conditionVariable).append(".value1} AND ").append(fieldName).append(" &lt; #{").append(conditionVariable).append(".value2}\n");
        xml.append("                    </if>\n");
        
        // 空值条件
        xml.append("                    <if test=\"").append(conditionVariable).append(".operator.name() == 'NA'\">\n");
        if ("String".equals(getFieldType(fieldName))) {
            xml.append("                        (").append(fieldName).append(" IS NULL OR ").append(fieldName).append(" = '')\n");
        } else {
            xml.append("                        ").append(fieldName).append(" IS NULL\n");
        }
        xml.append("                    </if>\n");
        
        return xml.toString();
    }
    
    /**
     * 生成完整字段查询块
     * @param fieldName 数据库字段名
     * @param conditionsKey 条件集合的键名（如：conditions.age）
     * @param conditionVariable 单个条件的变量名（如：ageCondition）
     * @return 完整的XML查询块
     */
    public String generateFieldQueryBlock(String fieldName, String conditionsKey, String conditionVariable) {
        StringBuilder xml = new StringBuilder();
        
        xml.append("            <if test=\"").append(conditionsKey).append(" != null\">\n");
        xml.append("                AND (\n");
        xml.append("                <foreach collection=\"").append(conditionsKey).append("\" item=\"").append(conditionVariable).append("\" separator=\" OR \">\n");
        xml.append(generateFieldConditions(fieldName, conditionVariable));
        xml.append("                </foreach>\n");
        xml.append("                )\n");
        xml.append("            </if>\n");
        
        return xml.toString();
    }
    
    /**
     * 为复合字段生成查询条件（保留小数处理）
     */
    public String generateCompositeFieldCondition(String fieldName, String conditionVariable, String decimalFormat) {
        StringBuilder xml = new StringBuilder();
        
        // 如果需要保留小数，使用ROUND函数
        String fieldExpression = fieldName;
        if (decimalFormat != null && !decimalFormat.isEmpty()) {
            fieldExpression = "ROUND(" + fieldName + ", " + decimalFormat + ")";
        }
        
        xml.append("                    <choose>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'EQ'\">\n");
        xml.append("                            ").append(fieldExpression).append(" = #{").append(conditionVariable).append(".value1}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'NE'\">\n");
        xml.append("                            ").append(fieldExpression).append(" != #{").append(conditionVariable).append(".value1}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'GT'\">\n");
        xml.append("                            ").append(fieldExpression).append(" > #{").append(conditionVariable).append(".value1}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'GTE'\">\n");
        xml.append("                            ").append(fieldExpression).append(" >= #{").append(conditionVariable).append(".value1}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'LT'\">\n");
        xml.append("                            ").append(fieldExpression).append(" &lt; #{").append(conditionVariable).append(".value1}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'LTE'\">\n");
        xml.append("                            ").append(fieldExpression).append(" &lt;= #{").append(conditionVariable).append(".value1}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'RANGE_CLOSED'\">\n");
        xml.append("                            ").append(fieldExpression).append(" >= #{").append(conditionVariable).append(".value1} AND ").append(fieldExpression).append(" &lt;= #{").append(conditionVariable).append(".value2}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'RANGE_LEFT_OPEN'\">\n");
        xml.append("                            ").append(fieldExpression).append(" > #{").append(conditionVariable).append(".value1} AND ").append(fieldExpression).append(" &lt;= #{").append(conditionVariable).append(".value2}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'RANGE_RIGHT_OPEN'\">\n");
        xml.append("                            ").append(fieldExpression).append(" >= #{").append(conditionVariable).append(".value1} AND ").append(fieldExpression).append(" &lt; #{").append(conditionVariable).append(".value2}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'RANGE_OPEN'\">\n");
        xml.append("                            ").append(fieldExpression).append(" > #{").append(conditionVariable).append(".value1} AND ").append(fieldExpression).append(" &lt; #{").append(conditionVariable).append(".value2}\n");
        xml.append("                        </when>\n");
        xml.append("                        <when test=\"").append(conditionVariable).append(".operator.name() == 'NA'\">\n");
        xml.append("                            ").append(fieldName).append(" IS NULL\n");
        xml.append("                        </when>\n");
        xml.append("                    </choose>\n");
        
        return xml.toString();
    }
    
    /**
     * 获取字段类型（简单实现，可根据需要扩展）
     */
    private String getFieldType(String fieldName) {
        // 字符串类型字段
        if (fieldName.contains("name") || fieldName.contains("status") || 
            fieldName.contains("email") || fieldName.contains("phone") || 
            fieldName.contains("department")) {
            return "String";
        }
        return "Other";
    }
}