package com.milesight.beaveriot.canvas.repository;

import com.milesight.beaveriot.canvas.po.CanvasPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;

/**
 * CanvasRepository
 *
 * @author simon
 * @date 2025/9/8
 */
@Tenant
public interface CanvasRepository extends BaseJpaRepository<CanvasPO, Long> {
}
