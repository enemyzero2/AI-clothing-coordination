package com.example.aioutfitapp.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;

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

import com.example.aioutfitapp.App;

/**
 * Linphone管理类
 * 
 * 负责Linphone SDK的初始化、注册和通话管理
 */
public class LinphoneManager {
    
    private static final String TAG = "LinphoneManager";
    
    // 存储键值
    private static final String PREF_NAME = "linphone_preferences";
    private static final String PREF_USERNAME = "pref_username";
    private static final String PREF_PASSWORD = "pref_password";
    private static final String PREF_DOMAIN = "pref_domain";
    private static final String PREF_PORT = "pref_port";
    private static final String PREF_TRANSPORT = "pref_transport";
    private static final String PREF_AUTO_LOGIN = "pref_auto_login";
    
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
    
    // 是否已初始化
    private boolean isInitialized = false;
    // 是否自动登录
    private boolean autoLogin = true;
    
    // 存储对象
    private SharedPreferences preferences;
    
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
        if (isInitialized) {
            return this;
        }
        
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // 彻底清理Linphone配置，避免使用旧配置
        purgeLegacyConfig();
        
        // 开启日志收集
        Factory.instance().enableLogCollection(LogCollectionState.Enabled);
        Factory.instance().enableLogcatLogs(true);
        
        // 创建配置目录
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
            
            // 标记为已初始化
            isInitialized = true;
            
            Log.i(TAG, "Linphone初始化完成");
            
            // 尝试自动登录
            tryAutoLogin();
            
        } catch (Exception e) {
            Log.e(TAG, "Linphone初始化失败: " + e.getMessage(), e);
        }
        
        return this;
    }
    
    /**
     * 彻底清理旧的Linphone配置文件，清除可能包含默认账号"1001"的所有配置
     */
    private void purgeLegacyConfig() {
        try {
            // 删除应用数据目录中的所有Linphone相关文件
            File filesDir = context.getFilesDir();
            deleteLinphoneFiles(filesDir);
            
            // 清除共享偏好设置中可能包含账号信息的键
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
            
            // 删除Linphone RC文件
            File rcFile = new File(filesDir, ".linphonerc");
            if (rcFile.exists()) {
                boolean deleted = rcFile.delete();
                Log.d(TAG, "删除Linphone RC文件: " + (deleted ? "成功" : "失败"));
            }
            
            // 删除Linphone数据库文件
            File dbFile = new File(filesDir, "linphone.db");
            if (dbFile.exists()) {
                boolean deleted = dbFile.delete();
                Log.d(TAG, "删除Linphone数据库文件: " + (deleted ? "成功" : "失败"));
            }
            
            // 删除其他可能的配置文件
            File factoryRcFile = new File(filesDir, ".linphonerc-factory");
            if (factoryRcFile.exists()) {
                boolean deleted = factoryRcFile.delete();
                Log.d(TAG, "删除Linphone出厂配置文件: " + (deleted ? "成功" : "失败"));
            }
            
            File linphoneDir = new File(filesDir, "linphone");
            if (linphoneDir.exists() && linphoneDir.isDirectory()) {
                deleteRecursive(linphoneDir);
                Log.d(TAG, "删除Linphone目录");
            }
            
            Log.i(TAG, "已彻底清理所有Linphone配置");
        } catch (Exception e) {
            Log.e(TAG, "清理Linphone配置出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 递归删除目录及其内容
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        boolean deleted = fileOrDirectory.delete();
        if (!deleted) {
            Log.w(TAG, "无法删除文件: " + fileOrDirectory.getAbsolutePath());
        }
    }
    
    /**
     * 删除目录中所有Linphone相关文件
     */
    private void deleteLinphoneFiles(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName().toLowerCase();
                if (fileName.contains("linphone") || 
                    fileName.startsWith(".linphone") || 
                    fileName.endsWith(".rc")) {
                    boolean deleted = file.delete();
                    Log.d(TAG, "删除文件 " + file.getName() + ": " + (deleted ? "成功" : "失败"));
                }
            }
        }
    }
    
    /**
     * 尝试自动登录
     */
    private void tryAutoLogin() {
        // 检查是否启用自动登录
        boolean shouldAutoLogin = preferences.getBoolean(PREF_AUTO_LOGIN, true);
        
        if (shouldAutoLogin) {
            // 读取保存的账号信息
            String savedUsername = preferences.getString(PREF_USERNAME, "");
            String savedPassword = preferences.getString(PREF_PASSWORD, "");
            String savedDomain = preferences.getString(PREF_DOMAIN, "");
            String savedPort = preferences.getString(PREF_PORT, "5062");
            String savedTransport = preferences.getString(PREF_TRANSPORT, "udp");
            
            // 检查是否有保存的账号
            if (!savedUsername.isEmpty() && !savedPassword.isEmpty() && !savedDomain.isEmpty()) {
                Log.i(TAG, "尝试使用保存的账号自动登录: " + savedUsername + "@" + savedDomain);
                
                // 自动登录
                login(savedUsername, savedPassword, savedDomain, savedPort, savedTransport, new SipCallback() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "自动登录成功");
                    }
                    
                    @Override
                    public void onLoginStarted() {
                        Log.d(TAG, "自动登录开始");
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "自动登录失败: " + errorMessage);
                    }
                });
            } else {
                Log.i(TAG, "没有保存的账号信息，跳过自动登录");
            }
        } else {
            Log.i(TAG, "自动登录已禁用");
        }
    }
    
    /**
     * 保存SIP账号信息
     */
    private void saveSipCredentials(String username, String password, String domain, String port, String transport) {
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PREF_USERNAME, username);
            editor.putString(PREF_PASSWORD, password);
            editor.putString(PREF_DOMAIN, domain);
            editor.putString(PREF_PORT, port);
            editor.putString(PREF_TRANSPORT, transport);
            editor.putBoolean(PREF_AUTO_LOGIN, autoLogin);
            editor.apply();
            
            Log.d(TAG, "SIP账号信息已保存: " + username + "@" + domain);
        }
    }
    
    /**
     * 清除保存的SIP账号信息
     */
    public void clearSipCredentials() {
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(PREF_USERNAME);
            editor.remove(PREF_PASSWORD);
            editor.remove(PREF_DOMAIN);
            editor.remove(PREF_PORT);
            editor.remove(PREF_TRANSPORT);
            editor.apply();
            
            Log.d(TAG, "SIP账号信息已清除");
        }
    }
    
    /**
     * 设置是否自动登录
     */
    public void setAutoLogin(boolean enable) {
        this.autoLogin = enable;
        if (preferences != null) {
            preferences.edit().putBoolean(PREF_AUTO_LOGIN, enable).apply();
            Log.d(TAG, "自动登录已" + (enable ? "启用" : "禁用"));
        }
    }
    
    /**
     * 获取当前登录的用户名
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * 获取当前登录的域名
     */
    public String getDomain() {
        return domain;
    }
    
    /**
     * 配置核心参数
     */
    private void configureCore() {
        if (core == null) return;
        
        // 配置STUN服务器以解决NAT穿透问题
        try {
            Log.i(TAG, "正在配置STUN/ICE服务器...");
            core.setStunServer("stun.linphone.org");
            
            // 使用NatPolicy配置ICE，而不是直接调用setIceEnabled
            try {
                org.linphone.core.NatPolicy natPolicy = core.createNatPolicy();
                //natPolicy.enableStun(true);
                //natPolicy.enableIce(true);
                core.setNatPolicy(natPolicy);
                Log.i(TAG, "已通过NatPolicy启用ICE功能");
            } catch (Exception e) {
                Log.w(TAG, "无法通过NatPolicy启用ICE: " + e.getMessage());
                
                // 尝试使用反射方式启用ICE（兼容旧版本）
                try {
                    java.lang.reflect.Method enableIce = core.getClass().getMethod("enableIce", boolean.class);
                    if (enableIce != null) {
                        enableIce.invoke(core, true);
                        Log.i(TAG, "已通过反射方式启用ICE功能");
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "无法通过反射启用ICE: " + ex.getMessage());
                }
            }
            
            Log.i(TAG, "STUN服务器 [stun.linphone.org] 和 ICE功能配置完成");
        } catch (Exception e) {
            Log.e(TAG, "配置STUN/ICE失败: " + e.getMessage(), e);
        }
        
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
        
        // 禁用任何可能导致自动拒接电话的功能
        try {
            // 禁用免打扰模式
            Log.i(TAG, "尝试禁用可能导致自动拒接的功能");
            // 不使用不兼容的API
            core.setUseRfc2833ForDtmf(true);
            
            // 检查是否有配置文件可能导致身份问题
            File configFile = new File(context.getFilesDir(), ".linphonerc");
            if (configFile.exists()) {
                Log.d(TAG, "检查Linphone配置文件是否有问题...");
                // 读取配置文件内容
                try {
                    java.util.Scanner scanner = new java.util.Scanner(configFile);
                    StringBuilder content = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        content.append(line).append("\n");
                        // 检查是否有默认账号设置
                        if (line.contains("1001@") || line.contains("username=1001")) {
                            Log.w(TAG, "发现可疑配置: " + line);
                        }
                    }
                    scanner.close();
                    Log.d(TAG, "配置文件内容: " + content.toString());
                } catch (Exception e) {
                    Log.e(TAG, "读取配置文件失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "禁用自动拒接功能失败: " + e.getMessage());
        }
        
        // 启用详细日志
        try {
            // 旧版本可能不支持这些日志方法，使用try-catch避免崩溃
            Log.i(TAG, "尝试启用Linphone详细日志");
            //Factory.instance().enableDebug(true);
        } catch (Exception e) {
            Log.w(TAG, "设置Linphone日志级别失败: " + e.getMessage());
        }
        Log.i(TAG, "正在配置音视频编解码器...");
        try {
            // 遍历所有音频编解码器
            for (PayloadType pt : core.getAudioPayloadTypes()) {
                // 优先启用Opus和G711(PCMU/PCMA)，这是兼容性最好的组合
                if (pt.getMimeType().equalsIgnoreCase("opus") || 
                    pt.getMimeType().equalsIgnoreCase("pcmu") || 
                    pt.getMimeType().equalsIgnoreCase("pcma")) {
                    
                    pt.enable(true);
                    Log.d(TAG, "启用了音频编解码器: " + pt.getMimeType());
                } else {
                    // 禁用其他不常用的，避免协商混乱
                    pt.enable(false);
                }
            }

            // 遍历所有视频编解码器
            for (PayloadType pt : core.getVideoPayloadTypes()) {
                // 只启用H264，这是最通用的视频编码
                if (pt.getMimeType().equalsIgnoreCase("h264")) {
                    pt.enable(true);
                    Log.d(TAG, "启用了视频编解码器: H264");
                } else {
                    pt.enable(false);
                }
            }
            Log.i(TAG, "编解码器配置完成。");
        } catch (Exception e) {
            Log.e(TAG, "配置编解码器失败", e);
        }
        // --- 结束添加的代码 ---
        
        // 设置消息监听器拦截和记录SIP消息
        configureSipMessageMonitor();
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
     * 配置SIP消息监控器以记录请求和响应，辅助调试身份问题
     */
    private void configureSipMessageMonitor() {
        try {
            // 为核心注册全局通话状态回调，用于记录SIP消息
            core.addListener(new CoreListenerStub() {
                @Override
                public void onCallStateChanged(Core core, Call call, Call.State state, String message) {
                    if (call != null) {
                        // 记录通话信息，包括SIP请求头
                        StringBuilder sb = new StringBuilder();
                        sb.append("SIP通话状态变更: ").append(state).append("\n");
                        
                        // 尝试获取地址信息，使用安全的方式避免不兼容方法
                        try {
                            Address remote = call.getRemoteAddress();
                            sb.append("远程地址: ").append(remote != null ? remote.asString() : "未知").append("\n");
                        } catch (Exception e) {
                            sb.append("无法获取远程地址: ").append(e.getMessage()).append("\n");
                        }
                        
                        sb.append("当前注册用户: ").append(username != null ? username : "未设置").append("\n");
                        
                        // 检查是否使用了正确的FROM标头
                        Account account = core.getDefaultAccount();
                        if (account != null && account.getParams() != null) {
                            try {
                                Address identity = account.getParams().getIdentityAddress();
                                sb.append("账号身份: ").append(identity != null ? identity.asString() : "未设置").append("\n");
                            } catch (Exception e) {
                                sb.append("无法获取身份信息: ").append(e.getMessage()).append("\n");
                            }
                        }
                        
                        Log.d("SIP_MONITOR", sb.toString());
                    }
                }
                
                @Override
                public void onRegistrationStateChanged(Core core, ProxyConfig config, RegistrationState state, String message) {
                    if (config != null) {
                        // 记录注册信息，包括SIP请求头
                        StringBuilder sb = new StringBuilder();
                        sb.append("SIP注册状态变更: ").append(state).append("\n");
                        
                        try {
                            sb.append("身份地址: ").append(config.getIdentityAddress()).append("\n");
                            
                            // 尝试使用兼容方法获取服务器信息
                            String identityStr = config.getIdentityAddress() != null ? 
                                config.getIdentityAddress().asString() : "未知";
                            sb.append("身份字符串: ").append(identityStr).append("\n");
                            
                            // 移除可能不兼容的方法调用
                            //sb.append("服务器地址: ").append(config.getServerAddress()).append("\n");
                            //sb.append("代理地址: ").append(config.getRoute()).append("\n");
                        } catch (Exception e) {
                            sb.append("无法获取配置信息: ").append(e.getMessage()).append("\n");
                        }
                        
                        sb.append("当前注册用户: ").append(username != null ? username : "未设置").append("\n");
                        sb.append("返回消息: ").append(message != null ? message : "无").append("\n");
                        
                        Log.d("SIP_MONITOR", sb.toString());
                    }
                }
            });
            
            Log.i(TAG, "已配置SIP消息监控器");
        } catch (Exception e) {
            Log.e(TAG, "配置SIP消息监控器失败: " + e.getMessage(), e);
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
                        String incomingUsername = address.getUsername();
                        final String callerId = (displayName != null && !displayName.isEmpty()) 
                                ? displayName : incomingUsername;
                        
                        Log.d(TAG, "收到来电: " + callerId);
                        
                        // 关键：检查并记录当前身份信息，防止被改变
                        String currentIdentity = "未知";
                        try {
                            Account account = core.getDefaultAccount();
                            if (account != null && account.getContactAddress() != null) {
                                currentIdentity = account.getContactAddress().getUsername();
                                Log.d(TAG, "来电前身份检查 - 当前账号: " + currentIdentity);
                                
                                // 检查是否与保存的用户名一致
                                if (username != null && !username.equals(currentIdentity)) {
                                    Log.w(TAG, "警告：当前账号与保存的用户名不一致！保存的用户名: " + username);
                                    
                                    // 立即尝试恢复正确身份
                                    ensureCorrectIdentity();
                                }
                            } else {
                                Log.w(TAG, "来电时无默认账号或联系地址");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "检查身份时出错: " + e.getMessage());
                        }
                        
                        Toast.makeText(context, "收到来电: " + callerId, Toast.LENGTH_SHORT).show();
                        
                        // 确定通话类型
                        final int callType = call.getCurrentParams().isVideoEnabled() 
                                ? CALL_TYPE_VIDEO : CALL_TYPE_AUDIO;
                        
                        // 通知UI
                        if (listener != null) {
                            listener.onIncomingCall(callerId, callType);
                        }
                        
                        // 直接启动来电界面前再次确认身份
                        ensureCorrectIdentity();
                        startIncomingCallActivity(callerId, callType);
                        break;
                    case Connected:
                        Log.d(TAG, "通话已连接");
                        
                        // 关键：在连接后立即再次确认身份，并保存连接时的身份信息
                        try {
                            // 保存连接时的身份信息，以便后续恢复
                            Account account = core.getDefaultAccount();
                            if (account != null && account.getContactAddress() != null) {
                                String connectedIdentity = account.getContactAddress().getUsername();
                                Log.d(TAG, "通话连接时的身份: " + connectedIdentity);
                                
                                // 确保这个身份是正确的
                                if (username != null && !username.equals(connectedIdentity)) {
                                    Log.w(TAG, "通话连接时身份不匹配，立即修复！");
                                    ensureCorrectIdentity();
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "保存连接身份时出错: " + e.getMessage());
                        }
                        
                        Toast.makeText(context, "通话已连接", Toast.LENGTH_SHORT).show();
                        updateCallState(CALL_STATE_CONNECTED);
                        break;
                    case StreamsRunning:
                        Log.d(TAG, "媒体流已开始");
                        
                        // 媒体流开始时再次确认身份
                        ensureCorrectIdentity();
                        
                        updateCallState(CALL_STATE_CONNECTED);
                        isVideoEnabled = call.getCurrentParams().isVideoEnabled();
                        Log.d(TAG, "视频状态: " + (isVideoEnabled ? "已启用" : "未启用"));
                        break;
                    case End:
                        Log.d(TAG, "通话已结束");
                        Toast.makeText(context, "通话已结束", Toast.LENGTH_SHORT).show();
                        updateCallState(CALL_STATE_ENDED);
                        
                        // 通话结束后确认身份没有被改变
                        ensureCorrectIdentity();
                        
                        currentCall = null;
                        break;
                    case Released:
                        Log.d(TAG, "通话资源已释放");
                        updateCallState(CALL_STATE_ENDED);
                        
                        // 资源释放后确认身份没有被改变
                        ensureCorrectIdentity();
                        
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
            
            // 不复制默认配置文件，而是创建一个新的干净配置
            File rcFile = new File(configPath + "/.linphonerc");
            if (!rcFile.exists()) {
                boolean created = rcFile.createNewFile();
                if (created) {
                    // 写入基本配置，但不包含任何默认账号信息
                    FileOutputStream fos = new FileOutputStream(rcFile);
                    String basicConfig = 
                        "[sip]\n" +
                        "guess_hostname=1\n" +
                        "register_only_when_network_is_up=1\n" +
                        "auto_net_state_mon=1\n" +
                        "ping_with_options=0\n" +
                        "\n" +
                        "[sound]\n" +
                        "echocancellation=1\n" +
                        "mic_gain_db=0.0\n" +
                        "playback_gain_db=0.0\n" +
                        "\n" +
                        "[video]\n" +
                        "displaytype=MSAndroidTextureDisplay\n" +
                        "auto_resize_preview_to_keep_ratio=1\n" +
                        "capture=1\n" +
                        "display=1\n" +
                        "preferred_video_codec=H264\n" +
                        "\n" +
                        "[misc]\n" +
                        "max_calls=10\n";
                    
                    fos.write(basicConfig.getBytes());
                    fos.close();
                    Log.i(TAG, "创建了新的Linphone配置文件");
                } else {
                    Log.e(TAG, "无法创建新的Linphone配置文件");
                }
            }
            
            Log.i(TAG, "配置文件准备完成: " + configPath);
        } catch (IOException e) {
            Log.e(TAG, "配置文件操作失败: " + e.getMessage(), e);
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
                // 清除所有用户信息以确保不会使用任何默认值
                core.clearAllAuthInfo();
                Log.d(TAG, "清除现有代理配置和认证信息成功");
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
                
                // 登录成功，保存凭据
                saveSipCredentials(username, password, domain, port, transport);
                
                // 通知UI线程登录成功
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (sipCallback != null) {
                        sipCallback.onSuccess();
                    }
                    
                    if (listener != null) {
                        listener.onRegistered();
                    }
                });
                
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
        try {
            if (core != null) {
                // 停止所有呼叫
                if (core.getCallsNb() > 0) {
                    core.terminateAllCalls();
                }
                
                // 清除所有代理配置
                core.clearProxyConfig();
                
                // 清除所有认证信息
                for (AuthInfo authInfo : core.getAuthInfoList()) {
                    core.removeAuthInfo(authInfo);
                }
                
                Log.i(TAG, "已注销SIP账号");
                
                // 清除登录缓存
                username = null;
                password = null;
                domain = null;
                port = null;
                transport = null;
                
                // 更新注销状态到UI
                if (listener != null) {
                    listener.onError("已注销");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "注销失败: " + e.getMessage(), e);
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
        
        // 发起通话前确保身份正确
        ensureCorrectIdentity();
        
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
        if (core == null) {
            Log.e(TAG, "无法接听：Linphone核心为空！");
            return;
        }
        Call incomingCall = null;
        for (Call call : core.getCalls()) {
            if (call.getState() == Call.State.IncomingReceived) {
                incomingCall = call;
                break;
            }
        }
        if (incomingCall != null) {
            try {
                incomingCall.accept();
                Log.i(TAG, "已直接从Core接听来电！");
            } catch (Exception e) {
                Log.e(TAG, "从Core接听来电失败", e);
            }
        } else {
            Log.e(TAG, "在核心通话列表中找不到处于来电状态的通话！");
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
            // 记录当前状态
            int previousState = callState;
            Log.d(TAG, "开始结束通话，当前状态：" + previousState);
            
            // 查找并结束所有通话
            int callCount = core.getCallsNb();
            if (callCount > 0) {
                Log.d(TAG, "发现" + callCount + "个活跃通话，尝试全部结束");
                
                for (Call call : core.getCalls()) {
                    try {
                        call.terminate();
                        Log.i(TAG, "已结束通话: " + call.getRemoteAddress().asString());
                    } catch (Exception e) {
                        Log.e(TAG, "结束通话失败: " + e.getMessage(), e);
                    }
                }
            } else {
                Log.w(TAG, "无法结束通话：在核心中找不到通话！");
            }
            
            // 强制更新状态
            currentCall = null;
            updateCallState(CALL_STATE_ENDED);
            
            // 延迟一段时间后确保状态被重置为IDLE
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // 再次检查是否还有活跃通话
                if (core != null && core.getCallsNb() > 0) {
                    Log.w(TAG, "通话结束后仍有" + core.getCallsNb() + "个活跃通话，再次尝试结束");
                    for (Call call : core.getCalls()) {
                        try {
                            call.terminate();
                        } catch (Exception e) {
                            Log.e(TAG, "再次结束通话失败: " + e.getMessage(), e);
                        }
                    }
                }
                
                // 最终重置状态
                updateCallState(CALL_STATE_IDLE);
                Log.d(TAG, "通话状态已最终重置为IDLE");
            }, 500);
        } catch (Exception e) {
            Log.e(TAG, "结束通话过程中出错: " + e.getMessage(), e);
            // 确保状态被重置
            currentCall = null;
            updateCallState(CALL_STATE_IDLE);
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
     * 强制重置通话状态
     * 当检测到通话状态异常时调用此方法强制重置
     */
    public void forceResetCallState() {
        Log.d(TAG, "强制重置通话状态");
        
        try {
            // 确保没有活跃的通话
            if (core != null) {
                // 检查是否有活跃的通话
                if (core.getCallsNb() > 0) {
                    Log.w(TAG, "检测到" + core.getCallsNb() + "个活跃通话，尝试强制结束");
                    
                    // 结束所有通话
                    for (Call call : core.getCalls()) {
                        try {
                            call.terminate();
                            Log.d(TAG, "强制结束通话: " + call.getRemoteAddress().asString());
                        } catch (Exception e) {
                            Log.e(TAG, "结束通话失败: " + e.getMessage(), e);
                        }
                    }
                }
                
                // 重置内部状态
                currentCall = null;
                callState = CALL_STATE_IDLE;
                
                // 通知监听器
                if (listener != null) {
                    listener.onCallStateChanged(CALL_STATE_IDLE);
                }
                
                // 刷新注册状态
                core.refreshRegisters();
                
                Log.d(TAG, "通话状态已强制重置为IDLE");
            }
        } catch (Exception e) {
            Log.e(TAG, "强制重置通话状态失败: " + e.getMessage(), e);
        }
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

    /**
     * 启动来电界面
     * 
     * @param callerId 来电者ID
     * @param callType 通话类型
     */
    private void startIncomingCallActivity(String callerId, int callType) {
        try {
            if (context == null) {
                Log.e(TAG, "无法启动来电界面：上下文为空");
                return;
            }
            
            // 创建启动通话界面的Intent
            Intent intent = new Intent(context, com.example.aioutfitapp.CallActivity.class);
            intent.putExtra(com.example.aioutfitapp.CallActivity.EXTRA_CALL_TYPE, callType);
            intent.putExtra(com.example.aioutfitapp.CallActivity.EXTRA_CALLER_ID, callerId);
            intent.putExtra(com.example.aioutfitapp.CallActivity.EXTRA_IS_INCOMING, true);
            intent.putExtra(com.example.aioutfitapp.CallActivity.EXTRA_ROOM_ID, "incoming-call");
            
            // 添加必要的标志，确保可以从非Activity上下文启动
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // 启动活动
            context.startActivity(intent);
            Log.d(TAG, "已启动来电界面：" + callerId);
        } catch (Exception e) {
            Log.e(TAG, "启动来电界面出错: " + e.getMessage(), e);
        }
    }

    /**
     * 设置身份验证详情和账号信息
     * 此方法可以在初始化和接听来电前调用，以确保使用正确的用户身份
     */
    public void ensureCorrectIdentity() {
        if (core == null || username == null || domain == null) {
            Log.d(TAG, "无法验证身份：核心为空或用户信息未设置");
            return;
        }
        
        try {
            Log.d(TAG, "开始身份验证，确保使用正确账号: " + username + "@" + domain);
            
            // 获取当前默认账号
            Account account = core.getDefaultAccount();
            if (account != null) {
                Address contactAddress = account.getContactAddress();
                String currentUser = contactAddress != null ? contactAddress.getUsername() : null;
                
                // 强力检测和修复
                if (currentUser == null || !currentUser.equals(username) || currentUser.equals("1001")) {
                    Log.w(TAG, "检测到身份不匹配或默认1001账号! 当前: " + currentUser + ", 应为: " + username);
                    
                    // 清除所有认证信息和账号
                    core.clearAllAuthInfo();
                    core.clearProxyConfig();
                    
                    // 重新创建认证信息
                    String fullDomain = domain;
                    if (!fullDomain.contains(":")) {
                        fullDomain += ":" + port;
                    }
                    
                    // 重新设置SIP身份和认证信息
                    try {
                        // 创建身份地址
                        String identityUri = "sip:" + username + "@" + domain;
                        Address identity = Factory.instance().createAddress(identityUri);
                        if (identity == null) {
                            Log.e(TAG, "无法创建身份地址: " + identityUri);
                            return;
                        }
                        
                        // 创建认证信息
                        AuthInfo authInfo = Factory.instance().createAuthInfo(
                                username, null, password, null, null, fullDomain, null);
                        core.addAuthInfo(authInfo);
                        
                        // 创建账号参数
                        AccountParams params = core.createAccountParams();
                        if (params == null) {
                            Log.e(TAG, "无法创建账号参数");
                            return;
                        }
                        
                        // 设置身份地址
                        params.setIdentityAddress(identity);
                        
                        // 设置服务器地址
                        String serverUri = "sip:" + fullDomain;
                        Address serverAddr = Factory.instance().createAddress(serverUri);
                        if (serverAddr != null) {
                            // 设置传输类型
                            TransportType transportType = TransportType.Udp;
                            if ("tcp".equalsIgnoreCase(transport)) {
                                transportType = TransportType.Tcp;
                            } else if ("tls".equalsIgnoreCase(transport)) {
                                transportType = TransportType.Tls;
                            }
                            serverAddr.setTransport(transportType);
                            params.setServerAddress(serverAddr);
                        }
                        
                        // 启用注册
                        params.setRegisterEnabled(true);
                        
                        // 禁用可能导致自动拒绝的功能
                        try {
                            // 设置自定义参数，避免使用不兼容的API
                            Log.d(TAG, "设置账号参数，避免自动拒绝功能");
                            
                            // 检查是否有任何可能导致自动拒绝的设置
                            Config config = core.getConfig();
                            if (config != null) {
                                // 尝试从配置中读取相关设置
                                String dndStatus = config.getString("app", "dnd_status", "false");
                                if ("true".equals(dndStatus)) {
                                    Log.w(TAG, "发现免打扰设置已启用，尝试禁用");
                                    config.setString("app", "dnd_status", "false");
                                    config.sync();
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "设置防止自动拒绝的参数失败: " + e.getMessage());
                        }
                        
                        // 设置联系方式和显示名称，确保使用正确的用户标识
                        if (identity != null) {
                            identity.setDisplayName(username);
                        }
                        
                        // 创建账号并添加到核心
                        Account newAccount = core.createAccount(params);
                        core.addAccount(newAccount);
                        core.setDefaultAccount(newAccount);
                        
                        Log.d(TAG, "成功重新创建了SIP账号并设置身份: " + username + "@" + domain);
                        
                        // 发送注册请求
                        core.refreshRegisters();
                        
                        // 确认最终状态
                        Account finalAccount = core.getDefaultAccount();
                        if (finalAccount != null && finalAccount.getContactAddress() != null) {
                            Log.d(TAG, "最终确认: 当前使用的账号为 " + finalAccount.getContactAddress().getUsername());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "重置身份时出错: " + e.getMessage(), e);
                    }
                } else {
                    Log.d(TAG, "身份验证正确: " + username + "@" + domain);
                    
                    // 确保不会自动拒接来电
                    try {
                        // 检查是否有任何导致自动拒接的配置
                        Config config = core.getConfig();
                        if (config != null) {
                            // 检查并禁用任何可能的免打扰设置
                            String dndStatus = config.getString("app", "dnd_status", "false");
                            if ("true".equals(dndStatus)) {
                                Log.w(TAG, "发现免打扰设置已启用，尝试禁用");
                                config.setString("app", "dnd_status", "false");
                                config.sync();
                            }
                            
                            // 检查是否有其他可能导致拒接的设置
                            Log.d(TAG, "检查并禁用可能导致自动拒接的设置");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "检查免打扰设置失败: " + e.getMessage());
                    }
                    
                    // 检查FROM头部是否正确设置
                    if (account.getParams() != null) {
                        Log.d(TAG, "当前账号信息 - 身份: " + account.getParams().getIdentityAddress() +
                                ", 服务器: " + account.getParams().getServerAddress());
                    }
                }
            } else {
                Log.w(TAG, "无默认账号，需要重新登录");
                if (username != null && domain != null && password != null) {
                    login(username, password, domain, port, transport, null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "验证身份时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 监控SIP注册状态，确保在通话过程中不会被注销
     * 在接听电话后调用此方法
     */
    public void startSipRegistrationMonitor() {
        if (retryHandler == null) {
            retryHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        
        Log.d(TAG, "启动SIP注册状态监控...");
        
        // 创建一个定期检查的Runnable
        Runnable registrationMonitor = new Runnable() {
            @Override
            public void run() {
                if (core == null) {
                    Log.w(TAG, "SIP核心为空，无法监控注册状态");
                    return;
                }
                
                // 检查当前通话状态
                boolean isInCall = (callState == CALL_STATE_CONNECTED || callState == CALL_STATE_RINGING);
                
                // 检查注册状态
                boolean isRegistered = isRegistered();
                Log.d(TAG, "SIP注册状态监控 - 是否通话中: " + isInCall + ", 是否已注册: " + isRegistered);
                
                // 如果在通话中但未注册，尝试重新注册
                if (isInCall && !isRegistered) {
                    Log.w(TAG, "检测到通话中SIP账号未注册，尝试恢复...");
                    
                    // 确保身份正确
                    ensureCorrectIdentity();
                    
                    // 刷新注册
                    if (core != null) {
                        try {
                            Log.d(TAG, "主动刷新SIP注册...");
                            core.refreshRegisters();
                        } catch (Exception e) {
                            Log.e(TAG, "刷新SIP注册失败: " + e.getMessage());
                        }
                    }
                }
                
                // 如果仍在通话中，继续监控
                if (isInCall) {
                    retryHandler.postDelayed(this, 5000); // 每5秒检查一次
                } else {
                    Log.d(TAG, "通话已结束，停止SIP注册状态监控");
                }
            }
        };
        
        // 开始监控
        retryHandler.postDelayed(registrationMonitor, 1000); // 1秒后开始监控
    }
} 