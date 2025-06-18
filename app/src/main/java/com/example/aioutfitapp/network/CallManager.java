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
    private LinphoneManager linphoneManager;
    private WebRTCHelper webRTCHelper;
    private SignalingService signalingService;
    private NetworkSimulator networkSimulator;
    
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
    
    // FreeSwitch服务器配置
    private String sipServerDomain = "10.29.206.148"; 
    private String sipServerAddress = "10.29.206.148";
    private int sipServerPort = 5060;               // FreeSwitch默认SIP端口
    private String sipTransport = "UDP";            // FreeSwitch传输协议
    
    // FreeSwitch账号配置
    private static final int MIN_ACCOUNT_NUMBER = 1001;
    private static final int MAX_ACCOUNT_NUMBER = 1010;
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
     */
    public List<String> getAvailableSipAccounts() {
        List<String> accounts = new ArrayList<>();
        for (int i = MIN_ACCOUNT_NUMBER; i <= MAX_ACCOUNT_NUMBER; i++) {
            accounts.add(String.valueOf(i));
        }
        return accounts;
    }
    
    /**
     * 获取当前使用的SIP账号
     */
    public String getCurrentSipAccount() {
        int accountNumber = MIN_ACCOUNT_NUMBER + currentAccountIndex;
        if (accountNumber > MAX_ACCOUNT_NUMBER) {
            accountNumber = MIN_ACCOUNT_NUMBER;
        }
        return String.valueOf(accountNumber);
    }
    
    /**
     * 设置当前使用的SIP账号索引
     */
    public void setCurrentAccountIndex(int index) {
        if (index >= 0 && index < (MAX_ACCOUNT_NUMBER - MIN_ACCOUNT_NUMBER + 1)) {
            this.currentAccountIndex = index;
            Log.d(TAG, "当前SIP账号设置为: " + getCurrentSipAccount());
        } else {
            Log.e(TAG, "无效的账号索引: " + index);
        }
    }
    
    /**
     * 初始化SIP服务
     */
    public boolean initializeSIP(String username, String domain, String password) {
        Log.d(TAG, "初始化SIP服务 - 用户名: " + username + ", 域名: " + domain);
        
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
     * 使用指定的FreeSwitch账号初始化SIP服务
     */
    public boolean initializeSipWithAccount(String accountNumber) {
        if (!isValidAccount(accountNumber)) {
            Log.e(TAG, "无效的账号: " + accountNumber);
            return false;
        }
        
        String username = accountNumber;
        String password = DEFAULT_PASSWORD;
        Log.d(TAG, "使用FreeSwitch账号初始化SIP服务 - 用户名: " + username + ", 服务器地址: " + sipServerAddress);
        
        return initializeSIP(username, sipServerDomain, password);
    }
    
    /**
     * 检查账号是否有效
     */
    private boolean isValidAccount(String accountNumber) {
        try {
            int account = Integer.parseInt(accountNumber);
            return account >= MIN_ACCOUNT_NUMBER && account <= MAX_ACCOUNT_NUMBER;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 使用默认测试账号初始化SIP服务
     */
    public boolean initializeDefaultSIP() {
        String username = getCurrentSipAccount();
        String password = DEFAULT_PASSWORD;
        Log.d(TAG, "使用FreeSwitch账号初始化SIP服务 - 用户名: " + username + ", 服务器地址: " + sipServerAddress);
        
        return initializeSIP(username, sipServerDomain, password);
    }
    
    /**
     * 切换到下一个SIP账号
     */
    public String switchToNextAccount() {
        currentAccountIndex = (currentAccountIndex + 1) % (MAX_ACCOUNT_NUMBER - MIN_ACCOUNT_NUMBER + 1);
        String newAccount = getCurrentSipAccount();
        Log.d(TAG, "切换到下一个SIP账号: " + newAccount);
        return newAccount;
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
     * 获取所有可用账号及其状态
     */
    public String getAllAccountsInfo() {
        StringBuilder info = new StringBuilder();
        info.append("FreeSWITCH可用账号(").append(MIN_ACCOUNT_NUMBER).append("-").append(MAX_ACCOUNT_NUMBER).append("):\n");
        
        for (int i = MIN_ACCOUNT_NUMBER; i <= MAX_ACCOUNT_NUMBER; i++) {
            info.append(i).append(" - ").append("密码: ").append(DEFAULT_PASSWORD);
            if (String.valueOf(i).equals(getCurrentSipAccount())) {
                info.append(" [当前账号]");
            }
            info.append("\n");
        }
        
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
        
        if (callType == CALL_TYPE_AUDIO) {
            // 接听SIP音频通话
            linphoneManager.answerCall();
        } else {
            // 接听视频通话
            // 首先创建PeerConnection
            webRTCHelper.createPeerConnection();
            
            // 创建应答
            webRTCHelper.createAnswer();
        }
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
        
        if (callType == CALL_TYPE_AUDIO || 
            (callType == CALL_TYPE_VIDEO && linphoneManager.getCallState() != CALL_STATE_IDLE)) {
            // 结束SIP通话
            linphoneManager.endCall();
        } else {
            // 结束WebRTC视频通话
            webRTCHelper.close();
            signalingService.sendLeave(remoteUserId);
        }
        
        updateCallState(CALL_STATE_IDLE);
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
}