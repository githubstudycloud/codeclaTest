package com.mycompany.dynamicquery.service;

import com.mycompany.dynamicquery.dto.FilterExpression;
import com.mycompany.dynamicquery.entity.FilterConfig;
import com.mycompany.dynamicquery.mapper.FilterConfigMapper;
import com.mycompany.dynamicquery.util.FilterExpressionValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FilterConfigService {

    @Autowired
    private FilterConfigMapper filterConfigMapper;

    @Autowired
    private FilterExpressionValidator validator;

    @Transactional
    public FilterConfig saveConfig(FilterConfig config) {
        validateFilterExpression(config.getFilterExpression());
        
        if (config.getId() == null) {
            filterConfigMapper.insert(config);
        } else {
            filterConfigMapper.updateById(config);
        }
        return config;
    }

    public void deleteConfig(Long id) {
        filterConfigMapper.deleteById(id);
    }

    public FilterConfig getConfigById(Long id) {
        return filterConfigMapper.selectById(id);
    }

    public List<FilterConfig> getAllConfigs() {
        return filterConfigMapper.selectAll();
    }

    public List<FilterConfig> getAllActiveConfigs() {
        return filterConfigMapper.selectActiveConfigs();
    }

    public FilterConfig getConfigByFieldName(String fieldName) {
        return filterConfigMapper.selectByFieldName(fieldName);
    }

    public void validateFilterExpression(String expression) {
        if (StringUtils.isBlank(expression)) {
            throw new IllegalArgumentException("表达式不能为空");
        }
        
        FilterExpression parsed = validator.parseExpression(expression);
        if (!parsed.isValid()) {
            String examples = String.join("\\n", validator.getValidExpressionExamples());
            throw new IllegalArgumentException(
                "表达式格式不正确: " + parsed.getErrorMessage() + 
                "\\n\\n正确的表达式格式示例:\\n" + examples
            );
        }
    }

    public List<String> getExpressionExamples() {
        return validator.getValidExpressionExamples();
    }
}