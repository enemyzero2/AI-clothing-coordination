package com.example.aioutfitapp.api;

import java.util.List;

import com.example.aioutfitapp.api.models.BodyData;
import com.example.aioutfitapp.api.models.BodyDataRequest;
import com.example.aioutfitapp.api.models.BodyDataResponse;
import com.example.aioutfitapp.api.models.ClothingRequest;
import com.example.aioutfitapp.api.models.ClothingResponse;
import com.example.aioutfitapp.api.models.CommentListResponse;
import com.example.aioutfitapp.api.models.CommentRequest;
import com.example.aioutfitapp.api.models.CommentResponse;
import com.example.aioutfitapp.api.models.DeleteResponse;
import com.example.aioutfitapp.api.models.FavoriteRequest;
import com.example.aioutfitapp.api.models.LikeRequest;
import com.example.aioutfitapp.api.models.LoginRequest;
import com.example.aioutfitapp.api.models.LoginResponse;
import com.example.aioutfitapp.api.models.PostListResponse;
import com.example.aioutfitapp.api.models.PostRequest;
import com.example.aioutfitapp.api.models.PostResponse;
import com.example.aioutfitapp.api.models.RegisterRequest;
import com.example.aioutfitapp.api.models.RegisterResponse;
import com.example.aioutfitapp.api.models.UserDetail;
import com.example.aioutfitapp.api.models.UserDetailResponse;
import com.example.aioutfitapp.api.models.UserUpdateRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API服务接口
 * 
 * 定义与后端服务器通信的所有API端点和方法
 */
public interface ApiService {

    //============================= 用户相关 API =============================
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求(用户名/邮箱和密码)
     * @return 登录响应(包含用户信息和token)
     */
    @POST("users/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);
    
    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求(用户名、邮箱、密码等)
     * @return 注册响应(包含用户信息和token)
     */
    @POST("users/register")
    Call<RegisterResponse> register(@Body RegisterRequest registerRequest);
    
    /**
     * 获取用户详情
     * 
     * @param userId 用户ID
     * @return 用户详情响应
     */
    @GET("users/{userId}")
    Call<UserDetailResponse> getUserDetail(@Path("userId") int userId);
    
    /**
     * 更新用户资料
     * 
     * @param userId 用户ID
     * @param userUpdateRequest 用户更新请求
     * @return 更新后的用户详情
     */
    @PUT("users/{userId}")
    Call<UserDetailResponse> updateUserProfile(@Path("userId") int userId, @Body UserUpdateRequest userUpdateRequest);

    /**
     * 更新用户身材数据
     * 
     * @param userId 用户ID
     * @param bodyDataRequest 身材数据请求
     * @return 更新后的用户身材数据
     */
    @PUT("users/{userId}/body-data")
    Call<BodyDataResponse> updateBodyData(@Path("userId") int userId, @Body BodyDataRequest bodyDataRequest);

    //============================= 衣柜相关 API =============================
    
    /**
     * 获取用户衣柜中所有服装
     * 
     * @param userId 用户ID
     * @return 服装列表
     */
    @GET("users/{userId}/clothes")
    Call<List<ClothingResponse>> getUserClothes(@Path("userId") int userId);
    
    /**
     * 根据类型获取用户衣柜中的服装
     * 
     * @param userId 用户ID
     * @param type 服装类型
     * @return 服装列表
     */
    @GET("users/{userId}/clothes")
    Call<List<ClothingResponse>> getUserClothesByType(
            @Path("userId") int userId, 
            @Query("type") String type);
    
    /**
     * 获取用户衣柜中的服装(带筛选和排序)
     * 
     * @param userId 用户ID
     * @param type 服装类型(可选)
     * @param seasons 季节列表(可选)
     * @param favorite 是否只显示收藏(可选)
     * @param sortBy 排序字段
     * @param sortOrder 排序方向(asc/desc)
     * @return 服装列表
     */
    @GET("users/{userId}/clothes")
    Call<List<ClothingResponse>> getUserClothesFiltered(
            @Path("userId") int userId,
            @Query("type") String type,
            @Query("seasons") List<String> seasons,
            @Query("favorite") Boolean favorite,
            @Query("sortBy") String sortBy,
            @Query("sortOrder") String sortOrder);
    
    /**
     * 添加服装到衣柜
     * 
     * @param userId 用户ID
     * @param clothingRequest 服装请求
     * @return 添加的服装详情
     */
    @POST("users/{userId}/clothes")
    Call<ClothingResponse> addClothing(@Path("userId") int userId, @Body ClothingRequest clothingRequest);
    
    /**
     * 更新服装信息
     * 
     * @param userId 用户ID
     * @param clothingId 服装ID
     * @param clothingRequest 服装请求
     * @return 更新后的服装详情
     */
    @PUT("users/{userId}/clothes/{clothingId}")
    Call<ClothingResponse> updateClothing(
            @Path("userId") int userId, 
            @Path("clothingId") int clothingId, 
            @Body ClothingRequest clothingRequest);
    
    /**
     * 删除服装
     * 
     * @param userId 用户ID
     * @param clothingId 服装ID
     * @return 删除结果
     */
    @DELETE("users/{userId}/clothes/{clothingId}")
    Call<DeleteResponse> deleteClothing(@Path("userId") int userId, @Path("clothingId") int clothingId);
    
    /**
     * 更新服装收藏状态
     * 
     * @param userId 用户ID
     * @param clothingId 服装ID
     * @param favoriteRequest 收藏请求
     * @return 更新后的服装详情
     */
    @PUT("users/{userId}/clothes/{clothingId}/favorite")
    Call<ClothingResponse> updateClothingFavorite(
            @Path("userId") int userId, 
            @Path("clothingId") int clothingId, 
            @Body FavoriteRequest favoriteRequest);

    //============================= 社区相关 API =============================
    
    /**
     * 获取社区帖子列表
     * 
     * @param page 页码
     * @param pageSize 每页数量
     * @return 帖子列表
     */
    @GET("posts")
    Call<PostListResponse> getPosts(@Query("page") int page, @Query("pageSize") int pageSize);
    
    /**
     * 获取特定用户的帖子列表
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 帖子列表
     */
    @GET("users/{userId}/posts")
    Call<PostListResponse> getUserPosts(
            @Path("userId") int userId, 
            @Query("page") int page, 
            @Query("pageSize") int pageSize);
    
    /**
     * 发布帖子
     * 
     * @param userId 用户ID
     * @param postRequest 帖子请求
     * @return 创建的帖子详情
     */
    @POST("users/{userId}/posts")
    Call<PostResponse> createPost(@Path("userId") int userId, @Body PostRequest postRequest);
    
    /**
     * 获取帖子详情
     * 
     * @param postId 帖子ID
     * @return 帖子详情
     */
    @GET("posts/{postId}")
    Call<PostResponse> getPostDetail(@Path("postId") int postId);
    
    /**
     * 更新帖子
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @param postRequest 帖子请求
     * @return 更新后的帖子详情
     */
    @PUT("users/{userId}/posts/{postId}")
    Call<PostResponse> updatePost(
            @Path("userId") int userId, 
            @Path("postId") int postId, 
            @Body PostRequest postRequest);
    
    /**
     * 删除帖子
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 删除结果
     */
    @DELETE("users/{userId}/posts/{postId}")
    Call<DeleteResponse> deletePost(@Path("userId") int userId, @Path("postId") int postId);
    
    /**
     * 点赞/取消点赞帖子
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @param likeRequest 点赞请求
     * @return 更新后的帖子详情
     */
    @PUT("users/{userId}/posts/{postId}/like")
    Call<PostResponse> likePost(
            @Path("userId") int userId, 
            @Path("postId") int postId, 
            @Body LikeRequest likeRequest);
    
    /**
     * 获取帖子评论
     * 
     * @param postId 帖子ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 评论列表
     */
    @GET("posts/{postId}/comments")
    Call<CommentListResponse> getPostComments(
            @Path("postId") int postId, 
            @Query("page") int page, 
            @Query("pageSize") int pageSize);
    
    /**
     * 添加评论
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @param commentRequest 评论请求
     * @return 创建的评论详情
     */
    @POST("users/{userId}/posts/{postId}/comments")
    Call<CommentResponse> addComment(
            @Path("userId") int userId, 
            @Path("postId") int postId, 
            @Body CommentRequest commentRequest);
} 