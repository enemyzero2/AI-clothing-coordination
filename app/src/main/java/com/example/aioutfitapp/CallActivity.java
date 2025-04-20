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

import com.example.aioutfitapp.R;
import com.example.aioutfitapp.network.CallManager;
import com.example.aioutfitapp.network.NetworkSimulator;

import org.webrtc.SurfaceViewRenderer;

/**
 * 通话活动界面
 * 
 * 负责显示音视频通话界面，处理通话相关的用户交互
 */
public class CallActivity extends AppCompatActivity implements CallManager.CallManagerListener {
    
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
            Manifest.permission.USE_SIP,
            Manifest.permission.INTERNET
    };
    
    // 通话管理器
    private CallManager callManager;
    
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
        callType = intent.getIntExtra(EXTRA_CALL_TYPE, CallManager.CALL_TYPE_AUDIO);
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
        if (callType == CallManager.CALL_TYPE_VIDEO) {
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
        try {
            callManager = CallManager.getInstance(this);
            callManager.setListener(this);
            
            // 初始化网络模拟器，使用4G网络
            callManager.startNetworkSimulation(NetworkSimulator.NETWORK_TYPE_4G);
            
            if (callType == CallManager.CALL_TYPE_VIDEO) {
                try {
                    // 初始化WebRTC
                    if (!callManager.initializeWebRTC(localVideoView, remoteVideoView)) {
                        Log.e(TAG, "初始化WebRTC失败");
                        Toast.makeText(this, "初始化WebRTC失败", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    
                    // 如果是拨出电话，开始拨号
                    if (!isIncoming) {
                        callManager.makeVideoCall(callerId, roomId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "WebRTC初始化或视频通话过程中出错", e);
                    Toast.makeText(this, "视频通话初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                try {
                    // 初始化SIP
                    if (!callManager.initializeSIP("user", "example.com", "password")) {
                        Log.e(TAG, "初始化SIP失败");
                        Toast.makeText(this, "初始化SIP失败", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    
                    // 如果是拨出电话，开始拨号
                    if (!isIncoming) {
                        callManager.makeAudioCall(callerId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "SIP初始化或音频通话过程中出错", e);
                    Toast.makeText(this, "音频通话初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "通话管理器初始化失败", e);
            Toast.makeText(this, "通话功能暂时不可用: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
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
        callManager.answerCall();
    }
    
    /**
     * 拒绝电话
     */
    private void onRejectCall() {
        callManager.rejectCall();
        finish();
    }
    
    /**
     * 结束通话
     */
    private void onEndCall() {
        callManager.endCall();
        finish();
    }
    
    /**
     * 切换静音状态
     */
    private void onToggleMute() {
        isMuted = !isMuted;
        callManager.setMicEnabled(!isMuted);
        
        // 更新UI
        muteButton.setImageResource(isMuted ? 
                R.drawable.ic_mic_off : R.drawable.ic_mic_on);
        
        Toast.makeText(this, isMuted ? "已静音" : "已取消静音", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 切换扬声器状态
     */
    private void onToggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        
        // 设置音频输出
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(isSpeakerOn);
        
        // 更新UI
        speakerButton.setImageResource(isSpeakerOn ? 
                R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
        
        Toast.makeText(this, isSpeakerOn ? "已开启扬声器" : "已关闭扬声器", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 切换摄像头
     */
    private void onSwitchCamera() {
        if (callType == CallManager.CALL_TYPE_VIDEO) {
            callManager.switchCamera();
            Toast.makeText(this, "已切换摄像头", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 切换视频状态
     */
    private void onToggleVideo() {
        if (callType == CallManager.CALL_TYPE_VIDEO) {
            isVideoEnabled = !isVideoEnabled;
            callManager.setVideoEnabled(isVideoEnabled);
            
            // 更新UI
            videoToggleButton.setImageResource(isVideoEnabled ? 
                    R.drawable.ic_video_on : R.drawable.ic_video_off);
            
            if (isVideoEnabled) {
                localVideoView.setVisibility(View.VISIBLE);
            } else {
                localVideoView.setVisibility(View.INVISIBLE);
            }
            
            Toast.makeText(this, isVideoEnabled ? "已开启视频" : "已关闭视频", Toast.LENGTH_SHORT).show();
        }
    }
    
    // CallManager.CallManagerListener 接口实现
    
    @Override
    public void onCallStateChanged(int state) {
        switch (state) {
            case CallManager.CALL_STATE_CONNECTING:
                callStateView.setText("正在连接...");
                break;
            case CallManager.CALL_STATE_RINGING:
                callStateView.setText("正在响铃...");
                break;
            case CallManager.CALL_STATE_CONNECTED:
                callStateView.setText("通话中");
                
                // 显示通话时长计时器
                callDurationView.setVisibility(View.VISIBLE);
                callDurationView.setBase(SystemClock.elapsedRealtime());
                callDurationView.start();
                
                // 隐藏来电控制区，显示通话中控制区
                incomingCallControls.setVisibility(View.GONE);
                ongoingCallControls.setVisibility(View.VISIBLE);
                break;
            case CallManager.CALL_STATE_ENDED:
                callStateView.setText("通话已结束");
                
                // 停止计时器
                callDurationView.stop();
                
                // 延迟关闭界面
                new Handler().postDelayed(this::finish, 1000);
                break;
        }
    }
    
    @Override
    public void onIncomingCall(String callerId, int callType) {
        // 在这个活动中，应该已经知道有来电了，所以不需要处理
    }
    
    @Override
    public void onMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onError(String errorMessage) {
        Toast.makeText(this, "错误: " + errorMessage, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "通话错误: " + errorMessage);
        
        // 出错时结束通话
        new Handler().postDelayed(this::finish, 2000);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 停止网络质量检测
        if (networkQualityHandler != null && networkQualityRunnable != null) {
            networkQualityHandler.removeCallbacks(networkQualityRunnable);
        }
        
        // 释放资源
        if (callManager != null) {
            callManager.release();
        }
    }
}