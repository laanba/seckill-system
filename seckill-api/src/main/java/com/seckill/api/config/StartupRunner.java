package com.seckill.api.config;

import com.seckill.api.service.Serviceimpl.GoodsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Application Startup Runner
 * Initialize Redis stock cache on application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupRunner implements ApplicationRunner {

    private final GoodsServiceImpl goodsService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing application...");
        
        // Initialize Redis stock for all ongoing seckill goods
        try {
            goodsService.initRedisStock();
            log.info("Redis stock initialization completed");
        } catch (Exception e) {
            log.warn("Redis stock initialization failed (Redis might not be available): {}", e.getMessage());
        }
        
        log.info("Application startup completed");
    }
}
