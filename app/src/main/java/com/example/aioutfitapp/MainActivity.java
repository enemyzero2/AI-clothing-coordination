package com.example.aioutfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用程序主界面
 * 
 * 显示搭配推荐、功能入口和底部导航
 */
public class MainActivity extends AppCompatActivity {

    private ViewPager2 outfitPager;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private List<OutfitCardAdapter.OutfitItem> outfitItems;
    
    // 功能按钮
    private LinearLayout addClothesBtn;
    private LinearLayout myWardrobeBtn;
    private LinearLayout outfitSuggestionBtn;
    private LinearLayout shoppingListBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化视图
        outfitPager = findViewById(R.id.outfit_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAdd = findViewById(R.id.fab_add);
        
        // 功能入口
        addClothesBtn = findViewById(R.id.add_clothes_btn);
        myWardrobeBtn = findViewById(R.id.my_wardrobe_btn);
        outfitSuggestionBtn = findViewById(R.id.outfit_suggestion_btn);
        shoppingListBtn = findViewById(R.id.shopping_list_btn);
        
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
                    Toast.makeText(MainActivity.this, "衣柜功能即将上线", Toast.LENGTH_SHORT).show();
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
        addClothesBtn.setOnClickListener(v -> {
            Toast.makeText(this, "添加服装功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        myWardrobeBtn.setOnClickListener(v -> {
            Toast.makeText(this, "我的衣柜功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        outfitSuggestionBtn.setOnClickListener(v -> {
            Toast.makeText(this, "搭配建议功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        shoppingListBtn.setOnClickListener(v -> {
            Toast.makeText(this, "购物清单功能即将上线", Toast.LENGTH_SHORT).show();
        });
    }
}

