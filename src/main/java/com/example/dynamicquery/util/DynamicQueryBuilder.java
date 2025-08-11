package com.example.dynamicquery.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 动态查询构建器（兼容性保持）
 * @deprecated 请使用 QueryConditionBuilder 代替
 */
@Component
@Deprecated
public class DynamicQueryBuilder {

    @Autowired
    private QueryConditionBuilder queryConditionBuilder;

    public Map<String, Object> buildQueryConditions(Map<String, List<String>> queryParams) {
        return queryConditionBuilder.buildQueryConditions(queryParams);
    }
}