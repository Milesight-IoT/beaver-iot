package com.milesight.beaveriot.blueprint.core.chart.node.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.milesight.beaveriot.base.utils.StringUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@NoArgsConstructor
public abstract class AbstractObjectNode extends AbstractBlueprintNode implements KeyValueNode<BlueprintNode> {

    private static final Map<Class<?>, Map<String, FieldTuple>> BLUEPRINT_NODE_FIELD_CACHE = new ConcurrentHashMap<>();

    private static Map<String, FieldTuple> getChildrenFields(Class<?> clazz) {
        return BLUEPRINT_NODE_FIELD_CACHE.computeIfAbsent(clazz, k -> {
            var blueprintNodeFields = new LinkedHashMap<String, FieldTuple>();
            var currentClass = clazz;

            while (currentClass != null && currentClass != AbstractObjectNode.class && currentClass != Object.class) {
                var fields = currentClass.getDeclaredFields();
                for (var field : fields) {
                    if (field.isSynthetic()) {
                        continue;
                    }
                    if (BlueprintNode.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        try {
                            blueprintNodeFields.put(field.getName(), new FieldTuple(field, MethodHandles.lookup().unreflectGetter(field), MethodHandles.lookup().unreflectSetter(field)));
                        } catch (IllegalAccessException e) {
                            log.warn("Failed to access fields for getChildren in class: {}", clazz.getName(), e);
                        }
                    }
                }
                currentClass = currentClass.getSuperclass();
            }

            return blueprintNodeFields;
        });
    }

    protected AbstractObjectNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    @JsonIgnore
    public List<Field> getChildrenFields() {
        return getChildrenFields(this.getClass())
                .values()
                .stream()
                .map(FieldTuple::field)
                .toList();
    }

    @JsonIgnore
    public void setAttribute(String fieldName, BlueprintNode value) {
        var blueprintNodeFields = getChildrenFields(this.getClass());
        var fieldTuple = blueprintNodeFields.get(fieldName);
        if (fieldTuple == null) {
            return;
        }

        try {
            fieldTuple.setter.invoke(this, value);
        } catch (Throwable e) {
            log.warn("Failed to access fields for setAttribute in class: {}", this.getClass().getName(), e);
        }
    }

    @JsonIgnore
    public BlueprintNode getAttribute(String fieldName) {
        var blueprintNodeFields = getChildrenFields(this.getClass());
        var fieldTuple = blueprintNodeFields.get(fieldName);
        if (fieldTuple == null) {
            return null;
        }
        try {
            return (BlueprintNode) fieldTuple.getter.invoke(this);
        } catch (Throwable e) {
            log.warn("Failed to access fields for getAttribute in class: {}", this.getClass().getName(), e);
            return null;
        }
    }

    @JsonIgnore
    @Override
    public List<BlueprintNode> getBlueprintNodeChildren() {
        var children = new ArrayList<BlueprintNode>();
        try {
            var blueprintNodeFields = getChildrenFields(this.getClass());
            for (var value : blueprintNodeFields.values()) {
                var fieldValue = value.getter.invoke(this);
                if (fieldValue != null) {
                    var fieldName = value.field.getName();
                    if (fieldValue instanceof BlueprintNode blueprintNode) {
                        if (blueprintNode.getBlueprintNodeName() == null) {
                            blueprintNode.setBlueprintNodeName(fieldName);
                        }
                        children.add(blueprintNode);
                    }
                }
            }
        } catch (Throwable e) {
            log.warn("Failed to access fields for getChildren in class: {}", this.getClass().getName(), e);
        }
        return children;
    }

    public List<BlueprintNode> getTypedChildren() {
        return getBlueprintNodeChildren();
    }

    public BlueprintNode getChild(String name) {
        Objects.requireNonNull(name);
        return getAttribute(StringUtils.toCamelCase(name));
    }

    public void removeChild(String name) {
        Objects.requireNonNull(name);
        setAttribute(StringUtils.toCamelCase(name), null);
    }

    protected record FieldTuple(Field field, MethodHandle getter, MethodHandle setter) {

    }

}
