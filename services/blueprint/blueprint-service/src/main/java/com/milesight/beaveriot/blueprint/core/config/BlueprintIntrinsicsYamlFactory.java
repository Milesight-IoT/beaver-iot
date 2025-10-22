package com.milesight.beaveriot.blueprint.core.config;


import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.BlueprintRuntimeFunctionName;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FunctionNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class BlueprintIntrinsicsYamlFactory implements SnakeYamlFactory {

    private final LoaderOptions loaderOptions;

    private final List<BlueprintRuntimeFunctionName> functions;

    @Override
    public Yaml newInstance() {
        return new Yaml(new Constructor(loaderOptions, functions));
    }

    /**
     * Allows snakeyaml to parse YAML templates that contain short forms of
     * Blueprint intrinsic functions.
     * <p>
     * Inspired by <a href="https://github.com/jenkinsci/aws-sam-plugin/blob/master/src/main/java/com/amazonaws/jenkins/plugins/sam/util/IntrinsicsYamlConstructor.java">jenkinsci/aws-sam-plugin</a>
     * License: MIT
     */
    public static class Constructor extends SafeConstructor {

        public Constructor(LoaderOptions loaderOptions, List<BlueprintRuntimeFunctionName> functions) {
            super(loaderOptions);
            this.yamlConstructors.put(null, new ConstructUnknownTag());

            if (functions == null || functions.isEmpty()) {
                log.warn("No functions found, skip adding intrinsic functions");
                return;
            }

            functions.forEach(parser -> {
                var functionName = parser.getFunctionName();
                if (functionName.startsWith(FunctionNode.PREFIX)) {
                    addIntrinsic(functionName.substring(4));
                } else {
                    addIntrinsic(functionName, false);
                }
            });
        }

        protected void addIntrinsic(String tag) {
            addIntrinsic(tag, true);
        }

        protected void addIntrinsic(String tag, boolean attachFnPrefix) {
            addIntrinsic(tag, attachFnPrefix, false);
        }

        protected void addIntrinsic(String tag, boolean attachFnPrefix, boolean forceSequenceValue) {
            this.yamlConstructors.put(new Tag("!" + tag), new ConstructFunction(attachFnPrefix, forceSequenceValue));
        }

        public class ConstructFunction extends AbstractConstruct {
            private final boolean attachFnPrefix;
            private final boolean forceSequenceValue;

            public ConstructFunction(boolean attachFnPrefix, boolean forceSequenceValue) {
                this.attachFnPrefix = attachFnPrefix;
                this.forceSequenceValue = forceSequenceValue;
            }

            public Object construct(Node node) {
                String key = node.getTag().getValue().substring(1);
                String prefix = attachFnPrefix ? FunctionNode.PREFIX : "";
                Map<String, Object> result = new HashMap<>();

                result.put(prefix + key, constructIntrinsicValueObject(node));
                return result;
            }

            protected Object constructIntrinsicValueObject(Node node) {
                if (node instanceof ScalarNode scalarNode) {
                    Object val = constructScalar(scalarNode);
                    if (forceSequenceValue) {
                        String strVal = (String) val;
                        int firstDotIndex = strVal.indexOf(".");
                        val = Arrays.asList(strVal.substring(0, firstDotIndex), strVal.substring(firstDotIndex + 1));
                    }
                    return val;
                } else if (node instanceof SequenceNode sequenceNode) {
                    return constructSequence(sequenceNode);
                } else if (node instanceof MappingNode mappingNode) {
                    return constructMapping(mappingNode);
                }
                throw new YAMLException("Intrinsics function arguments cannot be parsed.");
            }
        }

    }

    public static final class ConstructUnknownTag extends AbstractConstruct {

        @Override
        public Object construct(Node node) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_TEMPLATE_PARSING_FAILED, "Unknown YAML tag found in blueprint template");
        }
    }
}

