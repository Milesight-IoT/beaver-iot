package com.milesight.beaveriot.canvas.repository;

import com.milesight.beaveriot.canvas.po.CanvasEntityPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface CanvasEntityRepository extends BaseJpaRepository<CanvasEntityPO, Long> {

    @Modifying
    void deleteAllByCanvasId(Long canvasId);

    @Modifying
    void deleteAllByCanvasIdIn(List<Long> canvasId);
}
