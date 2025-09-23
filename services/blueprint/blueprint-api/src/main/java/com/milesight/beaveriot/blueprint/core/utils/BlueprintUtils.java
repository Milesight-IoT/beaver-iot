package com.milesight.beaveriot.blueprint.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.KeyValueNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.container.ArrayDataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.container.MapDataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.value.BoolValueNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.value.DoubleValueNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.value.LongValueNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.value.StringValueNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.ObjectSchemaPropertiesNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class BlueprintUtils {

    public static <T> Stream<T> iteratorToStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    public static <T> void forEachInReverseOrder(Iterator<T> iterator, Consumer<T> consumer) {
        var list = new ArrayList<T>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        Collections.reverse(list);
        for (var item : list) {
            consumer.accept(item);
        }
    }

    public static BlueprintNode getChildByName(BlueprintNode blueprintNode, String name) {
        if (name == null || name.isEmpty()) {
            return blueprintNode;
        }
        return blueprintNode.getBlueprintNodeChildren()
                .stream()
                .filter(node -> name.equals(node.getBlueprintNodeName()))
                .findFirst()
                .orElse(null);
    }

    public static BlueprintNode getChildByPath(BlueprintNode blueprintNode, String path) {
        return getChildByPath(blueprintNode, path, false);
    }

    public static BlueprintNode getChildByLongestMatchedPath(BlueprintNode blueprintNode, String path) {
        return getChildByPath(blueprintNode, path, true);
    }

    private static BlueprintNode getChildByPath(BlueprintNode blueprintNode, String path, boolean returnLongestMatch) {
        if (path == null || path.isEmpty()) {
            return blueprintNode;
        }

        path = path.replace("[", ".[");
        var tokens = path.split("\\.");
        for (String name : tokens) {
            var parent = blueprintNode;
            blueprintNode = getChildByName(parent, name);
            if (blueprintNode == null) {
                if (returnLongestMatch) {
                    return parent;
                }
                return null;
            }
        }

        return blueprintNode;
    }

    public static JsonNode getChildByPath(JsonNode jsonNode, String path) {
        if (path == null || path.isEmpty()) {
            return jsonNode;
        }

        path = path.replace("[", ".[");
        var tokens = path.split("\\.");
        for (String token : tokens) {
            if (jsonNode instanceof ObjectNode objectNode) {
                jsonNode = objectNode.get(token);
            } else if (jsonNode instanceof ArrayNode arrayNode) {
                jsonNode = arrayNode.get(Integer.parseInt(token.substring(1, token.length() - 1)));
            } else {
                return null;
            }
        }
        return jsonNode;
    }

    private BlueprintUtils() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlueprintNode> T findParentByType(BlueprintNode blueprintNode, Class<T> type) {
        while (blueprintNode != null && !type.isInstance(blueprintNode)) {
            blueprintNode = blueprintNode.getBlueprintNodeParent();
        }
        return (T) blueprintNode;
    }

    public static TemplateNode getCurrentTemplate(BlueprintNode blueprintNode) {
        return findParentByType(blueprintNode, TemplateNode.class);
    }

    public static boolean isTemplateParameter(BlueprintNode blueprintNode) {
        return findParentByType(blueprintNode, ObjectSchemaPropertiesNode.class) != null;
    }

    public static String getNodePath(BlueprintNode node) {
        return getNodePath(node, null);
    }

    public static String getNodePath(String nodeName, BlueprintNode parentNode) {
        var parentPath = getNodePath(parentNode);
        return parentNode instanceof KeyValueNode<?> ? parentPath + "." + nodeName : parentPath + nodeName;
    }

    public static String getNodePath(BlueprintNode node, BlueprintNode base) {
        if (node == null) {
            return null;
        }

        if (base == null) {
            base = node;
            while (base.getBlueprintNodeParent() != null) {
                base = base.getBlueprintNodeParent();
            }
        }

        var stack = new ArrayDeque<>();
        while (node != base) {
            stack.push(node.getBlueprintNodeName());
            if (node.getBlueprintNodeParent() instanceof KeyValueNode<?> parent && parent != base) {
                stack.push(".");
            }
            node = node.getBlueprintNodeParent();
        }

        var result = new StringBuilder();
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        return result.toString();
    }

    public static DataNode convertToDataNode(String nodeName, BlueprintNode parentNode, Object data) {
        var jsonNode = JsonUtils.toJsonNode(data);
        return convertToDataNode(nodeName, parentNode, jsonNode);
    }

    public static DataNode convertToDataNode(String nodeName, BlueprintNode parentNode, JsonNode data) {
        if (data == null) {
            return null;
        }

        if (data instanceof BooleanNode boolNode) {
            return new BoolValueNode(parentNode, nodeName, boolNode.booleanValue());
        }

        if (data instanceof TextNode textNode) {
            return new StringValueNode(parentNode, nodeName, textNode.textValue());
        }

        if (data instanceof NumericNode numericNode) {
            if (numericNode.isBigInteger() || numericNode.isBigDecimal()) {
                var nodePath = getNodePath(nodeName, parentNode);
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_TEMPLATE_PARSING_FAILED,
                        "Big number is unsupported. Path: " + nodePath);
            } else if (numericNode.isIntegralNumber()) {
                return new LongValueNode(parentNode, nodeName, numericNode.longValue());
            } else if (numericNode.isFloatingPointNumber()) {
                return new DoubleValueNode(parentNode, nodeName, numericNode.doubleValue());
            }
        }

        if (data instanceof ObjectNode objectNode) {
            var mapDataNode = new MapDataNode(parentNode, nodeName);
            objectNode.fields().forEachRemaining(entry ->
                            mapDataNode.addChildNode(convertToDataNode(entry.getKey(), mapDataNode, entry.getValue())));
            return mapDataNode;
        }

        if (data instanceof ArrayNode arrayNode) {
            var arrayDataNode = new ArrayDataNode(parentNode, nodeName);
            for (var i = 0; i < arrayNode.size(); i++) {
                var item = arrayNode.get(i);
                var itemName = "[" + i + "]";
                arrayDataNode.addChildNode(convertToDataNode(itemName, arrayDataNode, item));
            }
            return arrayDataNode;
        }

        return null;
    }

    public static void loadObjectSchemaDefaultValues(JsonNode objectSchema, Map<String, Object> defaultValues) {
        if (objectSchema != null && objectSchema.get("properties") instanceof ObjectNode properties) {
            properties.fields().forEachRemaining(entry -> {
                var defaultValue = entry.getValue().get("default");
                if (defaultValue != null && !defaultValues.containsKey(entry.getKey())) {
                    defaultValues.put(entry.getKey(), JsonUtils.cast(defaultValue, Object.class));
                }
            });
        }
    }

}
