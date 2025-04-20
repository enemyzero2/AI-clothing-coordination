package com.example.aioutfitapp.api.models;

import java.util.Date;
import java.util.List;

/**
 * 帖子响应模型
 */
public class PostResponse {
    private int id; // 帖子ID
    private int userId; // 用户ID
    private String userName; // 用户名
    private String userAvatar; // 用户头像
    private String content; // 帖子内容
    private String outfitImageUrl; // 穿搭图片URL
    private int outfitId; // 穿搭ID
    private List<String> tags; // 标签列表
    private int likeCount; // 点赞数
    private int commentCount; // 评论数
    private boolean isLiked; // 当前用户是否点赞
    private Date createdAt; // 创建时间
    private Date updatedAt; // 更新时间
    
    /**
     * 构造函数
     */
    public PostResponse() {
    }
    
    /**
     * 构造函数
     * 
     * @param id 帖子ID
     * @param userId 用户ID
     * @param userName 用户名
     * @param userAvatar 用户头像
     * @param content 帖子内容
     * @param outfitImageUrl 穿搭图片URL
     * @param outfitId 穿搭ID
     * @param tags 标签列表
     * @param likeCount 点赞数
     * @param commentCount 评论数
     * @param isLiked 是否点赞
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public PostResponse(int id, int userId, String userName, String userAvatar, String content,
                       String outfitImageUrl, int outfitId, List<String> tags, int likeCount,
                       int commentCount, boolean isLiked, Date createdAt, Date updatedAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.outfitImageUrl = outfitImageUrl;
        this.outfitId = outfitId;
        this.tags = tags;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.isLiked = isLiked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
    
    public String getOutfitImageUrl() {
        return outfitImageUrl;
    }
    
    public void setOutfitImageUrl(String outfitImageUrl) {
        this.outfitImageUrl = outfitImageUrl;
    }
    
    public int getOutfitId() {
        return outfitId;
    }
    
    public void setOutfitId(int outfitId) {
        this.outfitId = outfitId;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public int getLikeCount() {
        return likeCount;
    }
    
    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
    
    public int getCommentCount() {
        return commentCount;
    }
    
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }
    
    public boolean isLiked() {
        return isLiked;
    }
    
    public void setLiked(boolean liked) {
        isLiked = liked;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
} 