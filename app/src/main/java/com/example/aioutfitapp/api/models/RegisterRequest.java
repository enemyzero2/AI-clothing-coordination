package com.example.aioutfitapp.api.models;

/**
 * 注册请求模型
 */
public class RegisterRequest {
    private String username; // 用户名
    private String email; // 邮箱
    private String password; // 密码
    
    /**
     * 构造函数
     */
    public RegisterRequest() {
    }
    
    /**
     * 构造函数
     * 
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     */
    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    // Getter 和 Setter 方法
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
} 