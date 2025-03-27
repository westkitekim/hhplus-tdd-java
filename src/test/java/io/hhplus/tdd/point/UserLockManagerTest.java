package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class UserLockManagerTest{

    private final UserLockManager userLockManager = new UserLockManager();

    @Test
    @DisplayName("리턴값이 있는 executeLock 메서드가 정상 작동한다")
    void executeLock_withReturnValue() {
        String result = userLockManager.executeLock(1L, () -> "locked-result");

        assertThat(result).isEqualTo("locked-result");
    }

    @Test
    @DisplayName("리턴값이 없는 executeLock 메서드가 정상 작동한다")
    void executeLock_withoutReturnValue() {

        AtomicInteger value = new AtomicInteger(0);

        userLockManager.executeLock(1L, () -> value.set(20));

        assertThat(value.get()).isEqualTo(20);
    }

    @Test
    @DisplayName("동일한 userId에 대해 동일한 Lock 객체를 반환한다.")
    void sameUserId_shouldUseSameLockInstance() throws Exception {
        // given
        UserLockManager lockManager = new UserLockManager();

        // when
        lockManager.executeLock(1L, () -> {});
        lockManager.executeLock(1L, () -> {});

        // then
        Field userLocksField = UserLockManager.class.getDeclaredField("userLocks");
        userLocksField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<Long, ReentrantLock> userLocks =
                (Map<Long, ReentrantLock>) userLocksField.get(lockManager);

        ReentrantLock lock1 = userLocks.get(1L);
        ReentrantLock lock2 = userLocks.get(1L);

        assertThat(lock1).isSameAs(lock2);
    }

    @Test
    @DisplayName("요청 순서대로 Lock을 획득한다.")
    void fairLock_shouldAcquireLockInOrder() throws InterruptedException {
        UserLockManager lockManager = new UserLockManager();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(5);

        for (int i = 1; i <= 5; i++) {
            final int idx = i;
            executor.submit(() -> {
                lockManager.executeLock(1L, () -> {
                    order.add(idx);
                    sleep(10);
                });
                done.countDown();
            });
            Thread.sleep(5);
        }

        done.await(1, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(order).containsExactly(1, 2, 3, 4, 5);
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }
}
