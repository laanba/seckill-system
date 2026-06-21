package com.seckill.api.service.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Rate Limit Service using Redis sliding window
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Default max requests per time window
     */
    private static final int DEFAULT_MAX_REQUESTS = 10;
    
    /**
     * Default time window in seconds
     */
    private static final long DEFAULT_WINDOW_SECONDS = 60;

    /**
     * Check if request is allowed (sliding window algorithm)
     * 
     * @param userId user ID
     * @param apiName API name/identifier
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(Long userId, String apiName) {
        return isAllowed(userId, apiName, DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * Check if request is allowed with custom parameters
     */
    public boolean isAllowed(Long userId, String apiName, int maxRequests, long windowSeconds) {
        String key = getRateLimitKey(userId, apiName);
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000;//通过当前时间减去窗口时间，得到windowsatrt，在此之前的请求都要清除

        String script =
                "redis.call('ZREMRANGEBYSCORE',KEYS[1],0,ARGV[1])" +
                        "local count = redis.call('ZCARD',KEYS[1])"+
                        "if(count>=tonumber(ARGV[2]) then"+
                        "return 0"+
                        "else"+
                        "redis.call('ZADD',KEYS[1],ARGV[3],ARGV[3])"+
                        "redis.call('EXPIRE',KEYS[1],ARGV[4])"+
                        "return 1"+
                        "end";

        try{
            Long result = redisTemplate.execute(
                    //	public <T> T execute(RedisScript<T> script, List<K> keys, Object... args) {
                    //		return scriptExecutor.execute(script, keys, args);
                    //	}redisTemplte的execute方法
                    new DefaultRedisScript<>(script,Long.class),
                    //	public DefaultRedisScript(String script, @Nullable Class<T> resultType) {
                    //
                    //		this.setScriptText(script);
                    //		this.resultType = resultType;
                    //	}
                    Collections.singletonList(key),

                    String.valueOf(windowStart),
                    String.valueOf(maxRequests),
                    String.valueOf(now),
                    String.valueOf(windowSeconds));
            return Long.valueOf(1).equals(result);
        }catch (Exception e){
            log.error("Rate limit check failed: userId={},api={}",userId,apiName,e);
            return true;
        }
    }

    /**
     * Get current request count for user/api
     */
    public long getCurrentCount(Long userId, String apiName) {
        String key = getRateLimitKey(userId, apiName);
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0;
    }

    /**
     * Get remaining requests for user/api
     */
    public long getRemainingRequests(Long userId, String apiName, int maxRequests) {
        return Math.max(0, maxRequests - getCurrentCount(userId, apiName));
    }

    /**
     * Reset rate limit for user/api (for testing)
     */
    public void resetLimit(Long userId, String apiName) {
        String key = getRateLimitKey(userId, apiName);
        redisTemplate.delete(key);
        log.info("Rate limit reset: userId={}, api={}", userId, apiName);
    }

    private String getRateLimitKey(Long userId, String apiName) {
        return "seckill:ratelimit:" + userId + ":" + apiName;
    }
}
