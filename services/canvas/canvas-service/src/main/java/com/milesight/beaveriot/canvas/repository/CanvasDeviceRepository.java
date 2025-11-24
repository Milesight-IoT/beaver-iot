package com.milesight.beaveriot.canvas.repository;

import com.milesight.beaveriot.canvas.po.CanvasDevicePO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

/**
 * CanvasDeviceRepository
 *
 * @author simon
 * @date 2025/9/18
 */

public interface CanvasDeviceRepository extends BaseJpaRepository<CanvasDevicePO, Long> {
    @Modifying
    void deleteAllByCanvasIdIn(List<Long> canvasId);

    @Modifying
    void deleteAllByCanvasIdAndDeviceIdIn(Long canvasId, List<Long> deviceIds);
}
