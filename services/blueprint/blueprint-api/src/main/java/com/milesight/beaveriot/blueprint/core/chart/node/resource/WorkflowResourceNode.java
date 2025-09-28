package com.milesight.beaveriot.blueprint.core.chart.node.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.primitives.Longs;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.value.BoolValueNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.value.LongValueNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.value.StringValueNode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.springframework.stereotype.Component;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@FieldNameConstants
@NoArgsConstructor
public class WorkflowResourceNode extends AbstractResourceNode {

    public static final String RESOURCE_TYPE = "workflow";

    @JsonIgnore
    @ToString.Exclude
    private final Accessor accessor = new Accessor(this);

    private DataNode id;

    private DataNode deviceId;

    private DataNode name;

    private DataNode remark;

    private DataNode enabled;

    private DataNode data;

    @JsonIgnore
    private DataNode trigger;

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    public WorkflowResourceNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public record Accessor(WorkflowResourceNode node) {

        public String getId() {
            if(node.id == null || node.id.getValue() == null) {
                return null;
            }
            return String.valueOf(node.id.getValue());
        }

        public void setId(String id) {
            node.setId(new StringValueNode(node, Fields.id, id));
        }

        public Long getDeviceId() {
            if(node.deviceId == null || node.deviceId.getValue() == null) {
                return null;
            }
            if (node.deviceId.getValue() instanceof Long number) {
                return number;
            }
            return Longs.tryParse(String.valueOf(node.deviceId.getValue()));
        }

        public void setDeviceId(Long deviceId) {
            node.setDeviceId(new LongValueNode(node, Fields.deviceId, deviceId));
        }

        public String getName() {
            if(node.name == null || node.name.getValue() == null) {
                return null;
            }
            return String.valueOf(node.name.getValue());
        }

        public void setName(String name) {
            node.setName(new StringValueNode(node, Fields.name, name));
        }

        public String getRemark() {
            if(node.remark == null || node.remark.getValue() == null) {
                return null;
            }
            return String.valueOf(node.remark.getValue());
        }

        public void setRemark(String remark) {
            node.setRemark(new StringValueNode(node, Fields.remark, remark));
        }

        public boolean isEnabled() {
            if(node.enabled == null || node.enabled.getValue() == null) {
                return true;
            }
            if(node.enabled.getValue() instanceof Boolean boolValue){
                return boolValue;
            }
            if (node.enabled.getValue() instanceof String stringValue) {
                return !Boolean.FALSE.equals(Boolean.valueOf(stringValue));
            }
            return true;
        }

        public void setEnabled(boolean enabled) {
            node.setEnabled(new BoolValueNode(node, Fields.enabled, enabled));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Map<String, Object> getData() {
            if(node.data == null || node.data.getValue() == null) {
                return null;
            }
            return node.data.getValue() instanceof Map value ? value : null;
        }

        public void setData(JsonNode data) {
            node.setData(BlueprintUtils.convertToDataNode(Fields.data, node, data));
        }

        public void setTrigger(JsonNode entity) {
            node.setData(BlueprintUtils.convertToDataNode(Fields.trigger, node, entity));
        }

    }

    @Component
    public static class Parser extends AbstractResourceNode.Parser<WorkflowResourceNode> {

        @Override
        public String getResourceType() {
            return RESOURCE_TYPE;
        }

        @Override
        public WorkflowResourceNode createNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
            return new WorkflowResourceNode(blueprintNodeParent, blueprintNodeName);
        }

    }

}
