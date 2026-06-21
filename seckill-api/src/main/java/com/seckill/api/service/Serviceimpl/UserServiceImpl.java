package com.seckill.api.service.Serviceimpl;

import com.seckill.api.dto.LoginRequest;
import com.seckill.api.dto.LoginResponse;
import com.seckill.api.entity.User;
import com.seckill.api.mapper.UserMapper;
import com.seckill.api.exception.SeckillException;
import com.seckill.api.service.UserService;
import com.seckill.api.util.JwtUtil;
import com.seckill.api.util.Md5Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * User Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    /**
     * User login
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // Find user by username
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            log.warn("Login failed: user not found, username={}", request.getUsername());
            throw SeckillException.loginFailed();
        }

        // Verify password
//        String encryptedPassword = Md5Util.encrypt(request.getPassword());
        if (!request.getPassword().equals(user.getPassword())) {
            log.warn("Login failed: wrong password, username={}", request.getUsername());
            throw SeckillException.loginFailed();
        }

        // Check user status
        if (!user.isActive()) {
            log.warn("Login failed: user disabled, username={}", request.getUsername());
            throw SeckillException.loginFailed();
        }

        // Generate token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        
        log.info("User logged in successfully: userId={}, username={}", user.getId(), user.getUsername());
        return LoginResponse.of(token, user.getId(), user.getUsername());
    }

    /**
     * Get user by ID
     */
    @Override
    public User getUserById(Long userId) {
        return userMapper.findById(userId);
    }

    /**
     * Validate token and get user ID
     */
    @Override
    public Long validateTokenAndGetUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (!JwtUtil.validateToken(token)) {
            return null;
        }
        
        return JwtUtil.getUserId(token);
    }

    /**
     * Register user (simplified)
     */
    @Override
    public User register(String username, String password) {
        // Check if username exists
        User existingUser = userMapper.findByUsername(username);
        if (existingUser != null) {
            throw new SeckillException(400, "Username already exists");
        }

        // Create user
        User user = new User();
        user.setUsername(username);
        user.setPassword(Md5Util.encrypt(password));
        user.setStatus(1);
        
        userMapper.insertSelective(user);
        log.info("User registered: userId={}, username={}", user.getId(), username);
        
        return user;
    }
}
