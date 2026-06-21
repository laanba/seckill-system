package com.seckill.api.constant;

/**
 * Seckill System Constants
 */
public class SeckillConstant {

    /**
     * Order Status
     */
    public static final int ORDER_STATUS_PENDING = 0;      // 待支付
    public static final int ORDER_STATUS_PAID = 1;         // 已支付
    public static final int ORDER_STATUS_CANCELLED = 2;    // 已取消
    public static final int ORDER_STATUS_EXPIRED = 3;      // 已超时
    public static final int ORDER_STATUS_CREATING = 4;     // 创建中

    /**
     * Goods Status
     */
    public static final int GOODS_STATUS_NOT_PUBLISHED = 0;  // 未发布
    public static final int GOODS_STATUS_COMING_SOON = 1;    // 即将开始
    public static final int GOODS_STATUS_ONGOING = 2;         // 进行中
    public static final int GOODS_STATUS_ENDED = 3;           // 已结束

    /**
     * User Status
     */
    public static final int USER_STATUS_NORMAL = 1;
    public static final int USER_STATUS_DISABLED = 0;

    /**
     * Redis Keys
     */
    public static final String STOCK_KEY_PREFIX = "seckill:stock:";
    public static final String LOCK_KEY_PREFIX = "seckill:lock:";
    public static final String USER_ORDER_KEY_PREFIX = "seckill:user:order:";
    public static final String RATE_LIMIT_KEY_PREFIX = "seckill:ratelimit:";
    public static final String GOODS_STATUS_KEY_PREFIX = "seckill:goods:status:";

    /**
     * RabbitMQ Exchange, Queue, and Routing Keys
     */
    public static final String SECKILL_EXCHANGE = "seckill_exchange";
    public static final String SECKILL_QUEUE = "seckill_queue";
    public static final String SECKILL_ROUTING_KEY = "seckill_order";
    public static final String SECKILL_RESULT_ROUTING_KEY = "seckill_order_result";

    /**
     * Response Messages
     */
    public static final String MSG_SUCCESS = "Success";
    public static final String MSG_GOODS_NOT_FOUND = "Goods not found";
    public static final String MSG_GOODS_NOT_STARTED = "Seckill has not started yet";
    public static final String MSG_GOODS_ENDED = "Seckill has ended";
    public static final String MSG_STOCK_NOT_ENOUGH = "Stock not enough";
    public static final String MSG_ALREADY_PURCHASED = "You have already purchased this item";
    public static final String MSG_ORDER_CREATED = "Order created, please pay";
    public static final String MSG_ORDER_NOT_FOUND = "Order not found";
    public static final String MSG_ORDER_EXPIRED = "Order has expired";
    public static final String MSG_PAYMENT_SUCCESS = "Payment successful";
    public static final String MSG_LOGIN_FAILED = "Login failed";
    public static final String MSG_TOKEN_INVALID = "Token invalid or expired";
    public static final String MSG_RATE_LIMITED = "Too many requests, please try again later";
    public static final String MSG_SYSTEM_ERROR = "System error, please try again later";

    private SeckillConstant() {
    }
}
