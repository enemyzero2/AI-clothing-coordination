package com.example.aioutfitapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.aioutfitapp.network.LinphoneManager;

/**
 * 应用类
 * 
 * 用于初始化应用级别的组件和设置
 */
public class App extends Application {
    
    private static final String TAG = "App";
    
    private static App instance;
    
    // SharedPreferences名称
    public static final String PREF_NAME = "aioutfit_prefs";
    
    // 配置常量
    public static final String PREF_VOIP_USER = "pref_voip_username";
    public static final String PREF_VOIP_PWD = "pref_voip_password";
    public static final String PREF_VOIP_IP = "pref_voip_server_ip";
    public static final String PREF_VOIP_PORT = "pref_voip_server_port";
    public static final String PREF_VOIP_TRANSPORT = "pref_voip_transport_protocol";
    public static final String DEF_SIP_PORT = "5060"; // FreeSwitch默认端口
    public static final String DEF_SIP_TRANSPORT = "udp"; // 默认传输协议，小写
    
    // SIP账户保存键
    public static final String PREF_SIP_USERNAME = "pref_sip_username";
    public static final String PREF_SIP_PASSWORD = "pref_sip_password";
    public static final String PREF_SIP_DOMAIN = "pref_sip_domain";
    public static final String PREF_SIP_SERVER_ADDRESS = "pref_sip_server_address";
    public static final String PREF_SIP_SERVER_PORT = "pref_sip_server_port";
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        Log.i(TAG, "应用启动初始化");
        
        // 初始化Linphone管理器
        initLinphone();
    }
    
    /**
     * 初始化Linphone组件
     */
    private void initLinphone() {
        try {
            Log.d(TAG, "开始初始化Linphone");
            LinphoneManager.getInstance().init(this);
            LinphoneManager.getInstance().start();
            Log.d(TAG, "Linphone初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "Linphone初始化失败: " + e.getMessage(), e);
        }
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