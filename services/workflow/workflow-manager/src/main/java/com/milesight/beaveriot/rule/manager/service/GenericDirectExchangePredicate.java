package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.PredicateNode;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author leon
 */
@Slf4j
@Component
@RuleNode(value = RuleNodeNames.innerDirectExchangePredicate)
public class GenericDirectExchangePredicate implements PredicateNode<Exchange> {

    private final EntityServiceProvider entityServiceProvider;

    private final WorkflowEntityRelationService workflowEntityRelationService;

    public GenericDirectExchangePredicate(EntityServiceProvider entityServiceProvider,
                                          WorkflowEntityRelationService workflowEntityRelationService
    ) {
        this.entityServiceProvider = entityServiceProvider;
        this.workflowEntityRelationService = workflowEntityRelationService;
    }

    @Override
    public boolean matches(Exchange exchange) {
        ExchangePayload body = exchange.getIn().getBody(ExchangePayload.class);

        Entity entity = getTriggerWorkflowEntity(body.getExchangeEntities());

        if (entity == null || entity.getType() != EntityType.SERVICE) {
           return false;
        }

        log.debug("DirectExchangePredicate matches, Identifier is {}", entity.getIdentifier());

        exchange.getIn().setHeader(ExchangeHeaders.DIRECT_EXCHANGE_ENTITY, entity);

        return true;
    }

    private Entity getTriggerWorkflowEntity(Map<String, Entity> exchangeEntities) {
        if (ObjectUtils.isEmpty(exchangeEntities)) {
            return null;
        }
        List<Entity> serviceEntities = exchangeEntities.values().stream()
                .filter(entity -> entity.getType() == EntityType.SERVICE)
                .toList();

        if (serviceEntities.isEmpty()) {
            return null;
        }

        // Only match one service per exchange
        Entity parentServiceEntity = serviceEntities.stream().filter(entity -> !StringUtils.hasText(entity.getParentKey()))
                .findFirst()
                .orElseGet(()->findParentEntity(serviceEntities.get(0)));

        if (parentServiceEntity != null && workflowEntityRelationService.entityFlowExists(parentServiceEntity.getId())) {
            return parentServiceEntity;
        }

        return null;
    }

    private Entity findParentEntity(Entity entity) {
        return entityServiceProvider.findByKey(entity.getParentKey());
    }

}
