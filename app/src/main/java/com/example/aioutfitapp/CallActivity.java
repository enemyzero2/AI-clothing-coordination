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
        callManager = CallManager.getInstance(this);
        callManager.setListener(this);
        
        // 设置FreeSwitch服务器配置
        callManager.setSipServerConfig("10.29.206.148", "10.29.206.148", 5060, "UDP");
        
        // 如果来电，使用默认账号初始化SIP服务
        if (isIncoming) {
            if (!callManager.initializeDefaultSIP()) {
                Toast.makeText(this, "初始化SIP服务失败", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            // 如果是主动呼叫，显示账号选择对话框
            if (callType == CallManager.CALL_TYPE_AUDIO) {
                showAccountSelectionDialog();
                return;
            } else {
                // 视频通话暂时使用默认账号
                if (!callManager.initializeDefaultSIP()) {
                    Toast.makeText(this, "初始化SIP服务失败", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
        }
        
        // 对于视频通话，初始化WebRTC
        if (callType == CallManager.CALL_TYPE_VIDEO) {
            if (!callManager.initializeWebRTC(localVideoView, remoteVideoView)) {
                Toast.makeText(this, "初始化WebRTC失败", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        
        // 根据是否是来电处理
        if (isIncoming) {
            // 来电等待用户接听或拒绝
            callStateView.setText("来电: " + callerId);
        } else {
            if (callType == CallManager.CALL_TYPE_VIDEO) {
                // 视频通话
                callManager.makeVideoCall(callerId, roomId);
                callStateView.setText("正在视频呼叫: " + callerId);
            }
        }
    }
    
    /**
     * 显示SIP账号选择对话框
     */
    private void showAccountSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择SIP账号");
        
        // 获取所有可用的SIP账号
        final List<String> accounts = callManager.getAvailableSipAccounts();
        final String[] accountsArray = accounts.toArray(new String[0]);
        
        // 显示账号信息
        String[] displayArray = new String[accountsArray.length];
        for (int i = 0; i < accountsArray.length; i++) {
            displayArray[i] = accountsArray[i] + " (密码: 1234)";
        }
        
        builder.setSingleChoiceItems(displayArray, 0, null);
        
        // 添加确定按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < accounts.size()) {
                String selectedAccount = accounts.get(selectedPosition);
                
                // 设置当前账号
                int accountIndex = selectedPosition;
                callManager.setCurrentAccountIndex(accountIndex);
                
                // 初始化SIP服务
                if (callManager.initializeSipWithAccount(selectedAccount)) {
                    Toast.makeText(this, "已选择账号: " + selectedAccount, Toast.LENGTH_SHORT).show();
                    
                    // 稍等片刻后发起呼叫，确保SIP注册成功
                    new Handler().postDelayed(() -> {
                        if (callType == CallManager.CALL_TYPE_AUDIO) {
                            // 音频通话
                            showCallTargetDialog();
                        }
                    }, 1000);
                } else {
                    Toast.makeText(this, "初始化SIP服务失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
        
        // 添加取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
            finish();
        });
        
        builder.setCancelable(false);
        builder.show();
    }
    
    /**
     * 显示呼叫目标选择对话框
     */
    private void showCallTargetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择呼叫目标");
        
        // 获取所有可用的SIP账号
        final List<String> accounts = callManager.getAvailableSipAccounts();
        final String[] accountsArray = accounts.toArray(new String[0]);
        
        // 从可用账号中排除当前账号
        String currentAccount = callManager.getCurrentSipAccount();
        List<String> targetAccounts = new ArrayList<>();
        List<String> displayTargets = new ArrayList<>();
        
        for (String account : accountsArray) {
            if (!account.equals(currentAccount)) {
                targetAccounts.add(account);
                displayTargets.add(account + " (FreeSwitch账号)");
            }
        }
        
        // 自定义SIP地址选项
        displayTargets.add("自定义SIP地址...");
        
        builder.setSingleChoiceItems(displayTargets.toArray(new String[0]), 0, null);
        
        // 添加确定按钮
        builder.setPositiveButton("呼叫", (dialog, which) -> {
            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            
            if (selectedPosition >= 0) {
                if (selectedPosition < targetAccounts.size()) {
                    // 呼叫选定的FreeSwitch账号
                    String targetAccount = targetAccounts.get(selectedPosition);
                    callerId = targetAccount;
                    callManager.callFreeSwitchAccount(targetAccount);
                    callStateView.setText("正在呼叫: " + callerId);
                } else {
                    // 用户选择了自定义SIP地址
                    showCustomSipAddressDialog();
                }
            }
        });
        
        // 添加取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
            finish();
        });
        
        builder.setCancelable(false);
        builder.show();
    }
    
    /**
     * 显示自定义SIP地址输入对话框
     */
    private void showCustomSipAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入SIP地址");
        
        // 创建输入框
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("用户名@域名 (例如: 1001@10.29.206.148)");
        builder.setView(input);
        
        // 添加确定按钮
        builder.setPositiveButton("呼叫", (dialog, which) -> {
            String sipAddress = input.getText().toString().trim();
            if (!sipAddress.isEmpty()) {
                callerId = sipAddress;
                callManager.makeAudioCall(sipAddress);
                callStateView.setText("正在呼叫: " + callerId);
            } else {
                Toast.makeText(this, "请输入有效的SIP地址", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        
        // 添加取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
            finish();
        });
        
        builder.setCancelable(false);
        builder.show();
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
                    if (callType == CallManager.CALL_TYPE_VIDEO && isVideoEnabled) {
                        localVideoView.setVisibility(View.VISIBLE);
                        remoteVideoView.setVisibility(View.VISIBLE);
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
        if (callManager != null && callManager.getCallState() != CallManager.CALL_STATE_IDLE) {
            callManager.endCall();
        }
    }
}