package com.seckill.api.service.demo;

import com.rabbitmq.client.Channel;
import com.seckill.api.constant.SeckillConstant;
import com.seckill.api.dto.Message;
import com.seckill.api.entity.Goods;
import com.seckill.api.entity.Order;
import com.seckill.api.mapper.GoodsMapper;
import com.seckill.api.mapper.OrderMapper;
import com.seckill.api.exception.SeckillException;
import com.seckill.api.service.Serviceimpl.UserOrderCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;

/**
 * RabbitMQ Consumer Service - Process order creation asynchronously
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillMessageConsumer {

    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;
    private final RedisStockService redisStockService;
    private final UserOrderCheckService userOrderCheckService;

    @RabbitListener(queues = SeckillConstant.SECKILL_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received seckill message: orderNo={}, userId={}, goodsId={}",
                message.getOrderNo(), message.getUserId(), message.getGoodsId());

        try {
            processOrder(message);
            // Manual acknowledgment on success
            channel.basicAck(deliveryTag, false);
            log.info("Order processed successfully: orderNo={}", message.getOrderNo());
        } catch (Exception e) {
            log.error("Failed to process order: orderNo={}", message.getOrderNo(), e);
            // Rollback Redis operations
            handleOrderFailure(message, e.getMessage());
            try {
                // Reject message and requeue
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("Failed to nack message: orderNo={}", message.getOrderNo(), ioException);
            }
        }
    }

    /**
     * Process order creation
     */
    private void processOrder(Message message) {
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();
        String orderNo = message.getOrderNo();

        // 1. Check if order already exists (idempotency)
        Order existingOrder = orderMapper.findByOrderNo(orderNo);
        if (existingOrder != null) {
            log.warn("Order already exists: orderNo={}", orderNo);
            return;
        }

        // 2. Get goods info
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null) {
            throw SeckillException.goodsNotFound();
        }

        // 3. Create order
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setGoodsId(goodsId);
        order.setGoodsName(goods.getGoodsName());
        order.setGoodsPrice(goods.getSeckillPrice());
        order.setQuantity(1);
        order.setTotalAmount(goods.getSeckillPrice());
        order.setStatus(SeckillConstant.ORDER_STATUS_PENDING);
        order.setExpireTime(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // 15 minutes
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());

        // 4. Save order
        int result = orderMapper.insertSelective(order);
        if (result <= 0) {
            throw SeckillException.systemError();
        }

        // 5. Update database stock
        goodsMapper.decreaseStock(goodsId);

        log.info("Order created: orderId={}, orderNo={}", order.getId(), orderNo);
    }

    /**
     * Handle order creation failure
     */
    private void handleOrderFailure(Message message, String reason) {
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();
        String orderNo = message.getOrderNo();

        try {
            // Rollback Redis stock
            redisStockService.increaseStock(goodsId);

            // Remove user order marker
            userOrderCheckService.removeOrderMark(userId, goodsId);

            log.info("Order failure handled: orderNo={}, reason={}", orderNo, reason);
        } catch (Exception e) {
            log.error("Failed to handle order failure: orderNo={}", orderNo, e);
        }
    }
}
