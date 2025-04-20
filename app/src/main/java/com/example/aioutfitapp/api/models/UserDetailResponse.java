package com.example.aioutfitapp.api.models;

/**
 * 用户详情响应模型
 */
public class UserDetailResponse {
    private boolean success; // 是否成功
    private String message; // 响应消息
    private UserDetail user; // 用户详情
    
    /**
     * 构造函数
     */
    public UserDetailResponse() {
    }
    
    /**
     * 构造函数
     * 
     * @param success 是否成功
     * @param message 响应消息
     * @param user 用户详情
     */
    public UserDetailResponse(boolean success, String message, UserDetail user) {
        this.success = success;
        this.message = message;
        this.user = user;
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
    
    public UserDetail getUser() {
        return user;
    }
    
    public void setUser(UserDetail user) {
        this.user = user;
    }
} 