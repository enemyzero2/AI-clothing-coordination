package com.example.aioutfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.aioutfitapp.model.Clothing;
import com.example.aioutfitapp.model.WardrobeManager;
import com.example.aioutfitapp.network.LinphoneManager;

/**
 * 应用程序主界面
 * 
 * 显示搭配推荐、功能入口和底部导航
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    
    private ViewPager2 outfitPager;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private List<OutfitCardAdapter.OutfitItem> outfitItems;
    
    // 功能按钮
    private LinearLayout addClothesBtn;
    private LinearLayout myWardrobeBtn;
    private LinearLayout outfitSuggestionBtn;
    private LinearLayout shoppingListBtn;
    private LinearLayout sceneMatchingBtn;
    private LinearLayout fashionTrendsBtn;
    private LinearLayout personalizationBtn;
    private LinearLayout callFunctionBtn; // 新增通话功能按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化LinphoneManager
        initLinphoneManager();
        
        // 初始化视图
        outfitPager = findViewById(R.id.outfit_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAdd = findViewById(R.id.fab_add);
        
        // 功能入口
        addClothesBtn = findViewById(R.id.add_clothes_btn);
        myWardrobeBtn = findViewById(R.id.my_wardrobe_btn);
        outfitSuggestionBtn = findViewById(R.id.outfit_suggestion_btn);
        shoppingListBtn = findViewById(R.id.shopping_list_btn);
        sceneMatchingBtn = findViewById(R.id.scene_matching_btn);
        fashionTrendsBtn = findViewById(R.id.fashion_trends_btn);
        personalizationBtn = findViewById(R.id.personalization_btn);
        callFunctionBtn = findViewById(R.id.call_function_btn); // 初始化通话功能按钮
        
        // 设置底部导航
        setupBottomNavigation();
        
        // 设置搭配卡片
        setupOutfitPager();
        
        // 设置功能入口点击事件
        setupFeatureButtons();
    }
    
    /**
     * 设置底部导航栏
     */
    private void setupBottomNavigation() {
        // 禁用中间项的点击事件（由FAB代替）
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        
        // 设置导航项选择监听
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                
                if (itemId == R.id.navigation_home) {
                    // 已在主页，不需处理
                    return true;
                } else if (itemId == R.id.navigation_wardrobe) {
                    Intent intent = new Intent(MainActivity.this, WardrobeActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_outfit) {
                    Toast.makeText(MainActivity.this, "搭配功能即将上线", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    // 跳转到个人主页
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
        
        // 设置添加按钮点击事件
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "添加功能即将上线", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 设置搭配推荐卡片滑动
     */
    private void setupOutfitPager() {
        // 准备数据
        outfitItems = new ArrayList<>();
        outfitItems.add(new OutfitCardAdapter.OutfitItem(R.drawable.outfit_1, "休闲周末", "休闲", "日常", 3, false));
        outfitItems.add(new OutfitCardAdapter.OutfitItem(R.drawable.outfit_2, "商务简约", "正装", "办公室", 4, true));
        outfitItems.add(new OutfitCardAdapter.OutfitItem(R.drawable.outfit_3, "约会穿搭", "时尚", "聚会", 4, false));
        outfitItems.add(new OutfitCardAdapter.OutfitItem(R.drawable.outfit_4, "运动活力", "运动", "健身", 3, false));
        
        // 设置适配器
        OutfitCardAdapter adapter = new OutfitCardAdapter(this, outfitItems);
        outfitPager.setAdapter(adapter);
        
        // 设置卡片效果
        outfitPager.setClipToPadding(false);
        outfitPager.setClipChildren(false);
        outfitPager.setOffscreenPageLimit(3);
        
        // 设置卡片变换效果
        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(40));
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
        });
        
        outfitPager.setPageTransformer(transformer);
    }
    
    /**
     * 设置功能按钮点击事件
     */
    private void setupFeatureButtons() {
        // 查找社区按钮
        LinearLayout fashionCommunityBtn = findViewById(R.id.fashion_community_btn);
        
        addClothesBtn.setOnClickListener(v -> {
            Toast.makeText(this, "添加服装功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        myWardrobeBtn.setOnClickListener(v -> {
            // 自动添加示例衣物到衣柜
            addSampleClothes();
            
            // 跳转到衣柜页面
            Intent intent = new Intent(this, WardrobeActivity.class);
            startActivity(intent);
        });
        
        outfitSuggestionBtn.setOnClickListener(v -> {
            Toast.makeText(this, "搭配建议功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        shoppingListBtn.setOnClickListener(v -> {
            Toast.makeText(this, "购物清单功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        sceneMatchingBtn.setOnClickListener(v -> {
            Toast.makeText(this, "场景搭配功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        fashionTrendsBtn.setOnClickListener(v -> {
            Toast.makeText(this, "流行趋势功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        personalizationBtn.setOnClickListener(v -> {
            Toast.makeText(this, "个性定制功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        // 添加社区按钮点击事件，跳转到社区页面
        fashionCommunityBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityActivity.class);
            startActivity(intent);
        });
        
        // 添加通话功能按钮点击事件，跳转到通话界面
        callFunctionBtn.setOnClickListener(v -> {
            try {
                // 跳转到联系人列表页面
                Intent intent = new Intent(this, ContactsActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "启动联系人活动失败", e);
                Toast.makeText(this, "启动联系人功能失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 添加示例衣物到衣柜
     */
    private void addSampleClothes() {
        WardrobeManager wardrobeManager = WardrobeManager.getInstance(this);
        
        // 创建示例衣物列表
        List<Clothing> sampleClothes = createSampleClothes();
        
        // 添加到衣柜
        int addedCount = 0;
        for (Clothing clothing : sampleClothes) {
            boolean added = wardrobeManager.addClothing(clothing);
            if (added) {
                addedCount++;
            }
        }
        
        // 显示添加结果
        Toast.makeText(this, 
                getString(R.string.sample_clothes_added, addedCount, sampleClothes.size()), 
                Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 创建示例衣物列表
     */
    private List<Clothing> createSampleClothes() {
        List<Clothing> sampleClothes = new ArrayList<>();
        
        // 示例1: 黑色T恤
        Clothing tShirt = new Clothing();
        tShirt.setName("黑色基础款T恤");
        tShirt.setBrand("优衣库");
        tShirt.setType(Clothing.ClothingType.TOP);
        tShirt.setColors(Arrays.asList("黑色"));
        tShirt.setSeasons(Arrays.asList(
                Clothing.Season.SPRING, 
                Clothing.Season.SUMMER, 
                Clothing.Season.AUTUMN
        ));
        tShirt.setTags(Arrays.asList("基础款", "百搭", "日常"));
        tShirt.setNotes("非常舒适的基础款，适合日常穿着");
        sampleClothes.add(tShirt);
        
        // 示例2: 牛仔裤
        Clothing jeans = new Clothing();
        jeans.setName("直筒牛仔裤");
        jeans.setBrand("李维斯");
        jeans.setType(Clothing.ClothingType.BOTTOM);
        jeans.setColors(Arrays.asList("蓝色"));
        jeans.setSeasons(Arrays.asList(
                Clothing.Season.SPRING, 
                Clothing.Season.AUTUMN, 
                Clothing.Season.WINTER
        ));
        jeans.setTags(Arrays.asList("经典", "耐穿", "百搭"));
        jeans.setNotes("经典款牛仔裤，质量很好");
        jeans.setFavorite(true);
        sampleClothes.add(jeans);
        
        // 示例3: 羽绒服
        Clothing downJacket = new Clothing();
        downJacket.setName("轻薄羽绒服");
        downJacket.setBrand("优衣库");
        downJacket.setType(Clothing.ClothingType.OUTERWEAR);
        downJacket.setColors(Arrays.asList("深蓝色"));
        downJacket.setSeasons(Arrays.asList(
                Clothing.Season.WINTER
        ));
        downJacket.setTags(Arrays.asList("保暖", "轻薄", "户外"));
        downJacket.setNotes("冬季必备，保暖且轻便");
        sampleClothes.add(downJacket);
        
        // 示例4: 连衣裙
        Clothing dress = new Clothing();
        dress.setName("碎花连衣裙");
        dress.setBrand("ZARA");
        dress.setType(Clothing.ClothingType.DRESS);
        dress.setColors(Arrays.asList("红色", "白色"));
        dress.setSeasons(Arrays.asList(
                Clothing.Season.SPRING,
                Clothing.Season.SUMMER
        ));
        dress.setTags(Arrays.asList("甜美", "约会", "度假"));
        dress.setNotes("非常适合春夏季节的约会穿着");
        dress.setFavorite(true);
        sampleClothes.add(dress);
        
        // 示例5: 运动鞋
        Clothing sneakers = new Clothing();
        sneakers.setName("轻便跑鞋");
        sneakers.setBrand("耐克");
        sneakers.setType(Clothing.ClothingType.SHOES);
        sneakers.setColors(Arrays.asList("白色", "灰色"));
        sneakers.setSeasons(Arrays.asList(
                Clothing.Season.ALL_SEASON
        ));
        sneakers.setTags(Arrays.asList("运动", "舒适", "休闲"));
        sneakers.setNotes("非常舒适的日常跑步鞋");
        sampleClothes.add(sneakers);
        
        // 示例6: 包包
        Clothing bag = new Clothing();
        bag.setName("小号斜挎包");
        bag.setBrand("Coach");
        bag.setType(Clothing.ClothingType.BAG);
        bag.setColors(Arrays.asList("棕色"));
        bag.setSeasons(Arrays.asList(
                Clothing.Season.ALL_SEASON
        ));
        bag.setTags(Arrays.asList("简约", "实用", "百搭"));
        bag.setNotes("日常通勤必备单品");
        sampleClothes.add(bag);
        
        return sampleClothes;
    }

    /**
     * 初始化LinphoneManager
     */
    private void initLinphoneManager() {
        try {
            Log.d(TAG, "初始化LinphoneManager");
            // 获取LinphoneManager实例并初始化
            LinphoneManager linphoneManager = LinphoneManager.getInstance();
            if (linphoneManager.getCore() == null) {
                linphoneManager.init(this);
                linphoneManager.start();
                Log.d(TAG, "LinphoneManager初始化成功");
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化LinphoneManager失败", e);
        }
    }
}

