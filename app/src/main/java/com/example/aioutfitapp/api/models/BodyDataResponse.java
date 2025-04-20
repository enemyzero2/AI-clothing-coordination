package com.example.aioutfitapp.api.models;

/**
 * 身体数据响应模型
 */
public class BodyDataResponse {
    private boolean success; // 是否成功
    private String message; // 响应消息
    private BodyData bodyData; // 身体数据
    
    /**
     * 构造函数
     */
    public BodyDataResponse() {
    }
    
    /**
     * 构造函数
     * 
     * @param success 是否成功
     * @param message 响应消息
     * @param bodyData 身体数据
     */
    public BodyDataResponse(boolean success, String message, BodyData bodyData) {
        this.success = success;
        this.message = message;
        this.bodyData = bodyData;
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
    
    public BodyData getBodyData() {
        return bodyData;
    }
    
    public void setBodyData(BodyData bodyData) {
        this.bodyData = bodyData;
    }
} 