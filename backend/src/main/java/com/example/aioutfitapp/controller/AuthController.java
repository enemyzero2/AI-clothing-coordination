package com.example.aioutfitapp.controller;

import com.example.aioutfitapp.dto.request.LoginRequest;
import com.example.aioutfitapp.dto.request.RegisterRequest;
import com.example.aioutfitapp.dto.response.LoginResponse;
import com.example.aioutfitapp.dto.response.RegisterResponse;
import com.example.aioutfitapp.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 认证控制器
 * 
 * 处理用户认证相关的HTTP请求
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    /**
     * 认证服务
     */
    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("收到登录请求: {}", loginRequest.getUsername());
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @return 注册响应
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("收到注册请求: {}", registerRequest.getUsername());
        RegisterResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }
} 