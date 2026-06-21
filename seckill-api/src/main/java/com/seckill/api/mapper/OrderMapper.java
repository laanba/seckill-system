package com.seckill.api.mapper;

import com.seckill.api.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * Seckill Order Mapper
 */
@org.apache.ibatis.annotations.Mapper
public interface OrderMapper extends Mapper<Order> {

    /**
     * Find order by order number
     */
    @Select("SELECT * FROM seckill_order WHERE order_no = #{orderNo} LIMIT 1")
    Order findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * Find orders by user ID
     */
    @Select("SELECT * FROM seckill_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> findByUserId(@Param("userId") Long userId);

    /**
     * Find user's order for specific goods
     */
    @Select("SELECT * FROM seckill_order WHERE user_id = #{userId} AND goods_id = #{goodsId} LIMIT 1")
    Order findByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    /**
     * Check if user has existing order for goods (any non-cancelled status)
     */
    @Select("SELECT COUNT(*) FROM seckill_order WHERE user_id = #{userId} AND goods_id = #{goodsId} AND status != 2 AND status != 3")
    int countUserGoodsOrder(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    /**
     * Update order status
     */
    @Update("UPDATE seckill_order SET status = #{status}, update_time = NOW() WHERE id = #{orderId}")
    int updateStatus(@Param("orderId") Long orderId, @Param("status") Integer status);

    /**
     * Update order status with pay time
     */
    @Update("UPDATE seckill_order SET status = #{status}, pay_time = NOW(), update_time = NOW() WHERE id = #{orderId}")
    int updateStatusWithPayTime(@Param("orderId") Long orderId, @Param("status") Integer status);

    /**
     * Update order status with cancel time
     */
    @Update("UPDATE seckill_order SET status = #{status}, cancel_time = NOW(), update_time = NOW() WHERE id = #{orderId}")
    int updateStatusWithCancelTime(@Param("orderId") Long orderId, @Param("status") Integer status);

    /**
     * Find expired pending orders
     */
    @Select("SELECT * FROM seckill_order WHERE status = 0 AND expire_time < NOW()")
    List<Order> findExpiredOrders();
}
