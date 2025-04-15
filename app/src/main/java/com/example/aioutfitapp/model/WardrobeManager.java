package com.example.aioutfitapp.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 衣柜管理类
 * 
 * 负责管理服装数据的存储、检索和更新
 */
public class WardrobeManager {
    
    private static final String TAG = "WardrobeManager";
    private static final String PREF_NAME = "wardrobe_data";
    private static final String KEY_CLOTHING_LIST = "clothing_list";
    
    private static WardrobeManager instance;
    private final Context context;
    private final SharedPreferences preferences;
    private final Gson gson;
    
    private List<Clothing> clothingList;
    
    /**
     * 私有构造函数
     */
    private WardrobeManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().create();
        loadClothingList();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized WardrobeManager getInstance(Context context) {
        if (instance == null) {
            instance = new WardrobeManager(context);
        }
        return instance;
    }
    
    /**
     * 加载服装列表
     */
    private void loadClothingList() {
        String json = preferences.getString(KEY_CLOTHING_LIST, null);
        
        if (json != null) {
            try {
                Type type = new TypeToken<ArrayList<Clothing>>() {}.getType();
                clothingList = gson.fromJson(json, type);
                Log.d(TAG, "从SharedPreferences加载了 " + clothingList.size() + " 件服装");
            } catch (Exception e) {
                Log.e(TAG, "加载服装列表失败: " + e.getMessage());
                clothingList = new ArrayList<>();
            }
        } else {
            clothingList = new ArrayList<>();
            Log.d(TAG, "创建新的服装列表");
        }
    }
    
    /**
     * 保存服装列表
     */
    private void saveClothingList() {
        try {
            String json = gson.toJson(clothingList);
            preferences.edit().putString(KEY_CLOTHING_LIST, json).apply();
            Log.d(TAG, "保存了 " + clothingList.size() + " 件服装到SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "保存服装列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有服装
     */
    public List<Clothing> getAllClothing() {
        return new ArrayList<>(clothingList);
    }
    
    /**
     * 根据类型获取服装
     */
    public List<Clothing> getClothingByType(Clothing.ClothingType type) {
        return clothingList.stream()
                .filter(clothing -> clothing.getType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据季节获取服装
     */
    public List<Clothing> getClothingBySeason(Clothing.Season season) {
        return clothingList.stream()
                .filter(clothing -> clothing.getSeasons().contains(season))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据标签获取服装
     */
    public List<Clothing> getClothingByTag(String tag) {
        return clothingList.stream()
                .filter(clothing -> clothing.getTags().contains(tag))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取收藏的服装
     */
    public List<Clothing> getFavoriteClothing() {
        return clothingList.stream()
                .filter(Clothing::isFavorite)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID获取服装
     */
    public Clothing getClothingById(String id) {
        for (Clothing clothing : clothingList) {
            if (clothing.getId().equals(id)) {
                return clothing;
            }
        }
        return null;
    }
    
    /**
     * 添加服装
     */
    public boolean addClothing(Clothing clothing) {
        if (clothing == null) {
            return false;
        }
        
        // 确保ID唯一
        for (Clothing existingClothing : clothingList) {
            if (existingClothing.getId().equals(clothing.getId())) {
                Log.w(TAG, "服装ID已存在: " + clothing.getId());
                return false;
            }
        }
        
        clothingList.add(clothing);
        saveClothingList();
        return true;
    }
    
    /**
     * 更新服装
     */
    public boolean updateClothing(Clothing clothing) {
        if (clothing == null) {
            return false;
        }
        
        for (int i = 0; i < clothingList.size(); i++) {
            if (clothingList.get(i).getId().equals(clothing.getId())) {
                clothingList.set(i, clothing);
                saveClothingList();
                return true;
            }
        }
        
        Log.w(TAG, "未找到要更新的服装: " + clothing.getId());
        return false;
    }
    
    /**
     * 删除服装
     */
    public boolean deleteClothing(String id) {
        for (int i = 0; i < clothingList.size(); i++) {
            if (clothingList.get(i).getId().equals(id)) {
                clothingList.remove(i);
                saveClothingList();
                return true;
            }
        }
        
        Log.w(TAG, "未找到要删除的服装: " + id);
        return false;
    }
    
    /**
     * 搜索服装
     */
    public List<Clothing> searchClothing(String query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(clothingList);
        }
        
        String lowerQuery = query.toLowerCase();
        return clothingList.stream()
                .filter(clothing -> {
                    // 匹配名称
                    if (clothing.getName() != null && 
                            clothing.getName().toLowerCase().contains(lowerQuery)) {
                        return true;
                    }
                    
                    // 匹配品牌
                    if (clothing.getBrand() != null && 
                            clothing.getBrand().toLowerCase().contains(lowerQuery)) {
                        return true;
                    }
                    
                    // 匹配标签
                    for (String tag : clothing.getTags()) {
                        if (tag.toLowerCase().contains(lowerQuery)) {
                            return true;
                        }
                    }
                    
                    // 匹配备注
                    return clothing.getNotes() != null && 
                           clothing.getNotes().toLowerCase().contains(lowerQuery);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 排序服装列表
     */
    public List<Clothing> sortClothing(List<Clothing> clothingList, SortOption sortOption) {
        List<Clothing> result = new ArrayList<>(clothingList);
        
        switch (sortOption) {
            case NAME_ASC:
                Collections.sort(result, Comparator.comparing(Clothing::getName));
                break;
            case NAME_DESC:
                Collections.sort(result, Comparator.comparing(Clothing::getName).reversed());
                break;
            case DATE_ADDED_DESC:
                Collections.sort(result, Comparator.comparing(Clothing::getDateAdded).reversed());
                break;
            case DATE_ADDED_ASC:
                Collections.sort(result, Comparator.comparing(Clothing::getDateAdded));
                break;
            case FAVORITE_FIRST:
                Collections.sort(result, (c1, c2) -> {
                    if (c1.isFavorite() == c2.isFavorite()) {
                        return 0;
                    }
                    return c1.isFavorite() ? -1 : 1;
                });
                break;
            case FAVORITE_LEVEL_DESC:
                Collections.sort(result, Comparator.comparing(Clothing::getFavoriteLevel).reversed());
                break;
        }
        
        return result;
    }
    
    /**
     * 排序选项枚举
     */
    public enum SortOption {
        NAME_ASC,           // 按名称升序
        NAME_DESC,          // 按名称降序
        DATE_ADDED_DESC,    // 按添加日期降序（最新的在前）
        DATE_ADDED_ASC,     // 按添加日期升序（最旧的在前）
        FAVORITE_FIRST,     // 收藏的在前
        FAVORITE_LEVEL_DESC // 按喜爱程度降序
    }
} 