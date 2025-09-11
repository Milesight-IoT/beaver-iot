package com.milesight.beaveriot.context.support.function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.*;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: Luxb
 * create: 2025/9/11 13:55
 **/
@Slf4j
public class SpELTemplateEvaluator {
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{\\s*([^}]*)\\s*}");
    private final ExpressionParser parser = new SpelExpressionParser();
    private final StandardEvaluationContext context = new StandardEvaluationContext();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    private static final class InstanceHolder {
        private static final SpELTemplateEvaluator instance = new SpELTemplateEvaluator();
    }

    public static SpELTemplateEvaluator getInstance() {
        return InstanceHolder.instance;
    }

    private SpELTemplateEvaluator() {
        registerFunctions();

        List<PropertyAccessor> accessors = new ArrayList<>();
        accessors.add(new ReflectivePropertyAccessor());
        accessors.add(new MapAccessor());
        context.setPropertyAccessors(accessors);
    }

    private void registerFunctions() {
        SpELTemplateFunction yamlFunction = new YamlFunction();
        Map<String, Object> root = new HashMap<>();
        root.put(yamlFunction.getIdentifier(), yamlFunction);
        context.setRootObject(root);
    }

    public Object evaluate(Object input) {
        if (input == null) {
            return null;
        }

        if (!(input instanceof String inputString)) {
            return input;
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher(inputString);
        if (!matcher.find()) {
            return inputString;
        }

        StringBuilder result = new StringBuilder();
        matcher.reset();

        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            try {
                Expression expression = expressionCache.computeIfAbsent(expr, parser::parseExpression);
                Object value = expression.getValue(context);
                String replacement = value != null ? value.toString() : "";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                throw new RuntimeException("Error occurred while evaluating expression: " + expr + ", caused by: " + e.getMessage(), e);
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static class MapAccessor implements PropertyAccessor {
        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class[]{Map.class};
        }

        @Override
        public boolean canRead(@NonNull EvaluationContext context, Object target, @NonNull String name) {
            return target instanceof Map && ((Map<?, ?>) target).containsKey(name);
        }

        @Override
        @NonNull
        public TypedValue read(@NonNull EvaluationContext context, Object target, @NonNull String name) {
            Map<?, ?> map = (Map<?, ?>) target;
            if (map.containsKey(name)) {
                return new TypedValue(map.get(name));
            }
            throw new SpelEvaluationException(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, name);
        }

        @Override
        public boolean canWrite(@NonNull EvaluationContext context, Object target, @NonNull String name) {
            return false;
        }

        @Override
        public void write(@NonNull EvaluationContext context, Object target, @NonNull String name, Object newValue) {
            throw new UnsupportedOperationException("Write not supported");
        }
    }
}