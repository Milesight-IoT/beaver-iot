package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.base.utils.TypeUtil;
import jakarta.persistence.Table;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class RepositoryAspectUtils {

    public static String getTableName(Class<?> repositoryInterface) {
        if(Repository.class.isAssignableFrom(repositoryInterface)){
            Class<?> repositoryClassType = (Class<?>) TypeUtil.getTypeArgument(repositoryInterface, 0);
            if (repositoryClassType != null) {
                Table annotation = AnnotationUtils.getAnnotation(repositoryClassType, Table.class);
                if (annotation != null) {
                    return annotation.name();
                }
            }
        }
        return null;
    }

    public static void doAfterTransactionCompletion(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive() &&
                TransactionSynchronizationManager.isSynchronizationActive()) {
            // Register a synchronization to run after the transaction completes
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    action.run();
                }
            });
        } else {
            // If no transaction is active, run immediately
            action.run();
        }
    }

    private RepositoryAspectUtils() {
    }

}
