package com.seckill.api.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Order Number Generator
 */
public class OrderNoGenerator {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    
    private static long sequence = 0L;
    private static long lastTime = -1L;

    /**
     * Generate unique order number
     * Format: yyyyMMddHHmmssSSS + random(4 digits) + sequence(3 digits)
     * Example: 20240101123456789001234
     */
    public static String generate() {
        long currentTime = System.currentTimeMillis();
        
        synchronized (OrderNoGenerator.class) {
            if (currentTime == lastTime) {
                sequence = (sequence + 1) % 1000;
            } else {
                sequence = 0;
                lastTime = currentTime;
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(DATE_FORMAT.format(new Date(currentTime)));
        sb.append(String.format("%03d", sequence));
        
        return sb.toString();
    }

    /**
     * Generate order number with prefix
     */
    public static String generateWithPrefix(String prefix) {
        return prefix + generate();
    }

    private OrderNoGenerator() {
    }
}
