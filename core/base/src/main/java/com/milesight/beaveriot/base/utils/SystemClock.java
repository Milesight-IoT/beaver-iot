package com.milesight.beaveriot.base.utils;

import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SYSTEM CLOCK<br>
 * Optimization of performance issues of System.currentTimeMillis() in high concurrency scenarios
 * The call to System.currentTimeMillis() is much more time-consuming than new for an ordinary object (I haven’t tested how much longer it takes, some say it’s about 100 times)
 * System.currentTimeMillis() The reason why it was slow was because I had to deal with the system once.
 * The clock is updated regularly in the background, and when the JVM exits, the thread is automatically recycled
 * <p>
 * see： http://git.oschina.net/yu120/sequence
 *
 * @author lry, looly
 */
public class SystemClock {

    /**
     * Clock update interval in milliseconds
     */
    private final long period;
    /**
     * The current time in milliseconds
     */
    private volatile long now;

    /**
     * @param period Clock update interval in milliseconds
     */
    public SystemClock(long period) {
        this.period = period;
        this.now = System.currentTimeMillis();
        scheduleClockUpdating();
    }

    /**
     * Start timer thread
     */
    private void scheduleClockUpdating() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "System Clock");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(() -> now = System.currentTimeMillis(), period, period, TimeUnit.MILLISECONDS);
    }

    /**
     *
     */
    private long currentTimeMillis() {
        return now;
    }

    //------------------------------------------------------------------------ static

    /**
     * @author Looly
     */
    private static class InstanceHolder {
        public static final SystemClock INSTANCE = new SystemClock(1);
    }

    /**
     * @return current time
     */
    public static long now() {
        return InstanceHolder.INSTANCE.currentTimeMillis();
    }

    /**
     * @return Current time string representation
     */
    public static String nowDate() {
        return new Timestamp(InstanceHolder.INSTANCE.currentTimeMillis()).toString();
    }
}
