package exception;

import constant.SeckillConstant;
import lombok.Getter;

/**
 * Business Exception for Seckill System
 */
@Getter
public class SeckillException extends RuntimeException {

    private final int code;

    public SeckillException(String message) {
        super(message);
        this.code = 500;
    }

    public SeckillException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SeckillException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // Predefined exceptions
    public static SeckillException goodsNotFound() {
        return new SeckillException(404, SeckillConstant.MSG_GOODS_NOT_FOUND);
    }

    public static SeckillException goodsNotStarted() {
        return new SeckillException(400, SeckillConstant.MSG_GOODS_NOT_STARTED);
    }

    public static SeckillException goodsEnded() {
        return new SeckillException(400, SeckillConstant.MSG_GOODS_ENDED);
    }

    public static SeckillException stockNotEnough() {
        return new SeckillException(400, SeckillConstant.MSG_STOCK_NOT_ENOUGH);
    }

    public static SeckillException alreadyPurchased() {
        return new SeckillException(400, SeckillConstant.MSG_ALREADY_PURCHASED);
    }

    public static SeckillException orderNotFound() {
        return new SeckillException(404, SeckillConstant.MSG_ORDER_NOT_FOUND);
    }

    public static SeckillException orderExpired() {
        return new SeckillException(400, SeckillConstant.MSG_ORDER_EXPIRED);
    }

    public static SeckillException rateLimited() {
        return new SeckillException(429, SeckillConstant.MSG_RATE_LIMITED);
    }

    public static SeckillException systemError() {
        return new SeckillException(500, SeckillConstant.MSG_SYSTEM_ERROR);
    }

    public static SeckillException loginFailed() {
        return new SeckillException(401, SeckillConstant.MSG_LOGIN_FAILED);
    }

    public static SeckillException tokenInvalid() {
        return new SeckillException(401, SeckillConstant.MSG_TOKEN_INVALID);
    }
}
