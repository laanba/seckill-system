package com.seckill.api.service.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Distributed Lock Service using Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Lock timeout in seconds
     */
    private static final long LOCK_TIMEOUT = 10;

    /**
     * Try to acquire distributed lock
     * @param key lock key
     * @param value lock value (usually userId or UUID)
     * @return lock value if acquired, null if failed
     */
    public String tryLock(String key, String value) {
        String lockKey = getLockKey(key);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, value, LOCK_TIMEOUT, TimeUnit.SECONDS);
        
        if (Boolean.TRUE.equals(success)) {
            log.debug("Lock acquired: key={}, value={}", key, value);
            return value;
        }
        
        log.debug("Lock acquisition failed: key={}", key);
        return null;
    }

    /**
     * Try to acquire lock with custom timeout
     */
    public String tryLock(String key, String value, long timeout, TimeUnit unit) {
        String lockKey = getLockKey(key);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, value, timeout, unit);
        
        if (Boolean.TRUE.equals(success)) {
            log.debug("Lock acquired: key={}, value={}, timeout={}", key, value, timeout);
            return value;
        }
        
        return null;
    }

    /**
     * Release distributed lock
     * Only release if the lock value matches (prevent releasing others' lock)
     */
    public boolean releaseLock(String key, String value) {
        String lockKey = getLockKey(key);
//        String currentValue = redisTemplate.opsForValue().get(lockKey);
//
//        if (value.equals(currentValue)) {
//            redisTemplate.delete(lockKey);
//            log.debug("Lock released: key={}, value={}", key, value);
//            return true;
        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else " +
                        "return 0 " +
                        "end";

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(lockKey),
                value
        );

        boolean success = Long.valueOf(1).equals(result);
        if (success) {
            log.debug("Lock released: key={}, value={}", key, value);
        } else {
            log.warn("Lock release failed (value mismatch or lock expired): key={}, value={}",
                    key, value);
        }
        return success;
    }

    /**
     * Check if lock exists
     */
    public boolean isLocked(String key) {
        String lockKey = getLockKey(key);
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * Extend lock expiration
     */
    public boolean extendLock(String key, String value, long timeout, TimeUnit unit) {
        String lockKey = getLockKey(key);
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        
        if (value.equals(currentValue)) {
            return Boolean.TRUE.equals(redisTemplate.expire(lockKey, timeout, unit));
        }
        
        return false;
    }

    /**
     * Generate unique lock value
     */
    public String generateLockValue() {
        return UUID.randomUUID().toString();
    }

    private String getLockKey(String key) {
        return "seckill:lock:" + key;
    }
}
