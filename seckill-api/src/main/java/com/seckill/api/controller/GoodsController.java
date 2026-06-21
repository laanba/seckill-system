package com.seckill.api.controller;

import com.seckill.api.dto.GoodsDTO;
import com.seckill.api.service.GoodsService;
import com.seckill.api.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 货物 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {
    @Autowired
    private final GoodsService goodsService;

    /**
     * Get all goods
     */
    @GetMapping("/list")
    public Result<List<GoodsDTO>> getAllGoods() {
        List<GoodsDTO> goods = goodsService.getAllGoods();
        return Result.success(goods);
    }

    /**
     * Get ongoing seckill goods
     */
    @GetMapping("/ongoing")
    public Result<List<GoodsDTO>> getOngoingGoods() {
        List<GoodsDTO> goods = goodsService.getOngoingGoods();
        return Result.success(goods);
    }

    /**
     * Get coming soon seckill goods
     */
    @GetMapping("/coming-soon")
    public Result<List<GoodsDTO>> getComingSoonGoods() {
        List<GoodsDTO> goods = goodsService.getComingSoonGoods();
        return Result.success(goods);
    }

    /**
     * Get goods detail
     */
    @GetMapping("/{goodsId}")
    public Result<GoodsDTO> getGoodsDetail(@PathVariable Long goodsId) {
        GoodsDTO goods = goodsService.getGoodsDetail(goodsId);
        if (goods == null) {
            return Result.notFound("Goods not found");
        }
        return Result.success(goods);
    }

    /**
     * Get goods stock (for frontend polling)
     */
    @GetMapping("/{goodsId}/stock")
    public Result<Integer> getGoodsStock(@PathVariable Long goodsId) {
        GoodsDTO goods = goodsService.getGoodsDetail(goodsId);
        if (goods == null) {
            return Result.notFound("Goods not found");
        }
        return Result.success(goods.getStock());
    }
}
