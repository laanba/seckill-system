package com.seckill.api.service.Serviceimpl;

import dto.Message;
import dto.Response;
import entity.Goods;
import com.seckill.api.mapper.GoodsMapper;
import com.seckill.api.mapper.OrderMapper;
import constant.SeckillConstant;
import exception.SeckillException;
import com.seckill.api.service.demo.DistributedLockService;
import com.seckill.api.service.demo.RateLimitService;
import com.seckill.api.service.demo.RedisStockService;
import com.seckill.api.service.demo.SeckillMessageProducer;
import util.OrderNoGenerator;
import com.seckill.api.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Seckill Service Implementation - Core business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService{

    private final GoodsServiceImpl goodsService;
    private final GoodsMapper goodsMapper;
    private final OrderMapper orderMapper;
    private final RedisStockService redisStockService;
    private final DistributedLockService distributedLockService;
    private final RateLimitService rateLimitService;
    private final UserOrderCheckService userOrderCheckService;
    private final SeckillMessageProducer seckillMessageProducer;


    /**
     * Execute seckill operation
     * This is the core method that handles the seckill logic
     */
    @Override
    public Response executeSeckill(Long userId, Long goodsId) {
        log.info("Seckill request received: userId={}, goodsId={}", userId, goodsId);

        // 1. Rate limiting check
        if (!rateLimitService.isAllowed(userId, "seckill")) {
            log.warn("Rate limit exceeded: userId={}, goodsId={}", userId, goodsId);
            throw SeckillException.rateLimited();
        }

        // 2. Validate goods
        Goods goods = validateGoods(goodsId);

        // 3. Check if user already ordered this goods
        if (userOrderCheckService.hasOrdered(userId, goodsId)) {
            String existingOrderNo = userOrderCheckService.getUserOrderNo(userId, goodsId);
            log.warn("User already ordered: userId={}, goodsId={}, existingOrderNo={}",
                    userId, goodsId, existingOrderNo);
            throw SeckillException.alreadyPurchased();
        }

        // 4. Generate order number
        String orderNo = OrderNoGenerator.generateWithPrefix("SK");
        log.debug("Generated order number: orderNo={}", orderNo);

        // 5. Try to acquire distributed lock (for one-user-one-order guarantee)
        String lockValue = distributedLockService.generateLockValue();
        String lockKey = "user_goods_" + userId + "_" + goodsId;

        String acquiredLock = distributedLockService.tryLock(lockKey, lockValue, 5, java.util.concurrent.TimeUnit.SECONDS);
        if (acquiredLock == null) {
            log.warn("Failed to acquire lock: userId={}, goodsId={}", userId, goodsId);
            throw SeckillException.systemError();
        }

        try {
            // 6. Atomic stock decrement in Redis
            boolean stockDecremented = redisStockService.decreaseStock(goodsId);
            if (!stockDecremented) {
                log.info("Stock not enough: userId={}, goodsId={}", userId, goodsId);
                throw SeckillException.stockNotEnough();
            }

            // 7. Mark user as ordered in Redis
            userOrderCheckService.markOrdered(userId, goodsId, orderNo);

            // 8. Send message to RabbitMQ for async order creation
            Message message = Message.create(userId, goodsId, orderNo);
            seckillMessageProducer.sendSeckillMessage(message);

            log.info("Seckill request queued successfully: orderNo={}, userId={}, goodsId={}",
                    orderNo, userId, goodsId);

            return Response.queued(orderNo);

        } catch (SeckillException e) {
            // Rollback Redis operations
            redisStockService.increaseStock(goodsId);
            userOrderCheckService.removeOrderMark(userId, goodsId);
            throw e;
        } catch (Exception e) {
            // Rollback Redis operations
            redisStockService.increaseStock(goodsId);
            userOrderCheckService.removeOrderMark(userId, goodsId);
            log.error("Seckill failed unexpectedly: userId={}, goodsId={}", userId, goodsId, e);
            throw SeckillException.systemError();
        } finally {
            // Release lock
            distributedLockService.releaseLock(lockKey, lockValue);
        }
    }

    /**
     * Validate goods availability
     */
    private Goods validateGoods(Long goodsId) {
        Goods goods = goodsService.getGoodsById(goodsId);
        if (goods == null) {
            throw SeckillException.goodsNotFound();
        }

        // Check status
        if (goods.getStatus() == SeckillConstant.GOODS_STATUS_COMING_SOON) {
            throw SeckillException.goodsNotStarted();
        }

        if (goods.getStatus() == SeckillConstant.GOODS_STATUS_ENDED) {
            throw SeckillException.goodsEnded();
        }

        // Double check time
        Date now = new Date();
        if (now.before(goods.getStartTime())) {
            throw SeckillException.goodsNotStarted();
        }

        if (now.after(goods.getEndTime())) {
            throw SeckillException.goodsEnded();
        }

        return goods;
    }

    /**
     * Rollback stock (for timeout/cancelled orders)
     */
    @Override
    public void rollbackStock(Long goodsId) {
        redisStockService.increaseStock(goodsId);
        goodsMapper.increaseStock(goodsId);
        log.info("Stock rolled back: goodsId={}", goodsId);
    }

    /**
     * Rollback user order mark (for timeout/cancelled orders)
     */
    @Override
    public void rollbackUserOrderMark(Long userId, Long goodsId) {
        userOrderCheckService.removeOrderMark(userId, goodsId);
        log.info("User order mark rolled back: userId={}, goodsId={}", userId, goodsId);
    }
}
