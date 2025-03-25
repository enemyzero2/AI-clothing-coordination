package com.example.aioutfitapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 搭配卡片适配器
 * 
 * 用于在ViewPager2中显示搭配推荐卡片
 */
public class OutfitCardAdapter extends RecyclerView.Adapter<OutfitCardAdapter.OutfitViewHolder> {

    private List<OutfitItem> outfitItems;
    private Context context;

    /**
     * 搭配项数据类
     */
    public static class OutfitItem {
        private int imageResId;  // 搭配图片资源ID
        private String title;    // 搭配标题
        private String category; // 搭配分类
        private String occasion; // 适合场合
        private int ratingDots;  // 评分点数（1-5）
        private boolean isFavorite; // 是否收藏

        public OutfitItem(int imageResId, String title, String category, String occasion, int ratingDots, boolean isFavorite) {
            this.imageResId = imageResId;
            this.title = title;
            this.category = category;
            this.occasion = occasion;
            this.ratingDots = ratingDots;
            this.isFavorite = isFavorite;
        }

        public int getImageResId() {
            return imageResId;
        }

        public String getTitle() {
            return title;
        }

        public String getCategory() {
            return category;
        }

        public String getOccasion() {
            return occasion;
        }

        public int getRatingDots() {
            return ratingDots;
        }

        public boolean isFavorite() {
            return isFavorite;
        }

        public void setFavorite(boolean favorite) {
            isFavorite = favorite;
        }
    }

    /**
     * 构造函数
     */
    public OutfitCardAdapter(Context context, List<OutfitItem> outfitItems) {
        this.context = context;
        this.outfitItems = outfitItems;
    }

    @NonNull
    @Override
    public OutfitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_outfit_card, parent, false);
        return new OutfitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OutfitViewHolder holder, int position) {
        OutfitItem item = outfitItems.get(position);
        
        // 设置搭配图片
        holder.outfitImage.setImageResource(item.getImageResId());
        
        // 设置标题和场合
        holder.titleText.setText(item.getTitle());
        holder.occasionText.setText(item.getOccasion());
        
        // 设置收藏图标状态
        holder.favoriteIcon.setImageResource(
                item.isFavorite() ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
        
        // 设置评分点
        setupRatingDots(holder, item.getRatingDots());
        
        // 设置收藏按钮点击事件
        holder.favoriteIcon.setOnClickListener(v -> {
            boolean newState = !item.isFavorite();
            item.setFavorite(newState);
            holder.favoriteIcon.setImageResource(
                    newState ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
        });
    }

    /**
     * 设置评分点
     */
    private void setupRatingDots(OutfitViewHolder holder, int rating) {
        ImageView[] dots = {
                holder.ratingDot1, holder.ratingDot2, 
                holder.ratingDot3, holder.ratingDot4
        };
        
        for (int i = 0; i < dots.length; i++) {
            if (i < rating) {
                dots[i].setVisibility(View.VISIBLE);
            } else {
                dots[i].setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return outfitItems.size();
    }

    /**
     * 搭配卡片ViewHolder
     */
    public static class OutfitViewHolder extends RecyclerView.ViewHolder {
        ImageView outfitImage;
        TextView titleText;
        TextView occasionText;
        ImageView favoriteIcon;
        ImageView ratingDot1;
        ImageView ratingDot2;
        ImageView ratingDot3;
        ImageView ratingDot4;

        public OutfitViewHolder(@NonNull View itemView) {
            super(itemView);
            outfitImage = itemView.findViewById(R.id.outfit_image);
            titleText = itemView.findViewById(R.id.outfit_title);
            occasionText = itemView.findViewById(R.id.outfit_occasion);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            ratingDot1 = itemView.findViewById(R.id.rating_dot_1);
            ratingDot2 = itemView.findViewById(R.id.rating_dot_2);
            ratingDot3 = itemView.findViewById(R.id.rating_dot_3);
            ratingDot4 = itemView.findViewById(R.id.rating_dot_4);
        }
    }
} 