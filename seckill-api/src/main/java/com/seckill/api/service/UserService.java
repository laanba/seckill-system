package com.seckill.api.service;

import dto.LoginRequest;
import dto.LoginResponse;
import entity.User;

public interface UserService {
    public LoginResponse login(LoginRequest request);

    public User getUserById(Long userId);

    public Long validateTokenAndGetUserId(String token);

    public User register(String username, String password);


}
