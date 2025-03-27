package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PointController.class)
@DisplayName("PointController 단위테스트")
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @MockBean
    private UserPointTable userPointTable;

    @MockBean
    private PointHistoryTable pointHistoryTable;

    @Test
    @DisplayName("미사용자의 포인트 조회 시 0포인트가 반환된다.")
    void getNoUserPointZero() throws Exception {

        mockMvc.perform(get("/point/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(0));
    }

    @Test
    @DisplayName("사용자의 포인트 정보를 조회할 수 있다.")
    void getUserPoint_success() throws Exception {
        long userId = 1L;
        UserPoint mock = new UserPoint(userId, 1000L, System.currentTimeMillis());

        when(pointService.getUserPoint(userId)).thenReturn(mock);

        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(1000));
    }

    @Test
    @DisplayName("포인트 충전 성공시 누적된 포인트를 반환한다.")
    void charge_success() throws Exception {
        long userId = 1L;
        long amount = 3000L;
        UserPoint result = new UserPoint(userId, amount, System.currentTimeMillis());

        when(pointService.charge(userId, amount)).thenReturn(result);

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(amount));
    }

    @Test
    @DisplayName("포인트 사용 성공시 누적된 포인트에서 사용 포인트가 차감된다.")
    void use_success() throws Exception {
        // given
        long userId = 1L;
        long chargePoint = 3000L;
        long usePoint = 1000L;

        when(pointService.use(userId, usePoint))
                .thenReturn(new UserPoint(userId, 2000L, System.currentTimeMillis()));

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(usePoint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(chargePoint - usePoint));
    }

    @Test
    @DisplayName("사용자의 포인트 내역을 조회할 수 있다.")
    void getUserPointHistory_success() throws Exception {
        long userId = 1L;
        List<PointHistory> mockList = List.of(
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis())
        );

        when(pointService.getPointHistoryByUserId(userId)).thenReturn(mockList);

        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].amount").value(1000L))
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andExpect(jsonPath("$[1].amount").value(500L));
    }

    @Test
    @DisplayName("포인트 사용 시 잔액이 부족하면 예외를 던진다.")
    void usePoint_fail_insufficient_balance() throws Exception {
        long userId = 1L;
        long amount = 10_000L;

        when(pointService.use(userId, amount))
                .thenThrow(new IllegalArgumentException("포인트가 부족합니다."));

        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().is5xxServerError())  // or isBadRequest() if handled as 400
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("포인트가 부족합니다.", result.getResolvedException().getMessage()));
    }

    @Test
    @DisplayName("포인트 충전 시 최대 잔고를 초과하면 예외를 반환한다")
    void chargePoint_fail_maxBalanceExceeded() throws Exception {
        long userId = 1L;
        long amount = 10_000L;

        when(pointService.charge(userId, amount))
                .thenThrow(new IllegalArgumentException("최대 보유 가능한 포인트 금액을 초과했습니다. 현재 잔여포인트를 확인하세요."));

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().is5xxServerError())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals(
                        "최대 보유 가능한 포인트 금액을 초과했습니다. 현재 잔여포인트를 확인하세요.",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @DisplayName("충전/사용 시 0 이하의 금액이면 예외를 반환한다")
    void chargePoint_fail_zeroOrNegativeAmount() throws Exception {
        long userId = 1L;
        long amount = 0L;

        when(pointService.charge(userId, amount))
                .thenThrow(new IllegalArgumentException("포인트 요청금액은 0원을 초과해야 합니다"));

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().is5xxServerError())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals(
                        "포인트 요청금액은 0원을 초과해야 합니다",
                        result.getResolvedException().getMessage()));
    }

}
