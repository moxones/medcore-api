package com.medical.medcore.shared.tenancy;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class AfterTransaction {

    private AfterTransaction() {
    }

    public static void nowAndAfterCompletion(Runnable action) {
        action.run();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    action.run();
                }
            });
        }
    }
}
