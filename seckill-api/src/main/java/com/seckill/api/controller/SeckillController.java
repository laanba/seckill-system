package com.seckill.api.controller;

import dto.Request;
import dto.Response;
import result.Result;
import com.seckill.api.service.SeckillService;
import com.seckill.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Seckill Controller - Core seckill endpoint
 */
@Slf4j
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {
    @Autowired
    private final SeckillService seckillService;
    private final UserService userService;

    /**
     * Execute seckill
     * 
     * Flow:
     * 1. Token validation
     * 2. Rate limit check
     * 3. Goods validation
     * 4. One-user-one-order check
     * 5. Redis stock decrement (atomic)
     * 6. Send message to RabbitMQ
     * 7. Return immediately (async order creation)
     */
    @PostMapping("/execute")
    public Result<Response> executeSeckill(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Validated @RequestBody Request request) {
        
        // 1. Validate token
        Long userId = userService.validateTokenAndGetUserId(token);
        if (userId == null) {
            log.warn("Seckill attempt without valid token");
            return Result.unauthorized("Please login first");
        }

        // 2. Validate request
        if (!request.isValid()) {
            return Result.badRequest("Invalid request");
        }

        // 3. Execute seckill
        log.info("Seckill request: userId={}, goodsId={}", userId, request.getGoodsId());
        Response response = seckillService.executeSeckill(userId, request.getGoodsId());
        
        return Result.success(response);
    }

    /**
     * Check seckill result (poll order status)
     */
    @GetMapping("/result/{orderNo}")
    public Result<Response> checkResult(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String orderNo) {
        
        Long userId = userService.validateTokenAndGetUserId(token);
        if (userId == null) {
            return Result.unauthorized("Please login first");
        }

        // For now, just return the order number
        // In production, you might check order status from database
        return Result.success(Response.queued(orderNo));
    }
}
