package com.example.aioutfitapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aioutfitapp.network.LinphoneManager;
import com.example.aioutfitapp.network.NetworkSimulator;

import org.webrtc.SurfaceViewRenderer;

/**
 * 通话活动界面
 * 
 * 负责显示音视频通话界面，处理通话相关的用户交互
 */
public class CallActivity extends AppCompatActivity implements LinphoneManager.LinphoneManagerListener {
    
    private static final String TAG = "CallActivity";
    
    // Intent传递的参数键
    public static final String EXTRA_CALL_TYPE = "call_type";
    public static final String EXTRA_CALLER_ID = "caller_id";
    public static final String EXTRA_IS_INCOMING = "is_incoming";
    public static final String EXTRA_ROOM_ID = "room_id";
    
    // 权限请求码
    private static final int REQUEST_PERMISSIONS = 1;
    
    // 所需权限
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };
    
    // 通话管理器
    private LinphoneManager linphoneManager;
    
    // UI组件
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private ImageButton endCallButton;
    private ImageButton muteButton;
    private ImageButton speakerButton;
    private ImageButton switchCameraButton;
    private ImageButton videoToggleButton;
    private Chronometer callDurationView;
    private TextView callerNameView;
    private TextView callStateView;
    private LinearLayout incomingCallControls;
    private LinearLayout ongoingCallControls;
    private LinearLayout videoCallControls;
    private ImageView callerAvatarView;
    private TextView networkQualityView;
    
    // 通话参数
    private int callType;
    private String callerId;
    private boolean isIncoming;
    private String roomId;
    
    // 控制状态
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isVideoEnabled = true;
    
    // 网络质量检测定时器
    private Handler networkQualityHandler;
    private Runnable networkQualityRunnable;
    
    /**
     * 创建启动通话活动的Intent
     */
    public static Intent createOutgoingCallIntent(Context context, int callType, String callerId, String roomId) {
        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra(EXTRA_CALL_TYPE, callType);
        intent.putExtra(EXTRA_CALLER_ID, callerId);
        intent.putExtra(EXTRA_IS_INCOMING, false);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        return intent;
    }
    
    /**
     * 创建接收来电的Intent
     */
    public static Intent createIncomingCallIntent(Context context, int callType, String callerId, String roomId) {
        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra(EXTRA_CALL_TYPE, callType);
        intent.putExtra(EXTRA_CALLER_ID, callerId);
        intent.putExtra(EXTRA_IS_INCOMING, true);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏和保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_call);
        
        // 获取Intent参数
        Intent intent = getIntent();
        callType = intent.getIntExtra(EXTRA_CALL_TYPE, LinphoneManager.CALL_TYPE_AUDIO);
        callerId = intent.getStringExtra(EXTRA_CALLER_ID);
        isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false);
        roomId = intent.getStringExtra(EXTRA_ROOM_ID);
        
        // 初始化UI组件
        initViews();
        
        // 检查权限
        if (checkPermissions()) {
            initCallManager();
        }
        
        // 初始化网络质量检测
        initNetworkQualityChecker();
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        // 查找视图
        localVideoView = findViewById(R.id.local_video_view);
        remoteVideoView = findViewById(R.id.remote_video_view);
        endCallButton = findViewById(R.id.end_call_button);
        muteButton = findViewById(R.id.mute_button);
        speakerButton = findViewById(R.id.speaker_button);
        switchCameraButton = findViewById(R.id.switch_camera_button);
        videoToggleButton = findViewById(R.id.video_toggle_button);
        callDurationView = findViewById(R.id.call_duration);
        callerNameView = findViewById(R.id.caller_name);
        callStateView = findViewById(R.id.call_state);
        incomingCallControls = findViewById(R.id.incoming_call_controls);
        ongoingCallControls = findViewById(R.id.ongoing_call_controls);
        videoCallControls = findViewById(R.id.video_call_controls);
        callerAvatarView = findViewById(R.id.caller_avatar);
        networkQualityView = findViewById(R.id.network_quality);
        
        // 设置来电者信息
        callerNameView.setText(callerId);
        
        // 设置按钮点击事件
        findViewById(R.id.answer_call_button).setOnClickListener(v -> onAnswerCall());
        findViewById(R.id.reject_call_button).setOnClickListener(v -> onRejectCall());
        endCallButton.setOnClickListener(v -> onEndCall());
        muteButton.setOnClickListener(v -> onToggleMute());
        speakerButton.setOnClickListener(v -> onToggleSpeaker());
        switchCameraButton.setOnClickListener(v -> onSwitchCamera());
        videoToggleButton.setOnClickListener(v -> onToggleVideo());
        
        // 根据通话类型显示/隐藏视频控件
        if (callType == LinphoneManager.CALL_TYPE_VIDEO) {
            localVideoView.setVisibility(View.VISIBLE);
            remoteVideoView.setVisibility(View.VISIBLE);
            videoCallControls.setVisibility(View.VISIBLE);
            callerAvatarView.setVisibility(View.GONE);
        } else {
            localVideoView.setVisibility(View.GONE);
            remoteVideoView.setVisibility(View.GONE);
            videoCallControls.setVisibility(View.GONE);
            callerAvatarView.setVisibility(View.VISIBLE);
        }
        
        // 根据是否是来电显示不同的控制区
        if (isIncoming) {
            incomingCallControls.setVisibility(View.VISIBLE);
            ongoingCallControls.setVisibility(View.GONE);
            callStateView.setText("来电");
        } else {
            incomingCallControls.setVisibility(View.GONE);
            ongoingCallControls.setVisibility(View.VISIBLE);
            callStateView.setText("正在拨号...");
        }
    }
    
    /**
     * 初始化通话管理器
     */
    private void initCallManager() {
        // 获取LinphoneManager实例
        linphoneManager = LinphoneManager.getInstance();
        
        Log.d(TAG, "初始化通话界面 - 类型: " + (callType == LinphoneManager.CALL_TYPE_VIDEO ? "视频通话" : "音频通话") + 
                ", 来电者: " + callerId + ", 是否来电: " + isIncoming);
        
        // 设置监听器
        linphoneManager.setListener(this);
        
        // 根据通话类型初始化视频视图
        if (callType == LinphoneManager.CALL_TYPE_VIDEO) {
            try {
                // 初始化视频视图
                localVideoView.init(null, null);
                remoteVideoView.init(null, null);
                
                Log.d(TAG, "初始化视频视图成功");
            } catch (Exception e) {
                Log.e(TAG, "初始化视频视图失败: " + e.getMessage(), e);
                Toast.makeText(this, "初始化视频失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        
        // 处理来电或拨出电话
        if (isIncoming) {
            // 对于来电，不需要额外操作，等待用户接听或拒绝
            Log.d(TAG, "等待用户接听或拒绝来电");
        } else {
            Log.d(TAG, "正在发起呼叫到: " + callerId);
            
            // 检查是否注册到SIP服务器
            if (!linphoneManager.isRegistered()) {
                Log.e(TAG, "未注册到SIP服务器，无法发起呼叫");
                Toast.makeText(this, "未注册到SIP服务器，请先登录", Toast.LENGTH_LONG).show();
                new Handler().postDelayed(this::finish, 2000);
                return;
            }
            
            // 发起呼叫
            if (callType == LinphoneManager.CALL_TYPE_VIDEO) {
                linphoneManager.makeVideoCall(callerId);
            } else {
                linphoneManager.makeAudioCall(callerId);
            }
        }
    }
    
    /**
     * 初始化网络质量检测
     */
    private void initNetworkQualityChecker() {
        networkQualityHandler = new Handler();
        networkQualityRunnable = new Runnable() {
            @Override
            public void run() {
                updateNetworkQualityIndicator();
                networkQualityHandler.postDelayed(this, 2000); // 每2秒更新一次
            }
        };
        
        // 开始定时检测
        networkQualityHandler.postDelayed(networkQualityRunnable, 2000);
    }
    
    /**
     * 更新网络质量指示器
     */
    private void updateNetworkQualityIndicator() {
        NetworkSimulator networkSimulator = NetworkSimulator.getInstance(this);
        int networkType = networkSimulator.getCurrentNetworkType();
        int bandwidth = networkSimulator.getBandwidth();
        int latency = networkSimulator.getLatency();
        float packetLoss = networkSimulator.getPacketLoss();
        
        String networkTypeStr = networkType == NetworkSimulator.NETWORK_TYPE_4G ? "4G" : "5G";
        String qualityText = String.format("%s: %dMbps, %dms, %.1f%%丢包", 
                networkTypeStr, bandwidth, latency, packetLoss);
        
        networkQualityView.setText(qualityText);
        
        // 根据网络质量设置颜色指示
        if (packetLoss > 1.0f || latency > 70) {
            // 网络质量差
            networkQualityView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        } else if (packetLoss > 0.5f || latency > 40) {
            // 网络质量一般
            networkQualityView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light));
        } else {
            // 网络质量好
            networkQualityView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
        }
    }
    
    /**
     * 检查权限
     */
    private boolean checkPermissions() {
        boolean allGranted = true;
        
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS);
            return false;
        }
        
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                initCallManager();
            } else {
                Toast.makeText(this, "需要授予必要权限才能进行通话", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    /**
     * 接听电话
     */
    private void onAnswerCall() {
        Log.d(TAG, "接听来电");
        if (linphoneManager != null) {
            linphoneManager.answerCall();
        }
    }
    
    /**
     * 拒绝电话
     */
    private void onRejectCall() {
        Log.d(TAG, "拒绝来电");
        if (linphoneManager != null) {
            linphoneManager.rejectCall();
        }
        finish();
    }
    
    /**
     * 结束通话
     */
    private void onEndCall() {
        Log.d(TAG, "结束通话");
        if (linphoneManager != null) {
            linphoneManager.endCall();
        }
    }
    
    /**
     * 切换静音状态
     */
    private void onToggleMute() {
        isMuted = !isMuted;
        Log.d(TAG, "麦克风: " + (isMuted ? "已静音" : "已取消静音"));
        
        if (linphoneManager != null) {
            linphoneManager.setMicrophoneMuted(isMuted);
        }
        
        // 更新UI
        muteButton.setImageResource(isMuted ? 
                R.drawable.ic_mic_off : R.drawable.ic_mic_on);
    }
    
    /**
     * 切换扬声器状态
     */
    private void onToggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        Log.d(TAG, "扬声器: " + (isSpeakerOn ? "已开启" : "已关闭"));
        
        if (linphoneManager != null) {
            linphoneManager.setSpeakerEnabled(isSpeakerOn);
        }
        
        // 更新UI
        speakerButton.setImageResource(isSpeakerOn ? 
                R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
    }
    
    /**
     * 切换摄像头
     */
    private void onSwitchCamera() {
        Log.d(TAG, "切换摄像头");
        if (linphoneManager != null) {
            linphoneManager.switchCamera();
        }
    }
    
    /**
     * 切换视频状态
     */
    private void onToggleVideo() {
        isVideoEnabled = !isVideoEnabled;
        Log.d(TAG, "视频: " + (isVideoEnabled ? "已启用" : "已禁用"));
        
        if (linphoneManager != null) {
            linphoneManager.setVideoEnabled(isVideoEnabled);
        }
        
        // 更新UI
        videoToggleButton.setImageResource(isVideoEnabled ? 
                R.drawable.ic_video_on : R.drawable.ic_video_off);
        
        // 显示或隐藏视频视图
        if (isVideoEnabled) {
            localVideoView.setVisibility(View.VISIBLE);
            remoteVideoView.setVisibility(View.VISIBLE);
            callerAvatarView.setVisibility(View.GONE);
        } else {
            localVideoView.setVisibility(View.GONE);
            remoteVideoView.setVisibility(View.GONE);
            callerAvatarView.setVisibility(View.VISIBLE);
        }
        
        // 开始网络质量监测
        if (networkQualityHandler != null && networkQualityRunnable != null) {
            networkQualityHandler.post(networkQualityRunnable);
        }
    }
    
    // LinphoneManager.LinphoneManagerListener 接口实现
    
    @Override
    public void onCallStateChanged(int state) {
        runOnUiThread(() -> {
            // 更新通话状态UI
            switch (state) {
                case LinphoneManager.CALL_STATE_CONNECTING:
                    callStateView.setText("正在连接...");
                    Log.d(TAG, "通话状态: 正在连接");
                    break;
                case LinphoneManager.CALL_STATE_RINGING:
                    callStateView.setText("振铃中...");
                    Log.d(TAG, "通话状态: 振铃中");
                    break;
                case LinphoneManager.CALL_STATE_CONNECTED:
                    callStateView.setText("通话中");
                    Log.d(TAG, "通话状态: 已连接");
                    
                    // 显示通话控制按钮
                    incomingCallControls.setVisibility(View.GONE);
                    ongoingCallControls.setVisibility(View.VISIBLE);
                    
                    // 启动计时器
                    callDurationView.setVisibility(View.VISIBLE);
                    callDurationView.setBase(SystemClock.elapsedRealtime());
                    callDurationView.start();
                    
                    // 如果是视频通话，设置视频视图
                    if (callType == LinphoneManager.CALL_TYPE_VIDEO && linphoneManager.isVideoEnabled()) {
                        localVideoView.setVisibility(View.VISIBLE);
                        remoteVideoView.setVisibility(View.VISIBLE);
                        videoCallControls.setVisibility(View.VISIBLE);
                        callerAvatarView.setVisibility(View.GONE);
                        
                        // 设置视频视图
                        linphoneManager.setVideoWindows(localVideoView, remoteVideoView);
                    }
                    
                    // 开始网络质量监测
                    if (networkQualityHandler != null && networkQualityRunnable != null) {
                        networkQualityHandler.post(networkQualityRunnable);
                    }
                    break;
                case LinphoneManager.CALL_STATE_ENDED:
                    callStateView.setText("通话已结束");
                    Log.d(TAG, "通话状态: 已结束");
                    
                    // 停止计时器
                    callDurationView.stop();
                    
                    // 停止网络质量监测
                    if (networkQualityHandler != null && networkQualityRunnable != null) {
                        networkQualityHandler.removeCallbacks(networkQualityRunnable);
                    }
                    
                    // 延迟关闭活动
                    new Handler().postDelayed(this::finish, 1500);
                    break;
            }
        });
    }
    
    @Override
    public void onIncomingCall(String callerId, int callType) {
        // 在已开启的CallActivity中不需要处理
        Log.d(TAG, "收到来电: " + callerId + ", 类型: " + callType);
    }
    
    @Override
    public void onRegistered() {
        Log.d(TAG, "SIP注册成功");
        Toast.makeText(this, "SIP注册成功", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onError(String errorMessage) {
        Log.e(TAG, "错误: " + errorMessage);
        Toast.makeText(this, "错误: " + errorMessage, Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 停止网络质量检测
        if (networkQualityHandler != null && networkQualityRunnable != null) {
            networkQualityHandler.removeCallbacks(networkQualityRunnable);
        }
        
        // 释放资源
        if (linphoneManager != null) {
            linphoneManager.release();
        }
    }
}