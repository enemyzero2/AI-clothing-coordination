package com.example.aioutfitapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aioutfitapp.R;
import com.example.aioutfitapp.Post;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> postList;
    private final PostInteractionListener listener;

    // 构造函数
    public PostAdapter(Context context, List<Post> postList, PostInteractionListener listener) {
        this.context = context;
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 创建视图并绑定ViewHolder
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        try {
            // 绑定数据到视图
            Post post = postList.get(position);
            
            // 安全地设置文本
            if (holder.userName != null) holder.userName.setText(post.getUserName());
            if (holder.postContent != null) holder.postContent.setText(post.getContent());
            if (holder.likeCount != null) holder.likeCount.setText(String.valueOf(post.getLikeCount()));
            if (holder.commentCount != null) holder.commentCount.setText(String.valueOf(post.getCommentCount()));
            
            // 加载服装图片
            if (holder.outfitImage != null && post.getOutfitImageUrl() != null && !post.getOutfitImageUrl().isEmpty()) {
                holder.outfitImage.setVisibility(View.VISIBLE);
                try {
                    Glide.with(context)
                        .load(post.getOutfitImageUrl())
                        .placeholder(R.drawable.placeholder_outfit)
                        .error(R.drawable.error_outfit)
                        .centerCrop()
                        .into(holder.outfitImage);
                } catch (Exception e) {
                    // 图片加载失败，显示错误占位图
                    holder.outfitImage.setImageResource(R.drawable.error_outfit);
                    e.printStackTrace();
                }
            } else if (holder.outfitImage != null) {
                holder.outfitImage.setVisibility(View.GONE);
            }
            
            // 加载用户头像
            if (holder.userProfileImage != null) {
                try {
                    Glide.with(context)
                        .load("https://example.com/user_" + post.getUserId() + ".jpg") // 实际应该使用真实的用户头像URL
                        .placeholder(R.drawable.placeholder_user)
                        .error(R.drawable.error_user)
                        .circleCrop() // 圆形裁剪
                        .into(holder.userProfileImage);
                } catch (Exception e) {
                    // 头像加载失败，显示错误占位图
                    holder.userProfileImage.setImageResource(R.drawable.error_user);
                    e.printStackTrace();
                }
            }
            
            // 设置点赞状态
            if (holder.likeIcon != null && holder.likeCount != null) {
                if (post.isLiked()) {
                    holder.likeIcon.setImageResource(R.drawable.ic_liked);
                    holder.likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.likeColor));
                    holder.likeCount.setTextColor(ContextCompat.getColor(context, R.color.likeColor));
                } else {
                    holder.likeIcon.setImageResource(R.drawable.ic_like);
                    holder.likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.iconNormalColor));
                    holder.likeCount.setTextColor(ContextCompat.getColor(context, R.color.textSecondaryColor));
                }
            }

            // 设置点击事件
            if (holder.likeIcon != null) 
                holder.likeIcon.setOnClickListener(v -> listener.onLikeClicked(position));
            if (holder.commentIcon != null) 
                holder.commentIcon.setOnClickListener(v -> listener.onCommentClicked(position));
            if (holder.shareIcon != null) 
                holder.shareIcon.setOnClickListener(v -> listener.onShareClicked(position));
            if (holder.tryOnIcon != null) 
                holder.tryOnIcon.setOnClickListener(v -> listener.onTryOnClicked(position));
            if (holder.postView != null) 
                holder.postView.setOnClickListener(v -> listener.onPostClicked(position));
            if (holder.userProfileImage != null) 
                holder.userProfileImage.setOnClickListener(v -> listener.onUserProfileClicked(position));
        } catch (Exception e) {
            // 捕获绑定过程中的任何异常
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // ViewHolder类
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView userName, postContent, likeCount, commentCount;
        ImageView likeIcon, commentIcon, shareIcon, tryOnIcon, outfitImage, userProfileImage;
        View postView;

        public PostViewHolder(View itemView) {
            super(itemView);
            try {
                userName = itemView.findViewById(R.id.textUserName);
                postContent = itemView.findViewById(R.id.textPostContent);
                likeCount = itemView.findViewById(R.id.textLikeCount);
                commentCount = itemView.findViewById(R.id.textCommentCount);

                likeIcon = itemView.findViewById(R.id.likeIcon);
                commentIcon = itemView.findViewById(R.id.commentIcon);
                shareIcon = itemView.findViewById(R.id.shareIcon);
                tryOnIcon = itemView.findViewById(R.id.tryOnIcon);
                outfitImage = itemView.findViewById(R.id.imageOutfit);
                userProfileImage = itemView.findViewById(R.id.imageUserProfile);
                
                postView = itemView.findViewById(R.id.postView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 接口用于处理用户点击事件
    public interface PostInteractionListener {
        void onLikeClicked(int position);
        void onCommentClicked(int position);
        void onShareClicked(int position);
        void onTryOnClicked(int position);
        void onPostClicked(int position);
        void onUserProfileClicked(int position);
    }
}
