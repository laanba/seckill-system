package com.seckill.api.entity;

import lombok.Data;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Seckill Order Entity
 */
@Data
@Table(name = "seckill_order")
public class Order implements Serializable {

    @Id
    private Long id;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "goods_id")
    private Long goodsId;

    @Column(name = "goods_name")
    private String goodsName;

    @Column(name = "goods_price")
    private BigDecimal goodsPrice;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "status")
    private Integer status;

    @Column(name = "pay_time")
    private Date payTime;

    @Column(name = "cancel_time")
    private Date cancelTime;

    @Column(name = "expire_time")
    private Date expireTime;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    /**
     * Check if order is pending payment
     */
    public boolean isPending() {
        return status != null && status == 0;
    }

    /**
     * Check if order is expired
     */
    public boolean isExpired() {
        return expireTime != null && new Date().after(expireTime);
    }

    /**
     * Check if order can be cancelled
     */
    public boolean canCancel() {
        return status != null && (status == 0 || status == 4);
    }
}
