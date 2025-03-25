package com.example.aioutfitapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * 自定义视差滚动视图
 * 用于实现背景元素的视差滚动效果
 */
public class ParallaxScrollView extends ScrollView {

    private View backgroundView;
    private View nearCloudsView;
    private View farCloudsView;
    private float parallaxFactor = 0.5f;

    public ParallaxScrollView(Context context) {
        super(context);
    }

    public ParallaxScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ParallaxScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置视差效果的视图
     */
    public void setParallaxViews(View background, View nearClouds, View farClouds) {
        this.backgroundView = background;
        this.nearCloudsView = nearClouds;
        this.farCloudsView = farClouds;
    }

    /**
     * 设置视差因子
     * 值越大，视差效果越明显
     */
    public void setParallaxFactor(float factor) {
        this.parallaxFactor = factor;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        
        // 计算滚动偏移量
        float scrollOffset = t * parallaxFactor;
        
        // 应用视差效果
        if (backgroundView != null) {
            backgroundView.setTranslationY(scrollOffset * 0.2f);
        }
        
        if (nearCloudsView != null) {
            nearCloudsView.setTranslationY(scrollOffset * 0.5f);
        }
        
        if (farCloudsView != null) {
            farCloudsView.setTranslationY(scrollOffset * 0.8f);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 处理触摸事件，实现平滑的视差效果
        return super.onTouchEvent(ev);
    }
}

