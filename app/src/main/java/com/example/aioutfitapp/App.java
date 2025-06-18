package com.example.aioutfitapp;

import android.app.Application;
import android.content.Context;

import com.example.aioutfitapp.network.LinphoneManager;

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
    public static final String PREF_VOIP_TRANSPORT = "pref_voip_transport_protocol";
    public static final String DEF_SIP_PORT = "5060"; // FreeSwitch默认端口
    public static final String DEF_SIP_TRANSPORT = "UDP"; // 默认传输协议
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 初始化Linphone管理器
        LinphoneManager.getInstance().init(this);
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