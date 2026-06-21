package com.seckill.api.mapper;

import com.seckill.api.entity.Goods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * Seckill Goods Mapper
 */
@org.apache.ibatis.annotations.Mapper
public interface GoodsMapper extends Mapper<Goods> {

    @Select("SELECT * from seckill_goods ")
    List<Goods> selectAll();
    /**
     * Find goods by status
     */
    @Select("SELECT * FROM seckill_goods WHERE status = #{status} ORDER BY start_time ASC")
    List<Goods> findByStatus(@Param("status") Integer status);

    /**
     * Find ongoing goods
     */
    @Select("SELECT * FROM seckill_goods WHERE status = 2 ORDER BY start_time ASC")
    List<Goods> findOngoingGoods();

    /**
     * Find coming soon goods
     */
    @Select("SELECT * FROM seckill_goods WHERE status = 1 ORDER BY start_time ASC")
    List<Goods> findComingSoonGoods();

    /**
     * Update stock (decrease by 1)
     */
    @Update("UPDATE seckill_goods SET stock = stock - 1, update_time = NOW() WHERE id = #{goodsId} AND stock > 0")
    int decreaseStock(@Param("goodsId") Long goodsId);

    /**
     * Update stock (increase by 1) - for rollback
     */
    @Update("UPDATE seckill_goods SET stock = stock + 1, update_time = NOW() WHERE id = #{goodsId}")
    int increaseStock(@Param("goodsId") Long goodsId);

    /**
     * Batch update goods status
     */
    @Update("UPDATE seckill_goods SET status = #{newStatus}, update_time = NOW() " +
            "WHERE status = #{oldStatus} " +
            "<if test='startTime != null'>AND start_time &lt;= #{currentTime}</if>" +
            "<if test='endTime != null'>AND end_time &lt;= #{currentTime}</if>" +
            "</script>")
    int updateStatusByCondition(@Param("oldStatus") Integer oldStatus, 
                                 @Param("newStatus") Integer newStatus,
                                 @Param("currentTime") String currentTime);
}
