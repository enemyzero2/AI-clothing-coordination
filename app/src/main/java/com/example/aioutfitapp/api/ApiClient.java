package com.example.aioutfitapp.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API客户端
 * 
 * 配置和创建Retrofit实例，提供API服务接口
 */
public class ApiClient {
    // 基础URL，指向本地后端服务器
    // 模拟器使用10.0.2.2，真机使用开发电脑的实际IP地址
    // private static final String BASE_URL = "http://10.0.2.2:8080/api/";
    // private static final String BASE_URL = "http://10.0.0.2:8080/api/";
    private static final String BASE_URL = "http://10.29.206.148:8080/api/";
    
    // 认证令牌（登录后由服务器返回）
    private static String authToken = null;
    
    // Retrofit实例
    private static Retrofit retrofit = null;
    
    // API服务实例
    private static ApiService apiService = null;
    
    /**
     * 获取Retrofit实例
     * 
     * @return Retrofit实例
     */
    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            // 创建Gson实例，处理日期格式
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .create();
            
            // 创建OkHttpClient并添加拦截器
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            
            // 添加日志拦截器
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(loggingInterceptor);
            
            // 添加认证拦截器
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    
                    // 如果有认证令牌，添加到请求头
                    Request.Builder requestBuilder = original.newBuilder();
                    if (authToken != null && !authToken.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + authToken);
                    }
                    
                    Request request = requestBuilder
                            .header("Accept", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    
                    return chain.proceed(request);
                }
            });
            
            // 创建Retrofit实例
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClient.build())
                    .build();
        }
        
        return retrofit;
    }
    
    /**
     * 获取API服务接口
     * 
     * @return API服务接口
     */
    public static ApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofit().create(ApiService.class);
        }
        
        return apiService;
    }
    
    /**
     * 设置认证令牌
     * 
     * @param token 认证令牌
     */
    public static void setAuthToken(String token) {
        authToken = token;
        // 重置retrofit实例，以便在下次获取时应用新的认证令牌
        retrofit = null;
        apiService = null;
    }
    
    /**
     * 获取认证令牌
     * 
     * @return 认证令牌
     */
    public static String getAuthToken() {
        return authToken;
    }
    
    /**
     * 清除认证令牌（注销时使用）
     */
    public static void clearAuthToken() {
        authToken = null;
        // 重置retrofit实例
        retrofit = null;
        apiService = null;
    }
} 