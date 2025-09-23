package com.milesight.beaveriot.blueprint.core.chart.node.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.primitives.Longs;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
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
public class DeviceCanvasResourceNode extends AbstractResourceNode {

    public static final String RESOURCE_TYPE = "device_canvas";

    @JsonIgnore
    @ToString.Exclude
    private final Accessor accessor = new Accessor(this);

    private DataNode id;

    private DataNode deviceId;

    private DataNode data;

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    public DeviceCanvasResourceNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public record Accessor(DeviceCanvasResourceNode node) {

        public String getId() {
            if (node.id == null || node.id.getValue() == null) {
                return null;
            }
            return String.valueOf(node.id.getValue());
        }

        public void setId(String id) {
            node.setId(new StringValueNode(node, Fields.id, id));
        }

        public Long getDeviceId() {
            if (node.deviceId == null || node.deviceId.getValue() == null) {
                return null;
            }
            if (node.deviceId.getValue() instanceof Long number) {
                return number;
            }
            return Longs.tryParse(String.valueOf(node.deviceId.getValue()));
        }

        public void setDeviceId(Long deviceId) {
            node.setId(new LongValueNode(node, Fields.deviceId, deviceId));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Map<String, Object> getData() {
            if (node.data == null || node.data.getValue() == null) {
                return null;
            }
            return node.data.getValue() instanceof Map value ? value : null;
        }

        public void setData(JsonNode canvasData) {
            node.setData(BlueprintUtils.convertToDataNode(DeviceCanvasResourceNode.Fields.data, node, canvasData));
        }
    }

    @Component
    public static class Parser extends AbstractResourceNode.Parser<DeviceCanvasResourceNode> {

        @Override
        public String getResourceType() {
            return RESOURCE_TYPE;
        }

        @Override
        public DeviceCanvasResourceNode createNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
            return new DeviceCanvasResourceNode(blueprintNodeParent, blueprintNodeName);
        }

    }

}
