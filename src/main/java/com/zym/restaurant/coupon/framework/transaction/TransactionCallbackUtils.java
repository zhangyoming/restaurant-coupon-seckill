package com.zym.restaurant.coupon.framework.transaction;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务提交后回调工具。
 *
 * Redis、MQ 这类外部系统不会跟随数据库事务自动回滚，
 * 所以涉及“数据库成功后再刷新缓存/发消息”的逻辑，统一放到事务提交后执行。
 */
public final class TransactionCallbackUtils {

    private TransactionCallbackUtils() {
    }

    public static void afterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
            return;
        }
        runnable.run();
    }
}
