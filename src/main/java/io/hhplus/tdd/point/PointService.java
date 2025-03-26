package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 총 4가지 기본 기능 (포인트 조회, 포인트 충전, 포인트 사용 내역 조회, 포인트 사용)
 */
@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    /**
     * 사용자 포인트 정보 조회
     * @param userId
     * @return
     */
    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    /**
     * 포인트 사용내역 조회
     * @param userId
     * @return
     */
    public List<PointHistory> getPointHistoryByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전
     * @param userId
     * @param chargeAmt
     * @return
     */
    public UserPoint charge(long userId, long chargeAmt) {
        UserPoint currPoint = userPointTable.selectById(userId);

        currPoint.validate(TransactionType.CHARGE, chargeAmt);

        long addedPoint = currPoint.point() + chargeAmt;
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, addedPoint);

        pointHistoryTable.insert(userId, chargeAmt, TransactionType.CHARGE, System.currentTimeMillis());

        return updatedPoint;
    }

    /**
     * 포인트 차감
     * @param userId
     * @param useAmt
     * @return
     */
    public UserPoint use(long userId, long useAmt) {
        UserPoint currPoint = userPointTable.selectById(userId);

        currPoint.validate(TransactionType.USE, useAmt);

        long deductedPoint = currPoint.point() - useAmt;
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, deductedPoint);

        pointHistoryTable.insert(userId, useAmt, TransactionType.CHARGE, System.currentTimeMillis());

        return updatedPoint;
    }
}
