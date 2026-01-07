package com.milesight.beaveriot.entitytemplate.service;

import com.google.common.collect.Sets;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.integration.model.EntityTemplate;
import com.milesight.beaveriot.context.integration.model.EntityTemplateBuilder;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.entitytemplate.facade.IEntityTemplateFacade;
import com.milesight.beaveriot.entitytemplate.po.EntityTemplatePO;
import com.milesight.beaveriot.entitytemplate.repository.EntityTemplateRepository;
import com.milesight.beaveriot.entitytemplate.support.EntityTemplateSupporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * author: Luxb
 * create: 2025/8/20 9:44
 **/
@Slf4j
@Service
public class EntityTemplateService implements IEntityTemplateFacade {
    private final EntityTemplateRepository entityTemplateRepository;

    public EntityTemplateService(EntityTemplateRepository entityTemplateRepository) {
        this.entityTemplateRepository = entityTemplateRepository;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void save(EntityTemplate entityTemplate) {
        if (entityTemplate == null) {
            return;
        }

        batchSave(List.of(entityTemplate));
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void batchSave(List<EntityTemplate> entityTemplates) {
        if (CollectionUtils.isEmpty(entityTemplates)) {
            return;
        }

        Long userId = SecurityUserContext.getUserId();
        doBatchSave(userId, entityTemplates);
    }

    @Override
    public List<EntityTemplate> findAll() {
        return convertPOsToModels(entityTemplateRepository.findAll());
    }

    @Override
    public List<EntityTemplate> findByKeys(List<String> keys) {
        return convertPOsToModels(findPOByKeys(keys));
    }

    @Override
    public EntityTemplate findByKey(String key) {
        List<EntityTemplate> entityTemplates = findByKeys(List.of(key));
        if (CollectionUtils.isEmpty(entityTemplates)) {
            return null;
        }
        return entityTemplates.get(0);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void deleteByKey(String key) {
        deleteByKeys(List.of(key));
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void deleteByKeys(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }

        List<EntityTemplatePO> entityTemplatePOs = findPOByKeys(keys);
        if (CollectionUtils.isEmpty(entityTemplatePOs)) {
            return;
        }

        List<Long> ids = entityTemplatePOs.stream().map(EntityTemplatePO::getId).toList();
        entityTemplateRepository.deleteAllById(ids);
    }

    private void doBatchSave(Long userId, List<EntityTemplate> entityTemplates) {
        if (CollectionUtils.isEmpty(entityTemplates)) {
            return;
        }

        entityTemplates.forEach(EntityTemplate::initializeChildren);
        List<String> entityTemplateKeys = entityTemplates.stream().map(EntityTemplate::getKey).filter(StringUtils::hasText).toList();

        List<EntityTemplatePO> existEntityTemplatePOs = findPOByKeys(entityTemplateKeys);
        Map<String, EntityTemplatePO> existEntityTemplatePOMap = convertToPOMap(existEntityTemplatePOs);
        List<EntityTemplatePO> entityTemplatePOs = convertModelsToPOs(entityTemplates);
        Map<String, EntityTemplatePO> entityTemplatePOMap = convertToPOMap(entityTemplatePOs);

        List<EntityTemplatePO> toSavePOs = new ArrayList<>();
        Set<String> toDeleteKeys = Sets.difference(existEntityTemplatePOMap.keySet(), entityTemplatePOMap.keySet()).immutableCopy();
        Set<String> toCreateKeys = Sets.difference(entityTemplatePOMap.keySet(), existEntityTemplatePOMap.keySet()).immutableCopy();
        Set<String> maybeUpdateKeys = Sets.intersection(existEntityTemplatePOMap.keySet(), entityTemplatePOMap.keySet()).immutableCopy();

        if (!CollectionUtils.isEmpty(toDeleteKeys)) {
            List<EntityTemplatePO> toDeletePOs = toDeleteKeys.stream().map(existEntityTemplatePOMap::get).toList();
            entityTemplateRepository.deleteAll(toDeletePOs);
        }

        if (!CollectionUtils.isEmpty(toCreateKeys)) {
            List<EntityTemplatePO> toCreatePOs = toCreateKeys.stream()
                    .map(key -> {
                        EntityTemplatePO toCreatePO = entityTemplatePOMap.get(key);
                        toCreatePO.setId(SnowflakeUtil.nextId());
                        toCreatePO.setUserId(userId);
                        return toCreatePO;
                    }).toList();
            toSavePOs.addAll(toCreatePOs);
        }

        if (!CollectionUtils.isEmpty(maybeUpdateKeys)) {
            List<EntityTemplatePO> toUpdatePOs = maybeUpdateKeys.stream()
                    .map(key -> {
                        EntityTemplatePO newPO = entityTemplatePOMap.get(key);
                        EntityTemplatePO existPO = existEntityTemplatePOMap.get(key);
                        if (newPO.logicEquals(existPO)) {
                            return null;
                        }
                        newPO.setId(existPO.getId());
                        newPO.setUserId(existPO.getUserId());
                        newPO.setCreatedAt(existPO.getCreatedAt());
                        return newPO;
                    }).filter(Objects::nonNull).toList();

            if (!CollectionUtils.isEmpty(toUpdatePOs)) {
                toSavePOs.addAll(toUpdatePOs);
            }
        }

        if (!CollectionUtils.isEmpty(toSavePOs)) {
            entityTemplateRepository.saveAll(toSavePOs);
        }
    }

    private Map<String, EntityTemplatePO> convertToPOMap(List<EntityTemplatePO> entityTemplatePOs) {
        if (CollectionUtils.isEmpty(entityTemplatePOs)) {
            return Collections.emptyMap();
        }

        return entityTemplatePOs.stream().collect(Collectors.toMap(EntityTemplatePO::getKey, Function.identity()));
    }

    private List<EntityTemplatePO> findPOByKeys(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyList();
        }

        List<EntityTemplatePO> entityTemplatePOs = entityTemplateRepository.findAll(filter -> filter.in(EntityTemplatePO.Fields.key, keys.toArray()));
        if (CollectionUtils.isEmpty(entityTemplatePOs)) {
            return Collections.emptyList();
        }

        List<EntityTemplatePO> allEntityTemplatePOs = new ArrayList<>(entityTemplatePOs);
        List<String> parentKeys = entityTemplatePOs.stream()
                .filter(t -> t.getParent() == null)
                .map(EntityTemplatePO::getKey)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(parentKeys)) {
            return allEntityTemplatePOs;
        }

        List<EntityTemplatePO> childrenEntityTemplatePOs = entityTemplateRepository.findAll(
                filter -> filter.in(EntityTemplatePO.Fields.parent, parentKeys.toArray()));
        if (CollectionUtils.isEmpty(childrenEntityTemplatePOs)) {
            return allEntityTemplatePOs;
        }

        allEntityTemplatePOs.addAll(childrenEntityTemplatePOs);
        return allEntityTemplatePOs;
    }

    private List<EntityTemplatePO> convertModelsToPOs(List<EntityTemplate> entityTemplates) {
        if (CollectionUtils.isEmpty(entityTemplates)) {
            return Collections.emptyList();
        }

        Set<String> keys = new HashSet<>();
        List<EntityTemplatePO> entityTemplatePOs = new ArrayList<>();
        entityTemplates.forEach(entityTemplate -> {
            List<EntityTemplatePO> eachEntityTemplatePOs = convertModelToPOs(entityTemplate);
            if (!CollectionUtils.isEmpty(eachEntityTemplatePOs)) {
                eachEntityTemplatePOs.forEach(eachEntityTemplatePO -> {
                    if (!keys.contains(eachEntityTemplatePO.getKey())) {
                        entityTemplatePOs.add(eachEntityTemplatePO);
                        keys.add(eachEntityTemplatePO.getKey());
                    }
                });
            }
        });
        return entityTemplatePOs;
    }

    private List<EntityTemplatePO> convertModelToPOs(EntityTemplate entityTemplate) {
        if (entityTemplate == null) {
            return Collections.emptyList();
        }

        List<EntityTemplatePO> entityTemplatePOs = new ArrayList<>();
        EntityTemplatePO entityTemplatePO = convertModelToPO(entityTemplate);
        entityTemplatePOs.add(entityTemplatePO);

        if (CollectionUtils.isEmpty(entityTemplate.getChildren())) {
            return entityTemplatePOs;
        }

        List<EntityTemplatePO> childrenEntityTemplatePOs = entityTemplate.getChildren().stream()
                .map(this::convertModelToPO)
                .toList();
        entityTemplatePOs.addAll(childrenEntityTemplatePOs);
        return entityTemplatePOs;
    }

    private EntityTemplatePO convertModelToPO(EntityTemplate entityTemplate) {
        EntityTemplatePO entityTemplatePO = new EntityTemplatePO();
        entityTemplatePO.setKey(entityTemplate.getKey());
        entityTemplatePO.setName(entityTemplate.getName());
        entityTemplatePO.setType(entityTemplate.getType());
        entityTemplatePO.setAccessMod(entityTemplate.getAccessMod());
        entityTemplatePO.setValueStoreMod(entityTemplate.getValueStoreMod());
        entityTemplatePO.setParent(entityTemplate.getParentKey());
        entityTemplatePO.setValueType(entityTemplate.getValueType());
        entityTemplatePO.setValueAttribute(entityTemplate.getAttributes());
        entityTemplatePO.setDescription(entityTemplate.getDescription());
        entityTemplatePO.setVisible(entityTemplate.getVisible());
        return entityTemplatePO;
    }

    private List<EntityTemplate> convertPOsToModels(List<EntityTemplatePO> entityTemplatePOs) {
        if (CollectionUtils.isEmpty(entityTemplatePOs)) {
            return Collections.emptyList();
        }

        List<EntityTemplate> platEntityTemplates = entityTemplatePOs.stream().map(this::convertPOToModel).toList();
        return flatToNestedModels(platEntityTemplates);
    }

    private List<EntityTemplate> flatToNestedModels(List<EntityTemplate> flatEntityTemplates) {
        if (CollectionUtils.isEmpty(flatEntityTemplates)) {
            return Collections.emptyList();
        }

        List<EntityTemplate> nestedEntityTemplates = new ArrayList<>();
        Map<String, EntityTemplate> parentEntityTemplates = flatEntityTemplates.stream()
                .filter(entityTemplate -> entityTemplate.getParentKey() == null)
                .collect(Collectors.toMap(EntityTemplate::getKey, Function.identity()));

        if (!CollectionUtils.isEmpty(parentEntityTemplates)) {
            nestedEntityTemplates.addAll(parentEntityTemplates.values());
        }

        flatEntityTemplates.stream()
                .filter(entityTemplate -> entityTemplate.getParentKey() != null)
                .forEach(childEntityTemplate -> {
                    if (!parentEntityTemplates.containsKey(childEntityTemplate.getParentKey())) {
                        nestedEntityTemplates.add(childEntityTemplate);
                        return;
                    }

                    EntityTemplate parentEntityTemplate = parentEntityTemplates.get(childEntityTemplate.getParentKey());
                    parentEntityTemplate.addChild(childEntityTemplate);
                });
        return nestedEntityTemplates;
    }

    private EntityTemplate convertPOToModel(EntityTemplatePO entityTemplatePO) {
        EntityTemplateBuilder builder = EntityTemplateBuilder.builder()
                .id(entityTemplatePO.getId())
                .identifier(EntityTemplateSupporter.getIdentifierFromKey(entityTemplatePO.getKey()))
                .name(entityTemplatePO.getName())
                .type(entityTemplatePO.getType())
                .accessMod(entityTemplatePO.getAccessMod())
                .valueStoreMod(entityTemplatePO.getValueStoreMod())
                .parentIdentifier(EntityTemplateSupporter.getIdentifierFromKey(entityTemplatePO.getParent()))
                .valueType(entityTemplatePO.getValueType())
                .attributes(entityTemplatePO.getValueAttribute())
                .description(entityTemplatePO.getDescription())
                .visible(entityTemplatePO.getVisible());
        return builder.build();
    }
}