package com.milesight.beaveriot.base.executor;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

/**
 * This class is copied from the (<a href="https://github.com/chinabugotech/hutool">hutool</a>) project.
 * License: Mulan PSL v2
 */
public class BlockPolicy implements RejectedExecutionHandler {

    private final Consumer<Runnable> shutdownHandler;

    public BlockPolicy() {
        this(null);
    }

    public BlockPolicy(final Consumer<Runnable> shutdownHandler) {
        this.shutdownHandler = shutdownHandler;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        if (!e.isShutdown()) {
            try {
                e.getQueue().put(r);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Task " + r + " rejected from " + e);
            }
        } else if (null != shutdownHandler) {
            shutdownHandler.accept(r);
        }
    }
}
