package dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Seckill Order Detail DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO implements Serializable {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long goodsId;
    private String goodsName;
    private BigDecimal goodsPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;
    private Date payTime;
    private Date cancelTime;
    private Date expireTime;
    private Date createTime;

    /**
     * Get remaining seconds to pay
     */
    public Long getRemainingSeconds() {
        if (expireTime == null) {
            return 0L;
        }
        long remaining = (expireTime.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
}
