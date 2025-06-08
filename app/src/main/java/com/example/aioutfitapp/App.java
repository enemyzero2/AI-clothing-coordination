package com.example.aioutfitapp;

import android.app.Application;
import android.content.Context;

import com.example.aioutfitapp.network.LinphoneManager;

import org.linphone.core.Factory;

/**
 * 应用类
 * 
 * 用于初始化应用级别的组件和设置
 */
public class App extends Application {
    
    private static App instance;
    
    // 配置常量
    public static final String PREF_VOIP_USER = "pref_voip_username";
    public static final String PREF_VOIP_PWD = "pref_voip_password";
    public static final String PREF_VOIP_IP = "pref_voip_server_ip";
    public static final String PREF_VOIP_PORT = "pref_voip_server_port";
    public static final String DEF_SIP_PORT = "5062";
    
    // Linphone配置文件
    public static final String LINPHONE_CONFIG_DEF = ".linphonerc";
    public static final String LINPHONE_CONFIG_FAC = "linphonerc_factory";
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 初始化Linphone工厂
        Factory.instance();
    }
    
    /**
     * 获取应用实例
     */
    public static App getInstance() {
        return instance;
    }
    
    /**
     * 获取应用上下文
     */
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
} 