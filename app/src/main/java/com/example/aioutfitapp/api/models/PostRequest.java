package com.example.aioutfitapp.api.models;

import java.util.List;

/**
 * 帖子请求模型
 */
public class PostRequest {
    private String content; // 帖子内容
    private String outfitImageUrl; // 穿搭图片URL
    private int outfitId; // 穿搭ID (可选)
    private List<String> tags; // 标签列表
    
    /**
     * 构造函数
     */
    public PostRequest() {
    }
    
    /**
     * 构造函数
     * 
     * @param content 帖子内容
     * @param outfitImageUrl 穿搭图片URL
     * @param outfitId 穿搭ID
     * @param tags 标签列表
     */
    public PostRequest(String content, String outfitImageUrl, int outfitId, List<String> tags) {
        this.content = content;
        this.outfitImageUrl = outfitImageUrl;
        this.outfitId = outfitId;
        this.tags = tags;
    }
    
    // Getter 和 Setter 方法
    
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
} 