package com.milesight.beaveriot.rule.components.timer;

import lombok.extern.slf4j.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;


@Slf4j
@SuppressWarnings({"java:S6548"})
public class SimpleTimerScheduler {

    private static final Timer timer = new Timer("SimpleTimerScheduler");

    private static final ConcurrentSkipListMap<Long, ConcurrentHashMap<String, SimpleTimerTask>> epochSecondsToTasks = new ConcurrentSkipListMap<>();

    private static final ConcurrentHashMap<String, SimpleTimerTask> taskIndex = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    private static final SimpleTimerScheduler scheduler = new SimpleTimerScheduler();

    public static SimpleTimerScheduler instance() {
        return scheduler;
    }

    private SimpleTimerScheduler() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    scanAndRunTasks();
                } catch (Exception e) {
                    log.error("Schedule task execution error", e);
                }
            }
        }, 0, 5000);
    }

    private void scanAndRunTasks() {
        var now = System.currentTimeMillis() / 1000;
        List<SimpleTimerTask> tasks;
        synchronized (lock) {
            var headMap = epochSecondsToTasks.headMap(now, true);
            tasks = headMap.values()
                    .stream()
                    .map(Map::values)
                    .flatMap(Collection::stream)
                    .toList();
            headMap.clear();
            tasks.forEach(task -> task.nextExecution()
                            .ifPresentOrElse(this::scheduleTask, () -> taskIndex.remove(task.getId())));
        }
        tasks.forEach(SimpleTimerTask::run);
    }

    public void scheduleTask(SimpleTimerConsumer consumer, SimpleTimerRule rule) {
        var task = new SimpleTimerTask(consumer.getId(), consumer, rule);
        scheduleTask(task);
    }

    public void scheduleTask(SimpleTimerTask task) {
        synchronized (lock) {
            cancelSchedule(task.getId());
            var executionEpochSecond = task.getExecutionEpochSecond();
            if (task.isExecuted() || executionEpochSecond == null) {
                log.info("given task is invalid: {}", task.getId());
                return;
            }
            taskIndex.put(task.getId(), task);
            epochSecondsToTasks.computeIfAbsent(executionEpochSecond, k -> new ConcurrentHashMap<>())
                    .put(task.getId(), task);
            log.info("schedule task: {} {}", task.getId(), executionEpochSecond);
        }
    }

    public void cancelTask(String id) {
        synchronized (lock) {
            cancelSchedule(id);
            taskIndex.remove(id);
            log.info("cancel task: {}", id);
        }
    }

    private void cancelSchedule(String id) {
        var existingTask = taskIndex.get(id);
        if (existingTask == null) {
            return;
        }
        var taskIdToTask = epochSecondsToTasks.get(existingTask.getExecutionEpochSecond());
        if (taskIdToTask != null) {
            taskIdToTask.remove(id);
        }
    }

    public void clear() {
        synchronized (lock) {
            taskIndex.clear();
            epochSecondsToTasks.clear();
            log.info("cancel all tasks.");
        }
    }

}
