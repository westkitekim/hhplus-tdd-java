package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @ParameterizedTest
    @CsvSource(value = {"5:true", "-3:false", "오십원:false"}, delimiter = ':')
    @DisplayName("충전요금은 자연수이다.")
    void validateChargeAmt(int trialCnt, boolean expected) {
    }

    @Test
    @DisplayName("입력받은 포인트를 충전할 수 있다.")
    void chargePoint(int trialCnt, boolean expected) {
    }
}
