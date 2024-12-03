package com.milesight.beaveriot.dashboard.repository;

import com.milesight.beaveriot.dashboard.po.DashboardWidgetTemplatePO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;

/**
 * @author loong
 * @date 2024/10/17 16:39
 */
@Tenant
public interface DashboardWidgetTemplateRepository extends BaseJpaRepository<DashboardWidgetTemplatePO, Long> {
}
