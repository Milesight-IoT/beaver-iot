package com.milesight.beaveriot.entity.rule;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.PredicateNode;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.EXCHANGE_ENTITIES;

/**
 * @author leon
 */
@Slf4j
@Component
@RuleNode(value = RuleNodeNames.innerExchangeValidator, description = "innerExchangeValidator")
public class GenericExchangeValidator implements PredicateNode<ExchangePayload> {

    @Autowired
    private EntityServiceProvider entityServiceProvider;

    @Override
    public boolean matches(ExchangePayload exchange) {
        log.debug("Start ExchangeValidator matches, keys : {}", exchange.getKey());

        Map<String, Object> allPayloads = exchange.getAllPayloads();

        if (ObjectUtils.isEmpty(allPayloads)) {
            log.warn("ExchangeValidator matches failed, allPayloads is empty");
            return false;
        }

        Map<String, Entity> entityMap = exchange.getExchangeEntities();

        Boolean ignoreInvalidKey = exchange.getContext(ExchangeContextKeys.EXCHANGE_IGNORE_INVALID_KEY, false);

        if(ignoreInvalidKey) {
            exchange.entrySet().removeIf(entry -> {
                try{
                    return !validateEntity(entityMap.get(entry.getKey()));
                }catch (Exception e) {
                    log.error("Ignore Invalid Key :{}", entry.getKey());
                    return true;
                }
            });
        }else{
            boolean isValid = allPayloads.keySet().stream().allMatch(k -> validateEntity(entityMap.get(k)));
            if (!isValid) {
                return false;
            }
        }

        exchange.putContext(EXCHANGE_ENTITIES, entityMap);

        return true;
    }

    private boolean validateEntity(Entity entity) {

        if (entity == null) {
            log.warn("ExchangeValidator matches failed, entity is empty ");
            throw new ServiceException(ErrorCode.DATA_NO_FOUND, "ExchangeValidator matches failed, entity is empty");
        }
        if (!entity.loadActiveIntegration().isPresent()) {
            log.warn("ExchangeValidator matches failed, activeIntegration is empty :{}", entity.getIntegrationId());
            throw new ServiceException(ErrorCode.DATA_NO_FOUND, "ExchangeValidator matches failed, activeIntegration is empty " + entity.getIntegrationId());
        }
        if (StringUtils.hasText(entity.getDeviceKey()) && !entity.loadDevice().isPresent()) {
            log.warn("ExchangeValidator matches failed, device is empty : {}", entity.getDeviceKey());
            throw new ServiceException(ErrorCode.DATA_NO_FOUND, "ExchangeValidator matches failed, device is empty " + entity.getDeviceKey());
        }
        return true;
    }

}
