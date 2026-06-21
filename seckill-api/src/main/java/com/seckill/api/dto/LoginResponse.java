package com.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;

    public static LoginResponse of(String token, Long userId, String username) {
        return new LoginResponse(token, userId, username);
    }
}
