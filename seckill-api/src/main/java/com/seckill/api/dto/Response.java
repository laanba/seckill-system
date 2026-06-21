package com.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Seckill Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response implements Serializable {

    private Long orderId;
    private String orderNo;
    private Integer status;      // 0=queued, 1=success, 2=failed
    private String message;
    private Long waitTime;       // Estimated wait time in milliseconds

    public static Response queued(String orderNo) {
        return Response.builder()
                .orderNo(orderNo)
                .status(0)
                .message("Request is being processed, please wait")
                .build();
    }

    public static Response success(Long orderId, String orderNo) {
        return Response.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .status(1)
                .message("Seckill successful, please complete payment")
                .build();
    }

    public static Response failed(String message) {
        return Response.builder()
                .status(2)
                .message(message)
                .build();
    }
}
