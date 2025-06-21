package com.example.aioutfitapp.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * 通话管理服务
 * 
 * 集成SIP和WebRTC功能，提供统一的通话管理接口
 * 已更新为使用Linphone与FreeSwitch兼容
 */
public class CallManager implements 
        LinphoneManager.LinphoneManagerListener,
        WebRTCHelper.WebRTCHelperListener,
        SignalingService.SignalingListener {
    
    private static final String TAG = "CallManager";
    
    // 通话类型
    public static final int CALL_TYPE_AUDIO = 0;
    public static final int CALL_TYPE_VIDEO = 1;
    
    // 通话状态
    public static final int CALL_STATE_IDLE = 0;
    public static final int CALL_STATE_CONNECTING = 1;
    public static final int CALL_STATE_RINGING = 2;
    public static final int CALL_STATE_CONNECTED = 3;
    public static final int CALL_STATE_ENDED = 4;
    
    // SIP和WebRTC组件
    private LinphoneManager linphoneManager;  // 使用Linphone作为唯一的SIP实现
    private WebRTCHelper webRTCHelper;
    private SignalingService signalingService;
    private NetworkSimulator networkSimulator;
    
    // 注意：不再使用Android原生SIP API，完全依赖Linphone
    
    // 通话参数
    private int callType = CALL_TYPE_AUDIO;
    private int callState = CALL_STATE_IDLE;
    private String remoteUserId;
    private String roomId;
    
    // 上下文和回调
    private Context context;
    private CallManagerListener listener;
    private Handler mainHandler;
    
    // 视频渲染器
    private SurfaceViewRenderer localRenderer;
    private SurfaceViewRenderer remoteRenderer;
    
    // 是否是主叫方
    private boolean isInitiator = false;
    
    // 当前使用的SIP账号
    private int currentAccountIndex = 0;
    
    // 单例模式
    private static CallManager instance;
    
    // SIP服务器配置
    private String sipServerDomain = "10.29.206.148"; 
    private String sipServerAddress = "10.29.206.148";
    private int sipServerPort = 5060;               // FreeSwitch默认SIP端口
    private String sipTransport = "UDP";            // FreeSwitch传输协议
    
    // 当前登录的SIP账号信息
    private String currentUsername = null;
    private String currentPassword = null;
    
    // 废弃硬编码账号范围，改用自定义账号
    // private static final int MIN_ACCOUNT_NUMBER = 1001;
    // private static final int MAX_ACCOUNT_NUMBER = 1010;
    private static final String DEFAULT_PASSWORD = "1234";
    
    /**
     * 获取单例实例
     */
    public static synchronized CallManager getInstance(Context context) {
        if (instance == null) {
            instance = new CallManager(context);
        }
        return instance;
    }
    
    /**
     * 私有构造函数
     */
    private CallManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化组件
        this.linphoneManager = LinphoneManager.getInstance();
        this.linphoneManager.init(context);
        this.webRTCHelper = new WebRTCHelper(context);
        this.signalingService = new SignalingService(context);
        this.networkSimulator = NetworkSimulator.getInstance(context);
        
        // 设置监听器
        this.linphoneManager.setLinphoneManagerListener(this);
        this.webRTCHelper.setListener(this);
        this.signalingService.setListener(this);
        
        // 尝试从Linphone获取当前登录账号
        if (linphoneManager.getUsername() != null && !linphoneManager.getUsername().isEmpty()) {
            this.currentUsername = linphoneManager.getUsername();
            this.currentPassword = DEFAULT_PASSWORD; // 假设使用默认密码，因为无法获取已保存的密码
            Log.d(TAG, "从Linphone恢复用户登录身份: " + this.currentUsername);
        }
    }
    
    /**
     * 设置通话监听器
     */
    public void setListener(CallManagerListener listener) {
        this.listener = listener;
    }
    
    /**
     * 设置FreeSwitch服务器配置
     */
    public void setSipServerConfig(String domain, String address, int port, String transport) {
        this.sipServerDomain = domain;
        this.sipServerAddress = address;
        this.sipServerPort = port;
        this.sipTransport = transport;
    }
    
    /**
     * 获取可用的SIP账号列表
     * 
     * 注意：此方法已弃用硬编码账号列表，
     * 现在使用外部传入的实际用户账号
     */
    public List<String> getAvailableSipAccounts() {
        List<String> accounts = new ArrayList<>();
        // 添加当前登录账号
        if (currentUsername != null && !currentUsername.isEmpty()) {
            accounts.add(currentUsername);
        } else {
            accounts.add("请先登录");
        }
        return accounts;
    }
    
    /**
     * 获取当前使用的SIP账号
     */
    public String getCurrentSipAccount() {
        // 返回当前登录的账号
        if (currentUsername != null && !currentUsername.isEmpty()) {
            return currentUsername;
        }
        
        // 如果未登录，返回提示
        return "未登录";
    }
    
    /**
     * 设置当前使用的SIP账号索引
     * 
     * 注意：此方法已弃用，现在不再使用账号索引
     */
    @Deprecated
    public void setCurrentAccountIndex(int index) {
        Log.d(TAG, "账号索引切换功能已弃用");
        // 不再使用账号索引，方法保留是为了兼容性
    }
    
    /**
     * 初始化SIP服务
     */
    public boolean initializeSIP(String username, String domain, String password) {
        Log.d(TAG, "初始化SIP服务 - 用户名: " + username + ", 域名: " + domain);
        
        // 保存当前账号信息
        this.currentUsername = username;
        this.currentPassword = password;
        
        // 使用Linphone登录FreeSwitch服务器
        linphoneManager.login(username, password, domain,
                         String.valueOf(sipServerPort), sipTransport, new LinphoneManager.SipCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "SIP登录成功!");
                mainHandler.post(() -> {
                    if(listener != null) {
                        listener.onMessage("SIP登录成功");
                    }
                });
            }
            
            @Override
            public void onLoginStarted() {
                Log.d(TAG, "SIP登录开始...");
                mainHandler.post(() -> {
                    if(listener != null) {
                        listener.onMessage("正在连接SIP服务器...");
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "SIP登录失败: " + errorMessage);
                mainHandler.post(() -> {
                    if(listener != null) {
                        listener.onError("SIP登录失败: " + errorMessage);
                    }
                });
            }
            
            @Override
            public void onRetryScheduled(int currentRetry, int maxRetries) {
                Log.d(TAG, "SIP登录重试中 (" + currentRetry + "/" + maxRetries + ")");
                mainHandler.post(() -> {
                    if(listener != null) {
                        listener.onMessage("重新尝试连接 (" + currentRetry + "/" + maxRetries + ")");
                    }
                });
            }
        });
        
        // 登录过程是异步的，结果会通过回调通知
        return true;
    }
    
    /**
     * 使用指定的账号初始化SIP服务
     * 
     * 注意：此方法已更新为使用任意有效账号，不再限制为1001-1010范围
     */
    public boolean initializeSipWithAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            Log.e(TAG, "无效的账号: 账号不能为空");
            return false;
        }
        
        String username = accountNumber;
        String password = DEFAULT_PASSWORD;
        Log.d(TAG, "使用SIP账号初始化SIP服务 - 用户名: " + username + ", 服务器地址: " + sipServerAddress);
        
        return initializeSIP(username, sipServerDomain, password);
    }
    
    /**
     * 检查账号是否有效
     * 
     * 注意：此方法已更新，现在只检查是否为有效的SIP用户名格式
     */
    private boolean isValidAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含非法SIP用户名字符
        if (accountNumber.contains(" ") || 
            accountNumber.contains("@") ||
            accountNumber.contains(":")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 使用当前账号初始化SIP服务
     * 
     * 注意：此方法已更新为使用当前保存的用户名，不再使用默认硬编码账号
     */
    public boolean initializeDefaultSIP() {
        // 使用已登录的账号
        if (currentUsername != null && !currentUsername.isEmpty()) {
            String password = currentPassword != null ? currentPassword : DEFAULT_PASSWORD;
            Log.d(TAG, "使用当前SIP账号初始化SIP服务 - 用户名: " + currentUsername + ", 服务器地址: " + sipServerAddress);
            return initializeSIP(currentUsername, sipServerDomain, password);
        } else {
            // 如果没有当前账号，提示用户
            Log.e(TAG, "没有登录账号，无法初始化SIP服务");
            if (listener != null) {
                mainHandler.post(() -> listener.onError("请先登录SIP账号"));
            }
            return false;
        }
    }
    
    /**
     * 切换到下一个SIP账号
     * 
     * 注意：此方法已弃用，因为不再使用硬编码账号列表
     * 现在只返回当前登录账号
     */
    @Deprecated
    public String switchToNextAccount() {
        // 不再切换账号，只返回当前账号
        Log.d(TAG, "账号切换功能已弃用，继续使用当前账号: " + getCurrentSipAccount());
        return getCurrentSipAccount();
    }
    
    /**
     * 初始化WebRTC服务
     */
    public boolean initializeWebRTC(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer) {
        this.localRenderer = localRenderer;
        this.remoteRenderer = remoteRenderer;
        return webRTCHelper.initialize(localRenderer, remoteRenderer);
    }
    
    /**
     * 连接到信令服务器
     */
    public void connectToSignalingServer(String serverUrl, String userId, String roomId) {
        this.remoteUserId = userId;
        this.roomId = roomId;
        signalingService.connect(serverUrl, userId, roomId);
    }
    
    /**
     * 开始模拟网络环境
     */
    public void startNetworkSimulation(int networkType) {
        networkSimulator.startSimulation(networkType);
    }
    
    /**
     * 发起音频通话
     */
    public void makeAudioCall(String sipAddress) {
        if (callState != CALL_STATE_IDLE) {
            Log.e(TAG, "已经有一个通话正在进行，无法发起新通话");
            return;
        }
        
        callType = CALL_TYPE_AUDIO;
        updateCallState(CALL_STATE_CONNECTING);
        
        Log.d(TAG, "发起SIP音频通话: " + sipAddress);
        linphoneManager.makeAudioCall(sipAddress);
    }
    
    /**
     * 发起音频通话到测试用户
     */
    public void makeAudioCallToTestUser(String username) {
        Log.d(TAG, "发起通话到测试用户: " + username);
        // 构建完整的SIP地址
        String sipAddress = username + "@" + sipServerDomain;
        makeAudioCall(sipAddress);
    }
    
    /**
     * 拨打指定FreeSWITCH账号
     * 
     * @param accountNumber 目标账号(1001-1010)
     * @return 是否成功发起呼叫
     */
    public boolean callFreeSwitchAccount(String accountNumber) {
        if (!isValidAccount(accountNumber)) {
            Log.e(TAG, "无效的FreeSWITCH账号: " + accountNumber);
            mainHandler.post(() -> {
                if(listener != null) {
                    listener.onError("无效的FreeSWITCH账号: " + accountNumber);
                }
            });
            return false;
        }
        
        String sipAddress = accountNumber + "@" + sipServerDomain;
        Log.d(TAG, "拨打FreeSWITCH账号: " + sipAddress);
        makeAudioCall(sipAddress);
        return true;
    }
    
    /**
     * 获取SIP账号状态信息
     */
    public String getSipAccountStatus() {
        String currentAccount = getCurrentSipAccount();
        boolean isRegistered = linphoneManager.isRegistered();
        
        StringBuilder status = new StringBuilder();
        status.append("当前SIP账号: ").append(currentAccount).append("\n");
        status.append("SIP服务器: ").append(sipServerDomain).append(":").append(sipServerPort).append("\n");
        status.append("注册状态: ").append(isRegistered ? "已注册" : "未注册");
        
        return status.toString();
    }
    
    /**
     * 获取当前账号的信息
     * 
     * 注意：此方法已更新，不再显示1001-1010账号范围
     */
    public String getAllAccountsInfo() {
        StringBuilder info = new StringBuilder();
        info.append("当前SIP账号信息:\n");
        
        // 显示当前账号
        if (currentUsername != null && !currentUsername.isEmpty()) {
            info.append("用户名: ").append(currentUsername).append("\n");
            info.append("密码: ").append(currentPassword != null ? "******" : "未设置").append("\n");
            info.append("服务器: ").append(sipServerDomain).append(":").append(sipServerPort).append("\n");
            info.append("传输协议: ").append(sipTransport).append("\n");
        } else {
            info.append("未登录SIP账号\n");
        }
        
        // 增加使用说明
        info.append("\n提示: 请使用自定义账号登录，不再使用硬编码的1001-1010账号");
        
        return info.toString();
    }
    
    /**
     * 发起视频通话
     */
    public void makeVideoCall(String userId, String roomId) {
        if (callState != CALL_STATE_IDLE) {
            Log.e(TAG, "已经有一个通话正在进行，无法发起新通话");
            return;
        }
        
        callType = CALL_TYPE_VIDEO;
        this.remoteUserId = userId;
        this.roomId = roomId;
        updateCallState(CALL_STATE_CONNECTING);
        
        // 如果是SIP视频通话
        if (userId.contains("@")) {
            linphoneManager.makeVideoCall(userId);
            return;
        }
        
        // 否则使用WebRTC进行视频通话
        // 连接到信令服务器并创建房间
        String serverUrl = "ws://10.29.206.148:8080/ws"; // 实际项目中应替换为真实的服务器地址
        signalingService.connect(serverUrl, userId, roomId);
    }
    
    /**
     * 接听来电
     */
    public void answerCall() {
        if (callState != CALL_STATE_RINGING) {
            Log.e(TAG, "没有来电，无法接听");
            return;
        }
        
        // 接听前确认使用正确身份
        ensureCorrectLoginIdentity();
        
        // 保存当前账号信息，以便在接听后恢复
        final String savedUsername = currentUsername;
        final String savedPassword = currentPassword;
        
        // 添加更详细的账号信息日志
        Log.d(TAG, "接听前记录账号信息:");
        Log.d(TAG, "- CallManager当前用户名: " + (currentUsername != null ? currentUsername : "未设置"));
        Log.d(TAG, "- Linphone当前用户名: " + (linphoneManager.getUsername() != null ? 
                  linphoneManager.getUsername() : "未设置"));
        
        if (callType == CALL_TYPE_AUDIO) {
            // 接听SIP通话
            linphoneManager.answerCall();
            
            // 延迟一小段时间后再次确认身份，防止接听过程中身份变化
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "接听后检查身份是否被改变...");
                
                // 检查当前身份
                String linphoneUser = linphoneManager.getUsername();
                
                if (linphoneUser == null || !linphoneUser.equals(savedUsername)) {
                    Log.w(TAG, "接听后身份被改变! 当前: " + linphoneUser + ", 应为: " + savedUsername);
                    
                    // 恢复保存的身份
                    currentUsername = savedUsername;
                    currentPassword = savedPassword;
                    
                    // 强制恢复身份
                    ensureCorrectLoginIdentity();
                    
                    // 通知LinphoneManager也恢复身份
                    linphoneManager.ensureCorrectIdentity();
                } else {
                    Log.d(TAG, "接听后身份正确: " + linphoneUser);
                }
                
                // 二次检查确保接听成功，防止自动拒接
                if (linphoneManager.getCallState() != LinphoneManager.CALL_STATE_CONNECTED) {
                    Log.w(TAG, "通话可能未正常连接，检查状态: " + linphoneManager.getCallState());
                    // 尝试再次确认身份
                    ensureCorrectLoginIdentity();
                }
            }, 800);
            
            // 再次延迟检查，确保连接建立后身份仍然正确
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "连接建立后再次检查身份...");
                ensureCorrectLoginIdentity();
                linphoneManager.ensureCorrectIdentity();
            }, 3000); // 3秒后再次检查
        } else {
            // 接听WebRTC视频通话
            // 创建连接并应答
            webRTCHelper.createPeerConnection();
            webRTCHelper.createAnswer();
        }
        
        updateCallState(CALL_STATE_CONNECTED);
    }
    
    /**
     * 拒绝来电
     */
    public void rejectCall() {
        if (callState != CALL_STATE_RINGING) {
            Log.e(TAG, "没有来电，无法拒绝");
            return;
        }
        
        if (callType == CALL_TYPE_AUDIO) {
            // 拒绝SIP通话
            linphoneManager.rejectCall();
        } else {
            // 拒绝WebRTC视频通话
            // 发送拒绝信令
            signalingService.sendReject(remoteUserId);
        }
        
        updateCallState(CALL_STATE_IDLE);
    }
    
    /**
     * 结束通话
     */
    public void endCall() {
        if (callState == CALL_STATE_IDLE) {
            Log.e(TAG, "没有正在进行的通话");
            return;
        }
        
        Log.d(TAG, "开始结束通话，当前状态：" + callState);
        
        // 先更新状态为ENDED，防止重复调用
        updateCallState(CALL_STATE_ENDED);
        
        if (callType == CALL_TYPE_AUDIO || 
            (callType == CALL_TYPE_VIDEO && linphoneManager.getCallState() != CALL_STATE_IDLE)) {
            // 结束SIP通话
            linphoneManager.endCall();
            
            // 延迟一小段时间后进行身份确认，防止通话结束后身份丢失
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                ensureCorrectLoginIdentity();
                
                // 确保通话状态被重置为IDLE
                if (callState != CALL_STATE_IDLE) {
                    Log.d(TAG, "强制重置通话状态为IDLE");
                    updateCallState(CALL_STATE_IDLE);
                }
                
                // 确保Linphone内部状态也被重置
                if (linphoneManager.getCallState() != CALL_STATE_IDLE) {
                    Log.d(TAG, "Linphone通话状态未重置，尝试强制重置");
                    linphoneManager.forceResetCallState();
                }
            }, 500);
        } else {
            // 结束WebRTC视频通话
            webRTCHelper.close();
            signalingService.sendLeave(remoteUserId);
            
            // 确保状态被重置
            updateCallState(CALL_STATE_IDLE);
        }
        
        // 最后确保状态被重置为IDLE
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            updateCallState(CALL_STATE_IDLE);
            Log.d(TAG, "通话结束后最终状态检查和重置完成");
        }, 1000);
    }
    
    /**
     * 确保当前使用的是正确的登录身份，不使用默认账号
     */
    private void ensureCorrectLoginIdentity() {
        if (currentUsername != null && !currentUsername.isEmpty()) {
            // 先确认当前Linphone使用的身份
            String linphoneUser = linphoneManager.getUsername();
            
            // 如果不匹配或为null，则重新应用登录身份
            if (linphoneUser == null || !linphoneUser.equals(currentUsername) || 
                 "1001".equals(linphoneUser)) { // 特别检查是否错误使用了1001账号
                
                Log.w(TAG, "检测到账号身份不匹配! LinphoneID=" + linphoneUser + 
                           ", 当前登录ID=" + currentUsername + ", 尝试恢复身份");
                
                // 重新确认身份
                linphoneManager.ensureCorrectIdentity();
                
                // 短暂延迟确保设置生效
                try { 
                    Log.d(TAG, "短暂等待身份设置生效...");
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.w(TAG, "延迟等待被打断: " + e.getMessage());
                }
                
                // 再次检查以确认修复是否成功
                String newLinphoneUser = linphoneManager.getUsername();
                Log.d(TAG, "身份重置后检查: 旧=" + linphoneUser + ", 新=" + newLinphoneUser + 
                       ", 目标=" + currentUsername);
                
                // 记录确认结果
                Log.d(TAG, "身份确认完成，当前使用的SIP账号为: " + linphoneManager.getUsername());
            } else {
                Log.d(TAG, "当前SIP账号身份正确: " + currentUsername);
            }
        } else {
            Log.w(TAG, "未设置当前用户名，无法确认身份");
        }
    }
    
    /**
     * 设置麦克风是否启用
     */
    public void setMicEnabled(boolean enabled) {
        if (callType == CALL_TYPE_AUDIO || 
            (callType == CALL_TYPE_VIDEO && linphoneManager.getCallState() != CALL_STATE_IDLE)) {
            // SIP通话
            linphoneManager.setMicrophoneMuted(!enabled);
        } else {
            // WebRTC视频通话
            webRTCHelper.setMicEnabled(enabled);
        }
        
        Log.d(TAG, "麦克风已" + (enabled ? "启用" : "禁用"));
    }
    
    /**
     * 设置扬声器是否启用
     */
    public void setSpeakerEnabled(boolean enabled) {
        linphoneManager.setSpeakerEnabled(enabled);
        Log.d(TAG, "扬声器已" + (enabled ? "启用" : "禁用"));
    }
    
    /**
     * 设置视频是否启用
     */
    public void setVideoEnabled(boolean enabled) {
        if (callType == CALL_TYPE_VIDEO) {
            webRTCHelper.setVideoEnabled(enabled);
            Log.d(TAG, "视频已" + (enabled ? "启用" : "禁用"));
        }
    }
    
    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (callType == CALL_TYPE_VIDEO) {
            webRTCHelper.switchCamera();
            Log.d(TAG, "已切换摄像头");
        }
    }
    
    /**
     * 更新通话状态
     */
    private void updateCallState(int state) {
        callState = state;
        if (listener != null) {
            mainHandler.post(() -> listener.onCallStateChanged(state));
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        try {
            if (linphoneManager != null) {
                linphoneManager.release();
            }
            
            if (webRTCHelper != null) {
                webRTCHelper.close();
            }
            
            if (signalingService != null) {
                signalingService.disconnect();
            }
            
            instance = null;
        } catch (Exception e) {
            Log.e(TAG, "释放资源失败: " + e.getMessage(), e);
        }
    }
    
    // ============================== SIP回调 ==============================
    
    @Override
    public void onRegistered() {
        Log.d(TAG, "SIP注册成功");
        mainHandler.post(() -> {
            if(listener != null) {
                listener.onMessage("SIP账号已注册");
            }
        });
    }
    
    @Override
    public void onCallStateChanged(int state) {
        Log.d(TAG, "SIP通话状态变化: " + state);
        
        // 在通话状态变化时确认身份，特别是在连接和断开时
        if (state == LinphoneManager.CALL_STATE_CONNECTED) {
            Log.d(TAG, "通话已连接，确认身份...");
            ensureCorrectLoginIdentity();
            
            // 确保在通话连接后SIP账号仍然注册
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "通话连接后检查SIP注册状态...");
                if (linphoneManager != null && !linphoneManager.isRegistered()) {
                    Log.w(TAG, "通话连接后发现SIP未注册，尝试重新注册");
                    // 保存当前身份
                    final String savedUsername = currentUsername;
                    final String savedPassword = currentPassword;
                    
                    // 重新登录
                    if (savedUsername != null && !savedUsername.isEmpty()) {
                        linphoneManager.login(savedUsername, savedPassword, sipServerDomain, 
                                String.valueOf(sipServerPort), sipTransport, null);
                    }
                }
            }, 2000); // 2秒后检查
        } else if (state == LinphoneManager.CALL_STATE_ENDED) {
            Log.d(TAG, "通话已结束，确认身份...");
            ensureCorrectLoginIdentity();
            
            // 通话结束后确保SIP账号仍然注册
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "通话结束后检查SIP注册状态...");
                if (linphoneManager != null && !linphoneManager.isRegistered()) {
                    Log.w(TAG, "通话结束后发现SIP未注册，尝试重新注册");
                    // 保存当前身份
                    final String savedUsername = currentUsername;
                    final String savedPassword = currentPassword;
                    
                    // 重新登录
                    if (savedUsername != null && !savedUsername.isEmpty()) {
                        linphoneManager.login(savedUsername, savedPassword, sipServerDomain, 
                                String.valueOf(sipServerPort), sipTransport, null);
                    }
                }
            }, 1000); // 1秒后检查
        }
        
        updateCallState(state);
    }
    
    @Override
    public void onIncomingCall(String callerId, int callType) {
        Log.d(TAG, "收到来电: " + callerId + ", 类型: " + (callType == CALL_TYPE_AUDIO ? "音频" : "视频"));
        this.callType = callType;
        this.remoteUserId = callerId;
        updateCallState(CALL_STATE_RINGING);
        
        mainHandler.post(() -> {
            if(listener != null) {
                listener.onIncomingCall(callerId, callType);
            }
        });
    }
    
    @Override
    public void onError(String errorMessage) {
        Log.e(TAG, "SIP错误: " + errorMessage);
        mainHandler.post(() -> {
            if(listener != null) {
                listener.onError("SIP错误: " + errorMessage);
            }
        });
    }
    
    // ============================== WebRTC回调 ==============================
    
    @Override
    public void onLocalDescription(SessionDescription sessionDescription) {
        Log.d(TAG, "WebRTC本地描述符已创建");
        signalingService.sendSessionDescription(sessionDescription, remoteUserId);
    }
    
    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, "WebRTC ICE候选项已创建");
        signalingService.sendIceCandidate(iceCandidate, remoteUserId);
    }
    
    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG, "WebRTC ICE连接状态变化: " + iceConnectionState);
        
        if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED ||
            iceConnectionState == PeerConnection.IceConnectionState.COMPLETED) {
            updateCallState(CALL_STATE_CONNECTED);
        }
    }
    
    @Override
    public void onAddRemoteStream(MediaStream mediaStream) {
        Log.d(TAG, "WebRTC远程媒体流已添加");
        updateCallState(CALL_STATE_CONNECTED);
    }
    
    // ============================== 信令回调 ==============================
    
    @Override
    public void onConnected() {
        Log.d(TAG, "信令服务器已连接");
    }
    
    @Override
    public void onDisconnected() {
        Log.d(TAG, "信令服务器已断开");
        updateCallState(CALL_STATE_IDLE);
    }
    
    @Override
    public void onJoinedRoom(String roomId, boolean isInitiator) {
        Log.d(TAG, "已加入房间: " + roomId + ", 是否发起者: " + isInitiator);
        
        this.isInitiator = isInitiator;
        this.roomId = roomId;
        
        // 如果是发起者，创建offer
        if (isInitiator) {
            webRTCHelper.createPeerConnection();
            webRTCHelper.createOffer();
        }
    }
    
    @Override
    public void onRemoteOffer(SessionDescription sessionDescription) {
        Log.d(TAG, "收到远程offer");
        
        if (!isInitiator) {
            webRTCHelper.createPeerConnection();
            webRTCHelper.setRemoteDescription(sessionDescription);
            webRTCHelper.createAnswer();
        }
    }
    
    @Override
    public void onRemoteAnswer(SessionDescription sessionDescription) {
        Log.d(TAG, "收到远程answer");
        webRTCHelper.setRemoteDescription(sessionDescription);
    }
    
    @Override
    public void onRemoteIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, "收到远程ICE候选项");
        webRTCHelper.addRemoteIceCandidate(iceCandidate);
    }
    
    @Override
    public void onRemoteLeave(String userId) {
        Log.d(TAG, "远程用户离开: " + userId);
        
        if (callState != CALL_STATE_IDLE) {
            webRTCHelper.close();
            updateCallState(CALL_STATE_ENDED);
            
            if (listener != null) {
                mainHandler.post(() -> listener.onMessage("对方已挂断"));
            }
        }
    }
    
    /**
     * 获取通话状态
     */
    public int getCallState() {
        return callState;
    }
    
    /**
     * 获取通话类型
     */
    public int getCallType() {
        return callType;
    }
    
    /**
     * 通话管理监听器接口
     */
    public interface CallManagerListener {
        void onCallStateChanged(int state);
        void onIncomingCall(String callerId, int callType);
        void onMessage(String message);
        void onError(String errorMessage);
    }
    
    /**
     * 设置当前使用的SIP账号
     * 
     * @param username SIP用户名
     * @param password SIP密码
     * @return 是否设置成功
     */
    public boolean setCustomSipAccount(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            Log.e(TAG, "无效的SIP用户名");
            return false;
        }
        
        // 保存账号信息
        this.currentUsername = username;
        this.currentPassword = password != null && !password.isEmpty() ? password : DEFAULT_PASSWORD;
        
        Log.d(TAG, "设置当前SIP账号: " + username);
        
        // 如果已经登录了其他账号，尝试先注销
        if (linphoneManager.isRegistered() && 
            !username.equals(linphoneManager.getUsername())) {
            
            linphoneManager.logout();
            
            // 使用新账号登录
            return initializeSIP(username, sipServerDomain, this.currentPassword);
        }
        
        return true;
    }
}