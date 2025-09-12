package com.milesight.beaveriot.canvas.repository;

import com.milesight.beaveriot.canvas.po.CanvasWidgetPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/14 17:14
 */
@Tenant
public interface CanvasWidgetRepository extends BaseJpaRepository<CanvasWidgetPO, Long> {

    @Modifying
    void deleteByCanvasIdIn(List<Long> canvasIdList);
}
