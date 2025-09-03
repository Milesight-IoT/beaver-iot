package com.milesight.beaveriot.blueprint.core.config;


import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FunctionNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import org.yaml.snakeyaml.LoaderOptions;
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
import java.util.Map;
import java.util.Objects;

/**
 * Allows snakeyaml to parse YAML templates that contain short forms of
 * Blueprint intrinsic functions.
 * <p>
 * Inspired by jenkinsci/aws-sam-plugin (https://github.com/jenkinsci/aws-sam-plugin/blob/master/src/main/java/com/amazonaws/jenkins/plugins/sam/util/IntrinsicsYamlConstructor.java)
 * License: MIT
 */
public class BlueprintIntrinsicsYamlConstructor extends SafeConstructor {

    public BlueprintIntrinsicsYamlConstructor(LoaderOptions loaderOptions) {
        super(loaderOptions);
        this.yamlConstructors.put(null, new ConstructUnknownTag());
        addIntrinsic("Ref", true);
    }

    protected void addIntrinsic(String tag) {
        addIntrinsic(tag, true);
    }

    protected void addIntrinsic(String tag, boolean attachFnPrefix) {
        addIntrinsic(tag, attachFnPrefix, false);
    }

    protected void addIntrinsic(String tag, boolean attachFnPrefix, boolean forceSequenceValue) {
        var snakeCasedTag = StringUtils.toSnakeCase(tag);
        if (!Objects.equals(snakeCasedTag, tag)) {
            addIntrinsic(snakeCasedTag, attachFnPrefix, forceSequenceValue);
        }
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
            String key = StringUtils.toSnakeCase(node.getTag().getValue().substring(1));
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

    public static final class ConstructUnknownTag extends AbstractConstruct {

        @Override
        public Object construct(Node node) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_TEMPLATE_UNKNOWN_YAML_TAG);
        }
    }
}

