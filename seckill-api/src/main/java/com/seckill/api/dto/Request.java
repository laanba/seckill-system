package com.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Seckill Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request implements Serializable {

    private Long goodsId;

    /**
     * Simple validation
     */
    public boolean isValid() {
        return goodsId != null && goodsId > 0;
    }
}
