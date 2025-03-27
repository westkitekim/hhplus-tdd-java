package io.hhplus.tdd.point;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class UserLockManager {

    private final Map<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    // Generic Callback(리턴값 Y)
    public <T> T executeLock(long userId, LockCallback<T> callback) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, id -> new ReentrantLock(true));
        lock.lock();
        try {
            return callback.call();
        } finally {
            lock.unlock();
        }
    }

    @FunctionalInterface
    public interface LockCallback<T> {
        T call();
    }
}
