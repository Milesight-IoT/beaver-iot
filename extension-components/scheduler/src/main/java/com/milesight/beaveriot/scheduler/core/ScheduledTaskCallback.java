package com.milesight.beaveriot.scheduler.core;

import com.milesight.beaveriot.scheduler.core.model.ScheduledTask;

import java.util.function.Consumer;


public interface ScheduledTaskCallback extends Consumer<ScheduledTask> {

}
