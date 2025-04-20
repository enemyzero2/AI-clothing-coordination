package com.example.aioutfitapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 个人主页界面
 * 
 * 显示用户个人信息、统计数据、风格标签、身材数据和穿搭记录
 */
public class ProfileActivity extends AppCompatActivity {

    // 顶部导航
    private ImageView backButton;
    private ImageView settingsButton;
    
    // 用户信息
    private de.hdodenhof.circleimageview.CircleImageView userAvatar;
    private TextView userName;
    private TextView userId;
    private TextView userSignature;
    private MaterialButton editProfileBtn;
    
    // 用户统计信息
    private TextView clothesCount;
    private TextView outfitsCount;
    private TextView favoritesCount;
    
    // 风格标签
    private ChipGroup styleChipGroup;
    
    // 身材数据
    private TextView heightValue;
    private TextView weightValue;
    private TextView shoulderValue;
    private TextView chestValue;
    private MaterialButton editBodyDataBtn;
    
    // 穿搭记录
    private RecyclerView outfitRecyclerView;
    private TextView viewAllOutfits;
    private OutfitRecordAdapter outfitAdapter;
    
    // 相关功能
    private LinearLayout favoritesBtn;
    private LinearLayout calendarBtn;
    private LinearLayout shoppingListBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);
        
        // 初始化视图
        initViews();
        
        // 设置点击事件
        setupClickListeners();
        
        // 加载用户数据（这里使用模拟数据）
        loadUserData();
        
        // 加载风格标签
        loadStyleTags();
        
        // 加载穿搭记录
        loadOutfitRecords();
    }
    
    /**
     * 初始化界面视图
     */
    private void initViews() {
        // 顶部导航
        backButton = findViewById(R.id.back_button);
        settingsButton = findViewById(R.id.settings_icon);
        
        // 用户信息
        userAvatar = findViewById(R.id.user_avatar);
        userName = findViewById(R.id.user_name);
        userId = findViewById(R.id.user_id);
        userSignature = findViewById(R.id.user_signature);
        editProfileBtn = findViewById(R.id.edit_profile_btn);
        
        // 用户统计信息
        clothesCount = findViewById(R.id.clothes_count);
        outfitsCount = findViewById(R.id.outfits_count);
        favoritesCount = findViewById(R.id.favorites_count);
        
        // 风格标签
        styleChipGroup = findViewById(R.id.style_chip_group);
        
        // 身材数据
        heightValue = findViewById(R.id.height_value);
        weightValue = findViewById(R.id.weight_value);
        shoulderValue = findViewById(R.id.shoulder_value);
        chestValue = findViewById(R.id.chest_value);
        editBodyDataBtn = findViewById(R.id.edit_body_data_btn);
        
        // 穿搭记录
        outfitRecyclerView = findViewById(R.id.outfit_recycler_view);
        viewAllOutfits = findViewById(R.id.view_all_outfits);
        
        // 相关功能
        favoritesBtn = findViewById(R.id.favorites_btn);
        calendarBtn = findViewById(R.id.calendar_btn);
        shoppingListBtn = findViewById(R.id.shopping_list_btn);
    }
    
    /**
     * 设置点击事件监听
     */
    private void setupClickListeners() {
        // 返回按钮
        backButton.setOnClickListener(v -> finish());
        
        // 设置按钮
        settingsButton.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.settings)), Toast.LENGTH_SHORT).show();
        });
        
        // 编辑资料按钮
        editProfileBtn.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.edit_profile)), Toast.LENGTH_SHORT).show();
        });
        
        // 编辑身材数据按钮
        editBodyDataBtn.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.edit_body_data)), Toast.LENGTH_SHORT).show();
        });
        
        // 查看全部穿搭
        viewAllOutfits.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.view_all_outfits)), Toast.LENGTH_SHORT).show();
        });
        
        // 收藏夹
        favoritesBtn.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.favorites_btn)), Toast.LENGTH_SHORT).show();
        });
        
        // 穿搭日历
        calendarBtn.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.outfit_calendar)), Toast.LENGTH_SHORT).show();
        });
        
        // 购物清单
        shoppingListBtn.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.shopping_list)), Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * 加载用户数据（模拟数据）
     */
    private void loadUserData() {
        // 在实际应用中，这里应该从数据库或网络加载真实用户数据
        
        // 设置头像 (在实际应用中应该使用Glide或Picasso等库加载网络图片)
        userAvatar.setImageResource(R.drawable.default_avatar);
        
        // 设置用户信息
        userName.setText("时尚达人");
        userId.setText(getString(R.string.user_id_format, "stylish123"));
        userSignature.setText("热爱生活，热爱穿搭！");
        
        // 设置统计数据
        clothesCount.setText("48");
        outfitsCount.setText("23");
        favoritesCount.setText("36");
        
        // 设置身材数据
        heightValue.setText(getString(R.string.height_format, "175"));
        weightValue.setText(getString(R.string.weight_format, "65"));
        shoulderValue.setText(getString(R.string.width_format, "46"));
        chestValue.setText(getString(R.string.width_format, "95"));
    }
    
    /**
     * 加载风格标签（模拟数据）
     */
    private void loadStyleTags() {
        // 在实际应用中，这里应该从数据库加载用户的风格标签
        // 目前布局文件中已经静态添加了几个标签作为示例
        // 如果需要动态添加更多标签，可以使用下面的代码
        
        // 清除已有的标签（如果需要）
        // styleChipGroup.removeAllViews();
        
        // 示例：动态添加标签
        String[] additionalStyles = {getString(R.string.japanese_style), getString(R.string.street_fashion)};
        int[] colors = {R.color.colorAccent, R.color.colorPrimary};
        String[] bgColors = {"#FFF1F7", "#F1E6FF"};
        
        for (int i = 0; i < additionalStyles.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(additionalStyles[i]);
            chip.setTextSize(12);
            chip.setTextColor(getResources().getColor(colors[i % colors.length]));
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setBackgroundColor(android.graphics.Color.parseColor(bgColors[i % bgColors.length]));
            chip.setChipStrokeWidth(0);
            
            styleChipGroup.addView(chip);
        }
    }
    
    /**
     * 加载穿搭记录（模拟数据）
     */
    private void loadOutfitRecords() {
        // 准备数据
        List<OutfitRecord> outfitRecords = new ArrayList<>();
        outfitRecords.add(new OutfitRecord(R.drawable.outfit_1, getString(R.string.casual_weekend), "2023-03-18"));
        outfitRecords.add(new OutfitRecord(R.drawable.outfit_2, getString(R.string.business_casual), "2023-03-15"));
        outfitRecords.add(new OutfitRecord(R.drawable.outfit_3, getString(R.string.date_outfit), "2023-03-12"));
        outfitRecords.add(new OutfitRecord(R.drawable.outfit_4, getString(R.string.sport_active), "2023-03-10"));
        
        // 设置RecyclerView
        outfitAdapter = new OutfitRecordAdapter(outfitRecords);
        outfitRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        outfitRecyclerView.setAdapter(outfitAdapter);
    }
    
    /**
     * 穿搭记录数据模型
     */
    public static class OutfitRecord {
        private int imageResId;
        private String title;
        private String date;
        
        public OutfitRecord(int imageResId, String title, String date) {
            this.imageResId = imageResId;
            this.title = title;
            this.date = date;
        }
        
        public int getImageResId() {
            return imageResId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDate() {
            return date;
        }
    }
    
    /**
     * 穿搭记录适配器
     */
    private class OutfitRecordAdapter extends RecyclerView.Adapter<OutfitRecordAdapter.OutfitViewHolder> {
        
        private List<OutfitRecord> outfitRecords;
        
        public OutfitRecordAdapter(List<OutfitRecord> outfitRecords) {
            this.outfitRecords = outfitRecords;
        }
        
        @Override
        public OutfitViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_outfit_record, parent, false);
            return new OutfitViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(OutfitViewHolder holder, int position) {
            OutfitRecord record = outfitRecords.get(position);
            
            holder.outfitImage.setImageResource(record.getImageResId());
            holder.outfitTitle.setText(record.getTitle());
            holder.outfitDate.setText(record.getDate());
            
            // 点击查看穿搭详情
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(ProfileActivity.this, getString(R.string.feature_coming_soon, record.getTitle()), Toast.LENGTH_SHORT).show();
            });
        }
        
        @Override
        public int getItemCount() {
            return outfitRecords.size();
        }
        
        // 视图持有者
        class OutfitViewHolder extends RecyclerView.ViewHolder {
            ImageView outfitImage;
            TextView outfitTitle;
            TextView outfitDate;
            
            OutfitViewHolder(View itemView) {
                super(itemView);
                outfitImage = itemView.findViewById(R.id.outfit_image);
                outfitTitle = itemView.findViewById(R.id.outfit_title);
                outfitDate = itemView.findViewById(R.id.outfit_date);
            }
        }
    }
} 