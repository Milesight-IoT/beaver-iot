package com.milesight.beaveriot.permission.helper;

import com.alibaba.ttl.TransmittableThreadLocal;
import eu.ciechanowiec.sneakyfun.SneakyRunnable;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public class TenantValidationBypass {

    private static final TransmittableThreadLocal<AtomicInteger> bypassDepth = new TransmittableThreadLocal<>();

    public <X extends Exception> void run(SneakyRunnable<X> runnable) throws X {
        incrementOrInitDepth();

        try {
            runnable.run();
        } finally {
            bypassDepth.get().decrementAndGet();
        }
    }

    public <T, X extends Exception> T supply(SneakySupplier<T, X> supplier) throws X {
        incrementOrInitDepth();

        try {
            return supplier.get();
        } finally {
            bypassDepth.get().decrementAndGet();
        }
    }

    private static void incrementOrInitDepth() {
        if (bypassDepth.get() == null) {
            bypassDepth.set(new AtomicInteger(0));
        }
        bypassDepth.get().incrementAndGet();
    }

    public static boolean isBypassed() {
        return bypassDepth.get() != null && bypassDepth.get().get() > 0;
    }

}
