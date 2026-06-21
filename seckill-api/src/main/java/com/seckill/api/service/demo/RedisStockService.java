package com.seckill.api.service.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis Stock Service - Handle stock operations with Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Lua script for atomic stock decrement
     * Returns: 1 = success, 0 = stock not enough, -1 = key not exists
     */
    private static final String DECREASE_STOCK_SCRIPT =
            "if redis.call('exists', KEYS[1]) == 1 then " +
            "local stock = tonumber(redis.call('get', KEYS[1])); " +
            "if stock > 0 then " +
            "redis.call('decr', KEYS[1]); " +
            "return 1; " +
            "else return 0; " +
            "end; " +
            "else return -1; " +
            "end;";

    /**
     * Lua script for atomic stock increment (rollback)
     */
    private static final String INCREASE_STOCK_SCRIPT =
            "if redis.call('exists', KEYS[1]) == 1 then " +
            "redis.call('incr', KEYS[1]); " +
            "return 1; " +
            "else return 0; " +
            "end;";

    /**
     * Initialize stock in Redis
     */
    public void initStock(Long goodsId, Integer stock) {
        String key = getStockKey(goodsId);
        redisTemplate.opsForValue().set(key, String.valueOf(stock));
        log.info("Initialized stock in Redis: goodsId={}, stock={}", goodsId, stock);
    }

    /**
     * Get current stock from Redis
     */
    public Integer getStock(Long goodsId) {
        String key = getStockKey(goodsId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : null;
    }

    /**
     * Atomic decrease stock using Lua script
     * @return true if stock decreased successfully
     */
    public boolean decreaseStock(Long goodsId) {
        String key = getStockKey(goodsId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DECREASE_STOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key));
        
        if (result == null || result < 0) {
            log.warn("Stock not initialized in Redis: goodsId={}", goodsId);
            return false;
        }
        
        if (result == 0) {
            log.info("Stock not enough: goodsId={}", goodsId);
            return false;
        }
        
        log.debug("Stock decreased successfully: goodsId={}", goodsId);
        return true;
    }

    /**
     * Rollback stock (increase)
     */
    public boolean increaseStock(Long goodsId) {
        String key = getStockKey(goodsId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCREASE_STOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key));
        
        if (result != null && result == 1) {
            log.info("Stock rolled back successfully: goodsId={}", goodsId);
            return true;
        }
        
        log.warn("Failed to rollback stock: goodsId={}", goodsId);
        return false;
    }

    /**
     * Delete stock key from Redis
     */
    public void deleteStock(Long goodsId) {
        String key = getStockKey(goodsId);
        redisTemplate.delete(key);
        log.info("Deleted stock from Redis: goodsId={}", goodsId);
    }

    /**
     * Set expiration for stock key
     */
    public void setStockExpire(Long goodsId, long seconds) {
        String key = getStockKey(goodsId);
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    private String getStockKey(Long goodsId) {
        return "seckill:stock:" + goodsId;
    }
}
