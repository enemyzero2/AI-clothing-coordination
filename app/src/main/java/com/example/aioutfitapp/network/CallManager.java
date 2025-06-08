package com.example.aioutfitapp.network;

import android.content.Context;
import android.net.sip.SipAudioCall;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

/**
 * 通话管理服务
 * 
 * 集成SIP和WebRTC功能，提供统一的通话管理接口
 */
public class CallManager implements 
        com.example.aioutfitapp.network.SIPManager.SIPStateListener,
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
    private com.example.aioutfitapp.network.SIPManager sipManager;
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
    
    // 单例模式
    private static CallManager instance;
    
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
        this.sipManager = com.example.aioutfitapp.network.SIPManager.getInstance(context);
        this.webRTCHelper = new WebRTCHelper(context);
        this.signalingService = new SignalingService(context);
        this.networkSimulator = NetworkSimulator.getInstance(context);
        
        // 设置监听器
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
     * 初始化SIP服务
     */
    public boolean initializeSIP(String username, String domain, String password) {
        Log.d(TAG, "初始化SIP服务 - 用户名: " + username + ", 域名: " + domain);
        
        // 获取设备本地IP地址（实际应用中应该动态获取）
        String localIp = "192.168.1.x"; // 需要替换为实际IP
        int sipPort = 5062;
        
        // 配置SIP服务器
        sipManager.setSipServerConfig(domain, localIp, sipPort);
        
        // 初始化SIP
        return sipManager.initialize(username, domain, password, this);
    }
    
    /**
     * 使用默认测试账号初始化SIP服务
     */
    public boolean initializeDefaultSIP() {
        String sipServerAddress = sipManager.getSipServerAddress();
        String sipDomain = sipManager.getSipDomain();
        Log.d(TAG, "使用默认测试账号初始化SIP服务 - 服务器地址: " + sipServerAddress + ", 域名: " + sipDomain);
        return sipManager.initialize(this);
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
        
        sipManager.makeCall(sipAddress, new SipAudioCall.Listener() {
            @Override
            public void onCallEstablished(SipAudioCall call) {
                super.onCallEstablished(call);
                Log.d(TAG, "SIP通话已建立");
                updateCallState(CALL_STATE_CONNECTED);
            }
            
            @Override
            public void onCallEnded(SipAudioCall call) {
                super.onCallEnded(call);
                Log.d(TAG, "SIP通话已结束");
                updateCallState(CALL_STATE_ENDED);
            }
            
            @Override
            public void onError(SipAudioCall call, int errorCode, String errorMessage) {
                super.onError(call, errorCode, errorMessage);
                Log.e(TAG, "SIP音频通话错误: " + errorMessage);
                updateCallState(CALL_STATE_ENDED);
            }
        });
    }
    
    /**
     * 发起音频通话到测试用户
     */
    public void makeAudioCallToTestUser(String username) {
        Log.d(TAG, "发起通话到测试用户: " + username);
        sipManager.makeCallToTestUser(username, new SipAudioCall.Listener() {
            @Override
            public void onCallEstablished(SipAudioCall call) {
                super.onCallEstablished(call);
                Log.d(TAG, "通话到测试用户已建立");
                updateCallState(CALL_STATE_CONNECTED);
            }
        });
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
            // 接听音频通话
            // 这里应该有接听SIP通话的代码，但在这个例子中我们留空
        } else {
            // 接听视频通话
            // 首先创建PeerConnection
            webRTCHelper.createPeerConnection();
            
            // 创建应答
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
        
        endCall();
    }
    
    /**
     * 结束通话
     */
    public void endCall() {
        if (callState == CALL_STATE_IDLE) {
            return;
        }
        
        if (callType == CALL_TYPE_AUDIO) {
            // 结束音频通话
            sipManager.endCall();
        } else {
            // 结束视频通话
            signalingService.leaveRoom();
            signalingService.disconnect();
            webRTCHelper.release();
        }
        
        updateCallState(CALL_STATE_ENDED);
    }
    
    /**
     * 设置麦克风状态
     */
    public void setMicEnabled(boolean enabled) {
        if (callType == CALL_TYPE_AUDIO) {
            // 设置SIP音频通话麦克风状态
            // 在这个例子中我们留空
        } else {
            // 设置WebRTC视频通话麦克风状态
            webRTCHelper.setMicEnabled(enabled);
        }
    }
    
    /**
     * 设置扬声器状态
     */
    public void setSpeakerEnabled(boolean enabled) {
        // 在这个例子中我们留空
    }
    
    /**
     * 设置视频状态
     */
    public void setVideoEnabled(boolean enabled) {
        if (callType == CALL_TYPE_VIDEO) {
            webRTCHelper.setVideoEnabled(enabled);
        }
    }
    
    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (callType == CALL_TYPE_VIDEO) {
            webRTCHelper.switchCamera();
        }
    }
    
    /**
     * 更新通话状态
     */
    private void updateCallState(int state) {
        callState = state;
        
        if (listener != null) {
            mainHandler.post(() -> listener.onCallStateChanged(callState));
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        // 结束当前通话
        if (callState != CALL_STATE_IDLE && callState != CALL_STATE_ENDED) {
            endCall();
        }
        
        // 释放资源
        sipManager.close();
        webRTCHelper.release();
        signalingService.disconnect();
        networkSimulator.stopSimulation();
    }
    
    // SIPManager.SIPStateListener 接口实现
    
    @Override
    public void onRegistering() {
        if (listener != null) {
            mainHandler.post(() -> listener.onMessage("正在注册SIP账号..."));
        }
    }
    
    @Override
    public void onRegistered() {
        if (listener != null) {
            mainHandler.post(() -> listener.onMessage("SIP账号注册成功"));
        }
    }
    
    @Override
    public void onCallEstablished() {
        updateCallState(CALL_STATE_CONNECTED);
    }
    
    @Override
    public void onCallEnded() {
        updateCallState(CALL_STATE_ENDED);
    }
    
    @Override
    public void onIncomingCall(SipAudioCall call) {
        // 收到SIP来电
        callType = CALL_TYPE_AUDIO;
        updateCallState(CALL_STATE_RINGING);
        
        if (listener != null) {
            // 假设呼叫方ID是从SIP URI中提取的
            String callerId = call.getPeerProfile().getUriString();
            mainHandler.post(() -> listener.onIncomingCall(callerId, CALL_TYPE_AUDIO));
        }
    }
    
    @Override
    public void onError(String errorMessage) {
        if (listener != null) {
            mainHandler.post(() -> listener.onError(errorMessage));
        }
    }
    
    // WebRTCHelper.WebRTCHelperListener 接口实现
    
    @Override
    public void onLocalDescription(SessionDescription sessionDescription) {
        if (sessionDescription.type == SessionDescription.Type.OFFER) {
            signalingService.sendOffer(sessionDescription);
        } else if (sessionDescription.type == SessionDescription.Type.ANSWER) {
            signalingService.sendAnswer(sessionDescription);
        }
    }
    
    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        signalingService.sendIceCandidate(iceCandidate);
    }
    
    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
            updateCallState(CALL_STATE_CONNECTED);
        } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED ||
                iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
            updateCallState(CALL_STATE_ENDED);
        }
    }
    
    @Override
    public void onAddRemoteStream(MediaStream mediaStream) {
        // 已经在WebRTCHelper中处理了添加远程视频轨道到渲染器
    }
    
    // SignalingService.SignalingListener 接口实现
    
    @Override
    public void onConnected() {
        if (listener != null) {
            mainHandler.post(() -> listener.onMessage("已连接到信令服务器"));
        }
    }
    
    @Override
    public void onDisconnected() {
        if (listener != null) {
            mainHandler.post(() -> listener.onMessage("已断开与信令服务器的连接"));
        }
    }
    
    @Override
    public void onJoinedRoom(String roomId, boolean isInitiator) {
        this.isInitiator = isInitiator;
        
        if (listener != null) {
            mainHandler.post(() -> listener.onMessage("已加入房间: " + roomId));
        }
        
        // 如果是发起方，则创建提议
        if (isInitiator) {
            // 创建PeerConnection
            webRTCHelper.createPeerConnection();
            
            // 创建提议
            webRTCHelper.createOffer();
        }
    }
    
    @Override
    public void onRemoteOffer(SessionDescription sessionDescription) {
        // 收到对方的提议
        // 更新状态为振铃，通知UI有来电
        callType = CALL_TYPE_VIDEO;
        updateCallState(CALL_STATE_RINGING);
        
        if (listener != null) {
            mainHandler.post(() -> listener.onIncomingCall(remoteUserId, CALL_TYPE_VIDEO));
        }
        
        // 设置远程描述
        webRTCHelper.setRemoteDescription(sessionDescription);
    }
    
    @Override
    public void onRemoteAnswer(SessionDescription sessionDescription) {
        // 收到对方的应答
        webRTCHelper.setRemoteDescription(sessionDescription);
    }
    
    @Override
    public void onRemoteIceCandidate(IceCandidate iceCandidate) {
        // 收到对方的ICE候选
        webRTCHelper.addIceCandidate(iceCandidate);
    }
    
    @Override
    public void onRemoteLeave(String userId) {
        // 对方离开了通话
        updateCallState(CALL_STATE_ENDED);
        
        if (listener != null) {
            mainHandler.post(() -> listener.onMessage("对方已结束通话"));
        }
    }
    
    /**
     * 获取当前通话状态
     */
    public int getCallState() {
        return callState;
    }
    
    /**
     * 获取当前通话类型
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