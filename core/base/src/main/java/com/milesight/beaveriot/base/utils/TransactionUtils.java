package com.milesight.beaveriot.base.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@UtilityClass
public class TransactionUtils {

    public static void executeBeforeCommit(Runnable action) {
        registerSynchronization(action, TransactionPhase.BEFORE_COMMIT);
    }

    public static void executeAfterCommit(Runnable action) {
        registerSynchronization(action, TransactionPhase.AFTER_COMMIT);
    }

    public static void executeAfterRollback(Runnable action) {
        registerSynchronization(action, TransactionPhase.AFTER_ROLLBACK);
    }

    public static void executeAfterCompletion(Runnable action) {
        registerSynchronization(action, TransactionPhase.AFTER_COMPLETION);
    }

    public static void registerSynchronization(Runnable action, TransactionPhase transactionPhase) {
        registerSynchronization(action, transactionPhase, true);
    }

    public static void registerSynchronization(Runnable action, TransactionPhase transactionPhase, boolean executeIfNoTransaction) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            // Register a synchronization to run after the transaction completes
            TransactionSynchronization transactionSynchronization = new InnerTransactionSynchronizationAdapter(action, transactionPhase);
            TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);
        } else {
            log.trace("No active transaction.");
            if (executeIfNoTransaction) {
                log.trace("Execute immediately.");
                action.run();
            }
        }
    }

    private record InnerTransactionSynchronizationAdapter(Runnable action, TransactionPhase phase) implements TransactionSynchronization {

        @Override
        public void beforeCommit(boolean readOnly) {
            if (TransactionPhase.BEFORE_COMMIT.equals(phase)) {
                action.run();
            }
        }

        @Override
        public void afterCompletion(int status) {
            if ((this.phase == TransactionPhase.AFTER_COMMIT && status == TransactionSynchronization.STATUS_COMMITTED)
                    || (this.phase == TransactionPhase.AFTER_ROLLBACK && status == TransactionSynchronization.STATUS_ROLLED_BACK)
                    || (this.phase == TransactionPhase.AFTER_COMPLETION)) {
                this.action.run();
            }
        }
    }

}
