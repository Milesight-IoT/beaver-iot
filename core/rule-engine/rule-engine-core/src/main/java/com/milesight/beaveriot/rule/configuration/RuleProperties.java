package com.milesight.beaveriot.rule.configuration;

import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.flow.ComponentDefinitionCache;
import com.milesight.beaveriot.rule.model.RuleLanguage;
import com.milesight.beaveriot.rule.model.definition.BaseDefinition;
import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.milesight.beaveriot.rule.support.RuleFlowIdGenerator.FLOW_ID_PREFIX;

/**
 * @author leon
 */
@Data
@ConfigurationProperties(prefix = "camel.rule")
public class RuleProperties implements SmartInitializingSingleton, ApplicationContextAware {

    private ApplicationContext applicationContext;
    /**
     * The components defined in the rule engine. The key is the node type, the value is the list of components
     */
    private Map<String, List<BaseDefinition>> components = new LinkedHashMap<>();

    /**
     * The language properties, such as code and expression languages
     */
    private RuleLanguage languages;

    /**
     * Whether to enable tracing. Default is true.
     */
    private boolean enabledTracing = true;

    /**
     * The trace output mode. default is ALL
     */
    private TraceOutputMode traceOutputMode = TraceOutputMode.EVENT;

    /**
     * The path to the component schema directory. Default is /camel-schema.
     * The schema file is named as {componentName}.json, used to override the camel component's options.
     */
    private String componentSchemaPath = "camel-schema";

    /**
     * The prefix of the trace node id. Default is "flow."
     * default only trace the prefix flow node
     */
    private String traceNodePrefix = FLOW_ID_PREFIX;

    public List<String> getComponentNames() {
        if (ObjectUtils.isEmpty(components)) {
            return List.of();
        }
        return components.values().stream()
                .flatMap(List::stream)
                .map(BaseDefinition::getName)
                .toList();
    }

    @Override
    public void afterSingletonsInstantiated() {

        List<String> componentNames = getComponentNames();
        String[] beanNamesForAnnotation = applicationContext.getBeanNamesForAnnotation(RuleNode.class);
        for (String beanName : beanNamesForAnnotation) {
            Object bean = applicationContext.getBean(beanName);
            RuleNode ruleNode = AnnotationUtils.getAnnotation(bean.getClass(), RuleNode.class);
            if (ruleNode != null) {
                String componentName = ObjectUtils.isEmpty(ruleNode.value()) ? beanName : ruleNode.value();
                if (!ObjectUtils.isEmpty(ruleNode.type()) && components.containsKey(ruleNode.type()) && !componentNames.contains(componentName)) {
                    List<BaseDefinition> baseDefinitions = components.get(ruleNode.type());
                    if (baseDefinitions != null) {
                        ComponentDefinition componentDefinition = ComponentDefinitionCache.load(componentName);
                        baseDefinitions.add(BaseDefinition.create(componentName, componentDefinition.getComponent().getTitle()));
                    }
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public enum TraceOutputMode {
        LOGGING, EVENT, ALL
    }
}
