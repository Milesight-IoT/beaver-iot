package com.milesight.beaveriot.rule.enums;


import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
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
    IS_NOT_EMPTY("is not empty", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).isNotEmpty({0})"),
    GT("greater than", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).greaterThan({0},{1})"),
    GE("greater equals", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).greaterEquals({0},{1})"),
    LT("less than", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).lessThan({0},{1})"),
    LE("less equals", "T(com.milesight.beaveriot.rule.enums.ExpressionOperator).lessEquals({0},{1})");
    private String label;
    private String expression;

    public static final String EXPRESSION_PREFIX = "T(com.milesight.beaveriot.rule.enums.ExpressionOperator)";

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
            throw new IllegalArgumentException("Unsupported expression type, StartWith only supports strings ");
        }
    }

    public static boolean endsWith(Object str, Object suffix) {
        if (str instanceof CharSequence strValue && suffix instanceof CharSequence suffixValue) {
            return StringUtils.endsWith(strValue, suffixValue);
        } else {
            throw new IllegalArgumentException("Unsupported expression type, StartWith only supports strings ");
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
            throw new IllegalArgumentException("Unsupported expression type, contains only supports strings or arrays ");
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

    public static boolean notEquals(Object object1, Object object2) {
        return !equals(object1, object2);
    }

    public static boolean equals(Object object1, Object object2) {
        if ((object1 != null && object2 != null) && !object1.getClass().equals(object2.getClass())) {
            return object1.toString().equals(object2.toString());
        }
        return Objects.equals(object1, object2);
    }

    public static boolean greaterThan(Object object1, Object object2) {
        if (object1 == null || object2 == null) {
            return false;
        }
        return doCompare(object1, object2) > 0;
    }

    public static boolean greaterEquals(Object object1, Object object2) {
        if (object1 == null || object2 == null) {
            return false;
        }
        return doCompare(object1, object2) >= 0;
    }

    public static boolean lessThan(Object object1, Object object2) {
        if (object1 == null || object2 == null) {
            return false;
        }
        return doCompare(object1, object2) < 0;
    }

    public static boolean lessEquals(Object object1, Object object2) {
        if (object1 == null || object2 == null) {
            return false;
        }
        return doCompare(object1, object2) <= 0;
    }

    private static int doCompare(@Nonnull Object object1, @Nonnull Object object2) {
        if (!(object1 instanceof Number || NumberUtils.isCreatable(object1.toString()))
                || !(object2 instanceof Number || NumberUtils.isCreatable(object2.toString()))) {
            throw new IllegalArgumentException("Unsupported expression type, value comparison only supports numbers");
        }
        return ObjectUtils.compare(new BigDecimal(object1.toString()), new BigDecimal(object2.toString()));
    }

}
