package com.milesight.beaveriot.context.support;

import com.milesight.beaveriot.base.utils.StringUtils;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.ObjectUtils;
import org.springframework.util.SystemPropertyUtils;

import java.lang.reflect.Field;
import java.util.Map;

import static com.milesight.beaveriot.context.integration.entity.annotation.AnnotationEntityLoader.REPLACE_HOLDER_FIELD_NAME;

/**
 * @author leon
 */
public class EnhancePropertySourcesPropertyResolver extends PropertySourcesPropertyResolver {
    /**
     * Create a new resolver against the given property sources.
     *
     * @param propertySources the set of {@link PropertySources} objects to use
     */
    public EnhancePropertySourcesPropertyResolver(PropertySources propertySources) {
        super(propertySources);
    }

    public String resolvePlaceholders(String text, Map<String, Object> propertySource) {

        String value = super.resolvePlaceholders(text);
        if (value.contains(SystemPropertyUtils.PLACEHOLDER_PREFIX) && !ObjectUtils.isEmpty(propertySource)) {
            String removePlaceholder = text.replace(SystemPropertyUtils.PLACEHOLDER_PREFIX, "").replace(SystemPropertyUtils.PLACEHOLDER_SUFFIX, "");
            return (String) propertySource.get(removePlaceholder);
        } else {
            return value;
        }

    }

    public String resolvePlaceholders(String value, Field field) {
        Map<String, Object> dynamicProperties = Map.of(REPLACE_HOLDER_FIELD_NAME, StringUtils.toSnakeCase(field.getName()));
        return resolvePlaceholders(value, dynamicProperties);
    }

}
