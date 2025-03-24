package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    private static final long MAX_POINT = 1000000;
    public static final String MESSAGE_REQUEST_AMOUNT= "포인트 요청금액은 0원을 초과해야 합니다.";
    public static final String MESSAGE_MAX_BALANCE = "최대 보유 가능한 포인트 금액을 초과했습니다. 현재 잔여포인트를 확인하세요.";
    public static final String MESSAGE_INSUFFICIENT_BALANCE = "포인트 사용 금액은 잔여 포인트를 초과할 수 없습니다. 현재 잔여포인트를 확인하세요.";

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    /**
     * 필드 검증
     * <pre>
     *     1. 요청금액
     *     2. 잔고부족
     *     3. 최대잔고
     * </pre>
     * @param transactionType
     * @param requestAmt
     */
    public void validate(TransactionType transactionType, long requestAmt) {
        if (requestAmt <= 0) {
            throw new IllegalArgumentException(MESSAGE_REQUEST_AMOUNT);
        }

        switch (transactionType) {
            case CHARGE -> {
                if (point + requestAmt > MAX_POINT) {
                    throw new IllegalArgumentException(MESSAGE_MAX_BALANCE);
                }
            }
            case USE -> {
                if (point - requestAmt < 0) {
                    throw new IllegalArgumentException(MESSAGE_INSUFFICIENT_BALANCE);
                }
            }
        }
    }
}
