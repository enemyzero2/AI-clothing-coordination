package com.example.aioutfitapp.api.models;

/**
 * 登录响应模型
 */
public class LoginResponse {
    private boolean success; // 是否成功
    private String message; // 响应消息
    private String token; // 认证令牌
    private UserDetail user; // 用户详情
    private SipAccount sipAccount; // SIP账户信息
    
    /**
     * 构造函数
     */
    public LoginResponse() {
    }
    
    /**
     * 构造函数
     * 
     * @param success 是否成功
     * @param message 响应消息
     * @param token 认证令牌
     * @param user 用户详情
     * @param sipAccount SIP账户信息
     */
    public LoginResponse(boolean success, String message, String token, UserDetail user, SipAccount sipAccount) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
        this.sipAccount = sipAccount;
    }
    
    // Getter 和 Setter 方法
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public UserDetail getUser() {
        return user;
    }
    
    public void setUser(UserDetail user) {
        this.user = user;
    }
    
    public SipAccount getSipAccount() {
        return sipAccount;
    }
    
    public void setSipAccount(SipAccount sipAccount) {
        this.sipAccount = sipAccount;
    }
} 