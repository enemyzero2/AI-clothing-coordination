package com.example.aioutfitapp.api.models;

/**
 * 点赞请求模型
 */
public class LikeRequest {
    private boolean like; // 是否点赞
    
    /**
     * 构造函数
     */
    public LikeRequest() {
    }
    
    /**
     * 构造函数
     * 
     * @param like 是否点赞
     */
    public LikeRequest(boolean like) {
        this.like = like;
    }
    
    // Getter 和 Setter 方法
    
    public boolean isLike() {
        return like;
    }
    
    public void setLike(boolean like) {
        this.like = like;
    }
} 