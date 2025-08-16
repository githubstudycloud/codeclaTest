package com.mycompany.dynamicquery;

import com.mycompany.dynamicquery.dto.FilterExpression;
import com.mycompany.dynamicquery.enums.FilterOperator;
import com.mycompany.dynamicquery.util.QueryExpressionParser;
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
        // 测试闭区间：值1 <= 字段名 <= 值2
        FilterExpression result = parser.parseExpression("18 <= age <= 65");
        assertTrue(result.isValid());
        assertEquals("age", result.getFieldName());
        assertEquals(FilterOperator.RANGE_CLOSED, result.getOperator());
        assertEquals("18", result.getValue1());
        assertEquals("65", result.getValue2());

        // 测试左开右闭：值1 < 字段名 <= 值2
        result = parser.parseExpression("5000 < salary <= 10000");
        assertTrue(result.isValid());
        assertEquals("salary", result.getFieldName());
        assertEquals(FilterOperator.RANGE_LEFT_OPEN, result.getOperator());
        assertEquals("5000", result.getValue1());
        assertEquals("10000", result.getValue2());

        // 测试左闭右开：值1 <= 字段名 < 值2
        result = parser.parseExpression("60 <= score < 100");
        assertTrue(result.isValid());
        assertEquals("score", result.getFieldName());
        assertEquals(FilterOperator.RANGE_RIGHT_OPEN, result.getOperator());
        assertEquals("60", result.getValue1());
        assertEquals("100", result.getValue2());

        // 测试开区间：值1 < 字段名 < 值2
        result = parser.parseExpression("1 < experience < 5");
        assertTrue(result.isValid());
        assertEquals("experience", result.getFieldName());
        assertEquals(FilterOperator.RANGE_OPEN, result.getOperator());
        assertEquals("1", result.getValue1());
        assertEquals("5", result.getValue2());
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
        assertTrue(parser.validateExpression("5000 <= salary <= 10000"));
        assertTrue(parser.validateExpression("phone NA"));
        
        assertFalse(parser.validateExpression(""));
        assertFalse(parser.validateExpression("invalid expression"));
    }

    @Test
    void testGetExamples() {
        var examples = parser.getValidExpressionExamples();
        assertFalse(examples.isEmpty());
        assertTrue(examples.stream().anyMatch(e -> e.contains("<=")));
    }

    @Test
    void testDefaultFieldExpressions() {
        // 测试默认字段（无字段名指定）
        FilterExpression result = parser.parseExpression("> 18");
        assertTrue(result.isValid());
        assertEquals("field", result.getFieldName());
        assertEquals(FilterOperator.GT, result.getOperator());
        assertEquals("18", result.getValue1());

        result = parser.parseExpression("= active");
        assertTrue(result.isValid());
        assertEquals("field", result.getFieldName());
        assertEquals(FilterOperator.EQ, result.getOperator());
        assertEquals("active", result.getValue1());

        result = parser.parseExpression("NA");
        assertTrue(result.isValid());
        assertEquals("field", result.getFieldName());
        assertEquals(FilterOperator.NA, result.getOperator());
    }
}