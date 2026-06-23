package config;

import com.seckill.api.service.Serviceimpl.GoodsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Seckill Scheduler
 * Handles periodic tasks like updating goods status
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SeckillScheduler {
    @Autowired
    private final GoodsServiceImpl goodsService;

    /**
     * Update goods status every 30 seconds
     * Changes: Coming Soon -> Ongoing -> Ended
     */
//    @Scheduled(fixedRate = 30000)
    public void updateGoodsStatus() {
        try {
            goodsService.updateGoodsStatus();
        } catch (Exception e) {
            log.error("Failed to update goods status", e);
        }
    }
}
