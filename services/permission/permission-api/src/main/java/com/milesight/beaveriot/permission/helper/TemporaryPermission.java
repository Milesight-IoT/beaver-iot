package com.milesight.beaveriot.permission.helper;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import eu.ciechanowiec.sneakyfun.SneakyRunnable;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public static Context with(DataPermissionType permission, String... ids) {
        return with(permission, List.of(ids));
    }

    public static Context with(DataPermissionType permission, Collection<String> ids) {
        return new Context().with(permission, ids);
    }

    public static boolean contains(OperationPermissionCode... operationPermissionCodes) {
        return contains(List.of(operationPermissionCodes));
    }

    public static boolean contains(Collection<OperationPermissionCode> operationPermissionCodes) {
        var queue = contextThreadLocal.get();
        return queue != null && queue.stream().anyMatch(context -> context.contains(operationPermissionCodes));
    }


    public static boolean contains(DataPermissionType permission, String... ids) {
        return contains(permission, List.of(ids));
    }

    public static boolean contains(DataPermissionType permission, Collection<String> ids) {
        var queue = contextThreadLocal.get();
        return queue != null && queue.stream().anyMatch(context -> context.contains(permission, ids));
    }

    @NonNull
    public static List<String> getResourceIds(DataPermissionType permission) {
        var result = new ArrayList<String>();
        var queue = contextThreadLocal.get();
        if (queue == null) {
            return result;
        }

        queue.stream().map(context -> context.dataPermissionToResourceIds.get(permission))
                .filter(Objects::nonNull)
                .forEach(result::addAll);
        return result;
    }

    public static void clear() {
        contextThreadLocal.remove();
    }

    public static class Context {

        private final Set<OperationPermissionCode> operationPermissionCodes = new HashSet<>();

        private final Map<DataPermissionType, Set<String>> dataPermissionToResourceIds = new EnumMap<>(DataPermissionType.class);

        public Context with(OperationPermissionCode... operationPermissionCodes) {
            return with(List.of(operationPermissionCodes));
        }

        public Context with(Collection<OperationPermissionCode> operationPermissionCodes) {
            for (OperationPermissionCode operationPermissionCode : operationPermissionCodes) {
                if (operationPermissionCode != null) {
                    this.operationPermissionCodes.add(operationPermissionCode);
                }
            }
            return this;
        }

        public Context with(DataPermissionType permission, String... ids) {
            return with(permission, List.of(ids));
        }

        public Context with(DataPermissionType permission, Collection<String> ids) {
            var resourceIds = this.dataPermissionToResourceIds.computeIfAbsent(permission, k -> new HashSet<>());
            for (String id : ids) {
                if (id != null && !id.isEmpty()) {
                    resourceIds.add(id);
                }
            }
            return this;
        }

        private boolean contains(Collection<OperationPermissionCode> operationPermissionCodes) {
            return this.operationPermissionCodes.containsAll(operationPermissionCodes);
        }

        private boolean contains(DataPermissionType permission, Collection<String> ids) {
            return this.dataPermissionToResourceIds.getOrDefault(permission, Set.of()).containsAll(ids);
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
