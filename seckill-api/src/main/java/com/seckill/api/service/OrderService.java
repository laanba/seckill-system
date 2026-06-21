package com.seckill.api.service;

import com.seckill.api.dto.OrderDTO;

import java.util.List;

public interface OrderService {

    public OrderDTO getOrderByOrderNo(String orderNo);

    public OrderDTO getOrderById(Long orderId);

    public List<OrderDTO> getUserOrders(Long userId);

    public boolean hasUserOrdered(Long userId, Long goodsId);

    public OrderDTO payOrder(Long userId, String orderNo);

    public OrderDTO cancelOrder(Long userId, String orderNo);

    public void handleExpiredOrders();




}
