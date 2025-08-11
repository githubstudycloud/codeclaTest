package com.example.dynamicquery.mapper;

import com.example.dynamicquery.entity.FilterConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FilterConfigMapper {
    
    int insert(FilterConfig config);
    
    int updateById(FilterConfig config);
    
    int deleteById(Long id);
    
    FilterConfig selectById(Long id);
    
    List<FilterConfig> selectAll();
    
    List<FilterConfig> selectActiveConfigs();
    
    FilterConfig selectByFieldName(@Param("fieldName") String fieldName);
    
    List<FilterConfig> selectByFieldNames(@Param("fieldNames") List<String> fieldNames);
}