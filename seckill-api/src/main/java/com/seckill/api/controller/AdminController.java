package com.seckill.api.controller;

import com.seckill.api.mapper.UserMapper;
import entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import result.Result;
import util.JwtUtil;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Controller - 管理接口（压测辅助等）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;

    /**
     * 批量导出 Token 到 CSV
     *
     * 用于 JMeter 压测时提供每个虚拟用户的独立 Token。
     *
     * 使用方式:
     *   curl http://localhost:8080/api/admin/export-tokens?prefix=perfuser > tokens.csv
     *
     * CSV 格式: userId,username,token
     * 第一行为表头，之后每行一个用户
     */
    @GetMapping("/export-tokens")
    public String exportTokens(@RequestParam(defaultValue = "") String prefix) {
        // 查询用户
        List<User> users;
        if (prefix.isEmpty()) {
            users = userMapper.selectAll();
        } else {
            Example example = new Example(User.class);
            example.createCriteria().andLike("username", prefix + "%");
            users = userMapper.selectByExample(example);
        }

        // 生成 CSV
        StringBuilder sb = new StringBuilder();
        sb.append("userId,username,token\n");
        for (User user : users) {
            String token = JwtUtil.generateToken(user.getId(), user.getUsername());
            sb.append(user.getId()).append(",")
              .append(user.getUsername()).append(",")
              .append(token).append("\n");
        }

        log.info("Exported {} tokens (prefix={})", users.size(), prefix);
        return sb.toString();
    }

    /**
     * 批量导出 Token（JSON 格式）
     */
    @GetMapping("/export-tokens-json")
    public Result<List<TokenEntry>> exportTokensJson(@RequestParam(defaultValue = "") String prefix) {
        List<User> users;
        if (prefix.isEmpty()) {
            users = userMapper.selectAll();
        } else {
            Example example = new Example(User.class);
            example.createCriteria().andLike("username", prefix + "%");
            users = userMapper.selectByExample(example);
        }

        List<TokenEntry> tokens = users.stream()
                .map(u -> new TokenEntry(u.getId(), u.getUsername(),
                        JwtUtil.generateToken(u.getId(), u.getUsername())))
                .collect(Collectors.toList());

        log.info("Exported {} tokens in JSON (prefix={})", tokens.size(), prefix);
        return Result.success(tokens);
    }

    /**
     * Token 条目
     */
    public static class TokenEntry {
        private final Long userId;
        private final String username;
        private final String token;

        public TokenEntry(Long userId, String username, String token) {
            this.userId = userId;
            this.username = username;
            this.token = token;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getToken() { return token; }
    }
}
