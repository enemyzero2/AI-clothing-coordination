package com.example.aioutfitapp.api.models;

/**
 * 登录请求模型
 */
public class LoginRequest {
    private String username; // 用户名或邮箱
    private String password; // 密码
    
    /**
     * 构造函数
     * 
     * @param username 用户名或邮箱
     * @param password 密码
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Getter 和 Setter 方法
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
} 