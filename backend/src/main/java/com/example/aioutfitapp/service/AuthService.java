package com.example.aioutfitapp.service;

import com.example.aioutfitapp.dto.request.LoginRequest;
import com.example.aioutfitapp.dto.request.RegisterRequest;
import com.example.aioutfitapp.dto.response.LoginResponse;
import com.example.aioutfitapp.dto.response.RegisterResponse;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest loginRequest);
    
    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @return 注册响应
     */
    RegisterResponse register(RegisterRequest registerRequest);
} 