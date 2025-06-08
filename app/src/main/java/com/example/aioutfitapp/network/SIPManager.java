package com.example.aioutfitapp.network;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Build;
import android.util.Log;

/**
 * SIP协议管理器
 * 
 * 负责管理SIP协议的注册、会话创建、应答和终止等功能
 * 集成了Android原生SIP API
 */
public class SIPManager {
    
    private static final String TAG = "SIPManager";
    
    // SIP相关组件
    private android.net.sip.SipManager sipManager = null;
    private SipProfile sipProfile = null;
    private SipAudioCall call = null;
    private IncomingCallReceiver callReceiver;
    
    // 上下文和监听器
    private Context context;
    private SIPStateListener sipStateListener;
    
    // 单例模式
    private static SIPManager instance;
    
    // SIP账号信息
    private String username;
    private String domain;
    private String password;
    
    // SIP服务器配置
    // 本地开发环境配置
    private String sipDomain = "aioutfitapp.local";  // 本地Kamailio域名
    private String sipServerAddress = "10.0.2.2";    // 模拟器访问宿主机的IP地址
                                                    // 如果使用真机测试，请将此地址改为电脑的实际IP地址
                                                    // 注意：这应该是运行Docker的主机IP，不是Docker容器内部IP
    private int sipServerPort = 5062;               // Kamailio默认SIP端口
    
    // 默认测试用户
    private static final String DEFAULT_USERNAME = "test";
    private static final String DEFAULT_PASSWORD = "test123";
    
    /**
     * 获取单例实例
     */
    public static synchronized SIPManager getInstance(Context context) {
        if (instance == null) {
            instance = new SIPManager(context);
        }
        return instance;
    }
    
    /**
     * 私有构造函数
     */
    private SIPManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 初始化SIP管理器（使用提供的账号信息）
     */
    public boolean initialize(String username, String domain, String password, SIPStateListener listener) {
        this.username = username;
        this.domain = domain;
        this.password = password;
        this.sipStateListener = listener;
        
        return initializeSIP();
    }
    
    /**
     * 初始化SIP管理器（使用默认测试账号）
     */
    public boolean initialize(SIPStateListener listener) {
        this.username = DEFAULT_USERNAME;
        this.domain = sipDomain;
        this.password = DEFAULT_PASSWORD;
        this.sipStateListener = listener;
        
        Log.d(TAG, "使用默认测试账号: " + username + "@" + domain);
        return initializeSIP();
    }
    
    /**
     * SIP初始化通用逻辑
     */
    private boolean initializeSIP() {
        // 检查设备是否支持SIP
        if (!android.net.sip.SipManager.isVoipSupported(context)) {
            Log.e(TAG, "设备不支持SIP VOIP");
            if (sipStateListener != null) {
                sipStateListener.onError("设备不支持SIP VOIP");
            }
            return false;
        }
        
        // 检查SIP API是否可用
        if (!android.net.sip.SipManager.isApiSupported(context)) {
            Log.e(TAG, "设备不支持SIP API");
            if (sipStateListener != null) {
                sipStateListener.onError("设备不支持SIP API");
            }
            return false;
        }
        
        // 获取SipManager实例
        sipManager = android.net.sip.SipManager.newInstance(context);
        if (sipManager == null) {
            Log.e(TAG, "无法创建SIP管理器");
            if (sipStateListener != null) {
                sipStateListener.onError("无法创建SIP管理器");
            }
            return false;
        }
        
        try {
            // 注册来电接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.SipDemo.INCOMING_CALL");
            callReceiver = new IncomingCallReceiver();
            context.registerReceiver(callReceiver, filter);
            
            // 创建SIP配置文件
            createSipProfile();
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "SIP初始化错误: " + e.getMessage());
            if (sipStateListener != null) {
                sipStateListener.onError("SIP初始化错误: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 创建SIP配置文件
     */
    private void createSipProfile() throws SipException {
        try {
            if (sipManager == null) {
                Log.e(TAG, "SIP管理器未初始化");
                return;
            }
            
            Log.d(TAG, "创建SIP配置 - 用户名: " + username + ", 域名: " + domain + 
                   ", 服务器: " + sipServerAddress + ":" + sipServerPort);
            
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            builder.setAutoRegistration(true);
            
            // 设置SIP服务器地址和端口
            builder.setOutboundProxy(sipServerAddress + ":" + sipServerPort);
            builder.setSendKeepAlive(true);
            builder.setAutoRegistration(true);
            
            sipProfile = builder.build();
            
            // 注册SIP账号
            Intent intent = new Intent();
            intent.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pendingIntent;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            
            sipManager.open(sipProfile, pendingIntent, null);
            
            // 添加注册状态监听器
            sipManager.setRegistrationListener(sipProfile.getUriString(), new SipRegistrationListener() {
                @Override
                public void onRegistering(String localProfileUri) {
                    Log.d(TAG, "正在注册: " + localProfileUri);
                    if (sipStateListener != null) {
                        sipStateListener.onRegistering();
                    }
                }
                
                @Override
                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    Log.d(TAG, "注册成功: " + localProfileUri + ", 过期时间: " + expiryTime);
                    if (sipStateListener != null) {
                        sipStateListener.onRegistered();
                    }
                }
                
                @Override
                public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
                    Log.e(TAG, "注册失败: " + localProfileUri + ", 错误码: " + errorCode + ", 错误信息: " + errorMessage);
                    if (sipStateListener != null) {
                        sipStateListener.onError("注册失败: " + errorMessage);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "创建SIP配置文件失败: " + e.getMessage());
            if (sipStateListener != null) {
                sipStateListener.onError("创建SIP配置文件失败: " + e.getMessage());
            }
            throw new SipException("创建SIP配置文件失败", e);
        }
    }
    
    /**
     * 发起通话（指定完整SIP地址）
     */
    public void makeCall(String sipAddress, SipAudioCall.Listener listener) {
        if (sipManager == null || sipProfile == null) {
            Log.e(TAG, "SIP未初始化，无法发起通话");
            if (sipStateListener != null) {
                sipStateListener.onError("SIP未初始化，无法发起通话");
            }
            return;
        }
        
        try {
            SipAudioCall.Listener callListener = createCallListener(listener);
            
            Log.d(TAG, "发起通话到: " + sipAddress);
            call = sipManager.makeAudioCall(sipProfile.getUriString(), sipAddress, callListener, 30);
            
        } catch (Exception e) {
            Log.e(TAG, "发起通话失败: " + e.getMessage());
            if (sipStateListener != null) {
                sipStateListener.onError("发起通话失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 发起通话到测试用户（使用用户名）
     * 
     * 例如: makeCallToTestUser("alice") 将拨打 sip:alice@aioutfitapp.local
     */
    public void makeCallToTestUser(String username, SipAudioCall.Listener listener) {
        String sipUri = "sip:" + username + "@" + sipDomain;
        makeCall(sipUri, listener);
    }
    
    /**
     * 创建通话监听器
     */
    private SipAudioCall.Listener createCallListener(SipAudioCall.Listener listener) {
        return new SipAudioCall.Listener() {
            @Override
            public void onCallEstablished(SipAudioCall call) {
                // 通话建立后启用扬声器
                call.setSpeakerMode(true);
                call.startAudio();
                
                if (listener != null) {
                    listener.onCallEstablished(call);
                }
                
                if (sipStateListener != null) {
                    sipStateListener.onCallEstablished();
                }
                
                Log.d(TAG, "通话已建立");
            }
            
            @Override
            public void onCallEnded(SipAudioCall call) {
                if (listener != null) {
                    listener.onCallEnded(call);
                }
                
                if (sipStateListener != null) {
                    sipStateListener.onCallEnded();
                }
                
                Log.d(TAG, "通话已结束");
            }
            
            @Override
            public void onError(SipAudioCall call, int errorCode, String errorMessage) {
                if (listener != null) {
                    listener.onError(call, errorCode, errorMessage);
                }
                
                if (sipStateListener != null) {
                    sipStateListener.onError("通话错误: " + errorMessage);
                }
                
                Log.e(TAG, "通话错误: " + errorMessage);
            }
        };
    }
    
    /**
     * 接听来电
     */
    public void answerCall(SipAudioCall incomingCall, SipAudioCall.Listener listener) {
        try {
            call = incomingCall;
            call.answerCall(30);
            call.startAudio();
            call.setSpeakerMode(true);
            
            if (sipStateListener != null) {
                sipStateListener.onCallEstablished();
            }
            
            if (listener != null) {
                call.setListener(listener);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "接听通话失败: " + e.getMessage());
            if (sipStateListener != null) {
                sipStateListener.onError("接听通话失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 结束通话
     */
    public void endCall() {
        if (call != null) {
            try {
                call.endCall();
            } catch (SipException e) {
                Log.e(TAG, "结束通话失败: " + e.getMessage());
            }
            call.close();
            call = null;
        }
    }
    
    /**
     * 关闭SIP
     */
    public void close() {
        if (sipManager == null) {
            return;
        }
        
        // 结束当前通话
        if (call != null) {
            try {
                call.endCall();
            } catch (SipException e) {
                Log.e(TAG, "结束通话失败: " + e.getMessage());
            }
            call.close();
            call = null;
        }
        
        // 关闭SIP配置文件
        if (sipProfile != null) {
            try {
                sipManager.close(sipProfile.getUriString());
            } catch (Exception e) {
                Log.e(TAG, "关闭SIP配置文件失败: " + e.getMessage());
            }
        }
        
        // 注销来电接收器
        try {
            if (callReceiver != null) {
                context.unregisterReceiver(callReceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, "注销来电接收器失败: " + e.getMessage());
        }
    }
    
    /**
     * SIP状态监听器接口
     */
    public interface SIPStateListener {
        void onRegistering();
        void onRegistered();
        void onCallEstablished();
        void onCallEnded();
        void onIncomingCall(SipAudioCall call);
        void onError(String errorMessage);
    }
    
    /**
     * 来电接收器
     */
    private class IncomingCallReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                SipAudioCall incomingCall = sipManager.takeAudioCall(intent, null);
                
                SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                    @Override
                    public void onRinging(SipAudioCall call, SipProfile caller) {
                        try {
                            if (sipStateListener != null) {
                                sipStateListener.onIncomingCall(call);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "来电处理错误: " + e.getMessage());
                        }
                    }
                };
                
                incomingCall.setListener(listener);
                
            } catch (Exception e) {
                Log.e(TAG, "接收来电错误: " + e.getMessage());
                if (sipStateListener != null) {
                    sipStateListener.onError("接收来电错误: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 设置SIP服务器配置
     * 在生产环境中使用实际服务器地址和配置
     */
    public void setSipServerConfig(String domain, String serverAddress, int port) {
        this.sipDomain = domain;
        this.sipServerAddress = serverAddress;
        this.sipServerPort = port;
        
        Log.d(TAG, "设置SIP服务器配置 - 域名: " + domain + ", 服务器: " + serverAddress + ":" + port);
    }
    
    /**
     * 获取当前使用的SIP域名
     */
    public String getSipDomain() {
        return sipDomain;
    }
    
    /**
     * 获取SIP服务器地址
     */
    public String getSipServerAddress() {
        return sipServerAddress;
    }
}