package com.example.aioutfitapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * JWT令牌
     */
    private String token;
    
    /**
     * 令牌类型
     */
    private String tokenType;
    
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 电子邮件
     */
    private String email;
    
    /**
     * 角色列表
     */
    private List<String> roles;
    
    /**
     * 头像URI
     */
    private String avatarUri;
    
    /**
     * 显示名称
     */
    private String displayName;
    
    /**
     * SIP账户信息
     */
    private SipAccount sipAccount;
    
    /**
     * 用户对象（兼容APP端）
     */
    private UserDetail user;
    
    /**
     * SIP账户信息类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SipAccount {
        private String sipUsername;
        private String sipPassword;
        private String sipDomain;
        private String sipServerAddress;
        private String sipServerPort;
    }
    
    /**
     * 用户详情类（兼容APP端）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDetail {
        private Integer id;
        private String username;
        private String email;
        private String avatar;
        private List<String> roles;
        private SipAccount sipAccount;
    }
} 