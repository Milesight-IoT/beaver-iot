package com.milesight.beaveriot.rule.components.code;

import com.milesight.beaveriot.rule.RuleNodeDefinitionInterceptor;
import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.model.flow.route.AbstractNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.ToNodeDefinition;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import org.apache.camel.BeanScope;
import org.apache.camel.model.BeanDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
public class ScriptCodeRuleNodeDefinitionInterceptor implements RuleNodeDefinitionInterceptor {

    @Override
    public ProcessorDefinition postProcessNodeDefinition(String flowId, AbstractNodeDefinition nodeDefinition) {
        if (nodeDefinition instanceof ToNodeDefinition toNodeDefinition && toNodeDefinition.getUri().startsWith("bean:code")) {
            BeanDefinition beanDefinition = new BeanDefinition();
            Map<String, Object> parameters = toNodeDefinition.getParameters();
            ScriptCodeComponent scriptCodeComponent = new ScriptCodeComponent();
            scriptCodeComponent.setPayload(getParameterValue(parameters, "bean.payload", String.class));
            scriptCodeComponent.setExpression(getParameterValue(parameters, "bean.expression", ExpressionNode.class));
            scriptCodeComponent.setInputArguments(getParameterValue(parameters, "bean.inputArguments",Map.class));
            beanDefinition.setBean(scriptCodeComponent);
            beanDefinition.setScope(BeanScope.Prototype);
            beanDefinition.setId(RuleFlowIdGenerator.generateNamespacedId(flowId, nodeDefinition.getId()));
            beanDefinition.setDescription(toNodeDefinition.getNameNode());
            return beanDefinition;
        } else {
            return null;
        }
    }

    private <T> T getParameterValue(Map<String, Object> parameters, String key, Class<T> clazz) {
        if (ObjectUtils.isEmpty(parameters) || !parameters.containsKey(key)) {
            return null;
        }
        if (clazz == String.class) {
            return (T) parameters.get(key);
        } else {
            return JsonHelper.fromJSON((String) parameters.get(key), clazz);
        }
    }

}
