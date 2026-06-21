package com.seckill.api.controller;

import com.seckill.api.dto.LoginRequest;
import com.seckill.api.dto.LoginResponse;
import com.seckill.api.entity.User;
import com.seckill.api.result.Result;
import com.seckill.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    @Autowired
    private final UserService userService;

    /**
     * User login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        log.info("Login request: username={}", request.getUsername());
        LoginResponse response = userService.login(request);
        return Result.success("Login successful", response);
    }

    /**
     * Get current user info
     */
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = userService.validateTokenAndGetUserId(token);
        if (userId == null) {
            return Result.unauthorized("Please login first");
        }
        
        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.notFound("User not found");
        }
        
        return Result.success(user);
    }

    /**
     * Validate token
     */
    @GetMapping("/validate")
    public Result<Long> validateToken(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = userService.validateTokenAndGetUserId(token);
        if (userId == null) {
            return Result.unauthorized("Invalid token");
        }
        return Result.success(userId);
    }
}
