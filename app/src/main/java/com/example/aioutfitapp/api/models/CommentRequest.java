package com.example.aioutfitapp.api.models;

/**
 * 评论请求模型
 */
public class CommentRequest {
    private String content; // 评论内容
    
    /**
     * 构造函数
     */
    public CommentRequest() {
    }
    
    /**
     * 构造函数
     * 
     * @param content 评论内容
     */
    public CommentRequest(String content) {
        this.content = content;
    }
    
    // Getter 和 Setter 方法
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
} 