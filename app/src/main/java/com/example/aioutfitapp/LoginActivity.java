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
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录页面活动
 * 
 * 负责用户登录认证，提供美观的日夜过渡视觉效果
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    
    // UI组件
    private View backgroundView;
    private ImageView sunView;
    private ImageView moonView;
    private ImageView nearClouds;
    private ImageView farClouds;
    private View blurOverlay;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private MaterialButton loginButton;
    private TextView registerLink;

    // 动画状态
    private boolean isNightMode = false;
    private float parallaxScrollX = 0f;
    private AnimatorSet cloudAnimatorSet;
    private AnimatorSet dayNightAnimatorSet;
    
    // 测试用的预设用户名和密码
    private static final String DEMO_USERNAME = "test";
    private static final String DEMO_PASSWORD = "password123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化视图
        backgroundView = findViewById(R.id.background_view);
        sunView = findViewById(R.id.sun_view);
        moonView = findViewById(R.id.moon_view);
        nearClouds = findViewById(R.id.near_clouds);
        farClouds = findViewById(R.id.far_clouds);
        blurOverlay = findViewById(R.id.blur_overlay);
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        usernameLayout = findViewById(R.id.username_layout);
        passwordLayout = findViewById(R.id.password_layout);
        loginButton = findViewById(R.id.login_button);
        registerLink = findViewById(R.id.register_link);

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
            } else {
                // 密码输入框失去焦点时切换回日间模式
                animateDayNightTransition(true);
            }
        });
        
        // 监听密码输入，清除错误提示
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 清除密码输入框的错误消息
                passwordLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // 监听用户名输入，清除错误信息
        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 清除用户名输入框的错误消息
                usernameLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // 登录按钮点击事件
        loginButton.setOnClickListener(v -> {
            // 验证表单
            if (validateForm()) {
                // 增加模糊效果
                animateBlurEffect(true);
                
                // 模拟登录过程
                loginButton.setText("登录中...");
                loginButton.setEnabled(false);
                
                // 调试日志
                Log.d(TAG, "正在尝试登录: " + usernameInput.getText().toString());
                
                // 模拟网络延迟
                loginButton.postDelayed(() -> {
                    // 验证用户名和密码
                    attemptLogin(
                        usernameInput.getText().toString(),
                        passwordInput.getText().toString()
                    );
                }, 1500);
            }
        });
        
        // 注册链接点击事件（暂未实现）
        registerLink.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "注册功能即将推出", Toast.LENGTH_SHORT).show();
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
        String password = passwordInput.getText().toString();
        
        // 验证用户名
        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("请输入用户名");
            valid = false;
        } else {
            usernameLayout.setError(null);
        }
        
        // 验证密码
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("请输入密码");
            valid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("密码长度至少为6个字符");
            valid = false;
        } else {
            passwordLayout.setError(null);
        }
        
        return valid;
    }
    
    /**
     * 尝试登录操作
     * 
     * @param username 用户名
     * @param password 密码
     */
    private void attemptLogin(String username, String password) {
        // 这里应该连接到实际的认证服务
        // 目前使用预设的测试账号进行演示
        
        if (DEMO_USERNAME.equals(username) && DEMO_PASSWORD.equals(password)) {
            // 登录成功
            Log.d(TAG, "登录成功");
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            
            // 跳转到主界面
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish(); // 关闭登录页面
        } else {
            // 登录失败
            Log.d(TAG, "登录失败: 用户名或密码错误");
            Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
            
            // 重置状态
            loginButton.setText("登录");
            loginButton.setEnabled(true);
            animateBlurEffect(false);
            
            // 显示错误提示
            passwordLayout.setError("用户名或密码错误");
            
            // 震动动画表示错误
            ObjectAnimator.ofFloat(loginButton, "translationX", 0, -15, 15, -15, 15, -10, 10, -5, 5, 0)
                .setDuration(500)
                .start();
        }
    }

    /**
     * 启动云层视差动画
     */
    private void startCloudAnimation() {
        // 取消之前的动画
        if (cloudAnimatorSet != null && cloudAnimatorSet.isRunning()) {
            cloudAnimatorSet.cancel();
        }

        // 近处云层动画 - 移动速度较快
        ObjectAnimator nearCloudAnimator = ObjectAnimator.ofFloat(nearClouds, "translationX", 0f, -1000f);
        nearCloudAnimator.setDuration(60000); // 60秒一个循环
        nearCloudAnimator.setInterpolator(new LinearInterpolator());
        nearCloudAnimator.setRepeatCount(ValueAnimator.INFINITE);
        nearCloudAnimator.setRepeatMode(ValueAnimator.RESTART);

        // 远处云层动画 - 移动速度较慢
        ObjectAnimator farCloudAnimator = ObjectAnimator.ofFloat(farClouds, "translationX", 0f, -500f);
        farCloudAnimator.setDuration(120000); // 120秒一个循环
        farCloudAnimator.setInterpolator(new LinearInterpolator());
        farCloudAnimator.setRepeatCount(ValueAnimator.INFINITE);
        farCloudAnimator.setRepeatMode(ValueAnimator.RESTART);

        // 组合动画
        cloudAnimatorSet = new AnimatorSet();
        cloudAnimatorSet.playTogether(nearCloudAnimator, farCloudAnimator);
        cloudAnimatorSet.start();
    }
    
    /**
     * 设置日间或夜间模式（立即设置，无动画）
     * @param isDay 是否为日间模式
     */
    private void setDayMode(boolean isDay) {
        // 背景颜色设置
        int[] colors;
        if (isDay) {
            colors = new int[]{
                ContextCompat.getColor(this, android.R.color.holo_blue_light),
                ContextCompat.getColor(this, android.R.color.holo_blue_bright)
            };
            
            // 太阳/月亮显示
            sunView.setAlpha(1f);
            moonView.setAlpha(0f);
            
            // 云层显示
            nearClouds.setAlpha(1f);
            farClouds.setAlpha(1f);
        } else {
            colors = new int[]{
                ContextCompat.getColor(this, android.R.color.holo_blue_dark),
                ContextCompat.getColor(this, android.R.color.black)
            };
            
            // 太阳/月亮显示
            sunView.setAlpha(0f);
            moonView.setAlpha(1f);
            
            // 云层显示
            nearClouds.setAlpha(0.7f);
            farClouds.setAlpha(0.5f);
        }
        
        // 设置背景
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        backgroundView.setBackground(gradientDrawable);
        
        // 更新模式状态
        isNightMode = !isDay;
    }
    
    /**
     * 动画过渡到日间或夜间模式
     * @param toDay 是否过渡到日间模式
     */
    private void animateDayNightTransition(boolean toDay) {
        // 取消正在进行的动画
        if (dayNightAnimatorSet != null && dayNightAnimatorSet.isRunning()) {
            dayNightAnimatorSet.cancel();
        }
        
        // 创建动画集
        dayNightAnimatorSet = new AnimatorSet();
        List<Animator> animators = new ArrayList<>();
        
        // 背景色动画
        int[] startColors;
        int[] endColors;
        
        if (toDay) {
            // 夜间到日间
            startColors = new int[]{
                ContextCompat.getColor(this, android.R.color.holo_blue_dark),
                ContextCompat.getColor(this, android.R.color.black)
            };
            endColors = new int[]{
                ContextCompat.getColor(this, android.R.color.holo_blue_light),
                ContextCompat.getColor(this, android.R.color.holo_blue_bright)
            };
            
            // 太阳/月亮渐变
            animators.add(ObjectAnimator.ofFloat(sunView, "alpha", 0f, 1f));
            animators.add(ObjectAnimator.ofFloat(moonView, "alpha", 1f, 0f));
            
            // 太阳/月亮旋转
            animators.add(ObjectAnimator.ofFloat(sunView, "rotation", 180f, 0f));
            animators.add(ObjectAnimator.ofFloat(moonView, "rotation", 0f, -180f));
            
            // 云层透明度
            animators.add(ObjectAnimator.ofFloat(nearClouds, "alpha", 0.7f, 1f));
            animators.add(ObjectAnimator.ofFloat(farClouds, "alpha", 0.5f, 1f));
        } else {
            // 日间到夜间
            startColors = new int[]{
                ContextCompat.getColor(this, android.R.color.holo_blue_light),
                ContextCompat.getColor(this, android.R.color.holo_blue_bright)
            };
            endColors = new int[]{
                ContextCompat.getColor(this, android.R.color.holo_blue_dark),
                ContextCompat.getColor(this, android.R.color.black)
            };
            
            // 太阳/月亮渐变
            animators.add(ObjectAnimator.ofFloat(sunView, "alpha", 1f, 0f));
            animators.add(ObjectAnimator.ofFloat(moonView, "alpha", 0f, 1f));
            
            // 太阳/月亮旋转
            animators.add(ObjectAnimator.ofFloat(sunView, "rotation", 0f, 180f));
            animators.add(ObjectAnimator.ofFloat(moonView, "rotation", -180f, 0f));
            
            // 云层透明度
            animators.add(ObjectAnimator.ofFloat(nearClouds, "alpha", 1f, 0.7f));
            animators.add(ObjectAnimator.ofFloat(farClouds, "alpha", 1f, 0.5f));
        }
        
        // 背景颜色动画(使用ValueAnimator)
        ValueAnimator colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(800);
        colorAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            int[] transitionColors = new int[2];
            
            // 计算过渡颜色
            for (int i = 0; i < 2; i++) {
                transitionColors[i] = blendColors(startColors[i], endColors[i], fraction);
            }
            
            // 更新背景
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR, transitionColors);
            backgroundView.setBackground(gradientDrawable);
        });
        animators.add(colorAnimator);
        
        // 设置动画集属性
        dayNightAnimatorSet.playTogether(animators);
        dayNightAnimatorSet.setDuration(800);
        dayNightAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // 更新状态
        dayNightAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isNightMode = !toDay;
            }
        });
        
        // 开始动画
        dayNightAnimatorSet.start();
    }

    /**
     * 混合两种颜色
     */
    private int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        
        float a = (Color.alpha(color1) * inverseRatio) + (Color.alpha(color2) * ratio);
        float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
        float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
        float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
        
        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }

    /**
     * 动画高斯模糊效果
     */
    private void animateBlurEffect(boolean show) {
        ObjectAnimator blurAnimator = ObjectAnimator.ofFloat(
                blurOverlay, "alpha", show ? 0f : 0.8f, show ? 0.8f : 0f);
        blurAnimator.setDuration(500);
        blurAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        blurAnimator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停动画
        if (cloudAnimatorSet != null) {
            cloudAnimatorSet.pause();
        }
        if (dayNightAnimatorSet != null && dayNightAnimatorSet.isRunning()) {
            dayNightAnimatorSet.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 恢复动画
        if (cloudAnimatorSet != null && cloudAnimatorSet.isPaused()) {
            cloudAnimatorSet.resume();
        } else if (cloudAnimatorSet == null) {
            startCloudAnimation();
        }
        
        if (dayNightAnimatorSet != null && dayNightAnimatorSet.isPaused()) {
            dayNightAnimatorSet.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理动画资源
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

