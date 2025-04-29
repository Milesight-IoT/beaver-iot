package com.milesight.beaveriot.rule.flow.definition;

import com.milesight.beaveriot.base.utils.TypeUtil;
import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.model.definition.*;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.rule.utils.ComponentDefinitionHelper;
import com.milesight.beaveriot.rule.utils.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Category;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.isStatic;

/**
 * @author leon
 */
@Order(ComponentDefinitionLoader.ORDER_LEVEL_ANNOTATION)
@Slf4j
@SuppressWarnings({"java:S3776", "java:S6541"})
public class AnnotationComponentDefinitionLoader implements ComponentDefinitionLoader, ApplicationContextAware {

    private static final String[] IGNORE_PROPERTIES = {"bridgeErrorHandler", "exceptionHandler", "exchangePattern", "lazyStartProducer", "autowiredEnabled"};
    private static final Map<String, Class<?>> KNOWN_CLASSES_CACHE = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    @Override
    public String loadComponentDefinitionSchema(String name) throws IOException {

        if (!applicationContext.containsBean(name)) {
            return null;
        }

        Object bean = applicationContext.getBean(name);
        if (!bean.getClass().isAnnotationPresent(RuleNode.class)) {
            log.debug("Component {} is not a rule node", name);
            return null;
        }

        Class<?> componentClazz = bean.getClass();
        ComponentDefinition model = new ComponentDefinition();
        ComponentBaseDefinition baseDefinition = new ComponentBaseDefinition();
        model.setComponent(baseDefinition);
        baseDefinition.setJavaType(componentClazz.getName());

        UriEndpoint uriEndpoint = componentClazz.getAnnotation(UriEndpoint.class);
        RuleNode ruleNode = componentClazz.getAnnotation(RuleNode.class);

        //fill component properties
        fillComponentProperties(model, uriEndpoint);
        fillComponentProperties(model, ruleNode, componentClazz);

        //add endpoint headers
        final Class<?> headersClass = retrieveHeaderClass(ruleNode, uriEndpoint);
        if (headersClass != null && headersClass != void.class) {
            addEndpointHeaders(model, baseDefinition.getScheme(), headersClass, uriEndpoint);
        }

        //add exchange properties
        final Class<?> propertiesClass = retrieveValueOrderly("propertiesClass", ruleNode);
        if (propertiesClass != null && propertiesClass != void.class) {
            addExchangeProperties(model, baseDefinition.getScheme(), propertiesClass);
        }

        //add parameter properties and path properties
        addParameterProperties(model, componentClazz);

        //add output argument properties
        addOutputArgumentProperties(model, componentClazz);

        return JsonHelper.toJSON(model);
    }

    protected void fillComponentProperties(ComponentDefinition model, RuleNode ruleNode, Class<?> componentClazz) {
        ComponentBaseDefinition baseDefinition = model.getComponent();
        baseDefinition.setTestable(ruleNode.testable());
        baseDefinition.setType(ruleNode.type());
        if (!ObjectUtils.isEmpty(ruleNode.description())) {
            baseDefinition.setDescription(ruleNode.description());
        }
        if (!ObjectUtils.isEmpty(ruleNode.title())) {
            baseDefinition.setTitle(ruleNode.title());
        }
        if (!ObjectUtils.isEmpty(ruleNode.value())) {
            baseDefinition.setName(ruleNode.value());
        } else if (ObjectUtils.isEmpty(baseDefinition.getName())) {
            baseDefinition.setName(StringHelper.lowerFirst(componentClazz.getSimpleName()));
        }
    }

    protected void fillComponentProperties(ComponentDefinition model, UriEndpoint uriEndpoint) {
        if (uriEndpoint == null) {
            return;
        }
        ComponentBaseDefinition baseDefinition = model.getComponent();
        String scheme = uriEndpoint.scheme();
        String extendsScheme = uriEndpoint.extendsScheme();
        String title = uriEndpoint.title();
        String label = null;
        Category[] categories = uriEndpoint.category();
        if (categories.length > 0) {
            label = Arrays.stream(categories)
                    .map(Category::getValue)
                    .collect(Collectors.joining(","));
        }

        baseDefinition.setScheme(scheme);
        baseDefinition.setName(scheme);
        baseDefinition.setExtendsScheme(extendsScheme);
        // if the scheme is an alias then replace the scheme name from the
        // syntax with the alias
        String syntax = scheme + ":" + StringHelper.after(uriEndpoint.syntax(), ":");
        baseDefinition.setSyntax(syntax);
        baseDefinition.setTitle(title);
        baseDefinition.setLabel(label);
        baseDefinition.setConsumerOnly(uriEndpoint.consumerOnly());
        baseDefinition.setProducerOnly(uriEndpoint.producerOnly());
        baseDefinition.setRemote(uriEndpoint.remote());
    }

    protected void addParameterProperties(ComponentDefinition componentModel, Class<?> classElement) {
        while (true) {
            Metadata metadata;
            AtomicInteger idx = new AtomicInteger(0);
            for (final Field fieldElement : classElement.getDeclaredFields()) {
                metadata = fieldElement.getAnnotation(Metadata.class);
                if (metadata != null && metadata.skip()) {
                    continue;
                }
                if (ArrayUtils.contains(IGNORE_PROPERTIES, fieldElement.getName())) {
                    continue;
                }

                UriParam uriParam = fieldElement.getAnnotation(UriParam.class);
                UriPath pathParam = fieldElement.getAnnotation(UriPath.class);
                if (uriParam != null || pathParam != null) {
                    String fieldName = fieldElement.getName();
                    String oriName = retrieveValueOrderly("name", uriParam, pathParam);
                    String name = (ObjectUtils.isEmpty(oriName) ? fieldName : oriName);
                    String paramPrefix = retrieveValueOrderly("prefix", uriParam);
                    boolean multiValue = retrieveValueOrderly("multiValue", false, uriParam);
                    Object defaultValue = retrieveValueOrderly("defaultValue", uriParam, pathParam, metadata);
                    String defaultValueNote = retrieveValueOrderly("defaultValueNote", uriParam, pathParam);
                    boolean required = metadata != null && metadata.required();
                    String label = retrieveValueOrderly("label", uriParam, pathParam, metadata);
                    String displayName = retrieveValueOrderly("displayName", uriParam, pathParam, metadata);
                    // compute a display name if we don't have anything
                    if (ObjectUtils.isEmpty(displayName)) {
                        displayName = StringHelper.upperFirst(name);
                    }

                    // if the field type is a nested parameter then iterate
                    // through its fields
                    Class<?> fieldTypeElement = fieldElement.getType();
                    String fieldTypeName = fieldTypeElement.getTypeName();

                    String docComment = retrieveValueOrderly("description", uriParam, pathParam);
                    boolean isSecret = retrieveValueOrderly("secret", false, uriParam, pathParam);
                    String enumString = retrieveValueOrderly("enums", uriParam, pathParam);
                    List<String> enums = ComponentDefinitionHelper.gatherEnums(enumString, fieldTypeElement);
                    boolean autowired = metadata != null && metadata.autowired();

                    // the field type may be overloaded by another type
                    boolean isDuration = false;
                    String javaType = retrieveValueOrderly("javaType", uriParam, pathParam);
                    if (!ObjectUtils.isEmpty(javaType)) {
                        if ("java.time.Duration".equals(javaType)) {
                            isDuration = true;
                        } else {
                            fieldTypeName = javaType;
                        }
                    }
                    String kind = uriParam != null ? "parameter" : "path";
                    // prepare default value so its value is correct according to its type
                    defaultValue = ComponentDefinitionHelper.getDefaultValue(defaultValue, fieldTypeName, isDuration);

                    ComponentOptionDefinition option = new ComponentOptionDefinition();
                    option.setName(name);
                    option.setDisplayName(displayName);
                    option.setType(ComponentDefinitionHelper.getType(fieldTypeName, false, isDuration, Map.class.isAssignableFrom(fieldTypeElement)));
                    option.setJavaType(fieldTypeName);
                    option.setRequired(required);
                    option.setDefaultValue(defaultValue);
                    option.setDefaultValueNote(defaultValueNote);
                    option.setDescription(docComment);
                    option.setSecret(isSecret);
                    option.setLabel(label);
                    option.setEnums(enums);
                    option.setPrefix(paramPrefix);
                    option.setMultiValue(multiValue);
                    option.setKind(kind);
                    option.setIndex(idx.getAndIncrement());
                    option.setAutowired(autowired);
                    //fill extension parameter properties
                    fillExtensionParameterProperties(option, fieldElement);
                    componentModel.getProperties().put(option.getName(), option);
                }
            }

            // check super classes which may also have fields
            Class<?> superclass = classElement.getSuperclass();
            if (superclass != null) {
                classElement = superclass;
            } else {
                break;
            }
        }
    }

    protected void fillExtensionParameterProperties(ComponentOptionExtensionDefinition option, Field fieldElement) {
        if (fieldElement.isAnnotationPresent(UriParamExtension.class)) {
            UriParamExtension uriParamExtension = fieldElement.getAnnotation(UriParamExtension.class);
            option.setUiComponent(uriParamExtension.uiComponent());
            option.setUiComponentTags(uriParamExtension.uiComponentTags());
            option.setUiComponentGroup(uriParamExtension.uiComponentGroup());
            option.setLoggable(uriParamExtension.loggable());
            option.setInitialValue(uriParamExtension.initialValue());
        }
    }

    protected void addOutputArgumentProperties(ComponentDefinition model, Class<?> componentClazz) {
        for (final Field fieldElement : componentClazz.getDeclaredFields()) {
            if (fieldElement.isAnnotationPresent(OutputArguments.class)) {
                OutputArguments outputArguments = fieldElement.getAnnotation(OutputArguments.class);
                ComponentOutputDefinition componentOutputDefinition = new ComponentOutputDefinition();
                String fieldName = fieldElement.getName();
                String name = ObjectUtils.isEmpty(outputArguments.name()) ? fieldName : outputArguments.name();
                String displayName = ObjectUtils.isEmpty(outputArguments.displayName()) ? StringHelper.upperFirst(name) : outputArguments.displayName();
                String description = outputArguments.description();
                String javaType = ObjectUtils.isEmpty(outputArguments.javaType()) ? fieldElement.getType().getTypeName() : outputArguments.javaType();
                String type = ComponentDefinitionHelper.getType(javaType, false, "java.time.Duration".equals(javaType), Map.class.isAssignableFrom(fieldElement.getType()));
                componentOutputDefinition.setJavaType(javaType);
                componentOutputDefinition.setType(type);
                Type firstType = TypeUtil.getTypeArgument(fieldElement.getGenericType(), 0);
                if (firstType != null) {
                    componentOutputDefinition.setGenericType(firstType.getTypeName());
                }
                componentOutputDefinition.setName(name);
                componentOutputDefinition.setDisplayName(displayName);
                componentOutputDefinition.setDescription(description);
                componentOutputDefinition.setEditable(!model.getProperties().containsKey(name));
                fillExtensionParameterProperties(componentOutputDefinition, fieldElement);
                model.getOutputProperties().put(componentOutputDefinition.getName(), componentOutputDefinition);
            }
        }
    }

    private <T> T retrieveValueOrderly(String name, T defaultValue, Annotation... annotations) {
        T value = retrieveValueOrderly(name, annotations);
        return ObjectUtils.isEmpty(value) ? defaultValue : value;
    }

    private <T> T retrieveValueOrderly(String name, Annotation... annotations) {
        return (T) Arrays.stream(annotations).map(annotation -> {
                    if (annotation == null) {
                        return null;
                    }
                    Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(annotation);
                    return annotationAttributes.get(name);
                })
                .filter(value -> !ObjectUtils.isEmpty(value))
                .findFirst()
                .orElse(null);
    }

    protected void addEndpointHeaders(ComponentDefinition componentModel, String scheme, Class<?> headersClass, UriEndpoint uriEndpoint) {
        String headersNameProvider = uriEndpoint != null ? uriEndpoint.headersNameProvider() : null;
        final boolean isEnum = headersClass.isEnum();
        AtomicInteger idx = new AtomicInteger(0);
        for (Field field : headersClass.getFields()) {
            if ((isEnum || isStatic(field.getModifiers()) && field.getType() == String.class) && field.isAnnotationPresent(Metadata.class)) {
                ComponentOptionDefinition endpointOption = createEndpointMetadataOption(scheme, field, headersNameProvider);
                if (endpointOption != null) {
                    endpointOption.setIndex(idx.getAndIncrement());
                    componentModel.getHeaders().put(endpointOption.getName(), endpointOption);
                }
            }
        }
    }

    protected void addExchangeProperties(ComponentDefinition componentModel, String scheme, Class<?> propertiesClass) {
        for (Field field : propertiesClass.getFields()) {
            ComponentOptionDefinition endpointOption = createEndpointMetadataOption(scheme, field, null);
            if (endpointOption != null) {
                componentModel.getExchangeProperties().put(endpointOption.getName(), endpointOption);
            }
        }
    }

    private ComponentOptionDefinition createEndpointMetadataOption(String scheme, Field field, String headersNameProvider) {
        final Metadata metadata = field.getAnnotation(Metadata.class);
        if (metadata == null) {
            return null;
        }
        final String[] applicableFor = metadata.applicableFor();
        if (applicableFor.length > 0 && Arrays.stream(applicableFor).noneMatch(s -> s.equals(scheme))) {
            log.debug(String.format("The field %s in class %s is not applicable for %s", field.getName(), field.getDeclaringClass().getName(), scheme));
            return null;
        }
        final ComponentOptionDefinition header = new ComponentOptionDefinition();
        String description = metadata.description().trim();
        header.setDescription(description);
        header.setKind("header");
        header.setDisplayName(metadata.displayName());
        header.setJavaType(metadata.javaType());
        header.setRequired(metadata.required());
        header.setDefaultValue(metadata.defaultValue());
        header.setSecret(metadata.secret());
        header.setLabel(metadata.label());
        try {
            if (!ObjectUtils.isEmpty(metadata.enums())) {
                header.setEnums(ComponentDefinitionHelper.gatherEnums(metadata.enums(), header.getJavaType().isEmpty() ? null : loadClass(header.getJavaType())));
            }
        } catch (NoClassDefFoundError e) {
            log.warn(String.format("The java type %s could not be found", header.getJavaType()), e);
        }
        try {
            setHeaderNames(header, field, headersNameProvider);
        } catch (Exception e) {
            log.warn(String.format("The name of the header corresponding to the field %s in class %s cannot be retrieved", field.getName(), field.getDeclaringClass().getName()));
        }
        return header;
    }

    private void setHeaderNames(ComponentOptionDefinition header, Field field, String headersNameProvider) throws Exception {
        final Class<?> declaringClass = field.getDeclaringClass();
        if (field.getType().isEnum()) {
            if (!ObjectUtils.isEmpty(headersNameProvider)) {
                final Optional<?> value = Arrays.stream(declaringClass.getEnumConstants())
                        .filter(c -> ((Enum<?>) c).name().equals(field.getName()))
                        .findAny();
                if (value.isPresent()) {
                    final Optional<Field> headersNameProviderField = Arrays.stream(declaringClass.getFields())
                            .filter(f -> f.getName().equals(headersNameProvider))
                            .findAny();
                    if (headersNameProviderField.isPresent()) {
                        header.setName((String) headersNameProviderField.get().get(value.get()));
                        return;
                    }
                    final Optional<Method> headersNameProviderMethod = Arrays.stream(declaringClass.getMethods())
                            .filter(m -> m.getName().equals(headersNameProvider) && m.getParameterCount() == 0)
                            .findAny();
                    if (headersNameProviderMethod.isPresent()) {
                        header.setName((String) headersNameProviderMethod.get().invoke(value.get()));
                        return;
                    }
                    log.debug(String.format("No method %s without parameters could be found in the class %s", headersNameProvider, declaringClass));
                }
            }
            header.setName(field.getName());
            return;
        }
        header.setName((String) field.get(null));
    }

    private Class<?> retrieveHeaderClass(RuleNode ruleNode, UriEndpoint uriEndpoint) {
        if (ruleNode != null && ruleNode.headersClass() != void.class) {
            return ruleNode.headersClass();
        }
        return uriEndpoint != null ? uriEndpoint.headersClass() : null;
    }

    private Class<?> loadClass(String loadClassName) {
        return KNOWN_CLASSES_CACHE.computeIfAbsent(loadClassName, k -> {
            try {
                return getClass().getClassLoader().loadClass(loadClassName);
            } catch (ClassNotFoundException e) {
                throw new NoClassDefFoundError(loadClassName);
            }
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
