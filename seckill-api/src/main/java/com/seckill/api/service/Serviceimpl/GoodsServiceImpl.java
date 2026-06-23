package com.seckill.api.service.Serviceimpl;

import dto.GoodsDTO;
import entity.Goods;
import com.seckill.api.mapper.GoodsMapper;
import constant.SeckillConstant;
import com.seckill.api.service.GoodsService;
import com.seckill.api.service.demo.RedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Goods Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {
    @Autowired
    private final GoodsMapper goodsMapper;
    private final RedisStockService redisStockService;

    /**
     * Get goods by ID
     */
    @Override
    public Goods getGoodsById(Long goodsId) {
        return goodsMapper.selectByPrimaryKey(goodsId);
    }

    /**
     * Get goods detail with current stock from Redis
     */
    @Override
    public GoodsDTO getGoodsDetail(Long goodsId) {
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null) {
            return null;
        }
        return convertToDTO(goods);
    }

    /**
     * Get all goods
     */
    @Override
    public List<GoodsDTO> getAllGoods() {
        List<Goods> goodsList = goodsMapper.selectAll();
        return goodsList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get ongoing seckill goods
     */
    @Override
    public List<GoodsDTO> getOngoingGoods() {
        List<Goods> goodsList = goodsMapper.findOngoingGoods();
        return goodsList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get coming soon seckill goods
     */
    @Override
    public List<GoodsDTO> getComingSoonGoods() {
        List<Goods> goodsList = goodsMapper.findComingSoonGoods();
        return goodsList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update goods status based on time
     * Should be called periodically by scheduler
     */
    @Override
    public void updateGoodsStatus() {
        Date now = new Date();
        
        // Update: Coming Soon -> Ongoing
        List<Goods> comingSoon = goodsMapper.findComingSoonGoods();
        for (Goods goods : comingSoon) {
            if (now.after(goods.getStartTime()) && now.before(goods.getEndTime())) {
                goods.setStatus(SeckillConstant.GOODS_STATUS_ONGOING);
                goodsMapper.updateByPrimaryKeySelective(goods);
                
                // Initialize Redis stock
                redisStockService.initStock(goods.getId(), goods.getStock());
                
                log.info("Goods status updated to ONGOING: goodsId={}", goods.getId());
            } else if (now.after(goods.getEndTime())) {
                goods.setStatus(SeckillConstant.GOODS_STATUS_ENDED);
                goodsMapper.updateByPrimaryKeySelective(goods);
                log.info("Goods status updated to ENDED: goodsId={}", goods.getId());
            }
        }
        
        // Update: Ongoing -> Ended
        List<Goods> ongoing = goodsMapper.findOngoingGoods();
        for (Goods goods : ongoing) {
            if (now.after(goods.getEndTime())) {
                goods.setStatus(SeckillConstant.GOODS_STATUS_ENDED);
                goodsMapper.updateByPrimaryKeySelective(goods);
                
                // Clean up Redis stock
                redisStockService.deleteStock(goods.getId());
                
                log.info("Goods status updated to ENDED: goodsId={}", goods.getId());
            }
        }
    }

    /**
     * Initialize Redis stock for all ongoing goods
     * Should be called on application startup
     */
    @Override
    public void initRedisStock() {
        List<Goods> ongoingGoods = goodsMapper.findOngoingGoods();
        for (Goods goods : ongoingGoods) {
            redisStockService.initStock(goods.getId(), goods.getStock());
            log.info("Initialized Redis stock for ongoing goods: goodsId={}, stock={}", 
                    goods.getId(), goods.getStock());
        }
    }

    /**
     * Convert entity to DTO
     */

    private GoodsDTO convertToDTO(Goods goods) {
        // Get current stock from Redis
        Integer redisStock = redisStockService.getStock(goods.getId());
        Integer currentStock = redisStock != null ? redisStock : goods.getStock();

        String statusText;
        switch (goods.getStatus()) {
            case 0:
                statusText = "Not Published";
                break;
            case 1:
                statusText = "Coming Soon";
                break;
            case 2:
                statusText = "Ongoing";
                break;
            case 3:
                statusText = "Ended";
                break;
            default:
                statusText = "Unknown";
        }

        GoodsDTO  builder = GoodsDTO.builder()
                .id(goods.getId())
                .goodsName(goods.getGoodsName())
                .goodsDesc(goods.getGoodsDesc())
                .goodsPicture(goods.getGoodsPicture())
                .originalPrice(goods.getOriginalPrice())
                .seckillPrice(goods.getSeckillPrice())
                .stock(currentStock)
                .totalStock(goods.getTotalStock())
                .startTime(goods.getStartTime())
                .endTime(goods.getEndTime())
                .status(goods.getStatus())
                .statusText(statusText)
                .build();

        // Set status text

        // Calculate remaining seconds for coming soon goods
        if (goods.getStatus() == SeckillConstant.GOODS_STATUS_COMING_SOON && goods.getStartTime() != null) {
            long seconds = (goods.getStartTime().getTime() - System.currentTimeMillis()) / 1000;
            builder.setRemainingSeconds((int) Math.max(0, seconds));
        }

        return builder;
    }
}
