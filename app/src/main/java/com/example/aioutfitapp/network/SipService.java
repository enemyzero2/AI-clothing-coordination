package com.example.aioutfitapp.network;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.aioutfitapp.MainActivity;
import com.example.aioutfitapp.R;

/**
 * SIP服务
 * 
 * 作为App中唯一负责启动和管理Linphone核心的服务
 * 确保SIP连接在后台稳定运行，处理来电和通话
 */
public class SipService extends Service {
    
    private static final String TAG = "SipService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "sip_service_channel";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "SIP服务正在创建...");
        
        // 创建通知渠道
        createNotificationChannel();
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());
        
        // 在这里，进行唯一一次的Linphone初始化和STUN配置！
        LinphoneManager.getInstance().init(getApplicationContext());
        
        Log.i(TAG, "SIP服务创建完成");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "SIP服务已启动");
        
        // 确保Linphone核心在运行
        LinphoneManager.getInstance().start();
        
        // 如果服务被系统杀死，会自动重启
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "SIP服务正在销毁...");
        
        // 释放Linphone核心资源
        LinphoneManager.getInstance().release();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // 如果需要Activity和Service通信，可以在这里实现
        return null;
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SIP服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持SIP连接活跃的通知");
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle("AI衣搭通话服务")
                .setContentText("保持连接以接收来电")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        
        return builder.build();
    }
} 