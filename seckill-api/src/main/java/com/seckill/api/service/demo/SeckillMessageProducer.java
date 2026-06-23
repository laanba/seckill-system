package com.seckill.api.service.demo;

import constant.SeckillConstant;
import dto.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ Producer Service - Send async messages for order creation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Send seckill order message to RabbitMQ
     * @param message seckill message containing userId, goodsId, orderNo
     */
    public void sendSeckillMessage(Message message) {
        try {
            rabbitTemplate.convertAndSend(
                    SeckillConstant.SECKILL_EXCHANGE,
                    SeckillConstant.SECKILL_ROUTING_KEY,
                    message);
            log.info("Seckill message sent successfully: orderNo={}, userId={}, goodsId={}",
                    message.getOrderNo(), message.getUserId(), message.getGoodsId());
        } catch (Exception e) {
            log.error("Failed to send seckill message: orderNo={}", message.getOrderNo(), e);
            throw new RuntimeException("Failed to send order message", e);
        }
    }

    /**
     * Send order result message (for notification)
     */
    public void sendOrderResultMessage(String orderNo, boolean success) {
        try {
            String content = success ? "SUCCESS:" + orderNo : "FAILED:" + orderNo;
            rabbitTemplate.convertAndSend(
                    SeckillConstant.SECKILL_EXCHANGE,
                    SeckillConstant.SECKILL_RESULT_ROUTING_KEY,
                    content);
            log.info("Order result message sent: orderNo={}, success={}", orderNo, success);
        } catch (Exception e) {
            log.error("Failed to send order result message: orderNo={}", orderNo, e);
        }
    }
}
