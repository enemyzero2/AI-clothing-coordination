package com.example.aioutfitapp.api.models;

/**
 * 删除响应模型
 */
public class DeleteResponse {
    private boolean success; // 是否成功
    private String message; // 响应消息
    
    /**
     * 构造函数
     */
    public DeleteResponse() {
    }
    
    /**
     * 构造函数
     * 
     * @param success 是否成功
     * @param message 响应消息
     */
    public DeleteResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
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
} 