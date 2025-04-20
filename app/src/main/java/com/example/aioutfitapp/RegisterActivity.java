package com.example.aioutfitapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.aioutfitapp.api.ApiClient;
import com.example.aioutfitapp.api.ApiService;
import com.example.aioutfitapp.api.models.RegisterRequest;
import com.example.aioutfitapp.api.models.RegisterResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 注册页面活动
 * 
 * 负责用户注册，提供美观的日夜过渡视觉效果
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    
    // UI组件
    private View backgroundView;
    private ImageView sunView;
    private ImageView moonView;
    private ImageView nearClouds;
    private ImageView farClouds;
    private View blurOverlay;
    private ConstraintLayout registerContainer;
    private EditText usernameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private TextView usernameLabel;
    private TextView emailLabel;
    private TextView passwordLabel;
    private TextView confirmPasswordLabel;
    private Button registerButton;
    private CheckBox privacyCheckbox;
    private TextView welcomeText;
    private TextView loginLink;

    // 动画状态
    private boolean isNightMode = false;
    private float parallaxScrollX = 0f;
    private AnimatorSet cloudAnimatorSet;
    private AnimatorSet dayNightAnimatorSet;
    
    // API服务
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化API服务
        apiService = ApiClient.getApiService();

        // 初始化视图
        backgroundView = findViewById(R.id.background_view);
        sunView = findViewById(R.id.sun_view);
        moonView = findViewById(R.id.moon_view);
        nearClouds = findViewById(R.id.near_clouds);
        farClouds = findViewById(R.id.far_clouds);
        blurOverlay = findViewById(R.id.blur_overlay);
        registerContainer = findViewById(R.id.register_container);
        usernameInput = findViewById(R.id.username_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        usernameLabel = findViewById(R.id.username_label);
        emailLabel = findViewById(R.id.email_label);
        passwordLabel = findViewById(R.id.password_label);
        confirmPasswordLabel = findViewById(R.id.confirm_password_label);
        registerButton = findViewById(R.id.register_button);
        privacyCheckbox = findViewById(R.id.privacy_checkbox);
        welcomeText = findViewById(R.id.welcome_text);
        loginLink = findViewById(R.id.login_link);

        // 设置初始位置
        nearClouds.setTranslationX(0);
        farClouds.setTranslationX(0);
        
        // 设置初始状态为日间模式
        setDayMode(true);

        // 启动云层动画
        startCloudAnimation();

        // 监听密码输入框焦点变化
        passwordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 密码输入框获取焦点时切换到夜间模式
                animateDayNightTransition(false);
            } else if (!usernameInput.hasFocus() && !emailInput.hasFocus() && !confirmPasswordInput.hasFocus()) {
                // 当所有输入框都没有焦点时才切换回日间模式
                animateDayNightTransition(true);
            }
        });
        
        // 监听确认密码输入框焦点变化
        confirmPasswordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 确认密码输入框获取焦点时切换到夜间模式
                animateDayNightTransition(false);
            } else if (!usernameInput.hasFocus() && !emailInput.hasFocus() && !passwordInput.hasFocus()) {
                // 当所有输入框都没有焦点时才切换回日间模式
                animateDayNightTransition(true);
            }
        });
        
        // 监听用户名输入框焦点变化
        usernameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 用户名输入框获取焦点时切换到日间模式
                animateDayNightTransition(true);
            } else if (!emailInput.hasFocus() && !passwordInput.hasFocus() && !confirmPasswordInput.hasFocus()) {
                // 当所有输入框都没有焦点时保持当前模式
                // 不做任何切换
            }
        });
        
        // 监听邮箱输入框焦点变化
        emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 邮箱输入框获取焦点时切换到日间模式
                animateDayNightTransition(true);
            } else if (!usernameInput.hasFocus() && !passwordInput.hasFocus() && !confirmPasswordInput.hasFocus()) {
                // 当所有输入框都没有焦点时保持当前模式
                // 不做任何切换
            }
        });
        
        // 监听密码输入
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 清除密码输入框的错误提示
                passwordInput.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // 监听确认密码输入
        confirmPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 清除确认密码输入框的错误提示
                confirmPasswordInput.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // 监听用户名输入
        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 清除用户名输入框的错误提示
                usernameInput.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // 监听邮箱输入
        emailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 清除邮箱输入框的错误提示
                emailInput.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // 注册按钮点击事件
        registerButton.setOnClickListener(v -> {
            // 验证表单
            if (validateForm()) {
                // 增加模糊效果
                animateBlurEffect(true);
                
                // 开始注册过程
                registerButton.setText("注册中...");
                registerButton.setEnabled(false);
                
                // 调试日志
                Log.d(TAG, "正在尝试注册: " + usernameInput.getText().toString());
                
                // 调用API服务注册
                attemptRegister(
                    usernameInput.getText().toString(),
                    emailInput.getText().toString(),
                    passwordInput.getText().toString()
                );
            }
        });
        
        // 登录链接点击事件
        loginLink.setOnClickListener(v -> {
            // 跳转到登录页面
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // 结束当前活动
        });
    }
    
    /**
     * 验证表单输入
     * 
     * @return 表单是否有效
     */
    private boolean validateForm() {
        boolean valid = true;
        
        String username = usernameInput.getText().toString();
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        
        // 验证用户名
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("请输入用户名");
            valid = false;
        } else if (username.length() < 3) {
            usernameInput.setError("用户名长度至少为3个字符");
            valid = false;
        } else {
            usernameInput.setError(null);
        }
        
        // 验证邮箱
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("请输入邮箱");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("请输入有效的邮箱地址");
            valid = false;
        } else {
            emailInput.setError(null);
        }
        
        // 验证密码
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("请输入密码");
            valid = false;
        } else if (password.length() < 6) {
            passwordInput.setError("密码长度至少为6个字符");
            valid = false;
        } else {
            passwordInput.setError(null);
        }
        
        // 验证确认密码
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("请确认密码");
            valid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordInput.setError("两次输入的密码不一致");
            valid = false;
        } else {
            confirmPasswordInput.setError(null);
        }
        
        // 验证隐私协议同意
        if (!privacyCheckbox.isChecked()) {
            Toast.makeText(this, "请阅读并同意隐私政策", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * 尝试注册操作
     * 
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     */
    private void attemptRegister(String username, String email, String password) {
        // 创建注册请求
        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        
        // 调用API进行注册
        Call<RegisterResponse> call = apiService.register(registerRequest);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                // 恢复模糊效果
                animateBlurEffect(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();
                    
                    if (registerResponse.isSuccess()) {
                        // 注册成功
                        Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();
                        
                        // 保存用户信息和token
                        // TODO: 保存用户信息和token到SharedPreferences
                        
                        // 跳转到主界面
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // 结束当前活动
                    } else {
                        // 注册失败
                        Toast.makeText(RegisterActivity.this, "注册失败: " + registerResponse.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "注册失败: " + registerResponse.getMessage());
                        
                        // 重置按钮状态
                        registerButton.setText("注册");
                        registerButton.setEnabled(true);
                    }
                } else {
                    // API调用失败
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Toast.makeText(RegisterActivity.this, "注册失败: " + errorBody, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "API调用失败: " + errorBody);
                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this, "注册失败: 服务器错误", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "API调用失败: ", e);
                    }
                    
                    // 重置按钮状态
                    registerButton.setText("注册");
                    registerButton.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                // 网络请求失败
                animateBlurEffect(false);
                
                Toast.makeText(RegisterActivity.this, "网络错误，请稍后重试", Toast.LENGTH_LONG).show();
                Log.e(TAG, "网络请求失败: ", t);
                
                // 重置按钮状态
                registerButton.setText("注册");
                registerButton.setEnabled(true);
            }
        });
    }
    
    /**
     * 启动云层动画
     */
    private void startCloudAnimation() {
        // 停止正在运行的动画
        if (cloudAnimatorSet != null && cloudAnimatorSet.isRunning()) {
            cloudAnimatorSet.cancel();
        }
        
        // 定义云层移动距离
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        
        // 创建近处云层动画
        ObjectAnimator nearCloudAnimator = ObjectAnimator.ofFloat(nearClouds, "translationX", 0, -screenWidth);
        nearCloudAnimator.setDuration(35000); // 35秒
        nearCloudAnimator.setInterpolator(new LinearInterpolator());
        nearCloudAnimator.setRepeatCount(ValueAnimator.INFINITE);
        nearCloudAnimator.setRepeatMode(ValueAnimator.RESTART);
        
        // 创建远处云层动画
        ObjectAnimator farCloudAnimator = ObjectAnimator.ofFloat(farClouds, "translationX", 0, -screenWidth * 1.5f);
        farCloudAnimator.setDuration(70000); // 70秒
        farCloudAnimator.setInterpolator(new LinearInterpolator());
        farCloudAnimator.setRepeatCount(ValueAnimator.INFINITE);
        farCloudAnimator.setRepeatMode(ValueAnimator.RESTART);
        
        // 组合动画
        cloudAnimatorSet = new AnimatorSet();
        cloudAnimatorSet.playTogether(nearCloudAnimator, farCloudAnimator);
        cloudAnimatorSet.start();
    }
    
    /**
     * 设置日间模式
     * 
     * @param isDay 是否为日间模式
     */
    private void setDayMode(boolean isDay) {
        isNightMode = !isDay;
        
        // 设置背景渐变色
        GradientDrawable background;
        if (isDay) {
            // 日间渐变色
            background = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {
                            Color.parseColor("#4A90E2"),  // 天蓝色
                            Color.parseColor("#87CEFA"),  // 淡蓝色
                            Color.parseColor("#C2E6FF")   // 更淡的蓝色
                    }
            );
            
            // 显示太阳
            sunView.setAlpha(1f);
            moonView.setAlpha(0f);
            
            // 设置表单文字颜色
            welcomeText.setTextColor(Color.WHITE);
            usernameLabel.setTextColor(Color.WHITE);
            emailLabel.setTextColor(Color.WHITE);
            passwordLabel.setTextColor(Color.WHITE);
            confirmPasswordLabel.setTextColor(Color.WHITE);
            
            // 设置模糊效果
            blurOverlay.setAlpha(0.3f);
        } else {
            // 夜间渐变色
            background = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {
                            Color.parseColor("#0F1F3D"),  // 深蓝色
                            Color.parseColor("#283655"),  // 中蓝色
                            Color.parseColor("#4D648D")   // 浅蓝色
                    }
            );
            
            // 显示月亮
            sunView.setAlpha(0f);
            moonView.setAlpha(1f);
            
            // 设置表单文字颜色
            welcomeText.setTextColor(Color.WHITE);
            usernameLabel.setTextColor(Color.WHITE);
            emailLabel.setTextColor(Color.WHITE);
            passwordLabel.setTextColor(Color.WHITE);
            confirmPasswordLabel.setTextColor(Color.WHITE);
            
            // 设置模糊效果
            blurOverlay.setAlpha(0.6f);
        }
        
        backgroundView.setBackground(background);
    }
    
    /**
     * 动画过渡到日间或夜间模式
     * 
     * @param toDay 是否过渡到日间模式
     */
    private void animateDayNightTransition(boolean toDay) {
        // 如果当前已经是目标模式，则不执行动画
        if ((toDay && !isNightMode) || (!toDay && isNightMode)) {
            return;
        }
        
        // 停止正在运行的动画
        if (dayNightAnimatorSet != null && dayNightAnimatorSet.isRunning()) {
            dayNightAnimatorSet.cancel();
        }
        
        // 定义开始颜色和结束颜色
        int[] startColors;
        int[] endColors;
        
        if (toDay) {
            // 从夜间过渡到日间
            startColors = new int[] {
                    Color.parseColor("#0F1F3D"),  // 深蓝色
                    Color.parseColor("#283655"),  // 中蓝色
                    Color.parseColor("#4D648D")   // 浅蓝色
            };
            
            endColors = new int[] {
                    Color.parseColor("#4A90E2"),  // 天蓝色
                    Color.parseColor("#87CEFA"),  // 淡蓝色
                    Color.parseColor("#C2E6FF")   // 更淡的蓝色
            };
        } else {
            // 从日间过渡到夜间
            startColors = new int[] {
                    Color.parseColor("#4A90E2"),  // 天蓝色
                    Color.parseColor("#87CEFA"),  // 淡蓝色
                    Color.parseColor("#C2E6FF")   // 更淡的蓝色
            };
            
            endColors = new int[] {
                    Color.parseColor("#0F1F3D"),  // 深蓝色
                    Color.parseColor("#283655"),  // 中蓝色
                    Color.parseColor("#4D648D")   // 浅蓝色
            };
        }
        
        // 创建背景颜色动画
        ValueAnimator colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.addUpdateListener(animation -> {
            float ratio = (float) animation.getAnimatedValue();
            
            // 混合颜色
            int[] currentColors = new int[3];
            for (int i = 0; i < 3; i++) {
                currentColors[i] = blendColors(startColors[i], endColors[i], ratio);
            }
            
            // 更新背景渐变色
            GradientDrawable background = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    currentColors
            );
            backgroundView.setBackground(background);
        });
        
        // 创建太阳/月亮动画
        ObjectAnimator sunAlphaAnimator = ObjectAnimator.ofFloat(sunView, "alpha", toDay ? 0f : 1f, toDay ? 1f : 0f);
        ObjectAnimator moonAlphaAnimator = ObjectAnimator.ofFloat(moonView, "alpha", toDay ? 1f : 0f, toDay ? 0f : 1f);
        
        // 创建模糊效果动画
        ObjectAnimator blurAnimator = ObjectAnimator.ofFloat(blurOverlay, "alpha", toDay ? 0.6f : 0.3f, toDay ? 0.3f : 0.6f);
        
        // 组合动画
        dayNightAnimatorSet = new AnimatorSet();
        dayNightAnimatorSet.playTogether(colorAnimator, sunAlphaAnimator, moonAlphaAnimator, blurAnimator);
        dayNightAnimatorSet.setDuration(1000); // 1秒
        dayNightAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // 监听动画结束
        dayNightAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                
                // 更新夜间模式状态
                isNightMode = !toDay;
                
                // 确保最终状态正确
                setDayMode(toDay);
            }
        });
        
        dayNightAnimatorSet.start();
    }
    
    /**
     * 混合两种颜色
     * 
     * @param color1 颜色1
     * @param color2 颜色2
     * @param ratio 混合比例（0.0-1.0）
     * @return 混合后的颜色
     */
    private int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        
        float a = Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio;
        float r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio;
        float g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio;
        float b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio;
        
        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }
    
    /**
     * 动画模糊效果
     * 
     * @param show 是否显示模糊效果
     */
    private void animateBlurEffect(boolean show) {
        ObjectAnimator blurAnimator = ObjectAnimator.ofFloat(blurOverlay, "alpha", show ? 0.3f : 0.8f, show ? 0.8f : 0.3f);
        blurAnimator.setDuration(300);
        blurAnimator.start();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // 暂停云层动画
        if (cloudAnimatorSet != null && cloudAnimatorSet.isRunning()) {
            cloudAnimatorSet.pause();
        }
        
        // 暂停日夜过渡动画
        if (dayNightAnimatorSet != null && dayNightAnimatorSet.isRunning()) {
            dayNightAnimatorSet.pause();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 恢复云层动画
        if (cloudAnimatorSet != null && cloudAnimatorSet.isPaused()) {
            cloudAnimatorSet.resume();
        } else if (cloudAnimatorSet == null) {
            startCloudAnimation();
        }
        
        // 恢复日夜过渡动画
        if (dayNightAnimatorSet != null && dayNightAnimatorSet.isPaused()) {
            dayNightAnimatorSet.resume();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 取消所有动画
        if (cloudAnimatorSet != null) {
            cloudAnimatorSet.cancel();
            cloudAnimatorSet = null;
        }
        
        if (dayNightAnimatorSet != null) {
            dayNightAnimatorSet.cancel();
            dayNightAnimatorSet = null;
        }
    }
} 