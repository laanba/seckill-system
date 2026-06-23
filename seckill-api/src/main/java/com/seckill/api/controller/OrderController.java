package com.seckill.api.controller;

import dto.OrderDTO;
import com.seckill.api.service.OrderService;
import result.Result;
import com.seckill.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    @Autowired
    private final OrderService orderService;
    private final UserService userService;

    /**
     * Get user's orders
     */
    @GetMapping("/list")
    public Result<List<OrderDTO>> getUserOrders(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = userService.validateTokenAndGetUserId(token);
        if (userId == null) {
            return Result.unauthorized("Please login first");
        }

        List<OrderDTO> orders = orderService.getUserOrders(userId);
        return Result.success(orders);
    }

    /**
     * Get order detail
     */
    @GetMapping("/{orderNo}")
    public Result<OrderDTO> getOrderDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String orderNo) {
        
        Long userId = userService.validateTokenAndGetUserId(token);
        if (userId == null) {
            return Result.unauthorized("Please login first");
        }

        OrderDTO order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return Result.notFound("Order not found");
        }

        // Verify ownership
        if (!order.getUserId().equals(userId)) {
            return Result.notFound("Order not found");
        }

        return Result.success(order);
    }

    /**
     * Pay order
     */
    @PostMapping("/{orderNo}/pay")
    public Result<OrderDTO> payOrder(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String orderNo) {
        
        Long userId = userService.validateTokenAndGetUserId(token);
        if (userId == null) {
            return Result.unauthorized("Please login first");
        }

        log.info("Pay order request: userId={}, orderNo={}", userId, orderNo);
        OrderDTO order = orderService.payOrder(userId, orderNo);
        return Result.success("Payment successful", order);
    }

    /**
     * Cancel order
     */
    @PostMapping("/{orderNo}/cancel")
    public Result<OrderDTO> cancelOrder(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String orderNo) {
        
        Long userId = userService.validateTokenAndGetUserId(token);
        if (userId == null) {
            return Result.unauthorized("Please login first");
        }

        log.info("Cancel order request: userId={}, orderNo={}", userId, orderNo);
        OrderDTO order = orderService.cancelOrder(userId, orderNo);
        return Result.success("Order cancelled", order);
    }
}
