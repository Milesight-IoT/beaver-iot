package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnFormatNode;
import com.milesight.beaveriot.blueprint.core.helper.MapFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class FnFormatExecutor extends AbstractFunctionExecutor<FnFormatNode> {

    @SuppressWarnings("unchecked")
    @Override
    public void execute(FnFormatNode function, BlueprintDeployContext context) {
        var template = getParameter(function, 0, String.class);
        var params = Optional.ofNullable(getParameter(function, 1, false))
                .map(DataNode::getValue)
                .orElse(null);

        String result;
        if (params instanceof Map<?, ?> map) {
            result = MapFormat.format(template, (Map<Object, Object>) map);
        } else if (params instanceof List<?> list) {
            result = MessageFormat.format(template, list.toArray());
        } else {
            result = MapFormat.format(template, Collections.emptyMap());
        }

        setResult(function, result);
    }

    @Override
    public Class<FnFormatNode> getMatchedNodeType() {
        return FnFormatNode.class;
    }

}
