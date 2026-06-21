package com.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Seckill Goods Detail DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsDTO implements Serializable {

    private Long id;
    private String goodsName;
    private String goodsDesc;
    private String goodsPicture;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer totalStock;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private String statusText;
    private Integer remainingSeconds;  // Seconds until start (for coming soon)

    /**
     * Calculate discount rate
     */
    public Integer getDiscountRate() {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        BigDecimal discount = BigDecimal.ONE.subtract(seckillPrice.divide(originalPrice, 2, BigDecimal.ROUND_HALF_UP));
        return discount.multiply(BigDecimal.valueOf(100)).intValue();
    }
}
