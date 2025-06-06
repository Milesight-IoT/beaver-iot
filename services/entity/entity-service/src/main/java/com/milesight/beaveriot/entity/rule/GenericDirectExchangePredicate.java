package com.milesight.beaveriot.entity.rule;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.PredicateNode;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
@Slf4j
@Component
@RuleNode(value = RuleNodeNames.innerDirectExchangePredicate)
public class GenericDirectExchangePredicate implements PredicateNode<Exchange> {

    private EntityServiceProvider entityServiceProvider;

    public GenericDirectExchangePredicate(EntityServiceProvider entityServiceProvider) {
        this.entityServiceProvider = entityServiceProvider;
    }

    @Override
    public boolean matches(Exchange exchange) {

        ExchangePayload body = exchange.getIn().getBody(ExchangePayload.class);

        Entity entity = validateAndRetrieveCustomParentEntity(body.getExchangeEntities());

        if (entity == null || entity.getType() != EntityType.SERVICE) {
           return false;
        }

        log.debug("DirectExchangePredicate matches, Identifier is {}", entity.getIdentifier());

        exchange.getIn().setHeader(ExchangeHeaders.DIRECT_EXCHANGE_ENTITY, entity);

        return true;
    }

    private Entity validateAndRetrieveCustomParentEntity(Map<String, Entity> exchangeEntities) {
        if (ObjectUtils.isEmpty(exchangeEntities)) {
            return null;
        }
        List<Entity> customEntities = exchangeEntities.values().stream()
                .filter(entity -> entity.getIntegrationId().equals(IntegrationConstants.SYSTEM_INTEGRATION_ID)
                        && entity.getType() == EntityType.SERVICE)
                .toList();
        if (ObjectUtils.isEmpty(customEntities)) {
            return null;
        }

        //find parent entity
        return customEntities.stream()
                .filter(entity -> !StringUtils.hasText(entity.getParentKey()))
                .findFirst()
                .orElseGet(()->findParentEntity(customEntities.get(0)));
    }

    private Entity findParentEntity(Entity entity) {
        return entityServiceProvider.findByKey(entity.getParentKey());
    }

}
