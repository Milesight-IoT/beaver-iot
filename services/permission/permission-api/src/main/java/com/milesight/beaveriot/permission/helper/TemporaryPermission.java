package com.milesight.beaveriot.permission.helper;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import eu.ciechanowiec.sneakyfun.SneakyRunnable;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import lombok.experimental.UtilityClass;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@UtilityClass
public class TemporaryPermission {

    private static final TransmittableThreadLocal<Deque<Context>> contextThreadLocal = new TransmittableThreadLocal<>();

    public static Context with(OperationPermissionCode... operationPermissionCodes) {
        return with(List.of(operationPermissionCodes));
    }

    public static Context with(Collection<OperationPermissionCode> operationPermissionCodes) {
        return new Context().with(operationPermissionCodes);
    }

    public static boolean contains(OperationPermissionCode... operationPermissionCodes) {
        return contains(List.of(operationPermissionCodes));
    }

    public static boolean contains(Collection<OperationPermissionCode> operationPermissionCodes) {
        var queue = contextThreadLocal.get();
        return queue != null && queue.stream().anyMatch(context -> context.contains(operationPermissionCodes));
    }

    public static void clear() {
        contextThreadLocal.remove();
    }

    public static class Context {

        private final Set<OperationPermissionCode> operationPermissionCodes = new HashSet<>();

        public Context with(OperationPermissionCode... operationPermissionCodes) {
            return with(List.of(operationPermissionCodes));
        }

        public Context with(Collection<OperationPermissionCode> operationPermissionCodes) {
            this.operationPermissionCodes.addAll(operationPermissionCodes);
            return this;
        }

        public boolean contains(OperationPermissionCode... operationPermissionCodes) {
            return contains(List.of(operationPermissionCodes));
        }

        public boolean contains(Collection<OperationPermissionCode> operationPermissionCodes) {
            return this.operationPermissionCodes.containsAll(operationPermissionCodes);
        }

        @SuppressWarnings({"java:S1130"})
        public <X extends Exception> void run(SneakyRunnable<X> runnable) throws X {
            var queue = getOrInitContexts();
            queue.push(this);
            try {
                runnable.run();
            } finally {
                queue.pop();
            }
        }

        public <T, X extends Exception> T supply(SneakySupplier<T, X> supplier) throws X {
            var queue = getOrInitContexts();
            queue.push(this);
            try {
                return supplier.get();
            } finally {
                queue.pop();
            }
        }

        private static Deque<Context> getOrInitContexts() {
            var queue = contextThreadLocal.get();
            if (queue == null) {
                queue = new ArrayDeque<>();
                contextThreadLocal.set(queue);
            }
            return queue;
        }

    }

}
