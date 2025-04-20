package com.example.aioutfitapp.api.models;

import java.util.List;

/**
 * 用户详情模型
 */
public class UserDetail {
    private int id; // 用户ID
    private String username; // 用户名
    private String email; // 邮箱
    private String avatar; // 头像URL
    private String signature; // 个性签名
    private List<String> styleTags; // 风格标签
    private BodyData bodyData; // 身材数据
    private Stats stats; // 用户统计数据
    
    /**
     * 构造函数
     */
    public UserDetail() {
    }
    
    /**
     * 构造函数
     * 
     * @param id 用户ID
     * @param username 用户名
     * @param email 邮箱
     * @param avatar 头像URL
     * @param signature 个性签名
     * @param styleTags 风格标签
     * @param bodyData 身材数据
     * @param stats 用户统计数据
     */
    public UserDetail(int id, String username, String email, String avatar, String signature, 
                      List<String> styleTags, BodyData bodyData, Stats stats) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatar = avatar;
        this.signature = signature;
        this.styleTags = styleTags;
        this.bodyData = bodyData;
        this.stats = stats;
    }
    
    // Getter 和 Setter 方法
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
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
    
    public BodyData getBodyData() {
        return bodyData;
    }
    
    public void setBodyData(BodyData bodyData) {
        this.bodyData = bodyData;
    }
    
    public Stats getStats() {
        return stats;
    }
    
    public void setStats(Stats stats) {
        this.stats = stats;
    }
    
    /**
     * 用户统计数据内部类
     */
    public static class Stats {
        private int clothesCount; // 衣物数量
        private int outfitsCount; // 穿搭数量
        private int favoritesCount; // 收藏数量
        
        /**
         * 构造函数
         */
        public Stats() {
        }
        
        /**
         * 构造函数
         * 
         * @param clothesCount 衣物数量
         * @param outfitsCount 穿搭数量
         * @param favoritesCount 收藏数量
         */
        public Stats(int clothesCount, int outfitsCount, int favoritesCount) {
            this.clothesCount = clothesCount;
            this.outfitsCount = outfitsCount;
            this.favoritesCount = favoritesCount;
        }
        
        // Getter 和 Setter 方法
        
        public int getClothesCount() {
            return clothesCount;
        }
        
        public void setClothesCount(int clothesCount) {
            this.clothesCount = clothesCount;
        }
        
        public int getOutfitsCount() {
            return outfitsCount;
        }
        
        public void setOutfitsCount(int outfitsCount) {
            this.outfitsCount = outfitsCount;
        }
        
        public int getFavoritesCount() {
            return favoritesCount;
        }
        
        public void setFavoritesCount(int favoritesCount) {
            this.favoritesCount = favoritesCount;
        }
    }
} 