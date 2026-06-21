package com.seckill.api.service;

import com.seckill.api.dto.LoginRequest;
import com.seckill.api.dto.LoginResponse;
import com.seckill.api.entity.User;

public interface UserService {
    public LoginResponse login(LoginRequest request);

    public User getUserById(Long userId);

    public Long validateTokenAndGetUserId(String token);

    public User register(String username, String password);


}
