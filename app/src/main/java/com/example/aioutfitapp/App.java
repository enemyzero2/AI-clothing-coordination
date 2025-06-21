package com.example.aioutfitapp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.aioutfitapp.network.SipService;

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
        
        // 启动SIP服务，而不是直接初始化Linphone
        startSipService();
    }
    
    /**
     * 启动SIP服务
     */
    private void startSipService() {
        try {
            Log.d(TAG, "启动SIP服务...");
            Intent intent = new Intent(this, SipService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Log.d(TAG, "SIP服务启动请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "启动SIP服务失败: " + e.getMessage(), e);
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