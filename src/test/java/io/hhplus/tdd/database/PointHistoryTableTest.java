package io.hhplus.tdd.database;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.hhplus.tdd.point.TransactionType.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PointHistoryTable 테스트")
class PointHistoryTableTest {

    private final PointHistoryTable pointHistoryTable = new PointHistoryTable();

    @Test
    @DisplayName("입력받은 정보가 포인트 이력 테이블에 정상적으로 적재된다.")
    void insertPointHistoryTable() {
        // given
        long userId = 1L;
        long amount = 100L;
        TransactionType type = CHARGE;
        long updateMillis = System.currentTimeMillis();

        // when
        PointHistory result = pointHistoryTable.insert(userId, amount, type, updateMillis);

        // then
        assertThat(userId).isEqualTo(result.userId());
        assertThat(amount).isEqualTo(result.amount());
        assertThat(type).isEqualTo(result.type());
        assertThat(updateMillis).isEqualTo(result.updateMillis());
    }

    @Test
    @DisplayName("사용자에 해당하는 모든 포인트 이력을 조회할 수 있다.")
    void selectListHistoriesByUserId() {
        // given
        long userId = 1L;
        long amount = 100L;
        TransactionType type = CHARGE;
        long updateMillis = System.currentTimeMillis();
        pointHistoryTable.insert(userId, amount, type, updateMillis);

        long userId2 = 1L;
        long amount2 = 100L;
        TransactionType type2 = USE;
        long updateMillis2 = System.currentTimeMillis();
        pointHistoryTable.insert(userId2, amount2, type2, updateMillis2);

        // when
        List<PointHistory> result = pointHistoryTable.selectAllByUserId(userId);

        // then
        assertEquals(2, result.size());
        assertThat(userId).isEqualTo(result.get(0).userId());
        assertThat(amount).isEqualTo(result.get(0).amount());
        assertThat(type).isEqualTo(result.get(0).type());
        assertThat(updateMillis).isEqualTo(result.get(0).updateMillis());
        assertThat(userId2).isEqualTo(result.get(1).userId());
        assertThat(amount2).isEqualTo(result.get(1).amount());
        assertThat(type2).isEqualTo(result.get(1).type());
        assertThat(updateMillis2).isEqualTo(result.get(1).updateMillis());
    }
}
