package com.example.aioutfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.aioutfitapp.R;
import com.example.aioutfitapp.PostAdapter;
import com.example.aioutfitapp.Post;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity implements PostAdapter.PostInteractionListener {

    private RecyclerView recyclerViewPosts;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PostAdapter postAdapter;
    private List<Post> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_community);
            
            // 初始化工具栏
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            
            // 返回按钮
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            
            // 初始化视图
            recyclerViewPosts = findViewById(R.id.recyclerViewPosts);
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            FloatingActionButton fabCreatePost = findViewById(R.id.fabCreatePost);
            
            // 设置布局管理器
            recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));
            
            // 初始化数据
            postList = new ArrayList<>();
            
            // 先创建适配器
            postAdapter = new PostAdapter(this, postList, this);
            recyclerViewPosts.setAdapter(postAdapter);
            
            // 然后加载数据
            loadDummyPosts();
            
            // 设置下拉刷新监听器
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.primaryColor,
                    R.color.accentColor
            );
            swipeRefreshLayout.setOnRefreshListener(this::refreshPosts);
            
            // 设置发布按钮点击事件
            fabCreatePost.setOnClickListener(v -> navigateToCreatePost());
        } catch (Exception e) {
            // 捕获异常并显示错误信息
            Toast.makeText(this, getString(R.string.init_error, e.getMessage()), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            // 返回主页
            finish();
        }
    }
    
    private void loadDummyPosts() {
        try {
            // 确保列表和适配器已初始化
            if (postList == null) {
                postList = new ArrayList<>();
            }
            
            // 清空现有数据
            postList.clear();
            
            // 添加测试数据
            postList.add(new Post(1, "风格达人", 1001, "今天尝试了一套简约风格的搭配，黑色高领毛衣搭配米色直筒裤，非常适合秋冬季节！", 
                    false, 128, 24, 101, "https://example.com/outfit1.jpg"));
            
            postList.add(new Post(2, "时尚博主", 1002, "分享一套适合约会的甜美风穿搭，粉色针织衫加白色百褶裙，少女感十足～", 
                    true, 256, 42, 102, "https://example.com/outfit2.jpg"));
            
            postList.add(new Post(3, "潮流先锋", 1003, "街头风格永远不过时！oversized卫衣配工装裤，再加一双复古球鞋，回头率超高！", 
                    false, 89, 15, 103, "https://example.com/outfit3.jpg"));
            
            postList.add(new Post(4, "穿搭达人", 1004, "办公室穿搭不一定要正式，这套休闲西装+白T的组合既专业又不失个性。", 
                    false, 176, 31, 104, "https://example.com/outfit4.jpg"));
            
            postList.add(new Post(5, "极简主义", 1005, "Less is more! 纯色T恤配牛仔裤，永远是最百搭的选择。", 
                    false, 145, 19, 105, "https://example.com/outfit5.jpg"));
            
            // 确保适配器已初始化
            if (postAdapter != null) {
                postAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.load_data_error, e.getMessage()), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void refreshPosts() {
        // 实际应从API刷新数据
        // 模拟网络延迟
        swipeRefreshLayout.postDelayed(() -> {
            // 重新加载数据
            loadDummyPosts();
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(CommunityActivity.this, getString(R.string.refresh_success), Toast.LENGTH_SHORT).show();
        }, 1500);
    }
    
    private void navigateToCreatePost() {
        // Intent intent = new Intent(this, CreatePostActivity.class);
        // startActivity(intent);
        Toast.makeText(this, getString(R.string.create_post_coming_soon), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_community, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // 处理返回按钮点击事件
            finish();
            return true;
        } else if (id == R.id.action_search) {
            // 打开搜索页面
            Toast.makeText(this, getString(R.string.search_coming_soon_wait), Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_filter) {
            // 打开筛选页面
            Toast.makeText(this, getString(R.string.filter_coming_soon), Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            // 打开设置页面
            Toast.makeText(this, getString(R.string.settings_coming_soon), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // 实现PostAdapter.PostInteractionListener接口方法
    
    @Override
    public void onLikeClicked(int position) {
        Post post = postList.get(position);
        // 处理点赞逻辑
        boolean isLiked = !post.isLiked(); // 切换点赞状态
        post.setLiked(isLiked);
        if (isLiked) {
            post.setLikeCount(post.getLikeCount() + 1);
        } else {
            post.setLikeCount(post.getLikeCount() - 1);
        }
        postAdapter.notifyItemChanged(position);
        
        // 实际应调用API更新点赞状态
        Toast.makeText(this, isLiked ? getString(R.string.liked) : getString(R.string.unliked), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onCommentClicked(int position) {
        Post post = postList.get(position);
        // 打开评论页面
        // Intent intent = new Intent(this, CommentActivity.class);
        // intent.putExtra("POST_ID", post.getId());
        // startActivity(intent);
        Toast.makeText(this, getString(R.string.comment_coming_soon), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onShareClicked(int position) {
        Post post = postList.get(position);
        // 打开分享对话框
        Toast.makeText(this, getString(R.string.share_coming_soon), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onTryOnClicked(int position) {
        Post post = postList.get(position);
        // 打开一键换装页面
        // Intent intent = new Intent(this, TryOnActivity.class);
        // intent.putExtra("OUTFIT_ID", post.getOutfitId());
        // intent.putExtra("OUTFIT_IMAGE_URL", post.getOutfitImageUrl());
        // startActivity(intent);
        Toast.makeText(this, getString(R.string.try_on_coming_soon), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onPostClicked(int position) {
        Post post = postList.get(position);
        // 打开帖子详情页面
        // Intent intent = new Intent(this, PostDetailActivity.class);
        // intent.putExtra("POST_ID", post.getId());
        // startActivity(intent);
        Toast.makeText(this, getString(R.string.post_detail_coming_soon), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onUserProfileClicked(int position) {
        Post post = postList.get(position);
        // 打开用户个人主页
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("USER_ID", post.getUserId());
        startActivity(intent);
    }
}
