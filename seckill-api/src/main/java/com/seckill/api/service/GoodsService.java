package com.seckill.api.service;

import dto.GoodsDTO;
import entity.Goods;

import java.util.List;

public interface GoodsService {

    public Goods getGoodsById(Long goodsId);

    public GoodsDTO getGoodsDetail(Long goodsId);

    public List<GoodsDTO> getAllGoods();

    public List<GoodsDTO> getOngoingGoods();

    public List<GoodsDTO> getComingSoonGoods();

    public void updateGoodsStatus();

    public void initRedisStock();

}
