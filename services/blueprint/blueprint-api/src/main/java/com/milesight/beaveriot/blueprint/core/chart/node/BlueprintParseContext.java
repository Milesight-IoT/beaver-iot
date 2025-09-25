package com.milesight.beaveriot.blueprint.core.chart.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.support.ResourceLoader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintParseContext {

    public static final int MAX_NODES = 65535;

    public static final int MAX_NESTED_TEMPLATES = 16;

    private final AtomicInteger nodeCounter = new AtomicInteger(0);

    private final AtomicInteger nestedTemplateCounter = new AtomicInteger(0);

    private TemplateNode root;

    @Getter(AccessLevel.NONE)
    private Deque<BlueprintNode.ProcessingTask> taskStack;

    private ResourceLoader resourceLoader;

    private JsonNode templateJsonNode;

    private Map<String, Object> templateContext;

    public BlueprintParseContext(Deque<BlueprintNode.ProcessingTask> taskStack, ResourceLoader resourceLoader,
                                 JsonNode templateJsonNode, Map<String, Object> templateContext) {
        this.taskStack = taskStack;
        this.templateJsonNode = templateJsonNode;
        this.resourceLoader = resourceLoader;
        this.templateContext = templateContext;
    }

    public void pushTask(BlueprintNode.ProcessingTask task) {
        if (nodeCounter.incrementAndGet() > MAX_NODES) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_MAX_TEMPLATE_NODE_COUNT_EXCEED);
        }
        taskStack.push(task);
    }

    public void goIntoNestedTemplate() {
        pushTask(nestedTemplateCounter::decrementAndGet);
    }

    public void leaveFromNestedTemplate() {
        // because the task is processed in reverse order, we need to treat this method as 'go into nested template'
        pushTask(() -> {
            var count = nestedTemplateCounter.incrementAndGet();
            if (count > MAX_NESTED_TEMPLATES) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_MAX_NESTED_TEMPLATE_COUNT_EXCEED);
            }
        });
    }

}
