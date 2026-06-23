package dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Seckill Message for RabbitMQ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Goods ID
     */
    private Long goodsId;

    /**
     * Order number
     */
    private String orderNo;

    /**
     * Request timestamp
     */
    private Long timestamp;

    /**
     * Create from request
     */
    public static Message create(Long userId, Long goodsId, String orderNo) {
        return Message.builder()
                .userId(userId)
                .goodsId(goodsId)
                .orderNo(orderNo)
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
