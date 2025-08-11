package com.example.dynamicquery;

import com.example.dynamicquery.dto.FilterExpression;
import com.example.dynamicquery.enums.FilterOperator;
import com.example.dynamicquery.util.QueryExpressionParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QueryExpressionParserTest {

    @Autowired
    private QueryExpressionParser parser;

    @Test
    void testValidSingleValueExpressions() {
        // 测试基本比较操作符
        FilterExpression result = parser.parseExpression("age > 18");
        assertTrue(result.isValid());
        assertEquals("age", result.getFieldName());
        assertEquals(FilterOperator.GT, result.getOperator());
        assertEquals("18", result.getValue1());

        result = parser.parseExpression("name = 张三");
        assertTrue(result.isValid());
        assertEquals("name", result.getFieldName());
        assertEquals(FilterOperator.EQ, result.getOperator());
        assertEquals("张三", result.getValue1());

        result = parser.parseExpression("score >= 90.5");
        assertTrue(result.isValid());
        assertEquals("score", result.getFieldName());
        assertEquals(FilterOperator.GTE, result.getOperator());
        assertEquals("90.5", result.getValue1());
    }

    @Test
    void testValidRangeExpressions() {
        // 测试闭区间
        FilterExpression result = parser.parseExpression("age >= 18 且 <= 65");
        assertTrue(result.isValid());
        assertEquals("age", result.getFieldName());
        assertEquals(FilterOperator.RANGE_CLOSED, result.getOperator());
        assertEquals("18", result.getValue1());
        assertEquals("65", result.getValue2());

        // 测试左开右闭
        result = parser.parseExpression("salary > 5000 且 <= 10000");
        assertTrue(result.isValid());
        assertEquals(FilterOperator.RANGE_LEFT_OPEN, result.getOperator());

        // 测试左闭右开
        result = parser.parseExpression("score >= 60 且 < 100");
        assertTrue(result.isValid());
        assertEquals(FilterOperator.RANGE_RIGHT_OPEN, result.getOperator());

        // 测试开区间
        result = parser.parseExpression("experience > 1 且 < 5");
        assertTrue(result.isValid());
        assertEquals(FilterOperator.RANGE_OPEN, result.getOperator());
    }

    @Test
    void testValidNAExpression() {
        FilterExpression result = parser.parseExpression("phone NA");
        assertTrue(result.isValid());
        assertEquals("phone", result.getFieldName());
        assertEquals(FilterOperator.NA, result.getOperator());
    }

    @Test
    void testInvalidExpressions() {
        FilterExpression result = parser.parseExpression("");
        assertFalse(result.isValid());
        assertEquals("表达式不能为空", result.getErrorMessage());

        result = parser.parseExpression("age >> 18");
        assertFalse(result.isValid());
        assertEquals("表达式格式不正确", result.getErrorMessage());

        result = parser.parseExpression("age >= 18 且");
        assertFalse(result.isValid());
        assertEquals("表达式格式不正确", result.getErrorMessage());
    }

    @Test
    void testValidateExpression() {
        assertTrue(parser.validateExpression("age > 18"));
        assertTrue(parser.validateExpression("name = 张三"));
        assertTrue(parser.validateExpression("salary >= 5000 且 <= 10000"));
        assertTrue(parser.validateExpression("phone NA"));
        
        assertFalse(parser.validateExpression(""));
        assertFalse(parser.validateExpression("invalid expression"));
    }

    @Test
    void testGetExamples() {
        var examples = parser.getValidExpressionExamples();
        assertFalse(examples.isEmpty());
        assertTrue(examples.stream().anyMatch(e -> e.contains("且")));
    }
}