package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PointServiceIntgrTest {

    @Autowired
    private PointService pointService;

    @Nested
    class ChargePointTest {
        @Test
        @DisplayName("포인트 충전 후 수정된 포인트 내역을 조회할 수 있다.")
        void getPointInfoAfterChargePoint() {
            long userId = 2L;
            long chargeAmt = 5000L;

            UserPoint userPoint = pointService.charge(userId, chargeAmt);

            // UserPointTable
            UserPoint userInfo = pointService.getUserPoint(userId);
            assertThat(chargeAmt).isEqualTo(userPoint.point());
            assertThat(chargeAmt).isEqualTo(userInfo.point());

            // PointHistoryTable
            List<PointHistory> userHistory = pointService.getPointHistoryByUserId(userId);
            assertThat(userHistory.size()).isEqualTo(1);
            assertThat(userHistory.get(0).amount()).isEqualTo(chargeAmt);
            assertThat(userHistory.get(0).type()).isEqualTo(TransactionType.CHARGE);
        }
    }

    @Nested
    class UsePointTest {
        @Test
        @DisplayName("포인트 사용 후 수정된 포인트 내역을 조회할 수 있다.")
        void getPointInfoAfterUsePoint() {
            long userId = 3L;
            long chargeAmt = 5000L;
            long useAmt = 1000L;
            UserPoint charged = pointService.charge(userId, chargeAmt);
            assertThat(charged.point()).isEqualTo(chargeAmt);

            UserPoint used = pointService.use(userId, useAmt);
            assertThat(chargeAmt - useAmt).isEqualTo(used.point()); // 현재포인트

            // UserPointTable
            UserPoint userInfo = pointService.getUserPoint(userId);
            assertThat(chargeAmt - useAmt).isEqualTo(userInfo.point());

            // PointHistoryTable
            List<PointHistory> userHistory = pointService.getPointHistoryByUserId(userId);
            assertThat(userHistory.size()).isEqualTo(2);
            assertThat(userHistory.get(0).type()).isEqualTo(TransactionType.CHARGE);
            assertThat(userHistory.get(1).type()).isEqualTo(TransactionType.USE);
        }

        @Test
        @DisplayName("포인트 충전/사용중 에러 발생시 포인트 이력은 적재되지 않는다")
        void noHistoryProcByException() {
            long userId = 4L;
            long chargeAmt = -400L; // 충전요청금액 음수: throw Exception

            pointService.charge(userId, 10000L); // init
            int beforeHistorySize = pointService.getPointHistoryByUserId(userId).size();

            assertThatThrownBy(() -> pointService.use(userId, chargeAmt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("포인트 요청금액은 0원을 초과해야 합니다");

            // 이력 변경 확인
            List<PointHistory> afterHistories = pointService.getPointHistoryByUserId(userId);
            assertThat(afterHistories).hasSize(beforeHistorySize);

            // 포인트 변경 확인
            UserPoint userPoint = pointService.getUserPoint(userId);
            assertThat(userPoint.point()).isEqualTo(10000L);
        }
    }

    @Nested
    class ConcurrencyTest {

        @Test
        @DisplayName("동일 사용자의 충전/사용 요청이 동시에 들어와도 포인트는 정확히 계산된다")
        void concurrentChargeAndUse_shouldMaintainCorrectPoint() throws InterruptedException {
            long userId = 300L;
            pointService.charge(userId, 5000L); // 초기 포인트

            ExecutorService executor = Executors.newFixedThreadPool(20);
            CountDownLatch latch = new CountDownLatch(20);

            // CHARGE
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    pointService.charge(userId, 1000L);
                    latch.countDown();
                });
            }

            // USE
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    try {
                        pointService.use(userId, 500L);
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // 초기 5000 + 충전 10000 - 사용 5000 = 10000
            UserPoint result = pointService.getUserPoint(userId);
            assertThat(result.point()).isEqualTo(10000L);
            assertThat(pointService.getPointHistoryByUserId(userId)).hasSize(21);
        }
    }

    @Test
    @DisplayName("서로 다른 사용자의 요청은 동시에 처리된다")
    void diffUsers_ProcessedConcurrently() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        long userId1 = 5L;
        long userId2 = 10L;

        long[] timestamps = new long[2];

        executor.submit(() -> {
            pointService.charge(userId1, 5000L);
            timestamps[0] = System.currentTimeMillis();
            latch.countDown();
        });

        executor.submit(() -> {
            pointService.charge(userId2, 5000L);
            timestamps[1] = System.currentTimeMillis();
            latch.countDown();
        });

        latch.await();

        long diff = Math.abs(timestamps[0] - timestamps[1]);
        System.out.println("처리 시간차: " + diff + "ms");

        // 500ms 이내에 처리되었으면 거의 동시에 처리된 것으로 간주
        assertThat(diff).isLessThan(500);
    }

}
