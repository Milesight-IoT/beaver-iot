package com.milesight.beaveriot.scheduler.core;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettingsPO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface ScheduleSettingsRepository extends BaseJpaRepository<ScheduleSettingsPO, Long> {

    ScheduleSettingsPO findFirstByTaskKey(String taskKey);

    List<ScheduleSettingsPO> findAllByTaskKeyIn(Collection<String> taskKeys);


    @Query(value = "select distinct s.task_key from t_schedule_settings s left join t_scheduled_task t on s.task_key = t.task_key where t.id is null limit 10000", nativeQuery = true)
    List<String> findTerminatedTaskKeys();

    @Modifying
    @Transactional
    void deleteAllByTaskKeyIn(List<String> taskKeys);

}
