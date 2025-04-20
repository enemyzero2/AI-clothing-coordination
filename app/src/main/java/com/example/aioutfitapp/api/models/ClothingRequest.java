package com.example.aioutfitapp.api.models;

import java.util.List;

/**
 * 服装请求模型
 */
public class ClothingRequest {
    private String name; // 名称
    private String type; // 类型
    private String imageUrl; // 图片URL
    private String description; // 描述
    private String brand; // 品牌
    private String size; // 尺码
    private String color; // 颜色
    private List<String> seasons; // 适用季节
    private boolean favorite; // 是否收藏
    private int favoriteLevel; // 喜爱程度 (1-5)
    
    /**
     * 构造函数
     */
    public ClothingRequest() {
    }
    
    /**
     * 构造函数
     * 
     * @param name 名称
     * @param type 类型
     * @param imageUrl 图片URL
     * @param description 描述
     * @param brand 品牌
     * @param size 尺码
     * @param color 颜色
     * @param seasons 适用季节
     * @param favorite 是否收藏
     * @param favoriteLevel 喜爱程度
     */
    public ClothingRequest(String name, String type, String imageUrl, String description, 
                         String brand, String size, String color, List<String> seasons, 
                         boolean favorite, int favoriteLevel) {
        this.name = name;
        this.type = type;
        this.imageUrl = imageUrl;
        this.description = description;
        this.brand = brand;
        this.size = size;
        this.color = color;
        this.seasons = seasons;
        this.favorite = favorite;
        this.favoriteLevel = favoriteLevel;
    }
    
    // Getter 和 Setter 方法
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public List<String> getSeasons() {
        return seasons;
    }
    
    public void setSeasons(List<String> seasons) {
        this.seasons = seasons;
    }
    
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