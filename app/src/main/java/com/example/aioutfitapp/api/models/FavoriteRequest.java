package com.example.aioutfitapp.api.models;

/**
 * 收藏请求模型
 */
public class FavoriteRequest {
    private boolean favorite; // 是否收藏
    private int favoriteLevel; // 喜爱程度 (1-5)
    
    /**
     * 构造函数
     */
    public FavoriteRequest() {
    }
    
    /**
     * 构造函数
     * 
     * @param favorite 是否收藏
     * @param favoriteLevel 喜爱程度
     */
    public FavoriteRequest(boolean favorite, int favoriteLevel) {
        this.favorite = favorite;
        this.favoriteLevel = favoriteLevel;
    }
    
    // Getter 和 Setter 方法
    
    public boolean isFavorite() {
        return favorite;
    }
    
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
    
    public int getFavoriteLevel() {
        return favoriteLevel;
    }
    
    public void setFavoriteLevel(int favoriteLevel) {
        this.favoriteLevel = favoriteLevel;
    }
} 