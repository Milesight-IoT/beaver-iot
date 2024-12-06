package com.milesight.beaveriot.context.util;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.stream.IntStream;

/**
 * @author loong
 */
public class AnnotationSpelExpressionUtil {

    private static final ParameterNameDiscoverer DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

    public static String getSpelKeyValue(JoinPoint joinPoint, String key) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        EvaluationContext context = setUpContext(target, method, args);
        return key.contains("#") ? new SpelExpressionParser().parseExpression(key).getValue(context, String.class) : key;
    }

    private static EvaluationContext setUpContext(Object target, Method method, Object[] args) {
        EvaluationContext context = new MethodBasedEvaluationContext(target, method, args, DISCOVERER);
        String[] names = DISCOVERER.getParameterNames(method);
        if (names != null) {
            IntStream.range(0, names.length).forEach((i) -> {
                context.setVariable(names[i], args[i]);
            });
        }
        return context;
    }

}
