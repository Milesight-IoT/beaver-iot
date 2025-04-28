package com.milesight.beaveriot.scheduler.core;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTaskPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ScheduledTaskRepository extends BaseJpaRepository<ScheduledTaskPO, Long> {

    ScheduledTaskPO findFirstByTaskKeyAndExecutionEpochSecond(String taskKey, Long executionEpochSecond);

    @Query("select t from ScheduledTaskPO t where t.taskKey = :taskKey and t.triggeredAt = 0 order by t.executionEpochSecond asc")
    List<ScheduledTaskPO> findAllByTaskKey(@Param("taskKey") String taskKey);

    @Query("select t from ScheduledTaskPO t where t.executionEpochSecond >= :startEpochSecond and t.executionEpochSecond < :endEpochSecond and t.triggeredAt = 0")
    Page<ScheduledTaskPO> findAllTasksByExecutionEpochSecondBetween(@Param("startEpochSecond") Long startEpochSecond, @Param("endEpochSecond") Long endEpochSecond, Pageable pageable);

    @Modifying
    @Transactional
    @Query("update ScheduledTaskPO t set t.attempts = t.attempts + 1 where t.id in :ids")
    void increaseAttemptsByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("update ScheduledTaskPO t set t.triggeredAt = :triggeredAt where t.id in :ids")
    void updateTriggeredAtByIds(@Param("ids") List<Long> ids, @Param("triggeredAt") Long triggeredAt);

    @Modifying
    @Transactional
    @Query("update ScheduledTaskPO t set t.triggeredAt = :triggeredAt, t.attempts = t.attempts + 1 where t.id in :ids")
    void updateTriggeredAtAndAttemptsByIds(@Param("ids") List<Long> ids, @Param("triggeredAt") Long triggeredAt);

    @Modifying
    @Transactional
    @Query("delete from ScheduledTaskPO t where t.taskKey = :taskKey and t.triggeredAt = 0")
    void deleteByTaskKey(@Param("taskKey") String taskKey);

    @Modifying
    @Transactional
    @Query("delete from ScheduledTaskPO t where t.executionEpochSecond <= :expirationEpochSecond and t.triggeredAt > 0")
    void deleteAllExpired(@Param("expirationEpochSecond") Long expirationEpochSecond);

}
