package com.milesight.beaveriot.blueprint.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.JsonSchemaValidationException;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonSchemaUtils;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import com.milesight.beaveriot.blueprint.core.chart.parser.IBlueprintTemplateParser;
import com.milesight.beaveriot.blueprint.core.constant.BlueprintConstants;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.model.SystemContext;
import com.milesight.beaveriot.blueprint.core.po.BlueprintPO;
import com.milesight.beaveriot.blueprint.core.po.BlueprintResourcePO;
import com.milesight.beaveriot.blueprint.core.repository.BlueprintRepository;
import com.milesight.beaveriot.blueprint.core.repository.BlueprintResourceRepository;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import com.milesight.beaveriot.blueprint.facade.IBlueprintFacade;
import com.milesight.beaveriot.blueprint.support.TemplateLoader;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.context.security.TenantContext;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class BlueprintService implements IBlueprintFacade {

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private BlueprintResourceRepository blueprintResourceRepository;

    @Autowired
    private IBlueprintTemplateParser templateParser;

    @Autowired
    private BlueprintDeployer blueprintDeployer;

    @Override
    @Transactional
    public Long deployBlueprint(TemplateLoader templateLoader, Map<String, Object> variables) {
        Map<String, Object> context = new HashMap<>();
        templateParser.loadConstantsIntoContext(templateLoader, context);

        var variablesJsonSchema = templateParser.getVariableJsonSchema(templateLoader, context);
        if (variablesJsonSchema != null) {
            validateVariables(variables, variablesJsonSchema);
            // ensure map is writable
            variables = new HashMap<>(variables);
            BlueprintUtils.loadObjectSchemaDefaultValues(variablesJsonSchema, variables);
            context.put(BlueprintConstants.VARIABLES_KEY, variables);
            context.put(BlueprintConstants.PARAMETERS_KEY, variables);
        }

        var systemContext = getSystemContext();
        context.put(BlueprintConstants.SYSTEM_CONTEXT_KEY, systemContext);

        var chart = templateParser.parseBlueprint(templateLoader, context);
        var bindResources = blueprintDeployer.deploy(chart, context);

        val operatorId = SecurityUserContext.getUserId() != null
                ? SecurityUserContext.getUserId().toString()
                : "";

        var blueprintPO = BlueprintPO.builder()
                .id(SnowflakeUtil.nextId())
                .tenantId(systemContext.getTenantId())
                .chart(JsonUtils.toJSON(chart))
                .createdBy(operatorId)
                .updatedBy(operatorId)
                .build();
        blueprintRepository.save(blueprintPO);

        var blueprintResources = bindResources.stream()
                .map(bindResource -> BlueprintResourcePO.builder()
                        .id(SnowflakeUtil.nextId())
                        .blueprintId(blueprintPO.getId())
                        .resourceId(bindResource.id())
                        .resourceType(bindResource.resourceType())
                        .managed(bindResource.managed())
                        .build())
                .toList();
        blueprintResourceRepository.saveAll(blueprintResources);

        return blueprintPO.getId();
    }

    @NonNull
    private static SystemContext getSystemContext() {
        var systemContext = new SystemContext();
        var userId = SecurityUserContext.getUserId();
        if (userId != null) {
            systemContext.setUserId(userId);
        }
        var tenantId = TenantContext.tryGetTenantId()
                .orElseThrow(() -> new ServiceException(ErrorCode.FORBIDDEN_PERMISSION, "Tenant not found."));
        if (tenantId != null) {
            systemContext.setTenantId(tenantId);
        }
        return systemContext;
    }

    private static void validateVariables(Map<String, Object> variables, JsonNode variablesJsonSchema) {
        try {
            JsonSchemaUtils.validate(variablesJsonSchema, JsonUtils.toJsonNode(variables));
        } catch (JsonSchemaValidationException e) {
            log.debug("Json schema validation error: {}", e.getDetails());
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_PARAMETERS_VALIDATION_ERROR, e.getMessage(), e.getDetails());
        }
    }


    @Override
    @Transactional
    public void removeBlueprint(Long blueprintId) {
        var blueprintPO = getBlueprint(blueprintId);
        if (blueprintPO == null) {
            return;
        }

        var chart = JsonUtils.fromJSON(blueprintPO.getChart(), TemplateNode.class);
        if (chart != null) {
            var resourceTypeAndIdToIsManaged = blueprintResourceRepository.findByBlueprintId(blueprintId)
                    .stream()
                    .collect(Collectors.toMap(v -> this.resourceKey(v.getResourceType(), v.getResourceId()),
                            BlueprintResourcePO::getManaged, (a, b) -> a));

            blueprintDeployer.delete(chart,
                    (resourceType, resourceId) -> resourceTypeAndIdToIsManaged.getOrDefault(resourceKey(resourceType, resourceId), false));

            blueprintResourceRepository.deleteAllByBlueprintId(blueprintId);
            blueprintRepository.delete(blueprintPO);
        }
    }

    public String resourceKey(String resourceType, String resourceId) {
        return resourceType + "::" + resourceId;
    }

    public BlueprintPO getBlueprint(Long blueprintId) {
        return blueprintRepository.findById(blueprintId).orElse(null);
    }

    @Override
    @SneakyThrows
    public JsonNode getVariableJsonSchema(TemplateLoader templateLoader) {
        Map<String, Object> context = new HashMap<>();
        templateParser.loadConstantsIntoContext(templateLoader, context);
        return templateParser.getVariableJsonSchema(templateLoader, context);
    }
}
