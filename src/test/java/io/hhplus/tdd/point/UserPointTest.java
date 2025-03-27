package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class UserPointTest {

    @Test
    @DisplayName("포인트 요청금액이 0이하이면 예외를 던진다.")
    void chargeFails_amountZeroOrLess() {
        // given
        UserPoint userPoint = new UserPoint(1L, 1000, System.currentTimeMillis());
        long chargeAmount = -100;

        // when & then
        assertThatThrownBy(() -> userPoint.validate(TransactionType.CHARGE, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트 요청금액은 0원을 초과해야 합니다.");
    }

    @Test
    @DisplayName("포인트 충전금액이 최대금액 1000000원을 초과하면 예외를 던진다.")
    void chargeFails_exceedMaxAmount() {
        // given
        UserPoint userPoint = new UserPoint(1L, 999999, System.currentTimeMillis());
        long chargeAmount = 2;

        // when & then
        assertThatThrownBy(() -> userPoint.validate(TransactionType.CHARGE, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 보유 가능한 포인트 금액을 초과했습니다. 현재 잔여포인트를 확인하세요.");
    }

    @Test
    @DisplayName("포인트 사용 금액이 잔여 포인트 금액을 초과하면 예외를 던진다.")
    void useFails_exceedMaxAmount() {
        // given
        UserPoint userPoint = new UserPoint(1L, 10000, System.currentTimeMillis());
        long chargeAmount = 20000;

        // when & then
        assertThatThrownBy(() -> userPoint.validate(TransactionType.USE, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트 사용 금액은 잔여 포인트를 초과할 수 없습니다. 현재 잔여포인트를 확인하세요.");
    }
}
