package com.example.aioutfitapp.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import com.example.aioutfitapp.api.ApiClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * 信令服务
 * 
 * 用于处理WebRTC信令交换，包括SDP交换和ICE候选交换
 */
public class SignalingService {
    
    private static final String TAG = "SignalingService";
    
    // WebSocket相关
    private WebSocket webSocket;
    private OkHttpClient client;
    private SignalingListener listener;
    
    // 上下文和处理器
    private final Context context;
    private final Handler mainHandler;
    
    // 连接状态
    private boolean isConnected = false;
    
    // 用户信息
    private String userId;
    private String roomId;
    
    // 网络模拟器，用于模拟网络条件
    private NetworkSimulator networkSimulator;
    
    /**
     * 构造函数
     */
    public SignalingService(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.networkSimulator = NetworkSimulator.getInstance(context);
    }
    
    /**
     * 设置监听器
     */
    public void setListener(SignalingListener listener) {
        this.listener = listener;
    }
    
    /**
     * 连接到信令服务器
     */
    public void connect(String serverUrl, String userId, String roomId) {
        if (isConnected) {
            Log.d(TAG, "已经连接到信令服务器");
            return;
        }
        
        this.userId = userId;
        this.roomId = roomId;
        
        // 创建OkHttpClient，设置超时时间
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);
        
        client = builder.build();
        
        try {
            // 创建WebSocket请求，添加JWT认证头
            Request.Builder requestBuilder = new Request.Builder()
                    .url(serverUrl);
            
            // 从ApiClient获取JWT认证令牌并添加到请求头
            String authToken = ApiClient.getAuthToken();
            if (authToken != null && !authToken.isEmpty()) {
                Log.d(TAG, "添加JWT认证头到WebSocket连接");
                requestBuilder.header("Authorization", "Bearer " + authToken);
            } else {
                Log.w(TAG, "未找到JWT认证令牌，WebSocket连接可能会被拒绝");
            }
            
            // 创建最终请求
            Request request = requestBuilder.build();
            
            // 连接WebSocket
            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    Log.d(TAG, "WebSocket连接成功");
                    isConnected = true;
                    
                    // 模拟网络延迟
                    networkSimulator.simulateLatency(() -> {
                        // 发送加入房间消息
                        try {
                            JSONObject message = new JSONObject();
                            message.put("type", "join");
                            message.put("userId", userId);
                            message.put("roomId", roomId);
                            
                            sendMessage(message);
                            
                            if (listener != null) {
                                mainHandler.post(() -> listener.onConnected());
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "创建加入消息失败: " + e.getMessage());
                        }
                    });
                }
                
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    Log.d(TAG, "收到WebSocket消息: " + text);
                    
                    // 模拟丢包
                    if (networkSimulator.simulatePacketLoss()) {
                        Log.d(TAG, "模拟丢包，丢弃消息");
                        return;
                    }
                    
                    // 模拟网络延迟
                    networkSimulator.simulateLatency(() -> {
                        try {
                            JSONObject message = new JSONObject(text);
                            String type = message.getString("type");
                            
                            switch (type) {
                                case "joined":
                                    handleJoinedMessage(message);
                                    break;
                                case "offer":
                                    handleOfferMessage(message);
                                    break;
                                case "answer":
                                    handleAnswerMessage(message);
                                    break;
                                case "candidate":
                                    handleCandidateMessage(message);
                                    break;
                                case "leave":
                                    handleLeaveMessage(message);
                                    break;
                                default:
                                    Log.d(TAG, "未知消息类型: " + type);
                                    break;
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "解析消息失败: " + e.getMessage());
                        }
                    });
                }
                
                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    Log.d(TAG, "收到WebSocket二进制消息");
                }
                
                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    Log.d(TAG, "WebSocket正在关闭: " + code + ", " + reason);
                    webSocket.close(1000, null);
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    Log.d(TAG, "WebSocket已关闭: " + code + ", " + reason);
                    isConnected = false;
                    
                    if (listener != null) {
                        mainHandler.post(() -> listener.onDisconnected());
                    }
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    if (response != null) {
                        Log.e(TAG, "WebSocket连接失败: " + t.getMessage() + ", 状态码: " + response.code());
                    } else {
                        Log.e(TAG, "WebSocket连接失败: " + t.getMessage());
                    }
                    isConnected = false;
                    
                    if (listener != null) {
                        // 创建最终变量以在lambda表达式中使用
                        final String errorMsg;
                        if (response != null && response.code() == 401) {
                            errorMsg = "WebSocket连接失败: 认证失败，请确保您已登录";
                        } else if (t != null) {
                            errorMsg = "WebSocket连接失败: " + t.getMessage();
                        } else {
                            errorMsg = "WebSocket连接失败";
                        }
                        mainHandler.post(() -> listener.onError(errorMsg));
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "连接信令服务器失败: " + e.getMessage());
            if (listener != null) {
                listener.onError("连接信令服务器失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理加入房间成功消息
     */
    private void handleJoinedMessage(JSONObject message) throws JSONException {
        String roomId = message.getString("roomId");
        boolean isInitiator = message.getBoolean("isInitiator");
        
        if (listener != null) {
            mainHandler.post(() -> listener.onJoinedRoom(roomId, isInitiator));
        }
    }
    
    /**
     * 处理提议消息
     */
    private void handleOfferMessage(JSONObject message) throws JSONException {
        String sdp = message.getString("sdp");
        SessionDescription sessionDescription = new SessionDescription(
                SessionDescription.Type.OFFER, sdp);
        
        if (listener != null) {
            mainHandler.post(() -> listener.onRemoteOffer(sessionDescription));
        }
    }
    
    /**
     * 处理应答消息
     */
    private void handleAnswerMessage(JSONObject message) throws JSONException {
        String sdp = message.getString("sdp");
        SessionDescription sessionDescription = new SessionDescription(
                SessionDescription.Type.ANSWER, sdp);
        
        if (listener != null) {
            mainHandler.post(() -> listener.onRemoteAnswer(sessionDescription));
        }
    }
    
    /**
     * 处理ICE候选消息
     */
    private void handleCandidateMessage(JSONObject message) throws JSONException {
        String sdpMid = message.getString("sdpMid");
        int sdpMLineIndex = message.getInt("sdpMLineIndex");
        String sdp = message.getString("candidate");
        
        IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
        
        if (listener != null) {
            mainHandler.post(() -> listener.onRemoteIceCandidate(iceCandidate));
        }
    }
    
    /**
     * 处理离开房间消息
     */
    private void handleLeaveMessage(JSONObject message) throws JSONException {
        String userId = message.getString("userId");
        
        if (listener != null) {
            mainHandler.post(() -> listener.onRemoteLeave(userId));
        }
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(JSONObject message) {
        if (!isConnected || webSocket == null) {
            Log.e(TAG, "WebSocket未连接，无法发送消息");
            return;
        }
        
        // 模拟丢包
        if (networkSimulator.simulatePacketLoss()) {
            Log.d(TAG, "模拟丢包，丢弃发送消息: " + message.toString());
            return;
        }
        
        // 模拟网络延迟
        networkSimulator.simulateLatency(() -> {
            webSocket.send(message.toString());
            Log.d(TAG, "发送WebSocket消息: " + message.toString());
        });
    }
    
    /**
     * 发送提议
     */
    public void sendOffer(SessionDescription sessionDescription) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "offer");
            message.put("sdp", sessionDescription.description);
            message.put("userId", userId);
            message.put("roomId", roomId);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "创建提议消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送应答
     */
    public void sendAnswer(SessionDescription sessionDescription) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "answer");
            message.put("sdp", sessionDescription.description);
            message.put("userId", userId);
            message.put("roomId", roomId);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "创建应答消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送ICE候选
     */
    public void sendIceCandidate(IceCandidate iceCandidate) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "candidate");
            message.put("sdpMid", iceCandidate.sdpMid);
            message.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            message.put("candidate", iceCandidate.sdp);
            message.put("userId", userId);
            message.put("roomId", roomId);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "创建ICE候选消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 离开房间
     */
    public void leaveRoom() {
        if (!isConnected || webSocket == null) {
            return;
        }
        
        try {
            JSONObject message = new JSONObject();
            message.put("type", "leave");
            message.put("userId", userId);
            message.put("roomId", roomId);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "创建离开消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 关闭连接
     */
    public void disconnect() {
        if (webSocket != null) {
            // 先离开房间
            leaveRoom();
            
            // 关闭WebSocket
            webSocket.close(1000, "正常关闭");
            webSocket = null;
        }
        
        if (client != null) {
            // 关闭客户端
            client.dispatcher().executorService().shutdown();
            client = null;
        }
        
        isConnected = false;
    }
    
    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * 信令服务监听器接口
     */
    public interface SignalingListener {
        void onConnected();
        void onDisconnected();
        void onJoinedRoom(String roomId, boolean isInitiator);
        void onRemoteOffer(SessionDescription sessionDescription);
        void onRemoteAnswer(SessionDescription sessionDescription);
        void onRemoteIceCandidate(IceCandidate iceCandidate);
        void onRemoteLeave(String userId);
        void onError(String errorMessage);
    }
}