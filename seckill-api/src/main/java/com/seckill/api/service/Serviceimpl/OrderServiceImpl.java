package com.seckill.api.service.Serviceimpl;

import dto.OrderDTO;
import entity.Order;
import com.seckill.api.mapper.GoodsMapper;
import com.seckill.api.mapper.OrderMapper;
import constant.SeckillConstant;
import exception.SeckillException;
import com.seckill.api.service.OrderService;
import com.seckill.api.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Service Implementation
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;
    private final SeckillService seckillService;

    /**
     * Get order by order number
     */
    @Override
    public OrderDTO    getOrderByOrderNo(String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            return null;
        }
        return convertToDTO(order);
    }

    /**
     * Get order by ID
     */
    @Override
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            return null;
        }
        return convertToDTO(order);
    }

    /**
     * Get user's orders
     */
    @Override
    public List<OrderDTO> getUserOrders(Long userId) {
        List<Order> orders = orderMapper.findByUserId(userId);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if user has ordered specific goods
     */
    public boolean hasUserOrdered(Long userId, Long goodsId) {
        return orderMapper.countUserGoodsOrder(userId, goodsId) > 0;
    }

    /**
     * Pay order
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO payOrder(Long userId, String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw SeckillException.orderNotFound();
        }

        // Verify user
        if (!order.getUserId().equals(userId)) {
            throw SeckillException.orderNotFound();
        }

        // Check order status
        if (order.getStatus() != SeckillConstant.ORDER_STATUS_PENDING) {
            throw new SeckillException(400, "Order cannot be paid");
        }

        // Check if expired
        if (order.isExpired()) {
            cancelExpiredOrder(order);
            throw SeckillException.orderExpired();
        }

        // Update to paid
        orderMapper.updateStatusWithPayTime(order.getId(), SeckillConstant.ORDER_STATUS_PAID);
        order.setStatus(SeckillConstant.ORDER_STATUS_PAID);
        
        log.info("Order paid successfully: orderNo={}", orderNo);
        return convertToDTO(order);
    }

    /**
     * Cancel order
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO cancelOrder(Long userId, String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw SeckillException.orderNotFound();
        }

        // Verify user
        if (!order.getUserId().equals(userId)) {
            throw SeckillException.orderNotFound();
        }

        // Check if can cancel
        if (!order.canCancel()) {
            throw new SeckillException(400, "Order cannot be cancelled");
        }

        // Update to cancelled
        orderMapper.updateStatusWithCancelTime(order.getId(), SeckillConstant.ORDER_STATUS_CANCELLED);
        order.setStatus(SeckillConstant.ORDER_STATUS_CANCELLED);

        // Rollback stock and user order mark
        seckillService.rollbackStock(order.getGoodsId());
        seckillService.rollbackUserOrderMark(userId, order.getGoodsId());

        log.info("Order cancelled: orderNo={}", orderNo);
        return convertToDTO(order);
    }

    /**
     * Handle expired orders - scheduled task
     */
    @Override
//    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional(rollbackFor = Exception.class)
    public void handleExpiredOrders() {
        List<Order> expiredOrders = orderMapper.findExpiredOrders();
        
        for (Order order : expiredOrders) {
            try {
                cancelExpiredOrder(order);
                log.info("Expired order handled: orderNo={}", order.getOrderNo());
            } catch (Exception e) {
                log.error("Failed to handle expired order: orderNo={}", order.getOrderNo(), e);
            }
        }
        
        if (!expiredOrders.isEmpty()) {
            log.info("Handled {} expired orders", expiredOrders.size());
        }
    }

    /**
     * Cancel expired order
     */
    private void cancelExpiredOrder(Order order) {
        orderMapper.updateStatusWithCancelTime(order.getId(), SeckillConstant.ORDER_STATUS_EXPIRED);
        
        // Rollback stock and user order mark
        seckillService.rollbackStock(order.getGoodsId());
        seckillService.rollbackUserOrderMark(order.getUserId(), order.getGoodsId());
        
        log.info("Expired order cancelled: orderNo={}", order.getOrderNo());
    }

    /**
     * Convert entity to DTO
     */
    private OrderDTO convertToDTO(Order order) {
        String statusText;
        switch (order.getStatus()) {
            case 0:
                statusText = "Pending Payment";
                break;
            case 1:
                statusText = "Paid";
                break;
            case 2:
                statusText = "Cancelled";
                break;
            case 3:
                statusText = "Expired";
                break;
            case 4:
                statusText = "Creating";
                break;
            default:
                statusText = "Unknown";
        }

        OrderDTO builder = OrderDTO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .goodsId(order.getGoodsId())
                .goodsName(order.getGoodsName())
                .goodsPrice(order.getGoodsPrice())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .payTime(order.getPayTime())
                .cancelTime(order.getCancelTime())
                .expireTime(order.getExpireTime())
                .createTime(order.getCreateTime())
                .statusText(statusText)
                .build();

        // Set status text


        return builder;
    }
}
