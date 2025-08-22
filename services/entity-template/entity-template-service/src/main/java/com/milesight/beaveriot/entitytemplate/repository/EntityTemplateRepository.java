package com.milesight.beaveriot.entitytemplate.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entitytemplate.po.EntityTemplatePO;
import com.milesight.beaveriot.permission.aspect.Tenant;

/**
 * author: Luxb
 * create: 2025/8/20 9:42
 **/
@Tenant
public interface EntityTemplateRepository extends BaseJpaRepository<EntityTemplatePO, Long> {
}
