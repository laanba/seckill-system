package com.seckill.api.service;

import dto.Response;


public interface SeckillService {
    public Response executeSeckill(Long userId, Long goodsId);

    public void rollbackStock(Long goodsId);

    public void rollbackUserOrderMark(Long userId, Long goodsId);
}
