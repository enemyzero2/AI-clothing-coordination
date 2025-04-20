package com.example.aioutfitapp.api.models;

import java.util.List;

/**
 * 用户资料更新请求模型
 */
public class UserUpdateRequest {
    private String username; // 用户名
    private String email; // 邮箱
    private String avatar; // 头像URL
    private String signature; // 个性签名
    private List<String> styleTags; // 风格标签
    
    /**
     * 构造函数
     */
    public UserUpdateRequest() {
    }
    
    /**
     * 构造函数
     * 
     * @param username 用户名
     * @param email 邮箱
     * @param avatar 头像URL
     * @param signature 个性签名
     * @param styleTags 风格标签
     */
    public UserUpdateRequest(String username, String email, String avatar, 
                            String signature, List<String> styleTags) {
        this.username = username;
        this.email = email;
        this.avatar = avatar;
        this.signature = signature;
        this.styleTags = styleTags;
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
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public List<String> getStyleTags() {
        return styleTags;
    }
    
    public void setStyleTags(List<String> styleTags) {
        this.styleTags = styleTags;
    }
} 