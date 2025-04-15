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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

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
    private ConstraintLayout loginContainer;
    private EditText usernameInput;
    private EditText passwordInput;
    private TextView usernameLabel;
    private TextView passwordLabel;
    private Button loginButton;
    private TextView forgotPassword;
    private CheckBox rememberMe;
    private TextView welcomeText;

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
        loginContainer = findViewById(R.id.login_container);
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        usernameLabel = findViewById(R.id.username_label);
        passwordLabel = findViewById(R.id.password_label);
        loginButton = findViewById(R.id.login_button);
        forgotPassword = findViewById(R.id.forgot_password);
        rememberMe = findViewById(R.id.remember_me);
        welcomeText = findViewById(R.id.welcome_text);

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
            } else if (!usernameInput.hasFocus()) {
                // 当两个输入框都没有焦点时才切换回日间模式
                animateDayNightTransition(true);
            }
        });
        
        // 监听用户名输入框焦点变化
        usernameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 用户名输入框获取焦点时切换到日间模式
                animateDayNightTransition(true);
            } else if (!passwordInput.hasFocus()) {
                // 当两个输入框都没有焦点时保持当前模式
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
        
        // 忘记密码点击事件
        forgotPassword.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "密码重置功能即将推出", Toast.LENGTH_SHORT).show();
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
            usernameInput.setError("请输入用户名");
            valid = false;
        } else {
            usernameInput.setError(null);
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
            passwordInput.setError("用户名或密码错误");
            
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
        isNightMode = !isDay;
        
        if (isDay) {
            // 日间模式：5色渐变背景
            GradientDrawable dayBackground = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                    ContextCompat.getColor(this, R.color.dayGradientTop),
                    ContextCompat.getColor(this, R.color.dayGradientMiddleTop),
                    ContextCompat.getColor(this, R.color.dayGradientMiddle),
                    ContextCompat.getColor(this, R.color.dayGradientMiddleLower),
                    ContextCompat.getColor(this, R.color.dayGradientBottom)
                }
            );
            backgroundView.setBackground(dayBackground);
            sunView.setAlpha(1f);
            moonView.setAlpha(0f);
            blurOverlay.setAlpha(0.8f);
            loginContainer.setBackgroundResource(R.drawable.glass_background);
        } else {
            // 夜间模式：5色渐变背景
            GradientDrawable nightBackground = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                    ContextCompat.getColor(this, R.color.nightGradientTop),
                    ContextCompat.getColor(this, R.color.nightGradientMiddleTop),
                    ContextCompat.getColor(this, R.color.nightGradientMiddle),
                    ContextCompat.getColor(this, R.color.nightGradientMiddleLower),
                    ContextCompat.getColor(this, R.color.nightGradientBottom)
                }
            );
            backgroundView.setBackground(nightBackground);
            sunView.setAlpha(0f);
            moonView.setAlpha(1f);
            blurOverlay.setAlpha(0.8f);
            loginContainer.setBackgroundResource(R.drawable.glass_background_dark);
        }
    }

    /**
     * 动画过渡日间/夜间模式
     * 
     * @param toDay 是否过渡到日间模式
     */
    private void animateDayNightTransition(boolean toDay) {
        // 如果当前已处于目标模式，或者动画正在进行中，则不重复执行
        if ((toDay && !isNightMode) || (!toDay && isNightMode) || 
            (dayNightAnimatorSet != null && dayNightAnimatorSet.isRunning())) {
            return;
        }
        
        // 取消之前的动画（如果有）
        if (dayNightAnimatorSet != null) {
            dayNightAnimatorSet.cancel();
        }
        
        isNightMode = !toDay;
        
        // 动画参数
        final int duration = 1000; // 1秒
        
        // 创建新的动画集合
        dayNightAnimatorSet = new AnimatorSet();
        List<Animator> animators = new ArrayList<>();
        
        // 背景渐变动画准备
        final int dayTopColor = ContextCompat.getColor(this, R.color.dayGradientTop);
        final int dayMiddleTop = ContextCompat.getColor(this, R.color.dayGradientMiddleTop);
        final int dayMiddleBottom = ContextCompat.getColor(this, R.color.dayGradientMiddle);
        final int dayMiddleLower = ContextCompat.getColor(this, R.color.dayGradientMiddleLower);
        final int dayBottomColor = ContextCompat.getColor(this, R.color.dayGradientBottom);
        
        final int nightTopColor = ContextCompat.getColor(this, R.color.nightGradientTop);
        final int nightMiddleTop = ContextCompat.getColor(this, R.color.nightGradientMiddleTop);
        final int nightMiddleBottom = ContextCompat.getColor(this, R.color.nightGradientMiddle);
        final int nightMiddleLower = ContextCompat.getColor(this, R.color.nightGradientMiddleLower);
        final int nightBottomColor = ContextCompat.getColor(this, R.color.nightGradientBottom);
        
        // 玻璃效果颜色
        final int dayGlassColor = ContextCompat.getColor(this, R.color.glassEffectDay);
        final int nightGlassColor = ContextCompat.getColor(this, R.color.glassEffectNight);
        
        // 创建安全的drawable引用（避免lambda中的引用问题）
        final GradientDrawable safeBackgroundDrawable;
        final GradientDrawable safeGlassDrawable;
        
        // 准备背景drawable
        GradientDrawable backgroundDrawable = null;
        if (backgroundView.getBackground() instanceof GradientDrawable) {
            try {
                backgroundDrawable = (GradientDrawable) backgroundView.getBackground().mutate();
            } catch (Exception e) {
                Log.e(TAG, "背景转换失败", e);
            }
        }
        
        if (backgroundDrawable == null) {
            // 创建新的背景
            backgroundDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                    toDay ? nightTopColor : dayTopColor,
                    toDay ? nightMiddleTop : dayMiddleTop,
                    toDay ? nightMiddleBottom : dayMiddleBottom,
                    toDay ? nightMiddleLower : dayMiddleLower, 
                    toDay ? nightBottomColor : dayBottomColor
                }
            );
            backgroundView.setBackground(backgroundDrawable);
        }
        safeBackgroundDrawable = backgroundDrawable; // 创建安全的final引用
        
        // 准备玻璃效果drawable
        GradientDrawable glassDrawable = null;
        if (loginContainer.getBackground() instanceof GradientDrawable) {
            try {
                glassDrawable = (GradientDrawable) loginContainer.getBackground().mutate();
                // 直接设置固定的玻璃效果属性，不进行动画渐变
                glassDrawable.setCornerRadius(getResources().getDisplayMetrics().density * 16);
                glassDrawable.setStroke(1, ContextCompat.getColor(this, R.color.glassEffectStroke));
                glassDrawable.setColor(toDay ? dayGlassColor : nightGlassColor);
            } catch (Exception e) {
                Log.e(TAG, "玻璃效果背景转换失败", e);
            }
        }
        
        if (glassDrawable == null) {
            // 创建新的玻璃效果背景
            glassDrawable = new GradientDrawable();
            glassDrawable.setCornerRadius(getResources().getDisplayMetrics().density * 16);
            glassDrawable.setStroke(1, ContextCompat.getColor(this, R.color.glassEffectStroke));
            glassDrawable.setColor(toDay ? dayGlassColor : nightGlassColor);
            loginContainer.setBackground(glassDrawable);
        }
        safeGlassDrawable = glassDrawable; // 创建安全的final引用
        
        // 背景颜色动画
        ValueAnimator colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(duration);
        colorAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            
            try {
                // 背景渐变色过渡
                int[] backgroundColors = new int[] {
                    blendColors(
                        toDay ? nightTopColor : dayTopColor,
                        toDay ? dayTopColor : nightTopColor,
                        value
                    ),
                    blendColors(
                        toDay ? nightMiddleTop : dayMiddleTop,
                        toDay ? dayMiddleTop : nightMiddleTop,
                        value
                    ),
                    blendColors(
                        toDay ? nightMiddleBottom : dayMiddleBottom,
                        toDay ? dayMiddleBottom : nightMiddleBottom,
                        value
                    ),
                    blendColors(
                        toDay ? nightMiddleLower : dayMiddleLower,
                        toDay ? dayMiddleLower : nightMiddleLower,
                        value
                    ),
                    blendColors(
                        toDay ? nightBottomColor : dayBottomColor,
                        toDay ? dayBottomColor : nightBottomColor,
                        value
                    )
                };
                
                // 安全地设置背景颜色
                if (safeBackgroundDrawable != null) {
                    safeBackgroundDrawable.setColors(backgroundColors);
                }
                
                // 移除玻璃效果背景色过渡动画，使用固定透明效果
                // 不再进行颜色混合和动态变化
                
            } catch (Exception e) {
                // 捕获在动画过程中可能出现的任何异常
                Log.e(TAG, "动画更新颜色时出错", e);
            }
        });
        animators.add(colorAnimator);
        
        // 太阳/月亮过渡动画
        ObjectAnimator sunAlphaAnimator = ObjectAnimator.ofFloat(sunView, "alpha", 
                toDay ? 0f : 1f, toDay ? 1f : 0f);
        sunAlphaAnimator.setDuration(duration);
        animators.add(sunAlphaAnimator);
        
        ObjectAnimator moonAlphaAnimator = ObjectAnimator.ofFloat(moonView, "alpha", 
                toDay ? 1f : 0f, toDay ? 0f : 1f);
        moonAlphaAnimator.setDuration(duration);
        animators.add(moonAlphaAnimator);
        
        // 执行动画
        dayNightAnimatorSet.playTogether(animators);
        dayNightAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        dayNightAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                // 动画结束后设置最终状态
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
     * @param ratio 混合比例 (0.0-1.0)
     * @return 混合后的颜色
     */
    private int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        final float r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio;
        final float g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio;
        final float b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio;
        return Color.rgb((int) r, (int) g, (int) b);
    }
    
    /**
     * 动画控制模糊效果
     * 
     * @param show 是否显示模糊效果
     */
    private void animateBlurEffect(boolean show) {
        blurOverlay.animate()
            .alpha(show ? 0.9f : 0.8f)
            .setDuration(300)
            .start();
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


