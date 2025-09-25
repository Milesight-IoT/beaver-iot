package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnFormatNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class FnFormatExecutor extends AbstractFunctionExecutor<FnFormatNode> {

    public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    @SuppressWarnings("unchecked")
    @Override
    public void execute(FnFormatNode function, BlueprintDeployContext context) {
        setResult(function, format(
                getParameter(function, 0, String.class),
                getParameter(function, 1, Map.class)
        ));
    }

    /**
     * Inspired by <a href="https://www.baeldung.com/java-string-formatting-named-placeholders">this article</a>
     */
    private String format(String template, Map<Object, Object> params) {
        var args = new ArrayList<>();
        var newTemplate = new StringBuilder(template);
        var matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            var key = matcher.group(1);
            var placeHolder = "${" + key + "}";
            int index = newTemplate.indexOf(placeHolder);
            if (index != -1) {
                newTemplate.replace(index, index + placeHolder.length(), "%s");
                args.add(params.getOrDefault(key, ""));
            }
        }

        return String.format(newTemplate.toString(), args.toArray());
    }

    @Override
    public Class<FnFormatNode> getMatchedNodeType() {
        return FnFormatNode.class;
    }

}
