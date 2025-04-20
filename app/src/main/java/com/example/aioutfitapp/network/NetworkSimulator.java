 package com.example.aioutfitapp.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 网络模拟器
 * 
 * 用于模拟4G/5G网络环境，包括网络延迟、丢包率等参数的动态变化
 */
public class NetworkSimulator {
    
    private static final String TAG = "NetworkSimulator";
    
    // 网络类型常量
    public static final int NETWORK_TYPE_4G = 0;
    public static final int NETWORK_TYPE_5G = 1;
    
    // 网络参数
    private int currentNetworkType = NETWORK_TYPE_4G; // 默认4G网络
    private int bandwidth; // 带宽(Mbps)
    private int latency; // 延迟(ms)
    private float packetLoss; // 丢包率(%)
    private float jitter; // 抖动(ms)
    
    // 4G网络参数范围
    private static final int BANDWIDTH_4G_MIN = 5;    // Mbps
    private static final int BANDWIDTH_4G_MAX = 100;  // Mbps
    private static final int LATENCY_4G_MIN = 30;     // ms
    private static final int LATENCY_4G_MAX = 100;    // ms
    private static final float PACKET_LOSS_4G_MIN = 0.1f; // %
    private static final float PACKET_LOSS_4G_MAX = 2.0f; // %
    private static final float JITTER_4G_MIN = 5.0f;  // ms
    private static final float JITTER_4G_MAX = 20.0f; // ms
    
    // 5G网络参数范围
    private static final int BANDWIDTH_5G_MIN = 50;   // Mbps
    private static final int BANDWIDTH_5G_MAX = 1000; // Mbps
    private static final int LATENCY_5G_MIN = 1;      // ms
    private static final int LATENCY_5G_MAX = 30;     // ms
    private static final float PACKET_LOSS_5G_MIN = 0.01f; // %
    private static final float PACKET_LOSS_5G_MAX = 0.5f;  // %
    private static final float JITTER_5G_MIN = 1.0f;  // ms
    private static final float JITTER_5G_MAX = 10.0f; // ms
    
    // 上下文和随机数生成器
    private Context context;
    private Random random;
    
    // 定时任务执行器，用于定期更新网络参数
    private ScheduledExecutorService scheduler;
    private boolean isSimulating = false;
    
    // 网络状态监听器
    private NetworkSimulatorListener listener;
    
    // 主线程Handler，用于回调UI
    private Handler mainHandler;
    
    // 单例模式
    private static NetworkSimulator instance;
    
    /**
     * 获取单例实例
     */
    public static synchronized NetworkSimulator getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkSimulator(context);
        }
        return instance;
    }
    
    /**
     * 私有构造函数
     */
    private NetworkSimulator(Context context) {
        this.context = context.getApplicationContext();
        this.random = new Random();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化网络参数为4G
        initNetworkParams(NETWORK_TYPE_4G);
    }
    
    /**
     * 初始化网络参数
     */
    private void initNetworkParams(int networkType) {
        this.currentNetworkType = networkType;
        
        if (networkType == NETWORK_TYPE_4G) {
            // 4G网络参数
            this.bandwidth = randomInRange(BANDWIDTH_4G_MIN, BANDWIDTH_4G_MAX);
            this.latency = randomInRange(LATENCY_4G_MIN, LATENCY_4G_MAX);
            this.packetLoss = randomFloatInRange(PACKET_LOSS_4G_MIN, PACKET_LOSS_4G_MAX);
            this.jitter = randomFloatInRange(JITTER_4G_MIN, JITTER_4G_MAX);
        } else {
            // 5G网络参数
            this.bandwidth = randomInRange(BANDWIDTH_5G_MIN, BANDWIDTH_5G_MAX);
            this.latency = randomInRange(LATENCY_5G_MIN, LATENCY_5G_MAX);
            this.packetLoss = randomFloatInRange(PACKET_LOSS_5G_MIN, PACKET_LOSS_5G_MAX);
            this.jitter = randomFloatInRange(JITTER_5G_MIN, JITTER_5G_MAX);
        }
        
        Log.d(TAG, String.format("初始化%s网络参数: 带宽=%dMbps, 延迟=%dms, 丢包率=%.2f%%, 抖动=%.2fms",
                networkType == NETWORK_TYPE_4G ? "4G" : "5G",
                bandwidth, latency, packetLoss, jitter));
    }
    
    /**
     * 随机生成指定范围内的整数
     */
    private int randomInRange(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
    
    /**
     * 随机生成指定范围内的浮点数
     */
    private float randomFloatInRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
    
    /**
     * 设置监听器
     */
    public void setListener(NetworkSimulatorListener listener) {
        this.listener = listener;
    }
    
    /**
     * 开始网络模拟
     */
    public void startSimulation(int networkType) {
        if (isSimulating) {
            stopSimulation();
        }
        
        initNetworkParams(networkType);
        
        // 创建定时任务执行器
        scheduler = Executors.newScheduledThreadPool(1);
        
        // 每5秒更新一次网络参数
        scheduler.scheduleAtFixedRate(() -> {
            updateNetworkParams();
            notifyNetworkParamsChanged();
        }, 5, 5, TimeUnit.SECONDS);
        
        isSimulating = true;
        
        // 通知监听器
        if (listener != null) {
            mainHandler.post(() -> listener.onSimulationStarted(networkType));
        }
        
        Log.d(TAG, "开始网络模拟: " + (networkType == NETWORK_TYPE_4G ? "4G" : "5G"));
    }
    
    /**
     * 停止网络模拟
     */
    public void stopSimulation() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        
        isSimulating = false;
        
        // 通知监听器
        if (listener != null) {
            mainHandler.post(() -> listener.onSimulationStopped());
        }
        
        Log.d(TAG, "停止网络模拟");
    }
    
    /**
     * 更新网络参数
     */
    private void updateNetworkParams() {
        // 根据当前网络类型，在一定范围内波动网络参数
        if (currentNetworkType == NETWORK_TYPE_4G) {
            // 4G网络参数波动范围
            bandwidth += randomInRange(-10, 10);
            bandwidth = Math.max(BANDWIDTH_4G_MIN, Math.min(bandwidth, BANDWIDTH_4G_MAX));
            
            latency += randomInRange(-10, 10);
            latency = Math.max(LATENCY_4G_MIN, Math.min(latency, LATENCY_4G_MAX));
            
            packetLoss += randomFloatInRange(-0.5f, 0.5f);
            packetLoss = Math.max(PACKET_LOSS_4G_MIN, Math.min(packetLoss, PACKET_LOSS_4G_MAX));
            
            jitter += randomFloatInRange(-2.0f, 2.0f);
            jitter = Math.max(JITTER_4G_MIN, Math.min(jitter, JITTER_4G_MAX));
        } else {
            // 5G网络参数波动范围
            bandwidth += randomInRange(-50, 50);
            bandwidth = Math.max(BANDWIDTH_5G_MIN, Math.min(bandwidth, BANDWIDTH_5G_MAX));
            
            latency += randomInRange(-5, 5);
            latency = Math.max(LATENCY_5G_MIN, Math.min(latency, LATENCY_5G_MAX));
            
            packetLoss += randomFloatInRange(-0.1f, 0.1f);
            packetLoss = Math.max(PACKET_LOSS_5G_MIN, Math.min(packetLoss, PACKET_LOSS_5G_MAX));
            
            jitter += randomFloatInRange(-1.0f, 1.0f);
            jitter = Math.max(JITTER_5G_MIN, Math.min(jitter, JITTER_5G_MAX));
        }
        
        Log.d(TAG, String.format("更新%s网络参数: 带宽=%dMbps, 延迟=%dms, 丢包率=%.2f%%, 抖动=%.2fms",
                currentNetworkType == NETWORK_TYPE_4G ? "4G" : "5G",
                bandwidth, latency, packetLoss, jitter));
    }
    
    /**
     * 通知网络参数变化
     */
    private void notifyNetworkParamsChanged() {
        if (listener != null) {
            mainHandler.post(() -> listener.onNetworkParamsChanged(
                    currentNetworkType,
                    bandwidth,
                    latency,
                    packetLoss,
                    jitter
            ));
        }
    }
    
    /**
     * 切换网络类型
     */
    public void switchNetworkType(int networkType) {
        if (networkType != NETWORK_TYPE_4G && networkType != NETWORK_TYPE_5G) {
            Log.e(TAG, "无效的网络类型: " + networkType);
            return;
        }
        
        if (currentNetworkType == networkType) {
            Log.d(TAG, "已经是" + (networkType == NETWORK_TYPE_4G ? "4G" : "5G") + "网络，无需切换");
            return;
        }
        
        // 如果正在模拟，重新开始模拟
        boolean wasSimulating = isSimulating;
        if (wasSimulating) {
            stopSimulation();
        }
        
        currentNetworkType = networkType;
        initNetworkParams(networkType);
        
        if (wasSimulating) {
            startSimulation(networkType);
        }
        
        Log.d(TAG, "切换网络类型为: " + (networkType == NETWORK_TYPE_4G ? "4G" : "5G"));
    }
    
    /**
     * 获取当前网络类型
     */
    public int getCurrentNetworkType() {
        return currentNetworkType;
    }
    
    /**
     * 获取当前带宽
     */
    public int getBandwidth() {
        return bandwidth;
    }
    
    /**
     * 获取当前延迟
     */
    public int getLatency() {
        return latency;
    }
    
    /**
     * 获取当前丢包率
     */
    public float getPacketLoss() {
        return packetLoss;
    }
    
    /**
     * 获取当前抖动
     */
    public float getJitter() {
        return jitter;
    }
    
    /**
     * 检测真实网络类型
     */
    public int detectRealNetworkType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return NETWORK_TYPE_4G; // 默认返回4G
        }
        
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return NETWORK_TYPE_4G;
        }
        
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            return NETWORK_TYPE_4G;
        }
        
        // 判断是否是5G网络
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // 在Android API 29及以上版本，可以通过TelephonyManager判断是否是5G
            // 这里简化处理，通过网络速度近似判断
            if (capabilities.getLinkDownstreamBandwidthKbps() > 50000) { // 50Mbps
                return NETWORK_TYPE_5G;
            }
        }
        
        return NETWORK_TYPE_4G;
    }
    
    /**
     * 模拟网络延迟
     */
    public void simulateLatency(Runnable callback) {
        // 根据当前网络延迟参数，延迟执行回调
        int delay = latency + (int)(random.nextFloat() * jitter);
        mainHandler.postDelayed(callback, delay);
    }
    
    /**
     * 模拟丢包
     */
    public boolean simulatePacketLoss() {
        // 根据当前丢包率，随机决定是否丢包
        return random.nextFloat() * 100 < packetLoss;
    }
    
    /**
     * 网络模拟器监听器接口
     */
    public interface NetworkSimulatorListener {
        void onSimulationStarted(int networkType);
        void onSimulationStopped();
        void onNetworkParamsChanged(int networkType, int bandwidth, int latency, float packetLoss, float jitter);
    }
}