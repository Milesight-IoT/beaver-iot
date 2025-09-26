package com.milesight.beaveriot.blueprint.core.chart.node.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractObjectNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public abstract class AbstractResourceNode extends AbstractObjectNode implements ResourceNode {

    protected boolean managed = true;

    protected AbstractResourceNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public void validate() {
        // do nothing by default
    }

    @JsonIgnore
    public abstract String getResourceType();

    public abstract static class Parser<T extends AbstractResourceNode> implements BlueprintNode.Parser<T> {

        @Autowired
        protected DataNode.Parser dataNodeParser;

        public abstract String getResourceType();

        public abstract T createNode(BlueprintNode blueprintNodeParent, String blueprintNodeName);

        @Override
        public T parse(String propertyName, JsonNode propertyValue, BlueprintNode parentNode, BlueprintParseContext context) {
            var node = createNode(parentNode, propertyName);
            loadDataNodes(node, propertyValue, context);
            node.setManaged(node.getId() == null);
            return node;
        }

        protected void loadDataNodes(T resourceNode, JsonNode propertyValue, BlueprintParseContext context) {
            resourceNode.getChildrenFields()
                    .stream()
                    .filter(field -> DataNode.class.isAssignableFrom(field.getType()))
                    .map(Field::getName)
                    .forEach(fieldName -> {
                        var childPropertyName = StringUtils.toSnakeCase(fieldName);
                        context.pushTask(() -> resourceNode.setAttribute(fieldName, dataNodeParser.parse(childPropertyName, propertyValue.get(childPropertyName), resourceNode, context)));
                    });
        }

    }

}
