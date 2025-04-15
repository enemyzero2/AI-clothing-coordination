package com.example.aioutfitapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 服装数据模型
 * 
 * 定义了衣物的基本属性，包括类型、品牌、颜色、季节、标签等
 */
public class Clothing implements Parcelable {
    
    /**
     * 服装类型枚举
     */
    public enum ClothingType {
        TOP("上装"),         // 上衣、T恤、衬衫等
        BOTTOM("下装"),      // 裤子、短裤、裙子等
        OUTERWEAR("外套"),   // 夹克、大衣、风衣等
        DRESS("连衣裙"),     // 连衣裙、套装等
        SHOES("鞋子"),       // 各类鞋子
        ACCESSORY("配饰"),   // 帽子、围巾、首饰等
        BAG("包包"),         // 各类包包
        OTHER("其他");       // 其他类型
        
        private final String displayName;
        
        ClothingType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 季节枚举
     */
    public enum Season {
        SPRING("春季"),
        SUMMER("夏季"),
        AUTUMN("秋季"),
        WINTER("冬季"),
        ALL_SEASON("四季");
        
        private final String displayName;
        
        Season(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // 基本属性
    private String id;                 // 唯一标识符
    private String name;               // 名称
    private String brand;              // 品牌
    private ClothingType type;         // 类型
    private List<String> colors;       // 颜色列表
    private List<Season> seasons;      // 适用季节
    private List<String> tags;         // 标签
    private String imageUri;           // 图片URI
    private String notes;              // 备注
    private long dateAdded;            // 添加日期
    private int favoriteLevel;         // 喜爱程度(1-5)
    private boolean isFavorite;        // 是否收藏
    
    /**
     * 默认构造函数
     */
    public Clothing() {
        this.id = UUID.randomUUID().toString();
        this.colors = new ArrayList<>();
        this.seasons = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.dateAdded = System.currentTimeMillis();
        this.favoriteLevel = 3; // 默认中等喜爱程度
        this.isFavorite = false;
    }
    
    /**
     * 基本构造函数
     */
    public Clothing(String name, String brand, ClothingType type, List<String> colors, 
                    List<Season> seasons, String imageUri) {
        this();
        this.name = name;
        this.brand = brand;
        this.type = type;
        if (colors != null) this.colors = colors;
        if (seasons != null) this.seasons = seasons;
        this.imageUri = imageUri;
    }
    
    // Getter 和 Setter 方法
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public ClothingType getType() {
        return type;
    }
    
    public void setType(ClothingType type) {
        this.type = type;
    }
    
    public List<String> getColors() {
        return colors;
    }
    
    public void setColors(List<String> colors) {
        this.colors = colors;
    }
    
    public void addColor(String color) {
        if (!colors.contains(color)) {
            colors.add(color);
        }
    }
    
    public List<Season> getSeasons() {
        return seasons;
    }
    
    public void setSeasons(List<Season> seasons) {
        this.seasons = seasons;
    }
    
    public void addSeason(Season season) {
        if (!seasons.contains(season)) {
            seasons.add(season);
        }
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }
    
    public String getImageUri() {
        return imageUri;
    }
    
    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public long getDateAdded() {
        return dateAdded;
    }
    
    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }
    
    public int getFavoriteLevel() {
        return favoriteLevel;
    }
    
    public void setFavoriteLevel(int favoriteLevel) {
        this.favoriteLevel = favoriteLevel;
    }
    
    public boolean isFavorite() {
        return isFavorite;
    }
    
    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
    
    // Parcelable 实现
    
    protected Clothing(Parcel in) {
        id = in.readString();
        name = in.readString();
        brand = in.readString();
        type = ClothingType.valueOf(in.readString());
        colors = in.createStringArrayList();
        
        List<String> seasonStrings = in.createStringArrayList();
        seasons = new ArrayList<>();
        if (seasonStrings != null) {
            for (String s : seasonStrings) {
                seasons.add(Season.valueOf(s));
            }
        }
        
        tags = in.createStringArrayList();
        imageUri = in.readString();
        notes = in.readString();
        dateAdded = in.readLong();
        favoriteLevel = in.readInt();
        isFavorite = in.readByte() != 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(brand);
        dest.writeString(type.name());
        dest.writeStringList(colors);
        
        List<String> seasonStrings = new ArrayList<>();
        for (Season s : seasons) {
            seasonStrings.add(s.name());
        }
        dest.writeStringList(seasonStrings);
        
        dest.writeStringList(tags);
        dest.writeString(imageUri);
        dest.writeString(notes);
        dest.writeLong(dateAdded);
        dest.writeInt(favoriteLevel);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<Clothing> CREATOR = new Creator<Clothing>() {
        @Override
        public Clothing createFromParcel(Parcel in) {
            return new Clothing(in);
        }
        
        @Override
        public Clothing[] newArray(int size) {
            return new Clothing[size];
        }
    };
} 