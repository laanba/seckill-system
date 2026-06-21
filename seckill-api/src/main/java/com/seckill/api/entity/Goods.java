package com.seckill.api.entity;

import lombok.Data;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Seckill Goods Entity
 */
@Data
@Table(name = "seckill_goods")
public class Goods implements Serializable {

    @Id
    private Long id;

    @Column(name = "goods_name")
    private String goodsName;

    @Column(name = "goods_desc")
    private String goodsDesc;

    @Column(name = "goods_picture")
    private String goodsPicture;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "seckill_price")
    private BigDecimal seckillPrice;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "start_time")
    private Date startTime;

    @Column(name = "end_time")
    private Date endTime;

    @Column(name = "status")
    private Integer status;

    @Column(name = "total_stock")
    private Integer totalStock;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    /**
     * Check if seckill is ongoing
     */
    public boolean isOngoing() {
        Date now = new Date();
        return status != null && status == 2 
            && now.after(startTime) && now.before(endTime);
    }

    /**
     * Check if seckill is coming soon
     */
    public boolean isComingSoon() {
        Date now = new Date();
        return status != null && status == 1 
            && now.before(startTime);
    }

    /**
     * Get remaining stock
     */
    public Integer getRemainingStock() {
        return stock != null ? stock : 0;
    }
}
