package com.example.aioutfitapp;

public class Post {
    private int id;             // 帖子ID
    private String userName;    // 用户名
    private int userId;         // 用户ID，添加这个字段
    private String content;     // 帖子内容
    private boolean isLiked;    // 是否点赞
    private int likeCount;      // 点赞数
    private int commentCount;   // 评论数
    private int outfitId;       // 服装ID，用于换装功能
    private String outfitImageUrl; // 服装图片URL

    // 构造函数
    public Post(int id, String userName, String content, boolean isLiked, int likeCount, int commentCount, int outfitId, String outfitImageUrl) {
        this.id = id;
        this.userName = userName;
        this.content = content;
        this.isLiked = isLiked;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.outfitId = outfitId;
        this.outfitImageUrl = outfitImageUrl;
        this.userId = 0; // 默认值
    }

    // 带userId的新构造函数
    public Post(int id, String userName, int userId, String content, boolean isLiked, int likeCount, int commentCount, int outfitId, String outfitImageUrl) {
        this.id = id;
        this.userName = userName;
        this.userId = userId;
        this.content = content;
        this.isLiked = isLiked;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.outfitId = outfitId;
        this.outfitImageUrl = outfitImageUrl;
    }

    // Getter 和 Setter 方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
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

    public int getOutfitId() {
        return outfitId;
    }

    public void setOutfitId(int outfitId) {
        this.outfitId = outfitId;
    }

    public String getOutfitImageUrl() {
        return outfitImageUrl;
    }

    public void setOutfitImageUrl(String outfitImageUrl) {
        this.outfitImageUrl = outfitImageUrl;
    }
}
