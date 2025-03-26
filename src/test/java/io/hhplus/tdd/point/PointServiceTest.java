package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PointServiceTest {

    UserPointTable userPointTable;
    PointHistoryTable pointHistoryTable;
    PointService pointService;

    @BeforeEach
    void setUp() {
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Nested
    class SelectPointTest {
        @Test
        @DisplayName("사용자의 현재 포인트를 조회할 수 있다.")
        void getPointByUserId() {
            // given
            long userId = 1L;
            UserPoint mock = new UserPoint(userId, 3000, System.currentTimeMillis());

            when(userPointTable.selectById(userId)).thenReturn(mock);

            UserPoint result = pointService.getUserPoint(userId);

            assertThat(result).isEqualTo(mock);
        }

        @Test
        @DisplayName("사용자의 포인트 내역을 조회할 수 있다.")
        void getHistoryByUserId() {
            // given
            long id = 1L;
            long userId = 1L;
            List<PointHistory> mockList = List.of( new PointHistory(id, userId, 3000, TransactionType.CHARGE, System.currentTimeMillis()));
            when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockList);

            // when
            List<PointHistory> result = pointService.getPointHistoryByUserId(userId);
            // then
            assertThat(result).isEqualTo(mockList);
        }
    }

    @Nested
    class ChargePointTest {
        @Test
        @DisplayName("충전 후 금액이 최대잔고를 초과할 경우 에러를 던진다.")
        void charge_fail_exceed_max_balance() {
            long userId = 1L;
            when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 1000000L, System.currentTimeMillis()));

            assertThatThrownBy(() -> pointService.charge(userId, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("최대 보유 가능한 포인트 금액을 초과했습니다. 현재 잔여포인트를 확인하세요.");
            verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("충전금액이 0원 이하이면 포인트 충전시 에러를 던진다.")
        void charge_fail_min_amount() {
            long userId = 1L;
            when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));

            assertThatThrownBy(() -> pointService.charge(userId, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("포인트 요청금액은 0원을 초과해야 합니다");
            verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("사용자의 포인트를 충전할 수 있다.")
        void charge_success() {
            long userId = 1L;
            long currPoint = 200L;
            long chargePoint = 1800L;
            UserPoint beforePoint = new UserPoint(userId, currPoint, System.currentTimeMillis());
            UserPoint afterPoint = new UserPoint(userId, currPoint + chargePoint, System.currentTimeMillis());

            when(userPointTable.selectById(userId)).thenReturn(beforePoint);
            when(userPointTable.insertOrUpdate(userId, currPoint + chargePoint)).thenReturn(afterPoint);

            // when
            UserPoint result = pointService.charge(userId, chargePoint);

            // then
            assertThat(result.point()).isEqualTo(afterPoint.point());
            verify(pointHistoryTable).insert(eq(userId), eq(chargePoint), eq(TransactionType.CHARGE), anyLong());
        }
    }

    @Nested
    class UsePointTest {
        @Test
        @DisplayName("사용 포인트가 잔액을 초과할 경우 에러를 던진다.")
        void use_fail_insufficient_balance() {
            long userId = 1L;
            when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 100L, System.currentTimeMillis()));

            assertThatThrownBy(() -> pointService.use(userId, 300L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("포인트 사용 금액은 잔여 포인트를 초과할 수 없습니다. 현재 잔여포인트를 확인하세요.");
            verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("사용금액이 0원 이하이면 포인트 충전시 에러를 던진다.")
        void charge_fail_min_amount() {
            long userId = 1L;
            when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));

            assertThatThrownBy(() -> pointService.use(userId, -500L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("포인트 요청금액은 0원을 초과해야 합니다");
            verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("사용자는 자신의 포인트를 사용할 수 있다.")
        void use_success() {
            //given
            long userId = 1L;
            long currPoint = 10000L;
            long usePoint = 3000L;
            UserPoint beforePoint = new UserPoint(userId, currPoint, System.currentTimeMillis());
            UserPoint afterPoint = new UserPoint(userId, currPoint - usePoint, System.currentTimeMillis());
            when(userPointTable.selectById(userId)).thenReturn(beforePoint);
            when(userPointTable.insertOrUpdate(userId, currPoint - usePoint)).thenReturn(afterPoint);
            // when
            UserPoint result = pointService.use(userId, usePoint);
            // then
            assertThat(result.point()).isEqualTo(afterPoint.point());
            verify(pointHistoryTable).insert(eq(userId), eq(usePoint), eq(TransactionType.USE), anyLong());
        }
    }
}
