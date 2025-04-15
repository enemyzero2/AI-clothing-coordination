package com.example.aioutfitapp;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.example.aioutfitapp.model.Clothing;

import java.io.File;
import java.util.List;

/**
 * 服装适配器类
 * 
 * 处理服装项在RecyclerView中的显示，支持点击、长按等交互
 */
public class ClothingAdapter extends RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder> {
    
    private Context context;
    private List<Clothing> clothingList;
    private OnClothingItemClickListener itemClickListener;
    
    /**
     * 构造函数
     */
    public ClothingAdapter(Context context, List<Clothing> clothingList) {
        this.context = context;
        this.clothingList = clothingList;
    }
    
    /**
     * 设置点击监听器
     */
    public void setOnClothingItemClickListener(OnClothingItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    @NonNull
    @Override
    public ClothingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_clothing, parent, false);
        return new ClothingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ClothingViewHolder holder, int position) {
        Clothing clothing = clothingList.get(position);
        
        // 设置服装名称
        holder.nameTextView.setText(clothing.getName());
        
        // 设置品牌信息
        holder.brandTextView.setText("品牌: " + (clothing.getBrand() != null ? clothing.getBrand() : "未知"));
        
        // 设置类型标签
        if (clothing.getType() != null) {
            holder.typeLabel.setText(clothing.getType().getDisplayName());
            
            // 根据类型设置不同的标签颜色
            int color;
            switch (clothing.getType()) {
                case TOP:
                    color = Color.parseColor("#4CAF50"); // 绿色
                    break;
                case BOTTOM:
                    color = Color.parseColor("#2196F3"); // 蓝色
                    break;
                case OUTERWEAR:
                    color = Color.parseColor("#FF9800"); // 橙色
                    break;
                case DRESS:
                    color = Color.parseColor("#E91E63"); // 粉色
                    break;
                case SHOES:
                    color = Color.parseColor("#9C27B0"); // 紫色
                    break;
                case ACCESSORY:
                    color = Color.parseColor("#607D8B"); // 蓝灰色
                    break;
                case BAG:
                    color = Color.parseColor("#795548"); // 棕色
                    break;
                default:
                    color = Color.parseColor("#9E9E9E"); // 灰色
                    break;
            }
            
            holder.typeLabel.setBackgroundColor(color);
        } else {
            holder.typeLabel.setVisibility(View.GONE);
        }
        
        // 设置收藏图标
        holder.favoriteIcon.setVisibility(clothing.isFavorite() ? View.VISIBLE : View.GONE);
        
        // 设置季节标签
        setupSeasonChips(holder.seasonChipGroup, clothing);
        
        // 加载图片
        loadClothingImage(holder.clothingImage, clothing.getImageUri());
        
        // 设置点击事件
        holder.clothingCard.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onClothingItemClick(clothing);
            }
        });
        
        // 设置长按事件
        holder.clothingCard.setOnLongClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onClothingItemLongClick(clothing);
                return true;
            }
            return false;
        });
        
        // 设置收藏点击事件
        holder.favoriteIcon.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onFavoriteClick(clothing);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return clothingList.size();
    }
    
    /**
     * 设置季节芯片
     */
    private void setupSeasonChips(ChipGroup chipGroup, Clothing clothing) {
        // 清除之前的芯片
        chipGroup.removeAllViews();
        
        // 获取季节列表
        List<Clothing.Season> seasons = clothing.getSeasons();
        
        // 创建季节芯片
        if (seasons != null && !seasons.isEmpty()) {
            for (Clothing.Season season : seasons) {
                Chip chip = new Chip(context);
                chip.setChipBackgroundColorResource(R.color.colorChipBackground);
                chip.setTextSize(10);
                chip.setChipStrokeColorResource(R.color.colorDivider);
                chip.setChipStrokeWidth(0.5f);
                chip.setClickable(false);
                
                switch (season) {
                    case SPRING:
                        chip.setText("春");
                        break;
                    case SUMMER:
                        chip.setText("夏");
                        break;
                    case AUTUMN:
                        chip.setText("秋");
                        break;
                    case WINTER:
                        chip.setText("冬");
                        break;
                    case ALL_SEASON:
                        chip.setText("四季");
                        break;
                }
                
                chipGroup.addView(chip);
            }
        }
    }
    
    /**
     * 加载服装图片
     */
    private void loadClothingImage(ImageView imageView, String imageUri) {
        if (imageUri != null && !imageUri.isEmpty()) {
            try {
                // 判断是文件路径还是内容URI
                if (imageUri.startsWith("content://")) {
                    // 内容URI
                    Glide.with(context)
                            .load(Uri.parse(imageUri))
                            .placeholder(R.drawable.clothing_placeholder)
                            .error(R.drawable.clothing_placeholder)
                            .centerCrop()
                            .into(imageView);
                } else {
                    // 文件路径
                    Glide.with(context)
                            .load(new File(imageUri))
                            .placeholder(R.drawable.clothing_placeholder)
                            .error(R.drawable.clothing_placeholder)
                            .centerCrop()
                            .into(imageView);
                }
            } catch (Exception e) {
                e.printStackTrace();
                imageView.setImageResource(R.drawable.clothing_placeholder);
            }
        } else {
            // 没有图片URI，使用占位图
            imageView.setImageResource(R.drawable.clothing_placeholder);
        }
    }
    
    /**
     * 服装项的ViewHolder
     */
    static class ClothingViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView clothingCard;
        ImageView clothingImage;
        ImageView favoriteIcon;
        TextView typeLabel;
        TextView nameTextView;
        TextView brandTextView;
        ChipGroup seasonChipGroup;
        
        public ClothingViewHolder(@NonNull View itemView) {
            super(itemView);
            clothingCard = itemView.findViewById(R.id.clothing_card);
            clothingImage = itemView.findViewById(R.id.clothing_image);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            typeLabel = itemView.findViewById(R.id.type_label);
            nameTextView = itemView.findViewById(R.id.clothing_name);
            brandTextView = itemView.findViewById(R.id.clothing_brand);
            seasonChipGroup = itemView.findViewById(R.id.season_chip_group);
        }
    }
    
    /**
     * 服装项点击监听接口
     */
    public interface OnClothingItemClickListener {
        void onClothingItemClick(Clothing clothing);
        void onClothingItemLongClick(Clothing clothing);
        void onFavoriteClick(Clothing clothing);
    }
} 