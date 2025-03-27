package io.hhplus.tdd.point;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class UserLockManager {
    private final Map<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public void executeLock(long userId, Runnable task) {
        // key가 없을 때만 lock 신규(Atomic)
        ReentrantLock lock = userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
        lock.lock();
        try {
            task.run();
        } finally {
            lock.unlock();
        }
    }
}
