package com.milesight.beaveriot.scheduler.core;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettingsPO;

import java.util.Collection;
import java.util.List;

public interface ScheduleSettingsRepository extends BaseJpaRepository<ScheduleSettingsPO, Long> {

    ScheduleSettingsPO findFirstByTaskKey(String taskKey);

    List<ScheduleSettingsPO> findAllByTaskKeyIn(Collection<String> taskKeys);

}
