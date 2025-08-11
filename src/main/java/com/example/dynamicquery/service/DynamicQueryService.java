package com.example.dynamicquery.service;

import com.example.dynamicquery.dto.QueryCondition;
import com.example.dynamicquery.entity.DataRecord;
import com.example.dynamicquery.mapper.DataRecordMapper;
import com.example.dynamicquery.util.DynamicQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DynamicQueryService {

    @Autowired
    private DataRecordMapper dataRecordMapper;

    @Autowired
    private QueryConditionBuilder queryConditionBuilder;

    public List<DataRecord> queryByConditions(Map<String, List<String>> queryParams) {
        Map<String, Object> buildResult = queryConditionBuilder.buildQueryConditions(queryParams);
        @SuppressWarnings("unchecked")
        List<QueryCondition> conditions = (List<QueryCondition>) buildResult.get("conditions");
        
        if (conditions.isEmpty()) {
            return dataRecordMapper.selectAll();
        }
        
        Map<String, Object> mybatisConditions = convertToMybatisConditions(conditions);
        return dataRecordMapper.selectByDynamicConditions(mybatisConditions);
    }

    public int countByConditions(Map<String, List<String>> queryParams) {
        Map<String, Object> buildResult = queryConditionBuilder.buildQueryConditions(queryParams);
        @SuppressWarnings("unchecked")
        List<QueryCondition> conditions = (List<QueryCondition>) buildResult.get("conditions");
        
        if (conditions.isEmpty()) {
            return dataRecordMapper.selectAll().size();
        }
        
        Map<String, Object> mybatisConditions = convertToMybatisConditions(conditions);
        return dataRecordMapper.countByDynamicConditions(mybatisConditions);
    }

    private Map<String, Object> convertToMybatisConditions(List<QueryCondition> conditions) {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, List<QueryCondition>> groupedConditions = conditions.stream()
                .collect(Collectors.groupingBy(QueryCondition::getFieldName));
        
        for (Map.Entry<String, List<QueryCondition>> entry : groupedConditions.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        
        return result;
    }

    public DataRecord saveRecord(DataRecord record) {
        if (record.getId() == null) {
            dataRecordMapper.insert(record);
        } else {
            dataRecordMapper.updateById(record);
        }
        return record;
    }

    public void deleteRecord(Long id) {
        dataRecordMapper.deleteById(id);
    }

    public DataRecord getRecordById(Long id) {
        return dataRecordMapper.selectById(id);
    }

    public List<DataRecord> getAllRecords() {
        return dataRecordMapper.selectAll();
    }
}