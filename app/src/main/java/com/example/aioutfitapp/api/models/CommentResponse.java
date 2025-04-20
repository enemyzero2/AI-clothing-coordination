package com.example.aioutfitapp.api.models;

import java.util.Date;

/**
 * 评论响应模型
 */
public class CommentResponse {
    private int id; // 评论ID
    private int userId; // 用户ID
    private String userName; // 用户名
    private String userAvatar; // 用户头像
    private String content; // 评论内容
    private Date createdAt; // 创建时间
    
    /**
     * 构造函数
     */
    public CommentResponse() {
    }
    
    /**
     * 构造函数
     * 
     * @param id 评论ID
     * @param userId 用户ID
     * @param userName 用户名
     * @param userAvatar 用户头像
     * @param content 评论内容
     * @param createdAt 创建时间
     */
    public CommentResponse(int id, int userId, String userName, String userAvatar,
                         String content, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.createdAt = createdAt;
    }
    
    // Getter 和 Setter 方法
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserAvatar() {
        return userAvatar;
    }
    
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
} 