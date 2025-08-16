package com.mycompany.dynamicquery.mapper;

import com.mycompany.dynamicquery.entity.DataRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DataRecordMapper {
    
    int insert(DataRecord record);
    
    int updateById(DataRecord record);
    
    int deleteById(Long id);
    
    DataRecord selectById(Long id);
    
    List<DataRecord> selectAll();
    
    List<DataRecord> selectByDynamicConditions(@Param("conditions") Map<String, Object> conditions);
    
    int countByDynamicConditions(@Param("conditions") Map<String, Object> conditions);
}