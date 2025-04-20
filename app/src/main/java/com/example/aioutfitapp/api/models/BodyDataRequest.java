package com.example.aioutfitapp.api.models;

/**
 * 身体数据请求模型
 */
public class BodyDataRequest {
    private float height; // 身高(厘米)
    private float weight; // 体重(千克)
    private float shoulderWidth; // 肩宽(厘米)
    private float chestCircumference; // 胸围(厘米)
    
    /**
     * 构造函数
     */
    public BodyDataRequest() {
    }
    
    /**
     * 构造函数
     * 
     * @param height 身高
     * @param weight 体重
     * @param shoulderWidth 肩宽
     * @param chestCircumference 胸围
     */
    public BodyDataRequest(float height, float weight, float shoulderWidth, float chestCircumference) {
        this.height = height;
        this.weight = weight;
        this.shoulderWidth = shoulderWidth;
        this.chestCircumference = chestCircumference;
    }
    
    // Getter 和 Setter 方法
    
    public float getHeight() {
        return height;
    }
    
    public void setHeight(float height) {
        this.height = height;
    }
    
    public float getWeight() {
        return weight;
    }
    
    public void setWeight(float weight) {
        this.weight = weight;
    }
    
    public float getShoulderWidth() {
        return shoulderWidth;
    }
    
    public void setShoulderWidth(float shoulderWidth) {
        this.shoulderWidth = shoulderWidth;
    }
    
    public float getChestCircumference() {
        return chestCircumference;
    }
    
    public void setChestCircumference(float chestCircumference) {
        this.chestCircumference = chestCircumference;
    }
} 