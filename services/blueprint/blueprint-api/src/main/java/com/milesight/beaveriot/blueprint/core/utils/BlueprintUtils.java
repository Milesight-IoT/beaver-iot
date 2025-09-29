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
import com.milesight.beaveriot.blueprint.core.chart.node.enums.BlueprintNodeStatus;
import com.milesight.beaveriot.blueprint.core.chart.node.template.ObjectSchemaPropertiesNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

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
        if (!StringUtils.hasText(name)) {
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
        if (!StringUtils.hasText(path)) {
            return blueprintNode;
        }

        path = path.replace("[", ".[");
        var tokens = path.split("\\.");
        for (String name : tokens) {
            if (name.isEmpty()) {
                continue;
            }

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
        if (jsonNode == null) {
            return null;
        }

        if (!StringUtils.hasText(path)) {
            return jsonNode;
        }

        path = path.replace("[", ".[");
        var tokens = path.split("\\.");
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }

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
        if (data == null) {
            return null;
        }

        if (data instanceof DataNode dataNode) {
            return dataNode;
        }

        return convertToDataNode(nodeName, parentNode, JsonUtils.cast(data, JsonNode.class));
    }

    public static DataNode convertToDataNode(String nodeName, BlueprintNode parentNode, JsonNode data) {
        if (data == null) {
            return null;
        }

        DataNode result = null;
        if (data instanceof BooleanNode boolNode) {
            result = new BoolValueNode(parentNode, nodeName, boolNode.booleanValue());
        } else if (data instanceof TextNode textNode) {
            result = new StringValueNode(parentNode, nodeName, textNode.textValue());
        } else if (data instanceof NumericNode numericNode) {
            if (numericNode.isBigInteger() || numericNode.isBigDecimal()) {
                var nodePath = getNodePath(nodeName, parentNode);
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_TEMPLATE_PARSING_FAILED,
                        "Big number is unsupported. Path: " + nodePath);
            } else if (numericNode.isIntegralNumber()) {
                result = new LongValueNode(parentNode, nodeName, numericNode.longValue());
            } else if (numericNode.isFloatingPointNumber()) {
                result = new DoubleValueNode(parentNode, nodeName, numericNode.doubleValue());
            }
        } else if (data instanceof ObjectNode objectNode) {
            var mapDataNode = new MapDataNode(parentNode, nodeName);
            objectNode.fields().forEachRemaining(entry -> mapDataNode
                    .addChildNode(convertToDataNode(entry.getKey(), mapDataNode, entry.getValue())));
            result = mapDataNode;
        } else if (data instanceof ArrayNode arrayNode) {
            var arrayDataNode = new ArrayDataNode(parentNode, nodeName);
            for (var i = 0; i < arrayNode.size(); i++) {
                var item = arrayNode.get(i);
                var itemName = "[" + i + "]";
                arrayDataNode.addChildNode(convertToDataNode(itemName, arrayDataNode, item));
            }
            result = arrayDataNode;
        }

        if (result == null) {
            return null;
        }

        // The data node not contains any functions will be marked as finished directly
        result.setBlueprintNodeStatus(BlueprintNodeStatus.FINISHED);
        return result;
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

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T convertValue(Object value, String type) {
        if (type == null) {
            return (T) value;
        }

        type = type.toLowerCase();
        if (type.equals("json")) {
            return (T) JsonUtils.toJSON(value);
        }

        var clazz = switch (type) {
            case "string", "text" -> String.class;
            case "integer", "int", "long" -> Long.class;
            case "number", "float", "double" -> Double.class;
            case "boolean", "bool" -> Boolean.class;
            case "object", "map", "dict" -> ObjectNode.class;
            case "array", "list" -> ArrayNode.class;
            case "null" -> null;
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        };

        return (T) convertValue(value, clazz);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T convertValue(Object value, Class<T> type) {
        if (value == null || type == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return (T) value;
        }

        if (String.class.equals(type)) {
            return (T) String.valueOf(value);
        }

        if (Boolean.class.equals(type)) {
            if (value instanceof Number number) {
                return (T) (Boolean) !number.equals(0);
            } else {
                return (T) Boolean.valueOf(value.toString());
            }
        }

        return JsonUtils.cast(value, type);
    }

}
