package com.milesight.beaveriot.base.error;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.enums.ValidationErrorCode;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.base.utils.StringUtils;
import lombok.extern.slf4j.*;
import org.springframework.validation.BindingResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BindingResultResolver {

    private static final Map<Class<? extends Annotation>, List<Method>> ANNOTATION_METHOD_CACHE = new ConcurrentHashMap<>();

    private static final Set<String> ANNOTATION_VALUE_GETTERS = Set.of("min", "max", "value", "regexp");

    private BindingResultResolver() {
        throw new IllegalStateException("Utility class");
    }

    public static ResponseBody<Object> convertToErrorResponse(BindingResult bindingResult) {
        var errorHolders = new ArrayList<>();
        var target = bindingResult.getTarget();
        var fieldErrors = bindingResult.getFieldErrors();
        if (fieldErrors.isEmpty()) {
            if (bindingResult instanceof Throwable e) {
                return ResponseBuilder.fail(ErrorCode.PARAMETER_VALIDATION_FAILED, e.getMessage());
            } else {
                return ResponseBuilder.fail(ErrorCode.PARAMETER_VALIDATION_FAILED);
            }
        }

        for (var fieldError : fieldErrors) {
            var args = new HashMap<String, Object>();
            var fieldName = fieldError.getField();
            args.put("field", StringUtils.toSnakeCase(fieldName));

            var nestedPath = StringUtils.toSnakeCase(bindingResult.getNestedPath() + fieldName);
            args.put("path", nestedPath);

            var annotationName = fieldError.getCode();
            collectAnnotationValues(target, fieldName, annotationName, args);

            var errorCode = Optional.ofNullable(ValidationErrorCode.fromValidationCode(annotationName))
                    .map(ValidationErrorCode::getErrorCode)
                    .orElseGet(ErrorCode.PARAMETER_VALIDATION_FAILED::getErrorCode);
            var errorMessage = fieldError.getDefaultMessage();
            var errorHolder = ErrorHolder.of(errorCode, errorMessage, args);
            errorHolders.add(errorHolder);
        }

        return ResponseBuilder.fail(ErrorCode.MULTIPLE_ERROR, errorHolders);
    }

    @SuppressWarnings({"java:S3011"})
    private static void collectAnnotationValues(Object target, String fieldName, String annotationName, HashMap<String, Object> args) {
        try {
            if (target != null) {
                var field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                var annotations = field.getAnnotations();
                for (var annotation : annotations) {
                    var simpleName = annotation.annotationType().getSimpleName();
                    if (simpleName.equals(annotationName)) {
                        var methods = getAnnotationMethods(annotation.annotationType());
                        for (var method : methods) {
                            var value = method.invoke(annotation);
                            args.put(StringUtils.toSnakeCase(method.getName()), value);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get annotation args: {}", fieldName, e);
        }
    }

    private static List<Method> getAnnotationMethods(Class<? extends Annotation> annotationType) {
        var cachedMethods = ANNOTATION_METHOD_CACHE.get(annotationType);
        if (cachedMethods != null) {
            return cachedMethods;
        }

        var methods = Arrays.stream(annotationType.getDeclaredMethods())
                .filter(method -> ANNOTATION_VALUE_GETTERS.contains(method.getName()))
                .toList();

        ANNOTATION_METHOD_CACHE.put(annotationType, methods);
        return methods;
    }

}
