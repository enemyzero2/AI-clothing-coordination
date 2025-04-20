package com.example.aioutfitapp.api.models;

import java.util.Date;
import java.util.List;

/**
 * 服装响应模型
 */
public class ClothingResponse {
    private int id; // 服装ID
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
    private Date createdAt; // 创建时间
    private Date updatedAt; // 更新时间
    
    /**
     * 构造函数
     */
    public ClothingResponse() {
    }
    
    /**
     * 构造函数
     * 
     * @param id 服装ID
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
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public ClothingResponse(int id, String name, String type, String imageUrl, String description,
                          String brand, String size, String color, List<String> seasons,
                          boolean favorite, int favoriteLevel, Date createdAt, Date updatedAt) {
        this.id = id;
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