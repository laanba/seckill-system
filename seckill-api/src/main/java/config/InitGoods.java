package config;

import com.seckill.api.mapper.GoodsMapper;
import com.seckill.api.service.GoodsService;
import dto.GoodsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 启动初始化 — 服务启动后立即同步商品状态并预热 Redis 库存，
 * 确保秒杀活动可以立刻进行，不用等定时器首次触发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitGoods {

    private final GoodsMapper goodsMapper;

    @PostConstruct
    public void initGoods() {
        log.info("Initializing goods status and Redis stock...");
        try {
            // 预热 Redis 库存（仅对 Ongoing 状态的商品）
            goodsMapper.updateStatusByID();
            log.info("Goods initialization completed");
        } catch (Exception e) {
            log.error("Goods initialization failed: {}", e.getMessage(), e);
        }
    }
}
