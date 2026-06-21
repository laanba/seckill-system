package com.seckill.api.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * Login Request DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username cannot be empty")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
