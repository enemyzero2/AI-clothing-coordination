package com.example.aioutfitapp.network;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.linphone.core.Account;
import org.linphone.core.AccountParams;
import org.linphone.core.Address;
import org.linphone.core.AudioDevice;
import org.linphone.core.AuthInfo;
import org.linphone.core.Call;
import org.linphone.core.CallParams;
import org.linphone.core.Config;
import org.linphone.core.Core;
import org.linphone.core.CoreListener;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Factory;
import org.linphone.core.LogCollectionState;
import org.linphone.core.MediaEncryption;
import org.linphone.core.PayloadType;
import org.linphone.core.ProxyConfig;
import org.linphone.core.RegistrationState;
import org.linphone.core.TransportType;
import org.linphone.core.VideoDefinition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Linphone管理类
 * 
 * 负责Linphone SDK的初始化、注册和通话管理
 */
public class LinphoneManager {
    
    private static final String TAG = "LinphoneManager";
    
    // 通话类型
    public static final int CALL_TYPE_AUDIO = 0;
    public static final int CALL_TYPE_VIDEO = 1;
    
    // 通话状态
    public static final int CALL_STATE_IDLE = 0;
    public static final int CALL_STATE_CONNECTING = 1;
    public static final int CALL_STATE_RINGING = 2;
    public static final int CALL_STATE_CONNECTED = 3;
    public static final int CALL_STATE_ENDED = 4;
    
    // Linphone核心组件
    private static LinphoneManager instance;
    private Core core;
    private CoreListener coreListener;
    private Context context;
    private LinphoneManagerListener listener;
    private Call currentCall;
    private boolean isVideoEnabled = false;
    private int callState = CALL_STATE_IDLE;
    
    private static final int MAX_RETRY_COUNT = 3;
    private int retryCount = 0;
    private android.os.Handler retryHandler;
    private static final long RETRY_DELAY_MS = 5000; // 5秒
    
    // SIP账号信息
    private String username;
    private String password;
    private String domain;
    private String port;
    private String transport;
    private SipCallback sipCallback;
    
    /**
     * 获取单例实例
     */
    public static synchronized LinphoneManager getInstance() {
        if (instance == null) {
            instance = new LinphoneManager();
        }
        return instance;
    }
    
    /**
     * 初始化Linphone
     * 
     * @param context 应用上下文
     * @return LinphoneManager实例
     */
    public LinphoneManager init(Context context) {
        this.context = context.getApplicationContext();
        
        // 开启日志收集
        Factory.instance().enableLogCollection(LogCollectionState.Enabled);
        Factory.instance().enableLogcatLogs(true);
        
        // 创建配置文件目录
        File configDir = context.getFilesDir();
        if (!configDir.exists()) {
            if (!configDir.mkdir()) {
                Log.e(TAG, "无法创建配置目录");
            }
        }
        
        // 复制默认配置文件
        copyAssetsFromPackage();
        
        try {
            // 创建Core实例
            core = Factory.instance().createCore(null, null, context);
            
            // 初始化核心监听器
            initCoreListener();
            
            // 配置通话参数
            configureCore();
            
            Log.i(TAG, "Linphone初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "Linphone初始化失败: " + e.getMessage(), e);
        }
        
        return this;
    }
    
    /**
     * 配置核心参数
     */
    private void configureCore() {
        if (core == null) return;
        
        // 启用来电铃声和振动
        core.setNativeRingingEnabled(true);
        core.setVibrationOnIncomingCallEnabled(true);
        
        // 启用音频优化
        core.setEchoCancellationEnabled(true);
        core.setAdaptiveRateControlEnabled(true);
        
        // 配置视频
        core.setVideoDisplayEnabled(true);
        core.setVideoCaptureEnabled(true);
        core.getVideoActivationPolicy().setAutomaticallyAccept(true);
        core.getVideoActivationPolicy().setAutomaticallyInitiate(true);
        
        // 配置编解码器偏好
        configureCodecs();
    }
    
    /**
     * 配置音视频编解码器
     */
    private void configureCodecs() {
        PayloadType[] audioPayloads = core.getAudioPayloadTypes();
        for (PayloadType pt : audioPayloads) {
            // 启用所有音频编解码器
            pt.enable(true);
        }
        
        PayloadType[] videoPayloads = core.getVideoPayloadTypes();
        for (PayloadType pt : videoPayloads) {
            // 优先使用H264编解码器
            boolean isH264 = pt.getMimeType().equals("H264");
            pt.enable(isH264);
            
            // 如果是H264，设置较高的比特率
            if (isH264) {
                pt.setNormalBitrate(1024);
            }
        }
    }
    
    /**
     * 初始化核心监听器
     */
    private void initCoreListener() {
        coreListener = new CoreListenerStub() {
            @Override
            public void onCallStateChanged(Core core, Call call, Call.State state, String message) {
                Log.i(TAG, "通话状态变化: " + state + ", 信息: " + message);
                currentCall = call;
                
                switch (state) {
                    case OutgoingInit:
                        Log.d(TAG, "通话初始化中");
                        Toast.makeText(context, "通话初始化中", Toast.LENGTH_SHORT).show();
                        updateCallState(CALL_STATE_CONNECTING);
                        break;
                    case OutgoingProgress:
                        Log.d(TAG, "正在接通通话...");
                        Toast.makeText(context, "正在接通通话...", Toast.LENGTH_SHORT).show();
                        updateCallState(CALL_STATE_CONNECTING);
                        break;
                    case OutgoingRinging:
                        Log.d(TAG, "对方振铃中...");
                        Toast.makeText(context, "对方振铃中...", Toast.LENGTH_SHORT).show();
                        updateCallState(CALL_STATE_RINGING);
                        break;
                    case IncomingReceived:
                        updateCallState(CALL_STATE_RINGING);
                        // 获取来电者信息
                        Address address = call.getRemoteAddress();
                        String displayName = address.getDisplayName();
                        String username = address.getUsername();
                        final String callerId = (displayName != null && !displayName.isEmpty()) 
                                ? displayName : username;
                        
                        Log.d(TAG, "收到来电: " + callerId);
                        Toast.makeText(context, "收到来电: " + callerId, Toast.LENGTH_SHORT).show();
                        
                        // 确定通话类型
                        final int callType = call.getCurrentParams().isVideoEnabled() 
                                ? CALL_TYPE_VIDEO : CALL_TYPE_AUDIO;
                        
                        // 通知UI
                        if (listener != null) {
                            listener.onIncomingCall(callerId, callType);
                        }
                        break;
                    case Connected:
                        Log.d(TAG, "通话已连接");
                        Toast.makeText(context, "通话已连接", Toast.LENGTH_SHORT).show();
                        updateCallState(CALL_STATE_CONNECTED);
                        break;
                    case StreamsRunning:
                        Log.d(TAG, "媒体流已开始");
                        updateCallState(CALL_STATE_CONNECTED);
                        isVideoEnabled = call.getCurrentParams().isVideoEnabled();
                        Log.d(TAG, "视频状态: " + (isVideoEnabled ? "已启用" : "未启用"));
                        break;
                    case End:
                        Log.d(TAG, "通话已结束");
                        Toast.makeText(context, "通话已结束", Toast.LENGTH_SHORT).show();
                        updateCallState(CALL_STATE_ENDED);
                        currentCall = null;
                        break;
                    case Released:
                        Log.d(TAG, "通话资源已释放");
                        updateCallState(CALL_STATE_ENDED);
                        currentCall = null;
                        break;
                    case Error:
                        Log.e(TAG, "通话错误: " + message);
                        Toast.makeText(context, "通话错误: " + message, Toast.LENGTH_LONG).show();
                        updateCallState(CALL_STATE_ENDED);
                        if (listener != null) {
                            listener.onError("通话错误: " + message);
                        }
                        currentCall = null;
                        break;
                }
            }
            
            @Override
            public void onRegistrationStateChanged(Core core, ProxyConfig config, RegistrationState state, String message) {
                Log.i(TAG, "注册状态变化: " + state + ", 信息: " + message);
                
                switch (state) {
                    case Ok:
                        Log.d(TAG, "SIP注册成功");
                        Toast.makeText(context, "SIP注册成功", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onRegistered();
                        }
                        if (sipCallback != null) {
                            sipCallback.onSuccess();
                        }
                        break;
                    case Failed:
                        String errorReason = "未知错误";
                        if (message != null && !message.isEmpty()) {
                            errorReason = message;
                        } else {
                            // 从proxy配置获取错误信息
                            org.linphone.core.Reason reason = config.getError();
                            if (reason != org.linphone.core.Reason.None) {
                                errorReason = "原因: " + reason.toString();
                            }
                        }
                        
                        Log.e(TAG, "SIP注册失败: " + errorReason);
                        
                        // 检查常见的错误类型并提供更友好的消息
                        String friendlyErrorMsg = getFriendlyErrorMessage(errorReason);
                        
                        if (listener != null) {
                            listener.onError(friendlyErrorMsg);
                        }
                        if (sipCallback != null) {
                            sipCallback.onError(friendlyErrorMsg);
                        }
                        break;
                    case Progress:
                        Log.d(TAG, "SIP注册进行中...");
                        break;
                    case Cleared:
                        Log.d(TAG, "SIP注册已清除");
                        break;
                }
            }
            
            @Override
            public void onNetworkReachable(Core core, boolean reachable) {
                Log.i(TAG, "网络状态变化: " + (reachable ? "可达" : "不可达"));
                if (!reachable) {
                    Toast.makeText(context, "网络不可达，请检查连接", Toast.LENGTH_LONG).show();
                }
            }
        };
        
        core.addListener(coreListener);
    }
    
    /**
     * 复制配置文件
     */
    private void copyAssetsFromPackage() {
        // 配置目录
        String configPath = context.getFilesDir().getAbsolutePath();
        
        try {
            // 创建用户证书目录
            String userCertsPath = configPath + "/user-certs";
            File userCertsDir = new File(userCertsPath);
            if (!userCertsDir.exists() && !userCertsDir.mkdir()) {
                Log.e(TAG, "无法创建用户证书目录");
            }
            
            // 准备默认配置文件
            copyFromPackage("linphonerc_default", configPath + "/.linphonerc");
            
            Log.i(TAG, "配置文件已复制到: " + configPath);
        } catch (IOException e) {
            Log.e(TAG, "复制配置文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从assets目录复制文件到指定位置
     */
    private void copyFromPackage(String assetFilename, String destination) throws IOException {
        InputStream inStream = null;
        FileOutputStream outStream = null;
        
        try {
            inStream = context.getAssets().open(assetFilename);
            if (inStream == null) {
                Log.w(TAG, "无法从assets读取: " + assetFilename);
                return;
            }
            
            outStream = new FileOutputStream(destination);
            
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, read);
            }
            
            Log.i(TAG, "文件已复制: " + assetFilename + " -> " + destination);
        } finally {
            if (inStream != null) inStream.close();
            if (outStream != null) outStream.close();
        }
    }
    
    /**
     * 设置监听器
     */
    public void setLinphoneManagerListener(LinphoneManagerListener listener) {
        this.listener = listener;
    }
    
    /**
     * 设置监听器
     * @deprecated 使用 setLinphoneManagerListener 代替
     */
    @Deprecated
    public void setListener(LinphoneManagerListener listener) {
        this.listener = listener;
    }
    
    /**
     * 启动Linphone核心
     */
    public void start() {
        if (core != null) {
            try {
                core.start();
                Log.i(TAG, "Linphone核心已启动");
            } catch (Exception e) {
                Log.e(TAG, "启动Linphone核心失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 登录SIP服务器
     */
    public void login(String username, String password, String domain, String port, 
                      String transport, final SipCallback callback) {
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.port = port;
        this.transport = transport;
        this.sipCallback = callback;
        this.retryCount = 0;
        
        // 初始化重试处理器
        if (retryHandler == null) {
            retryHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        
        try {
            // 显示连接中状态
            Toast.makeText(context, "正在连接SIP服务器", Toast.LENGTH_SHORT).show();
            if (sipCallback != null) {
                sipCallback.onLoginStarted();
            }
            
            // 记录所有参数
            Log.d(TAG, "SIP登录参数: 用户名=" + username + ", 域名=" + domain + ", 端口=" + port);
            
            if (core == null) {
                Log.e(TAG, "Linphone核心未初始化");
                Toast.makeText(context, "错误: Linphone核心未初始化", Toast.LENGTH_LONG).show();
                return;
            }
            
            // 清除现有账号
            try {
                core.clearProxyConfig();
                Log.d(TAG, "清除现有代理配置成功");
            } catch (Exception e) {
                Log.e(TAG, "清除代理配置失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
            }
            
            // 添加诊断信息
            boolean addressResolved = false;
            try {
                java.net.InetAddress address = java.net.InetAddress.getByName(domain);
                addressResolved = true;
                Log.d(TAG, "域名解析结果: " + domain + " -> " + address.getHostAddress());
            } catch (java.net.UnknownHostException e) {
                Log.e(TAG, "域名解析失败: " + domain + " - " + e.getMessage(), e);
                Toast.makeText(context, "域名解析失败: " + domain, Toast.LENGTH_LONG).show();
                // 不要立即返回，继续尝试连接
            }
            
            String fullDomain = domain;
            if (!fullDomain.contains(":")) {
                fullDomain += ":" + port;
            }
            
            Log.d(TAG, "尝试登录SIP服务器: " + fullDomain + ", 用户名: " + username);
            Toast.makeText(context, "正在连接SIP服务器: " + fullDomain, Toast.LENGTH_SHORT).show();
            
            // 主要登录逻辑
            AuthInfo authInfo = null;
            AccountParams accountParams = null;
            Address identity = null;
            Address serverAddress = null;
            Account account = null;
            
            try {
                // 第1步：创建认证信息
                try {
                    Log.d(TAG, "步骤1: 创建认证信息...");
                    authInfo = Factory.instance().createAuthInfo(
                            username, null, password, null, null, fullDomain, null);
                    if (authInfo != null) {
                        Log.d(TAG, "认证信息创建成功: " + authInfo);
                    } else {
                        Log.e(TAG, "认证信息创建返回null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "创建认证信息失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    throw e; // 重新抛出以便被外层catch捕获
                }
                
                // 第2步：创建账号参数
                try {
                    Log.d(TAG, "步骤2: 创建账号参数...");
                    accountParams = core.createAccountParams();
                    if (accountParams != null) {
                        Log.d(TAG, "账号参数创建成功: " + accountParams);
                    } else {
                        Log.e(TAG, "账号参数创建返回null");
                        throw new RuntimeException("账号参数创建失败，返回null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "创建账号参数失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    throw e;
                }
                
                // 第3步：设置身份地址
                try {
                    Log.d(TAG, "步骤3: 设置身份地址...");
                    String sipAddress = "sip:" + username + "@" + fullDomain;
                    Log.d(TAG, "尝试创建地址: " + sipAddress);
                    
                    identity = Factory.instance().createAddress(sipAddress);
                    if (identity != null) {
                        accountParams.setIdentityAddress(identity);
                        Log.d(TAG, "已设置身份地址: " + sipAddress);
                    } else {
                        Log.e(TAG, "创建身份地址失败: 返回null");
                        throw new RuntimeException("创建身份地址失败，返回null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "设置身份地址失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    throw e;
                }
                
                // 第4步：设置服务器地址
                try {
                    Log.d(TAG, "步骤4: 设置服务器地址...");
                    String serverSipAddress = "sip:" + fullDomain;
                    Log.d(TAG, "尝试创建服务器地址: " + serverSipAddress);
                    
                    serverAddress = Factory.instance().createAddress(serverSipAddress);
                    if (serverAddress != null) {
                        // 尝试使用直接IP连接
                        if (!addressResolved && !isIPAddress(domain)) {
                            Log.w(TAG, "域名解析失败，尝试使用本机IP建立连接...");
                            try {
                                // 尝试使用本机IP重建服务器地址
                                String localIp = getLocalIpAddress();
                                String alternativeServer = "sip:" + localIp + ":" + port;
                                Log.d(TAG, "尝试替代服务器地址: " + alternativeServer);
                                Address alternativeAddress = Factory.instance().createAddress(alternativeServer);
                                if (alternativeAddress != null) {
                                    serverAddress = alternativeAddress;
                                    Log.d(TAG, "已替换为本地IP地址: " + alternativeServer);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "替代连接失败: " + e.getMessage());
                            }
                        }
                        
                        // 设置传输协议
                        try {
                            // 根据传入的transport参数设置传输协议
                            TransportType transportType = TransportType.Udp; // 默认UDP
                            
                            if (transport != null && !transport.isEmpty()) {
                                if (transport.equalsIgnoreCase("tcp")) {
                                    transportType = TransportType.Tcp;
                                    Log.d(TAG, "使用TCP传输协议");
                                } else if (transport.equalsIgnoreCase("tls")) {
                                    transportType = TransportType.Tls;
                                    Log.d(TAG, "使用TLS传输协议");
                                } else if (transport.equalsIgnoreCase("udp")) {
                                    transportType = TransportType.Udp;
                                    Log.d(TAG, "使用UDP传输协议");
                                } else {
                                    Log.w(TAG, "未知传输协议: " + transport + "，使用默认UDP");
                                }
                            } else {
                                Log.d(TAG, "未指定传输协议，使用默认UDP");
                            }
                            
                            serverAddress.setTransport(transportType);
                            Log.d(TAG, "已设置传输协议: " + transportType);
                            
                            // FreeSwitch特定设置
                            accountParams.setPublishEnabled(false); // FreeSwitch通常不需要PUBLISH
                            // 注意：AVPF设置在当前SDK版本中可能有不同的API，暂时移除
                            // 如果需要设置AVPF，请查阅当前版本Linphone SDK的文档
                        } catch (Exception e) {
                            Log.e(TAG, "设置传输协议失败: " + e.getMessage());
                        }
                        
                        // 设置服务器地址
                        try {
                            accountParams.setServerAddress(serverAddress);
                            Log.d(TAG, "已设置服务器地址: " + serverAddress.asString());
                        } catch (Exception e) {
                            Log.e(TAG, "应用服务器地址失败: " + e.getMessage());
                            throw e;
                        }
                    } else {
                        Log.e(TAG, "创建服务器地址失败: 返回null");
                        throw new RuntimeException("创建服务器地址失败，返回null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "设置服务器地址失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    throw e;
                }
                
                // 第5步：启用注册和设置其他参数
                try {
                    Log.d(TAG, "步骤5: 设置注册参数...");
                    
                    accountParams.setRegisterEnabled(true);
                    Log.d(TAG, "注册功能已启用");
                    
                    try {
                        // FreeSwitch配置
                        // 设置较短的注册超时，FreeSwitch默认注册过期为3600秒
                        accountParams.setExpires(3600);
                        Log.d(TAG, "已设置注册超时: 3600秒");
                        
                        // 注意：在当前SDK版本中没有直接设置注册刷新间隔的方法
                        // 系统会自动处理注册刷新
                        
                        // 设置其他FreeSwitch友好参数
                        accountParams.setInternationalPrefix("");
                        accountParams.setUseInternationalPrefixForCallsAndChats(false);
                    } catch (Exception e) {
                        Log.e(TAG, "设置FreeSwitch参数失败: " + e.getMessage());
                        // 不中断流程，继续
                    }
                    
                    // 可以根据需要设置其他参数，如代理
                    // accountParams.setRouteAddress(...)
                } catch (Exception e) {
                    Log.e(TAG, "设置注册参数失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    throw e;
                }
                
                // 第6步：创建账号
                try {
                    Log.d(TAG, "步骤6: 创建SIP账号...");
                    account = core.createAccount(accountParams);
                    if (account != null) {
                        Log.d(TAG, "SIP账号创建成功: " + account.getState());
                    } else {
                        Log.e(TAG, "创建SIP账号失败: 返回null");
                        throw new RuntimeException("创建SIP账号失败，返回null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "创建SIP账号失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    throw e;
                }
                
                // 第7步：添加认证和账号信息
                try {
                    Log.d(TAG, "步骤7: 添加认证信息和账号到Core...");
                    core.addAuthInfo(authInfo);
                    Log.d(TAG, "认证信息已添加");
                    
                    core.addAccount(account);
                    Log.d(TAG, "账号已添加");
                    
                    core.setDefaultAccount(account);
                    Log.d(TAG, "默认账号已设置");
                } catch (Exception e) {
                    Log.e(TAG, "添加账号信息失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    throw e;
                }
                
                // 第8步：设置用户代理
                try {
                    Log.d(TAG, "步骤8: 设置用户代理...");
                    core.setUserAgent("AI衣搭SIP客户端", null);
                    Log.d(TAG, "用户代理已设置");
                } catch (Exception e) {
                    Log.e(TAG, "设置用户代理失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    // 不中断流程
                }
                
                // 第9步：启动Core
                try {
                    Log.d(TAG, "步骤9: 启动Linphone核心...");
                    core.start();
                    Log.d(TAG, "Linphone核心已启动");
                } catch (Exception e) {
                    Log.e(TAG, "启动Linphone核心失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    throw e;
                }
                
                Log.i(TAG, "SIP账号注册中: " + (identity != null ? identity.asString() : "未知地址"));
                Toast.makeText(context, "SIP账号注册中...", Toast.LENGTH_SHORT).show();
                
                // 测试模拟回调，因为有可能回调没有正确触发
                checkRegistrationStatus(account);
                
            } catch (Exception e) {
                // 获取更多异常信息
                String errorMsg;
                Throwable rootCause = e;
                
                // 获取最根本的异常原因
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }
                
                if (rootCause.getMessage() != null) {
                    errorMsg = "SIP注册失败: " + rootCause.getMessage();
                } else {
                    // 提供详细的异常类型信息
                    errorMsg = "SIP注册失败: 异常类型 - " + rootCause.getClass().getName();
                    
                    // 记录完整堆栈跟踪
                    Log.e(TAG, "SIP注册失败，详细堆栈：", rootCause);
                    
                    // 添加核心状态诊断信息
                    errorMsg += "\n连接诊断:\n";
                    
                    // 添加网络诊断信息
                    try {
                        boolean isConnected = isNetworkConnected();
                        errorMsg += "网络连接: " + (isConnected ? "正常" : "异常") + "\n";
                        
                        if (isConnected) {
                            String localIp = getLocalIpAddress();
                            errorMsg += "本机IP: " + localIp + "\n";
                            
                            if (domain != null) {
                                try {
                                    java.net.InetAddress address = java.net.InetAddress.getByName(domain);
                                    errorMsg += "服务器IP: " + address.getHostAddress() + "\n";
                                } catch (Exception ex) {
                                    errorMsg += "服务器IP解析失败: " + domain + "\n";
                                }
                                
                                try {
                                    boolean isPortOpen = isPortOpen(domain, Integer.parseInt(port));
                                    errorMsg += "端口 " + port + " 状态: " + (isPortOpen ? "开放" : "关闭") + "\n";
                                } catch (Exception ex) {
                                    errorMsg += "端口检查失败: " + port + "\n";
                                }
                            } else {
                                errorMsg += "服务器域名为空\n";
                            }
                        }
                        
                        // 添加Linphone状态信息
                        if (core != null) {
                            errorMsg += "Linphone核心: 已初始化\n";
                            errorMsg += "网络可达状态: " + core.isNetworkReachable() + "\n";
                            errorMsg += "认证信息: " + (authInfo != null ? "已创建" : "未创建") + "\n";
                            errorMsg += "账号参数: " + (accountParams != null ? "已创建" : "未创建") + "\n";
                            errorMsg += "身份地址: " + (identity != null ? identity.asString() : "未创建") + "\n";
                            errorMsg += "服务器地址: " + (serverAddress != null ? serverAddress.asString() : "未创建") + "\n";
                        } else {
                            errorMsg += "Linphone核心: 未初始化\n";
                        }
                    } catch (Exception diagEx) {
                        Log.e(TAG, "生成诊断信息时出错", diagEx);
                        errorMsg += "诊断信息生成失败: " + diagEx.getMessage() + "\n";
                    }
                }
                
                Log.e(TAG, errorMsg);
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
                if (listener != null) {
                    listener.onError(errorMsg);
                }
                
                // 添加调试信息
                try {
                    debugSipConnection(domain, port);
                    
                    // 尝试提供一个直接的解决方案
                    if (!isIPAddress(domain)) {
                        Log.d(TAG, "推荐: 由于域名解析问题，尝试直接使用主机IP地址替代域名");
                        String localIp = getLocalIpAddress();
                        if (!localIp.equals("127.0.0.1")) {
                            String message = "建议：尝试使用IP地址 " + localIp + " 替代域名 " + domain;
                            Log.d(TAG, message);
                            showDetailedErrorDialog(context, errorMsg);
                        }
                    } else {
                        showDetailedErrorDialog(context, errorMsg);
                    }
                } catch (Exception debugEx) {
                    Log.e(TAG, "调试信息收集失败: " + debugEx.getMessage());
                    showDetailedErrorDialog(context, "SIP连接失败\n\n详细错误: " + errorMsg);
                }
            }
            
            // 设置连接超时处理
            setupConnectionTimeout();

            return;
        } catch (Exception e) {
            // ... 现有错误处理代码 ...
            
            // 如果是网络原因导致的错误，尝试自动重试
            if (shouldRetryConnection(e)) {
                scheduleRetry();
            }
        }
    }
    
    /**
     * 定期检查注册状态
     */
    private void checkRegistrationStatus(final Account account) {
        try {
            if (account == null) {
                Log.e(TAG, "注册状态检查: 账号为null");
                return;
            }
            
            // 创建一个延迟任务检查注册状态
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (account.getState() == RegistrationState.Ok) {
                            Log.i(TAG, "注册状态检查: 注册成功");
                            if (listener != null) {
                                listener.onRegistered();
                            }
                            if (sipCallback != null) {
                                sipCallback.onSuccess();
                            }
                        } else {
                            Log.w(TAG, "注册状态检查: 当前状态 = " + account.getState());
                            
                            // 如果仍在进行中，再次延迟检查
                            if (account.getState() == RegistrationState.Progress) {
                                new android.os.Handler().postDelayed(this, 2000);
                            } else if (account.getState() == RegistrationState.Failed) {
                                String reason = "未知";
                                try {
                                    // 在不同版本的Linphone SDK中，错误原因的获取方式可能不同
                                    org.linphone.core.Reason errorReason = account.getError();
                                    if (errorReason != null) {
                                        reason = errorReason.toString();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "获取错误原因失败", e);
                                }
                                
                                Log.e(TAG, "注册状态检查: 注册失败，原因: " + reason);
                                
                                if (listener != null) {
                                    listener.onError("SIP注册失败: " + reason);
                                }
                                if (sipCallback != null) {
                                    sipCallback.onError("SIP注册失败: " + reason);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "注册状态检查异常: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), e);
                    }
                }
            }, 3000); // 3秒后检查
        } catch (Exception e) {
            Log.e(TAG, "设置注册状态检查失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查指定端口是否开放
     */
    private boolean isPortOpen(String host, int port) {
        java.net.Socket socket = null;
        try {
            // 设置连接超时为3秒
            socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 3000);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "端口检测失败: " + host + ":" + port, e);
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, "关闭Socket失败", e);
                }
            }
        }
    }

    /**
     * 判断字符串是否为IP地址
     */
    private boolean isIPAddress(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        
        try {
            String[] parts = host.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 调试SIP连接信息
     */
    private void debugSipConnection(String domain, String port) {
        Log.d(TAG, "========== SIP连接调试信息 ==========");
        Log.d(TAG, "目标域名/IP: " + domain);
        Log.d(TAG, "目标端口: " + port);
        
        try {
            // 检查网络连接
            boolean isConnected = isNetworkConnected();
            Log.d(TAG, "网络连接状态: " + (isConnected ? "已连接" : "未连接"));
            
            // 获取本地IP
            String localIp = getLocalIpAddress();
            Log.d(TAG, "本地IP地址: " + localIp);
            
            // 尝试DNS解析
            if (domain != null && !isIPAddress(domain)) {
                try {
                    java.net.InetAddress address = java.net.InetAddress.getByName(domain);
                    Log.d(TAG, "DNS解析结果: " + address.getHostAddress());
                } catch (Exception e) {
                    Log.d(TAG, "DNS解析失败: " + e.getMessage());
                }
            }
            
            // 尝试Ping测试
            boolean pingSuccess = pingHost(domain);
            Log.d(TAG, "Ping测试: " + (pingSuccess ? "成功" : "失败"));
            
            // 检查端口状态
            if (port != null && !port.isEmpty()) {
                try {
                    int portNum = Integer.parseInt(port);
                    boolean portOpen = isPortOpen(domain, portNum);
                    Log.d(TAG, "端口 " + port + " 状态: " + (portOpen ? "开放" : "关闭"));
                } catch (Exception e) {
                    Log.d(TAG, "端口检测失败: " + e.getMessage());
                }
            }
            
            // 检查Linphone核心状态
            if (core != null) {
                Log.d(TAG, "Linphone核心状态: 已初始化");
                Log.d(TAG, "Linphone网络可达性: " + core.isNetworkReachable());
                
                // 检查注册状态
                org.linphone.core.ProxyConfig[] proxyConfigs = core.getProxyConfigList();
                if (proxyConfigs != null && proxyConfigs.length > 0) {
                    for (int i = 0; i < proxyConfigs.length; i++) {
                        org.linphone.core.ProxyConfig proxy = proxyConfigs[i];
                        Log.d(TAG, "代理配置 #" + i + " 状态: " + proxy.getState());
                        Log.d(TAG, "代理配置 #" + i + " 错误: " + proxy.getError());
                    }
                } else {
                    Log.d(TAG, "没有发现代理配置");
                }
            } else {
                Log.d(TAG, "Linphone核心状态: 未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "SIP连接调试过程中出错", e);
        }
        Log.d(TAG, "========== 调试信息结束 ==========");
    }
    
    /**
     * 简单的Ping测试
     */
    private boolean pingHost(String host) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("/system/bin/ping -c 1 -W 3 " + host);
            int exitValue = process.waitFor();
            return (exitValue == 0);
        } catch (Exception e) {
            Log.e(TAG, "Ping测试失败", e);
            return false;
        }
    }
    
    /**
     * 获取本机IP地址
     */
    private String getLocalIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface networkInterface = interfaces.nextElement();
                
                // 忽略回环和非活动接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                // 输出接口信息便于调试
                Log.d(TAG, "网络接口: " + networkInterface.getDisplayName() + 
                      " (" + networkInterface.getName() + ")");
                
                java.util.Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (address instanceof java.net.Inet4Address) {
                        String ip = address.getHostAddress();
                        Log.d(TAG, "  - IPv4: " + ip);
                        if (!ip.startsWith("127.")) {
                            return ip; // 返回第一个非回环IPv4地址
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取本机IP失败: " + e.getMessage(), e);
        }
        
        return "127.0.0.1"; // 默认返回本地回环地址
    }
    
    /**
     * 注销SIP账号
     */
    public void logout() {
        if (core == null) return;
        
        Account defaultAccount = core.getDefaultAccount();
        if (defaultAccount != null) {
            AccountParams params = defaultAccount.getParams().clone();
            params.setRegisterEnabled(false);
            defaultAccount.setParams(params);
            
            Log.i(TAG, "SIP账号已注销");
        }
    }
    
    /**
     * 发起语音通话
     * 
     * @param sipAddress 被叫SIP地址
     */
    public void makeAudioCall(String sipAddress) {
        makeCall(sipAddress, false);
    }
    
    /**
     * 发起视频通话
     * 
     * @param sipAddress 被叫SIP地址
     */
    public void makeVideoCall(String sipAddress) {
        makeCall(sipAddress, true);
    }
    
    /**
     * 发起通话
     * 
     * @param to 被叫SIP地址或用户名
     * @param withVideo 是否启用视频
     */
    public void makeCall(String to, boolean withVideo) {
        if (core == null) {
            Log.e(TAG, "Linphone核心未初始化");
            Toast.makeText(context, "错误: Linphone核心未初始化", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (callState != CALL_STATE_IDLE) {
            Log.w(TAG, "当前已有通话进行中");
            Toast.makeText(context, "当前已有通话进行中", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查SIP注册状态
        if (!isRegistered()) {
            Log.e(TAG, "SIP账号未注册，无法拨打电话");
            Toast.makeText(context, "SIP账号未注册，请先登录", Toast.LENGTH_LONG).show();
            if (listener != null) {
                listener.onError("SIP账号未注册，无法拨打电话");
            }
            return;
        }
        
        // 格式化SIP地址
        String sipAddress = to;
        if (!to.startsWith("sip:")) {
            Account account = core.getDefaultAccount();
            if (account != null) {
                Address serverAddress = account.getParams().getServerAddress();
                String domain = serverAddress.getDomain();
                
                // 处理FreeSwitch特定的格式
                // 使用分机号/用户ID而不是完整的SIP URI
                if (to.contains("@")) {
                    // 已经包含域名部分的地址
                    sipAddress = "sip:" + to;
                } else {
                    // 只有用户名/分机号的地址
                    sipAddress = "sip:" + to + "@" + domain;
                }
                
                Log.d(TAG, "格式化后的SIP地址: " + sipAddress);
            }
        }
        
        try {
            Log.d(TAG, "尝试呼叫SIP地址: " + sipAddress + " 视频模式: " + withVideo);
            Toast.makeText(context, "呼叫中: " + sipAddress, Toast.LENGTH_SHORT).show();
            
            // 创建地址
            Address remoteAddress = Factory.instance().createAddress(sipAddress);
            if (remoteAddress == null) {
                Log.e(TAG, "无法创建地址: " + sipAddress);
                Toast.makeText(context, "错误: 无法创建地址", Toast.LENGTH_LONG).show();
                return;
            }
            
            // 创建呼叫参数
            CallParams params = core.createCallParams(null);
            if (params != null) {
                // 设置通话参数
                params.setVideoEnabled(withVideo);
                params.setMediaEncryption(MediaEncryption.None); // FreeSwitch默认不加密
                Log.d(TAG, "已设置呼叫参数: 视频=" + withVideo);
                
                // FreeSwitch特定参数
                // 设置优先使用的编解码器
                params.setAudioBandwidthLimit(0); // 不限制带宽
                
                // 为较慢的网络优化参数
                if (core.isNetworkReachable()) {
                    // 在良好网络条件下启用优质编解码器
                    Log.d(TAG, "网络可达，使用优质编解码器");
                } else {
                    // 在网络条件不佳时启用低带宽模式
                    params.setLowBandwidthEnabled(true);
                    Log.d(TAG, "网络不佳，启用低带宽模式");
                }
                
                // 发起通话
                core.inviteAddressWithParams(remoteAddress, params);
                Log.i(TAG, "正在呼叫: " + sipAddress + ", 视频: " + withVideo);
            } else {
                core.inviteAddress(remoteAddress);
                Log.i(TAG, "正在呼叫(无参数): " + sipAddress);
            }
            
            updateCallState(CALL_STATE_CONNECTING);
        } catch (Exception e) {
            Log.e(TAG, "发起通话失败: " + e.getMessage(), e);
            Toast.makeText(context, "发起通话失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (listener != null) {
                listener.onError("发起通话失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 接听来电
     */
    public void answerCall() {
        if (core == null || currentCall == null) {
            Log.e(TAG, "无法接听来电:核心未初始化或无当前来电");
            return;
        }
        
        try {
            CallParams params = core.createCallParams(currentCall);
            if (params != null) {
                // 设置接听参数
                boolean hasVideo = currentCall.getRemoteParams().isVideoEnabled();
                params.setVideoEnabled(hasVideo);
                
                // 接听通话
                currentCall.acceptWithParams(params);
            } else {
                currentCall.accept();
            }
            
            Log.i(TAG, "已接听来电");
        } catch (Exception e) {
            Log.e(TAG, "接听来电失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 拒绝来电
     */
    public void rejectCall() {
        if (core == null || currentCall == null) return;
        
        try {
            currentCall.decline(org.linphone.core.Reason.Declined);
            Log.i(TAG, "已拒绝来电");
        } catch (Exception e) {
            Log.e(TAG, "拒绝来电失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 结束通话
     */
    public void endCall() {
        if (core == null) return;
        
        try {
            if (currentCall != null) {
                currentCall.terminate();
                Log.i(TAG, "已结束通话");
            } else if (core.getCallsNb() > 0) {
                // 结束所有通话
                for (Call call : core.getCalls()) {
                    call.terminate();
                }
                Log.i(TAG, "已结束所有通话");
            }
        } catch (Exception e) {
            Log.e(TAG, "结束通话失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 设置麦克风静音
     * 
     * @param mute 是否静音
     */
    public void setMicrophoneMuted(boolean mute) {
        if (core != null) {
            core.setMicEnabled(!mute);
            Log.i(TAG, "麦克风已" + (mute ? "静音" : "取消静音"));
        }
    }
    
    /**
     * 切换扬声器
     * 
     * @param enable 是否启用扬声器
     */
    public void setSpeakerEnabled(boolean enable) {
        if (core == null) return;
        
        try {
            AudioDevice[] devices = core.getAudioDevices();
            AudioDevice speakerDevice = null;
            AudioDevice earpieiece = null;
            
            // 查找扬声器和耳机设备
            for (AudioDevice device : devices) {
                if (device.getType() == AudioDevice.Type.Speaker) {
                    speakerDevice = device;
                } else if (device.getType() == AudioDevice.Type.Earpiece) {
                    earpieiece = device;
                }
            }
            
            // 设置输出设备
            if (enable && speakerDevice != null) {
                core.setOutputAudioDevice(speakerDevice);
                Log.i(TAG, "已切换到扬声器");
            } else if (!enable && earpieiece != null) {
                core.setOutputAudioDevice(earpieiece);
                Log.i(TAG, "已切换到听筒");
            }
        } catch (Exception e) {
            Log.e(TAG, "切换音频设备失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 切换视频开关
     * 
     * @param enable 是否启用视频
     */
    public void setVideoEnabled(boolean enable) {
        if (core == null || currentCall == null) return;
        
        try {
            // 获取当前参数
            CallParams params = core.createCallParams(currentCall);
            if (params != null) {
                // 更新视频状态
                params.setVideoEnabled(enable);
                
                // 应用更新
                currentCall.update(params);
                
                isVideoEnabled = enable;
                Log.i(TAG, "视频已" + (enable ? "启用" : "禁用"));
            }
        } catch (Exception e) {
            Log.e(TAG, "切换视频状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (core == null) return;
        
        try {
            String currentCamera = core.getVideoDevice();
            String[] cameras = core.getVideoDevicesList();
            
            if (cameras.length > 1) {
                // 找到当前摄像头以外的第一个摄像头
                for (String camera : cameras) {
                    if (!camera.equals(currentCamera)) {
                        core.setVideoDevice(camera);
                        Log.i(TAG, "已切换摄像头: " + camera);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "切换摄像头失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 设置视频窗口
     * 
     * @param localView 本地预览视图
     * @param remoteView 远程视频视图
     */
    public void setVideoWindows(Object localView, Object remoteView) {
        if (core == null) return;
        
        try {
            // 设置视频窗口
            core.setNativeVideoWindowId(remoteView);
            core.setNativePreviewWindowId(localView);
            Log.i(TAG, "已设置视频窗口");
        } catch (Exception e) {
            Log.e(TAG, "设置视频窗口失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (core != null) {
            try {
                // 结束正在进行的通话
                if (currentCall != null) {
                    currentCall.terminate();
                }
                
                // 移除监听器
                core.removeListener(coreListener);
                
                // 停止核心
                core.stop();
                
                Log.i(TAG, "Linphone核心已释放");
            } catch (Exception e) {
                Log.e(TAG, "释放Linphone核心失败: " + e.getMessage(), e);
            } finally {
                core = null;
                currentCall = null;
            }
        }
    }
    
    /**
     * 更新通话状态
     * 
     * @param state 新的通话状态
     */
    private void updateCallState(int state) {
        callState = state;
        
        if (listener != null) {
            listener.onCallStateChanged(state);
        }
    }
    
    /**
     * 获取当前通话状态
     * 
     * @return 通话状态代码
     */
    public int getCallState() {
        return callState;
    }
    
    /**
     * 检查是否有视频功能
     * 
     * @return 是否启用了视频
     */
    public boolean isVideoEnabled() {
        return isVideoEnabled;
    }
    
    /**
     * 获取SIP账号状态
     * 
     * @return 是否已注册
     */
    public boolean isRegistered() {
        if (core == null) return false;
        
        Account account = core.getDefaultAccount();
        if (account == null) return false;
        
        return account.getState() == RegistrationState.Ok;
    }
    
    /**
     * 获取Linphone Core对象
     * 
     * @return Core对象，如果未初始化则为null
     */
    public Core getCore() {
        return core;
    }
    
    /**
     * 检查网络是否连接
     */
    private boolean isNetworkConnected() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "检查网络连接状态失败", e);
            return false;
        }
    }
    
    /**
     * 显示详细错误对话框
     */
    private void showDetailedErrorDialog(Context context, String errorMsg) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("SIP连接诊断");
            
            // 创建一个可滚动的文本视图
            android.widget.TextView textView = new android.widget.TextView(context);
            textView.setText(errorMsg);
            textView.setPadding(20, 20, 20, 20);
            
            android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
            scrollView.addView(textView);
            
            builder.setView(scrollView);
            builder.setPositiveButton("确定", null);
            builder.setNeutralButton("复制报告", (dialog, which) -> {
                android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = 
                    android.content.ClipData.newPlainText("SIP诊断报告", errorMsg);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "诊断报告已复制到剪贴板", Toast.LENGTH_SHORT).show();
            });
            
            android.app.AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "显示错误对话框失败", e);
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 将技术错误消息转换为用户友好的错误消息
     */
    private String getFriendlyErrorMessage(String errorReason) {
        if (errorReason == null) {
            return "未知错误";
        }
        
        errorReason = errorReason.toLowerCase();
        
        if (errorReason.contains("auth") || errorReason.contains("unauthorized") || 
            errorReason.contains("forbidden") || errorReason.contains("403") || 
            errorReason.contains("401")) {
            return "认证失败：用户名或密码错误";
        } else if (errorReason.contains("not found") || errorReason.contains("404")) {
            return "找不到用户或域名";
        } else if (errorReason.contains("timeout") || errorReason.contains("timed out")) {
            return "连接超时，请检查网络连接和服务器地址";
        } else if (errorReason.contains("network") || errorReason.contains("unreachable")) {
            return "网络不可达，请检查网络连接";
        } else if (errorReason.contains("io error") || errorReason.contains("connection")) {
            return "连接错误，服务器可能未启动或端口不正确";
        } else if (errorReason.contains("dns")) {
            return "DNS解析失败，请检查服务器地址或使用IP地址";
        } else if (errorReason.contains("bad") && errorReason.contains("credentials")) {
            return "凭据错误，请检查用户名和密码";
        } else if (errorReason.contains("busy")) {
            return "服务器忙，请稍后重试";
        } else if (errorReason.contains("transport")) {
            return "传输层错误，请检查网络设置";
        }
        
        return "SIP注册失败: " + errorReason;
    }
    
    /**
     * 判断是否应该自动重试连接
     */
    private boolean shouldRetryConnection(Exception e) {
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.d(TAG, "已达到最大重试次数: " + MAX_RETRY_COUNT);
            return false;
        }
        
        // 检查是否是网络相关异常
        boolean isNetworkError = false;
        
        if (e instanceof java.net.UnknownHostException
            || e instanceof java.net.SocketTimeoutException
            || e instanceof java.net.ConnectException
            || e instanceof java.io.IOException) {
            isNetworkError = true;
        }
        
        // 如果异常消息包含网络相关关键字
        if (e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("network") 
                || msg.contains("timeout") 
                || msg.contains("connect") 
                || msg.contains("unreachable")
                || msg.contains("dns")) {
                isNetworkError = true;
            }
        }
        
        return isNetworkError && isNetworkConnected();
    }
    
    /**
     * 安排自动重试
     */
    private void scheduleRetry() {
        retryCount++;
        Log.d(TAG, "安排SIP连接重试 #" + retryCount + " (在" + RETRY_DELAY_MS/1000 + "秒后)");
        
        if (listener != null) {
            listener.onRetryScheduled(retryCount, MAX_RETRY_COUNT);
        }
        
        // 通知用户
        Toast.makeText(context, 
            "网络连接问题，将在" + RETRY_DELAY_MS/1000 + "秒后重试 (" + retryCount + "/" + MAX_RETRY_COUNT + ")", 
            Toast.LENGTH_SHORT).show();
        
        // 延迟重试
        retryHandler.postDelayed(() -> {
            Log.d(TAG, "执行重试 #" + retryCount);
            login(username, password, domain, port, transport, sipCallback);
        }, RETRY_DELAY_MS);
    }
    
    /**
     * 设置连接超时处理
     */
    private void setupConnectionTimeout() {
        // 如果30秒后仍未收到注册结果，则认为连接超时
        retryHandler.postDelayed(() -> {
            if (core != null) {
                org.linphone.core.ProxyConfig[] proxyConfigs = core.getProxyConfigList();
                if (proxyConfigs != null && proxyConfigs.length > 0) {
                    for (org.linphone.core.ProxyConfig config : proxyConfigs) {
                        if (config.getState() == org.linphone.core.RegistrationState.Progress) {
                            Log.e(TAG, "SIP连接超时");
                            
                            // 向用户显示错误
                            String timeoutError = "连接超时，请检查网络和服务器设置";
                            Toast.makeText(context, timeoutError, Toast.LENGTH_LONG).show();
                            
                            if (listener != null) {
                                listener.onError(timeoutError);
                            }
                            
                            // 如果有网络连接，尝试自动重试
                            if (retryCount < MAX_RETRY_COUNT && isNetworkConnected()) {
                                scheduleRetry();
                            } else {
                                // 显示详细的错误诊断
                                String detailedError = "SIP连接超时\n\n" +
                                    "诊断信息:\n" +
                                    "- 服务器: " + domain + ":" + port + "\n" +
                                    "- 本机IP: " + getLocalIpAddress() + "\n" +
                                    "- 网络状态: " + (isNetworkConnected() ? "已连接" : "未连接") + "\n";
                                
                                showDetailedErrorDialog(context, detailedError);
                            }
                        }
                    }
                }
            }
        }, 30000); // 30秒超时
    }
    
    /**
     * 诊断SIP服务器连接问题
     * 该方法可以在用户操作界面中调用，用于主动检测SIP服务器连接情况
     */
    public String diagnoseSipConnection() {
        StringBuilder report = new StringBuilder();
        report.append("===== SIP连接诊断报告 =====\n\n");
        
        // 基本网络检查
        try {
            boolean isConnected = isNetworkConnected();
            report.append("网络连接: ").append(isConnected ? "正常" : "异常").append("\n");
            
            if (isConnected) {
                // 获取设备网络信息
                String localIp = getLocalIpAddress();
                report.append("本机IP: ").append(localIp).append("\n");
                
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
                android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    report.append("网络类型: ").append(activeNetwork.getTypeName()).append("\n");
                    report.append("网络子类型: ").append(activeNetwork.getSubtypeName()).append("\n");
                    report.append("网络连接状态: ").append(activeNetwork.getState()).append("\n");
                }
            }
        } catch (Exception e) {
            report.append("网络信息获取失败: ").append(e.getMessage()).append("\n");
        }
        
        // SIP服务器信息
        report.append("\n服务器信息:\n");
        report.append("域名/IP: ").append(domain != null ? domain : "未设置").append("\n");
        report.append("端口: ").append(port != null ? port : "未设置").append("\n");
        report.append("传输协议: ").append(transport != null ? transport : "未设置").append("\n");
        
        // DNS解析检查
        if (domain != null && !isIPAddress(domain)) {
            try {
                report.append("\nDNS解析:\n");
                java.net.InetAddress address = java.net.InetAddress.getByName(domain);
                report.append("域名解析IP: ").append(address.getHostAddress()).append("\n");
            } catch (Exception e) {
                report.append("DNS解析失败: ").append(e.getMessage()).append("\n");
            }
        }
        
        // 端口检查
        if (domain != null && port != null) {
            try {
                report.append("\n端口连通性:\n");
                int portNum = Integer.parseInt(port);
                boolean isPortOpen = isPortOpen(domain, portNum);
                report.append("端口 ").append(port).append(": ").append(isPortOpen ? "开放" : "关闭").append("\n");
            } catch (Exception e) {
                report.append("端口检查失败: ").append(e.getMessage()).append("\n");
            }
        }
        
        // Linphone状态
        report.append("\nLinphone状态:\n");
        if (core != null) {
            report.append("核心状态: 已初始化\n");
            report.append("Linphone版本: ").append(core.getVersion()).append("\n");
            report.append("网络可达: ").append(core.isNetworkReachable()).append("\n");
            
            // 代理配置
            org.linphone.core.ProxyConfig[] proxyConfigs = core.getProxyConfigList();
            if (proxyConfigs != null && proxyConfigs.length > 0) {
                for (int i = 0; i < proxyConfigs.length; i++) {
                    org.linphone.core.ProxyConfig config = proxyConfigs[i];
                    report.append("代理配置 #").append(i).append(":\n");
                    report.append("  - 状态: ").append(config.getState()).append("\n");
                    report.append("  - 错误: ").append(config.getError()).append("\n");
                    report.append("  - 服务器地址: ").append(config.getServerAddr()).append("\n");
                    report.append("  - 身份: ").append(config.getIdentityAddress()).append("\n");
                }
            } else {
                report.append("无代理配置\n");
            }
        } else {
            report.append("核心状态: 未初始化\n");
        }
        
        report.append("\n===== 诊断报告结束 =====");
        
        // 记录到日志
        Log.d(TAG, report.toString());
        
        return report.toString();
    }

    /**
     * 显示SIP连接诊断对话框
     */
    public void showSipDiagnosticDialog(Context context) {
        String diagnosticReport = diagnoseSipConnection();
        showDetailedErrorDialog(context, diagnosticReport);
    }
    
    /**
     * Linphone管理器监听器接口
     */
    public interface LinphoneManagerListener {
        /**
         * 通话状态变化回调
         * 
         * @param state 新的通话状态
         */
        void onCallStateChanged(int state);
        
        /**
         * 来电回调
         * 
         * @param callerId 来电者ID
         * @param callType 通话类型(音频/视频)
         */
        void onIncomingCall(String callerId, int callType);
        
        /**
         * 注册成功回调
         */
        void onRegistered();
        
        /**
         * 错误回调
         * 
         * @param errorMessage 错误信息
         */
        void onError(String errorMessage);
        
        /**
         * 重试调度回调
         * 
         * @param currentRetry 当前重试次数
         * @param maxRetries 最大重试次数
         */
        default void onRetryScheduled(int currentRetry, int maxRetries) {}
    }

    /**
     * SIP回调接口
     */
    public interface SipCallback {
        /**
         * 登录成功回调
         */
        void onSuccess();
        
        /**
         * 登录开始回调
         */
        void onLoginStarted();
        
        /**
         * 错误回调
         * 
         * @param errorMessage 错误信息
         */
        void onError(String errorMessage);
        
        /**
         * 重试调度回调
         * 
         * @param currentRetry 当前重试次数
         * @param maxRetries 最大重试次数
         */
        default void onRetryScheduled(int currentRetry, int maxRetries) {}
    }
} 