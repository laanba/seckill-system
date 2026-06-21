package com.seckill.api.service.Serviceimpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * User Order Check Service - Ensure one user can only order one item per seckill
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrderCheckService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Check if user has already ordered this goods
     * @param userId user ID
     * @param goodsId goods ID
     * @return true if user has already ordered
     */
    public boolean hasOrdered(Long userId, Long goodsId) {
        String key = getUserOrderKey(userId, goodsId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Mark user as ordered for this goods
     * @param userId user ID
     * @param goodsId goods ID
     * @param orderNo order number
     */
    public void markOrdered(Long userId, Long goodsId, String orderNo) {
        String key = getUserOrderKey(userId, goodsId);
        redisTemplate.opsForValue().set(key, orderNo);
        redisTemplate.expire(key,1, TimeUnit.HOURS);
        log.debug("User order marked: userId={}, goodsId={}, orderNo={}", userId, goodsId, orderNo);
    }

    /**
     * Get user's order number for this goods
     * @param userId user ID
     * @param goodsId goods ID
     * @return order number or null
     */
    public String getUserOrderNo(Long userId, Long goodsId) {
        String key = getUserOrderKey(userId, goodsId);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Remove user's order marker (for rollback)
     */
    public void removeOrderMark(Long userId, Long goodsId) {
        String key = getUserOrderKey(userId, goodsId);
        redisTemplate.delete(key);
        log.debug("User order mark removed: userId={}, goodsId={}", userId, goodsId);
    }

    private String getUserOrderKey(Long userId, Long goodsId) {
        return "seckill:user:order:" + userId + ":" + goodsId;
    }
}
