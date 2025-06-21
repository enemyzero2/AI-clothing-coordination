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

import com.example.aioutfitapp.network.CallManager;
import com.example.aioutfitapp.network.LinphoneManager;
import com.example.aioutfitapp.network.NetworkSimulator;

import org.webrtc.SurfaceViewRenderer;

import android.app.AlertDialog;
import java.util.List;
import java.util.ArrayList;
import android.widget.EditText;
import android.text.InputType;

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
            Manifest.permission.INTERNET,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
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
        
        // 使用带阴影的按钮背景
        muteButton.setBackgroundResource(R.drawable.circle_white_shadow);
        speakerButton.setBackgroundResource(R.drawable.circle_white_shadow);
        switchCameraButton.setBackgroundResource(R.drawable.circle_white_shadow);
        videoToggleButton.setBackgroundResource(R.drawable.circle_white_shadow);
        
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
            
            // 添加视频通话按钮到操作栏
            ImageButton videoCallBtn = new ImageButton(this);
            videoCallBtn.setImageResource(R.drawable.ic_video_call);
            videoCallBtn.setBackgroundResource(R.drawable.circle_white_shadow);
            videoCallBtn.setContentDescription("视频通话");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(android.R.dimen.app_icon_size),
                    getResources().getDimensionPixelSize(android.R.dimen.app_icon_size));
            params.setMargins(16, 0, 16, 0);
            videoCallBtn.setLayoutParams(params);
            videoCallBtn.setOnClickListener(v -> upgradeToVideoCall());
            ongoingCallControls.addView(videoCallBtn, 0);
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
     * 升级到视频通话
     */
    private void upgradeToVideoCall() {
        // 如果当前是音频通话，升级为视频通话
        if (callType == CallManager.CALL_TYPE_AUDIO && callManager.getCallState() == CallManager.CALL_STATE_CONNECTED) {
            Toast.makeText(this, "正在升级为视频通话...", Toast.LENGTH_SHORT).show();
            
            callType = CallManager.CALL_TYPE_VIDEO;
            
            // 显示视频控件
            localVideoView.setVisibility(View.VISIBLE);
            remoteVideoView.setVisibility(View.VISIBLE);
            videoCallControls.setVisibility(View.VISIBLE);
            callerAvatarView.setVisibility(View.GONE);
            
            // 尝试初始化WebRTC
            if (callManager.initializeWebRTC(localVideoView, remoteVideoView)) {
                // 启用视频
                callManager.setVideoEnabled(true);
            } else {
                Toast.makeText(this, "无法初始化视频通话", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 初始化通话管理器
     */
    private void initCallManager() {
        callManager = CallManager.getInstance(this);
        callManager.setListener(this);
        
        // 从SharedPreferences获取SIP服务器配置
        android.content.SharedPreferences prefs = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE);
        String sipServerAddress = prefs.getString(App.PREF_SIP_SERVER_ADDRESS, "127.0.0.1");
        String sipDomain = prefs.getString(App.PREF_SIP_DOMAIN, "localhost");
        String sipServerPort = prefs.getString(App.PREF_SIP_SERVER_PORT, "5060").trim();
        
        // 使用配置设置SIP服务器
        int port = 5060;
        try {
            port = Integer.parseInt(sipServerPort);
        } catch (NumberFormatException e) {
            Log.e(TAG, "无效的SIP端口: " + sipServerPort + ", 使用默认端口5060", e);
        }
        
        // 使用实际服务器地址，而非域名
        String serverAddress = sipServerAddress.isEmpty() ? sipDomain : sipServerAddress;
        
        Log.d(TAG, "配置SIP服务器: 地址=" + serverAddress + ", 域名=" + sipDomain + ", 端口=" + port);
        callManager.setSipServerConfig(serverAddress, sipDomain, port, "UDP");
        
        // 【重要】对于来电，我们假定SIP服务已在后台正确初始化和注册。
        // 通话界面绝不应再次初始化或登录，因此删除整个 if(isIncoming) 逻辑块。
        if (isIncoming) {
            // 什么都不做！
            // 只是安静地等待用户按下接听或拒绝按钮。
            Log.d(TAG, "这是一个来电，等待用户操作...");
        } else {
            // 如果是主动呼叫，直接使用当前已登录的SIP账号
            LinphoneManager linphoneManager = LinphoneManager.getInstance();
            if (!linphoneManager.isRegistered()) {
                Toast.makeText(this, "SIP未登录，无法发起呼叫", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // 显示正在使用的SIP账号信息（调试用）
            String sipUsername = prefs.getString(App.PREF_SIP_USERNAME, "");
            if (!sipUsername.isEmpty()) {
                Toast.makeText(this, "使用SIP账号: " + sipUsername + "@" + sipDomain, Toast.LENGTH_SHORT).show();
            }
            
            // 发起呼叫
            if (callType == CallManager.CALL_TYPE_AUDIO) {
                // 音频通话
                callManager.makeAudioCall(callerId);
                callStateView.setText("正在呼叫: " + callerId);
            } else {
                // 视频通话
                // 对于视频通话，初始化WebRTC
                if (!callManager.initializeWebRTC(localVideoView, remoteVideoView)) {
                    Toast.makeText(this, "初始化WebRTC失败", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                callManager.makeVideoCall(callerId, roomId);
                callStateView.setText("正在视频呼叫: " + callerId);
            }
        }
        
        // 根据是否是来电处理
        if (isIncoming) {
            // 来电等待用户接听或拒绝
            callStateView.setText("来电: " + callerId);
        }
    }
    
    /**
     * 显示SIP账号选择对话框（已弃用）
     */
    @Deprecated
    private void showAccountSelectionDialog() {
        // 此方法已不再使用，直接发起呼叫
        initCallManager();
    }
    
    /**
     * 显示呼叫目标选择对话框（已弃用）
     */
    @Deprecated
    private void showCallTargetDialog() {
        // 此方法已不再使用，直接发起呼叫
    }
    
    /**
     * 显示自定义SIP地址输入对话框（已弃用）
     */
    @Deprecated
    private void showCustomSipAddressDialog() {
        // 此方法已不再使用，直接发起呼叫
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
                // 每2秒更新一次
                networkQualityHandler.postDelayed(this, 2000);
            }
        };
        // 开始定时更新
        networkQualityHandler.postDelayed(networkQualityRunnable, 2000);
    }
    
    /**
     * 更新网络质量指示器
     */
    private void updateNetworkQualityIndicator() {
        // 获取当前网络质量
        int quality = NetworkSimulator.getInstance(this).getCurrentNetworkQuality();
        String qualityText;
        
        // 根据质量设置不同的文本和颜色
        switch (quality) {
            case NetworkSimulator.NETWORK_QUALITY_EXCELLENT:
                qualityText = "网络质量：极佳";
                networkQualityView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case NetworkSimulator.NETWORK_QUALITY_GOOD:
                qualityText = "网络质量：良好";
                networkQualityView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                break;
            case NetworkSimulator.NETWORK_QUALITY_FAIR:
                qualityText = "网络质量：一般";
                networkQualityView.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                break;
            case NetworkSimulator.NETWORK_QUALITY_POOR:
                qualityText = "网络质量：较差";
                networkQualityView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            default:
                qualityText = "网络质量：不佳";
                networkQualityView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                break;
        }
        
        networkQualityView.setText(qualityText);
    }
    
    /**
     * 检查权限
     */
    private boolean checkPermissions() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS);
                return false;
            }
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
                Toast.makeText(this, "需要所有权限才能进行通话", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    /**
     * 接听来电
     */
    private void onAnswerCall() {
        Log.d(TAG, "接听来电");
        
        // 隐藏来电控制区，显示通话中控制区
        incomingCallControls.setVisibility(View.GONE);
        ongoingCallControls.setVisibility(View.VISIBLE);
        
        // 接听电话
        callManager.answerCall();
        
        // 开始计时
        startCallDurationTimer();
    }
    
    /**
     * 拒绝来电
     */
    private void onRejectCall() {
        Log.d(TAG, "拒绝来电");
        
        // 拒绝电话
        callManager.rejectCall();
        
        // 关闭活动
        finish();
    }
    
    /**
     * 结束通话
     */
    private void onEndCall() {
        Log.d(TAG, "结束通话");
        
        // 结束通话
        callManager.endCall();
        
        // 关闭活动
        finish();
    }
    
    /**
     * 切换麦克风静音状态
     */
    private void onToggleMute() {
        isMuted = !isMuted;
        Log.d(TAG, "切换麦克风: " + (isMuted ? "静音" : "取消静音"));
        
        // 更新UI
        muteButton.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
        
        // 设置麦克风状态
        callManager.setMicEnabled(!isMuted);
    }
    
    /**
     * 切换扬声器状态
     */
    private void onToggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        Log.d(TAG, "切换扬声器: " + (isSpeakerOn ? "开启" : "关闭"));
        
        // 更新UI
        speakerButton.setImageResource(isSpeakerOn ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
        
        // 设置扬声器状态
        callManager.setSpeakerEnabled(isSpeakerOn);
    }
    
    /**
     * 切换摄像头
     */
    private void onSwitchCamera() {
        Log.d(TAG, "切换摄像头");
        
        // 切换摄像头
        callManager.switchCamera();
    }
    
    /**
     * 切换视频状态
     */
    private void onToggleVideo() {
        isVideoEnabled = !isVideoEnabled;
        Log.d(TAG, "切换视频: " + (isVideoEnabled ? "开启" : "关闭"));
        
        // 更新UI
        videoToggleButton.setImageResource(isVideoEnabled ? R.drawable.ic_video_on : R.drawable.ic_video_off);
        
        // 设置视频状态
        callManager.setVideoEnabled(isVideoEnabled);
        
        // 显示/隐藏本地视频预览
        localVideoView.setVisibility(isVideoEnabled ? View.VISIBLE : View.INVISIBLE);
    }
    
    /**
     * 开始通话计时器
     */
    private void startCallDurationTimer() {
        callDurationView.setVisibility(View.VISIBLE);
        callDurationView.setBase(SystemClock.elapsedRealtime());
        callDurationView.start();
    }
    
    @Override
    public void onCallStateChanged(int state) {
        Log.d(TAG, "通话状态变更: " + state);
        
        runOnUiThread(() -> {
            switch (state) {
                case CallManager.CALL_STATE_CONNECTING:
                    callStateView.setText("正在连接...");
                    break;
                case CallManager.CALL_STATE_RINGING:
                    callStateView.setText("正在响铃...");
                    break;
                case CallManager.CALL_STATE_CONNECTED:
                    callStateView.setText("通话中");
                    startCallDurationTimer();
                    
                    // 如果是视频通话，确保视频控件可见
                    if (callType == CallManager.CALL_TYPE_VIDEO) {
                        localVideoView.setVisibility(View.VISIBLE);
                        remoteVideoView.setVisibility(View.VISIBLE);
                        videoCallControls.setVisibility(View.VISIBLE);
                        callerAvatarView.setVisibility(View.GONE);
                        
                        // 默认开启视频
                        callManager.setVideoEnabled(true);
                    }
                    break;
                case CallManager.CALL_STATE_ENDED:
                    callStateView.setText("通话结束");
                    new Handler().postDelayed(this::finish, 1000);
                    break;
            }
        });
    }
    
    @Override
    public void onIncomingCall(String callerId, int callType) {
        // 已经在通话界面，不需要处理
    }
    
    @Override
    public void onMessage(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onError(String errorMessage) {
        runOnUiThread(() -> {
            Toast.makeText(this, "错误: " + errorMessage, Toast.LENGTH_LONG).show();
            new Handler().postDelayed(this::finish, 2000);
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 停止网络质量检测
        if (networkQualityHandler != null && networkQualityRunnable != null) {
            networkQualityHandler.removeCallbacks(networkQualityRunnable);
        }
        
        // 如果通话仍在进行，结束通话
        if (callManager != null) {
            // 记录当前状态用于调试
            int currentState = callManager.getCallState();
            Log.d(TAG, "Activity销毁时的通话状态: " + currentState);
            
            if (currentState != CallManager.CALL_STATE_IDLE) {
                Log.d(TAG, "Activity销毁时通话未结束，强制结束通话");
                callManager.endCall();
                
                // 确保通话状态被重置
                new Handler().postDelayed(() -> {
                    // 获取LinphoneManager实例直接操作
                    LinphoneManager linphoneManager = LinphoneManager.getInstance();
                    if (linphoneManager.getCallState() != LinphoneManager.CALL_STATE_IDLE) {
                        Log.d(TAG, "强制重置Linphone通话状态");
                        linphoneManager.forceResetCallState();
                    }
                }, 300);
            } else {
                // 即使状态是IDLE，也进行一次强制检查和清理
                Log.d(TAG, "Activity销毁时进行最终状态检查和清理");
                LinphoneManager.getInstance().forceResetCallState();
            }
        }
    }
}