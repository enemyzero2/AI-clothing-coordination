package com.example.aioutfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.example.aioutfitapp.model.Clothing;
import com.example.aioutfitapp.model.WardrobeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 衣柜活动类
 * 
 * 负责显示用户的服装列表，提供筛选、排序和添加服装的功能
 */
public class WardrobeActivity extends AppCompatActivity {
    
    private static final String TAG = "WardrobeActivity";
    
    // UI 组件
    private ImageView backButton;
    private ImageView searchButton;
    private TabLayout tabLayout;
    private LinearLayout emptyState;
    private RecyclerView clothingRecyclerView;
    private FloatingActionButton addClothingFab;
    private MaterialButton addFirstClothingBtn;
    private Spinner sortSpinner;
    private Chip chipSpring, chipSummer, chipAutumn, chipWinter, chipFavorite;
    
    // 数据
    private WardrobeManager wardrobeManager;
    private ClothingAdapter clothingAdapter;
    private List<Clothing> currentClothingList;
    private List<Clothing.Season> selectedSeasons;
    private boolean showFavoritesOnly = false;
    private Clothing.ClothingType currentType = null; // null 表示全部
    private WardrobeManager.SortOption currentSortOption = WardrobeManager.SortOption.DATE_ADDED_DESC;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wardrobe);
        
        // 初始化视图
        initViews();
        
        // 初始化数据
        initData();
        
        // 设置点击事件
        setupListeners();
        
        // 加载衣柜数据
        loadWardrobeData();
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        searchButton = findViewById(R.id.search_button);
        tabLayout = findViewById(R.id.tab_layout);
        emptyState = findViewById(R.id.empty_state);
        clothingRecyclerView = findViewById(R.id.clothing_recycler_view);
        addClothingFab = findViewById(R.id.add_clothing_fab);
        addFirstClothingBtn = findViewById(R.id.add_first_clothing_btn);
        sortSpinner = findViewById(R.id.sort_spinner);
        
        // 季节筛选芯片
        chipSpring = findViewById(R.id.chip_spring);
        chipSummer = findViewById(R.id.chip_summer);
        chipAutumn = findViewById(R.id.chip_autumn);
        chipWinter = findViewById(R.id.chip_winter);
        chipFavorite = findViewById(R.id.chip_favorite);
        
        // 设置网格布局
        clothingRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }
    
    /**
     * 初始化数据
     */
    private void initData() {
        // 获取衣柜管理器实例
        wardrobeManager = WardrobeManager.getInstance(this);
        
        // 初始化列表
        currentClothingList = new ArrayList<>();
        selectedSeasons = new ArrayList<>();
        
        // 初始化适配器
        clothingAdapter = new ClothingAdapter(this, currentClothingList);
        clothingRecyclerView.setAdapter(clothingAdapter);
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 返回按钮
        backButton.setOnClickListener(v -> finish());
        
        // 搜索按钮
        searchButton.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.search_coming_soon), Toast.LENGTH_SHORT).show();
            // TODO: 实现搜索功能
        });
        
        // 添加服装按钮
        addClothingFab.setOnClickListener(v -> openAddClothingActivity());
        addFirstClothingBtn.setOnClickListener(v -> openAddClothingActivity());
        
        // 设置Tab选择监听
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 根据选中的Tab更新当前选中的服装类型
                switch (tab.getPosition()) {
                    case 0: // 全部
                        currentType = null;
                        break;
                    case 1: // 上装
                        currentType = Clothing.ClothingType.TOP;
                        break;
                    case 2: // 下装
                        currentType = Clothing.ClothingType.BOTTOM;
                        break;
                    case 3: // 外套
                        currentType = Clothing.ClothingType.OUTERWEAR;
                        break;
                    case 4: // 连衣裙
                        currentType = Clothing.ClothingType.DRESS;
                        break;
                    case 5: // 鞋子
                        currentType = Clothing.ClothingType.SHOES;
                        break;
                    case 6: // 配饰
                        currentType = Clothing.ClothingType.ACCESSORY;
                        break;
                }
                
                // 刷新列表
                filterAndSortClothing();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // 不需要处理
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // 不需要处理
            }
        });
        
        // 设置排序监听
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // 最新添加
                        currentSortOption = WardrobeManager.SortOption.DATE_ADDED_DESC;
                        break;
                    case 1: // 最早添加
                        currentSortOption = WardrobeManager.SortOption.DATE_ADDED_ASC;
                        break;
                    case 2: // 名称 A-Z
                        currentSortOption = WardrobeManager.SortOption.NAME_ASC;
                        break;
                    case 3: // 名称 Z-A
                        currentSortOption = WardrobeManager.SortOption.NAME_DESC;
                        break;
                    case 4: // 收藏优先
                        currentSortOption = WardrobeManager.SortOption.FAVORITE_FIRST;
                        break;
                    case 5: // 喜爱程度
                        currentSortOption = WardrobeManager.SortOption.FAVORITE_LEVEL_DESC;
                        break;
                }
                
                // 刷新列表
                filterAndSortClothing();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不需要处理
            }
        });
        
        // 设置季节筛选监听
        setupSeasonChips();
    }
    
    /**
     * 设置季节芯片监听
     */
    private void setupSeasonChips() {
        // 春季芯片
        chipSpring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedSeasons.add(Clothing.Season.SPRING);
            } else {
                selectedSeasons.remove(Clothing.Season.SPRING);
            }
            filterAndSortClothing();
        });
        
        // 夏季芯片
        chipSummer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedSeasons.add(Clothing.Season.SUMMER);
            } else {
                selectedSeasons.remove(Clothing.Season.SUMMER);
            }
            filterAndSortClothing();
        });
        
        // 秋季芯片
        chipAutumn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedSeasons.add(Clothing.Season.AUTUMN);
            } else {
                selectedSeasons.remove(Clothing.Season.AUTUMN);
            }
            filterAndSortClothing();
        });
        
        // 冬季芯片
        chipWinter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedSeasons.add(Clothing.Season.WINTER);
            } else {
                selectedSeasons.remove(Clothing.Season.WINTER);
            }
            filterAndSortClothing();
        });
        
        // 收藏芯片
        chipFavorite.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showFavoritesOnly = isChecked;
            filterAndSortClothing();
        });
    }
    
    /**
     * 加载衣柜数据
     */
    private void loadWardrobeData() {
        // 获取所有服装
        List<Clothing> allClothing = wardrobeManager.getAllClothing();
        
        // 根据筛选和排序条件更新列表
        filterAndSortClothing();
        
        // 更新空状态显示
        updateEmptyState();
    }
    
    /**
     * 根据筛选和排序条件更新列表
     */
    private void filterAndSortClothing() {
        // 获取所有服装
        List<Clothing> allClothing = wardrobeManager.getAllClothing();
        
        // 清空当前列表
        currentClothingList.clear();
        
        // 筛选
        for (Clothing clothing : allClothing) {
            // 类型筛选
            if (currentType != null && clothing.getType() != currentType) {
                continue;
            }
            
            // 季节筛选
            boolean seasonMatch = selectedSeasons.isEmpty();
            for (Clothing.Season season : selectedSeasons) {
                if (clothing.getSeasons().contains(season)) {
                    seasonMatch = true;
                    break;
                }
            }
            if (!seasonMatch) {
                continue;
            }
            
            // 收藏筛选
            if (showFavoritesOnly && !clothing.isFavorite()) {
                continue;
            }
            
            // 通过所有筛选条件，添加到列表
            currentClothingList.add(clothing);
        }
        
        // 排序
        wardrobeManager.sortClothing(currentClothingList, currentSortOption);
        
        // 通知适配器数据已更改
        clothingAdapter.notifyDataSetChanged();
        
        // 更新空状态显示
        updateEmptyState();
    }
    
    /**
     * 更新空状态显示
     */
    private void updateEmptyState() {
        if (currentClothingList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            clothingRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            clothingRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 打开添加服装活动
     */
    private void openAddClothingActivity() {
        // TODO: 实现添加服装功能
        // Intent intent = new Intent(this, AddClothingActivity.class);
        // startActivityForResult(intent, ADD_CLOTHING_REQUEST_CODE);
        Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.add_clothing)), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 处理活动结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // TODO: 处理添加服装结果
    }
    
    /**
     * 活动恢复时刷新数据
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadWardrobeData();
    }
} 