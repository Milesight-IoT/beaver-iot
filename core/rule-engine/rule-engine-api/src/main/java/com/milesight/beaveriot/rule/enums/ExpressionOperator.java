package com.milesight.beaveriot.rule.enums;


import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author leon
 */
public enum ExpressionOperator {
    CONTAINS("contains", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).contains({0},{1})"),
    NOT_CONTAINS("not contains", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).notContains({0},{1})"),
    START_WITH("start with", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).startsWith({0},{1})"),
    END_WITH("end with", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).endsWith({0},{1})"),
    EQ("is", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).equals({0},{1})"),
    NE("is not", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).notEquals({0},{1})"),
    IS_EMPTY("is empty", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).isEmpty({0})"),
    IS_NOT_EMPTY("is not empty", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).isNotEmpty({0})");
    private String label;
    private String expression;

    ExpressionOperator(String label, String expression) {
        this.label = label;
        this.expression = expression;
    }

    public String getLabel() {
        return label;
    }

    public String getExpression() {
        return expression;
    }

    public static boolean startsWith(Object str, Object prefix) {
        if (str instanceof CharSequence strValue && prefix instanceof CharSequence prefixValue) {
            return StringUtils.startsWith(strValue, prefixValue);
        } else {
            return false;
        }
    }

    public static boolean endsWith(Object str, Object suffix) {
        if (str instanceof CharSequence strValue && suffix instanceof CharSequence suffixValue) {
            return StringUtils.startsWith(strValue, suffixValue);
        } else {
            return false;
        }
    }

    public static boolean contains(Object str, Object searchSeq) {
        if (str instanceof CharSequence strValue && searchSeq instanceof CharSequence searchSeqValue) {
            return StringUtils.contains(strValue, searchSeqValue);
        } else if (str instanceof Object[] objects) {
            return ArrayUtils.contains(objects, searchSeq);
        } else if (str instanceof List<?> list) {
            return list.contains(searchSeq);
        } else {
            return false;
        }
    }

    public static boolean notContains(Object str, Object searchSeq) {
        return !contains(str, searchSeq);
    }

    public static boolean isNotEmpty(Object value) {
        return ObjectUtils.isNotEmpty(value);
    }

    public static boolean isEmpty(Object value) {
        return ObjectUtils.isEmpty(value);
    }

    public static boolean notEqual(Object object1, Object object2) {
        return ObjectUtils.notEqual(object1, object2);
    }

    public static boolean equals(Object object1, Object object2) {
        return Objects.equals(object1, object2);
    }

}
