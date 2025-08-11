package com.example.dynamicquery;

import com.example.dynamicquery.dto.FilterExpression;
import com.example.dynamicquery.enums.FilterOperator;
import com.example.dynamicquery.util.FilterExpressionValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilterExpressionValidatorTest {

    private final FilterExpressionValidator validator = new FilterExpressionValidator();

    @Test
    void testValidSingleValueExpressions() {
        // 测试基本比较操作符
        FilterExpression result = validator.parseExpression("age > 18");
        assertTrue(result.isValid());
        assertEquals("age", result.getFieldName());
        assertEquals(FilterOperator.GT, result.getOperator());
        assertEquals("18", result.getValue1());

        result = validator.parseExpression("name = 张三");
        assertTrue(result.isValid());
        assertEquals("name", result.getFieldName());
        assertEquals(FilterOperator.EQ, result.getOperator());
        assertEquals("张三", result.getValue1());

        result = validator.parseExpression("score >= 90.5");
        assertTrue(result.isValid());
        assertEquals("score", result.getFieldName());
        assertEquals(FilterOperator.GTE, result.getOperator());
        assertEquals("90.5", result.getValue1());
    }

    @Test
    void testValidBetweenExpressions() {
        FilterExpression result = validator.parseExpression("age BETWEEN 18,65");
        assertTrue(result.isValid());
        assertEquals("age", result.getFieldName());
        assertEquals(FilterOperator.BETWEEN, result.getOperator());
        assertEquals("18", result.getValue1());
        assertEquals("65", result.getValue2());

        result = validator.parseExpression("salary BETWEEN_LO 5000,10000");
        assertTrue(result.isValid());
        assertEquals(FilterOperator.BETWEEN_LEFT_OPEN, result.getOperator());

        result = validator.parseExpression("score BETWEEN_RO 60,100");
        assertTrue(result.isValid());
        assertEquals(FilterOperator.BETWEEN_RIGHT_OPEN, result.getOperator());

        result = validator.parseExpression("experience BETWEEN_O 1,5");
        assertTrue(result.isValid());
        assertEquals(FilterOperator.BETWEEN_OPEN, result.getOperator());
    }

    @Test
    void testValidNAExpression() {
        FilterExpression result = validator.parseExpression("phone NA");
        assertTrue(result.isValid());
        assertEquals("phone", result.getFieldName());
        assertEquals(FilterOperator.NA, result.getOperator());
    }

    @Test
    void testInvalidExpressions() {
        FilterExpression result = validator.parseExpression("");
        assertFalse(result.isValid());
        assertEquals("表达式不能为空", result.getErrorMessage());

        result = validator.parseExpression("age >> 18");
        assertFalse(result.isValid());
        assertEquals("表达式格式不正确", result.getErrorMessage());

        result = validator.parseExpression("age BETWEEN 18");
        assertFalse(result.isValid());
        assertEquals("表达式格式不正确", result.getErrorMessage());
    }

    @Test
    void testValidateExpression() {
        assertTrue(validator.validateExpression("age > 18"));
        assertTrue(validator.validateExpression("name = 张三"));
        assertTrue(validator.validateExpression("salary BETWEEN 5000,10000"));
        assertTrue(validator.validateExpression("phone NA"));
        
        assertFalse(validator.validateExpression(""));
        assertFalse(validator.validateExpression("invalid expression"));
    }
}