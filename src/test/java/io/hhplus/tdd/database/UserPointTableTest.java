package io.hhplus.tdd.database;

import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserPointTableTest {

    private final UserPointTable userPointTable = new UserPointTable();

    @Test
    @DisplayName("사용자 id가 없는 경우 사용자 기본 정보를 반환한다.")
    void getNoUserDefaultInfo() {
        // given
        long userId = 1L;
        long point = 0;

        // when
        UserPoint result = userPointTable.selectById(userId);

        // then
        assertThat(userId).isEqualTo(result.id());
        assertThat(point).isEqualTo(result.point());
        assertThat(result.updateMillis() > 0).isTrue();
    }

    @Test
    @DisplayName("사용자의 포인트 정보를 조회할 수 있다.")
    void getUserPointInfo() {
        // given, when
        UserPoint expected = userPointTable.insertOrUpdate(1L, 3500);

        // when
        UserPoint result = userPointTable.selectById(1L);

        // then
        assertThat(result.id()).isEqualTo(expected.id());
        assertThat(result.point()).isEqualTo(expected.point());
        assertThat(result.updateMillis() > 0).isTrue();
    }

    // insert point
    @Test
    @DisplayName("신규 사용자 포인트 정보를 저장할 수 있다.")
    void insertUserPointInfo() {
        // given
        long userId =1L;
        long point = 2000L;

        // when
        UserPoint result = userPointTable.insertOrUpdate(userId, point);

        // then
        assertThat(userId).isEqualTo(result.id());
        assertThat(point).isEqualTo(result.point());
        assertThat(result.updateMillis() > 0).isTrue();
    }

    // update point
    @Test
    @DisplayName("기존 사용자 포인트 정보를 수정할 수 있다.")
    void updateUserPointInfo() {
        // given
        long userId =1L;
        long point = 2000L;
        userPointTable.insertOrUpdate(userId, point);

        // when
        long modifiedPoint = 2300L;
        UserPoint result = userPointTable.insertOrUpdate(userId, modifiedPoint);

        // then
        assertThat(userId).isEqualTo(result.id());
        assertThat(modifiedPoint).isEqualTo(result.point());
        assertThat(result.updateMillis() > 0).isTrue();
    }
}

