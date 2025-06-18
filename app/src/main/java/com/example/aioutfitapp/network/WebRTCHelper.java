package com.example.aioutfitapp.network;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * WebRTC辅助类
 * 
 * 用于处理WebRTC相关功能，包括音视频采集、传输等
 */
public class WebRTCHelper {
    
    private static final String TAG = "WebRTCHelper";
    
    // WebRTC相关常量
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String LOCAL_STREAM_ID = "ARDAMS";
    
    // WebRTC组件
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private EglBase eglBase;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoCapturer videoCapturer;
    private AudioSource audioSource;
    private VideoSource videoSource;
    private MediaStream localMediaStream;
    
    // SurfaceViewRenderer
    private SurfaceViewRenderer localRenderer;
    private SurfaceViewRenderer remoteRenderer;
    
    // 上下文和回调接口
    private Context context;
    private WebRTCHelperListener listener;
    
    // 是否启用视频
    private boolean videoEnabled = true;
    
    /**
     * 构造函数
     */
    public WebRTCHelper(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 设置监听器
     */
    public void setListener(WebRTCHelperListener listener) {
        this.listener = listener;
    }
    
    /**
     * 初始化WebRTC
     */
    public boolean initialize(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer) {
        this.localRenderer = localRenderer;
        this.remoteRenderer = remoteRenderer;
        
        try {
            // 创建EglBase
            eglBase = EglBase.create();
            
            // 初始化渲染器
            localRenderer.init(eglBase.getEglBaseContext(), null);
            localRenderer.setMirror(true);
            localRenderer.setEnableHardwareScaler(true);
            
            remoteRenderer.init(eglBase.getEglBaseContext(), null);
            remoteRenderer.setMirror(false);
            remoteRenderer.setEnableHardwareScaler(true);
            
            // 初始化音频管理器
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
            
            // 初始化PeerConnectionFactory
            PeerConnectionFactory.InitializationOptions initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions();
            PeerConnectionFactory.initialize(initOptions);
            
            // 创建PeerConnectionFactory
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            
            DefaultVideoEncoderFactory videoEncoderFactory = new DefaultVideoEncoderFactory(
                    eglBase.getEglBaseContext(), true, true);
            DefaultVideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(
                    eglBase.getEglBaseContext());
            
            peerConnectionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setVideoEncoderFactory(videoEncoderFactory)
                    .setVideoDecoderFactory(videoDecoderFactory)
                    .createPeerConnectionFactory();
            
            // 创建音频轨道
            createAudioTrack();
            
            // 如果启用视频，创建视频轨道
            if (videoEnabled) {
                createVideoTrack();
            }
            
            // 创建本地媒体流
            localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);
            localMediaStream.addTrack(localAudioTrack);
            
            if (videoEnabled && localVideoTrack != null) {
                localMediaStream.addTrack(localVideoTrack);
                // 显示本地视频
                localVideoTrack.addSink(localRenderer);
            }
            
            Log.d(TAG, "WebRTC初始化成功");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "WebRTC初始化失败: " + e.getMessage());
            release();
            return false;
        }
    }
    
    /**
     * 创建音频轨道
     */
    private void createAudioTrack() {
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);
    }
    
    /**
     * 创建视频轨道
     */
    private void createVideoTrack() {
        VideoCapturer videoCapturer = createVideoCapturer();
        if (videoCapturer == null) {
            Log.e(TAG, "无法创建视频捕获器");
            return;
        }
        
        this.videoCapturer = videoCapturer;
        
        // 创建视频源
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
        
        // 启动视频捕获
        videoCapturer.startCapture(640, 480, 30);
        
        // 创建视频轨道
        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        localVideoTrack.setEnabled(true);
    }
    
    /**
     * 创建视频捕获器
     */
    private VideoCapturer createVideoCapturer() {
        // 优先使用Camera2
        if (Camera2Enumerator.isSupported(context)) {
            return createCameraCapturer(new Camera2Enumerator(context));
        } else {
            return createCameraCapturer(new Camera1Enumerator(false));
        }
    }
    
    /**
     * 创建相机捕获器
     */
    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        // 尝试使用前置摄像头
        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        
        // 如果没有前置摄像头，尝试使用后置摄像头
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 创建对等连接
     */
    public void createPeerConnection() {
        // 创建ICE服务器列表
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        
        // 创建PeerConnection配置
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.enableDtlsSrtp = true;
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        
        // 创建PeerConnection
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: " + signalingState);
            }
            
            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: " + iceConnectionState);
                if (listener != null) {
                    listener.onIceConnectionChange(iceConnectionState);
                }
            }
            
            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(TAG, "onIceConnectionReceivingChange: " + b);
            }
            
            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: " + iceGatheringState);
            }
            
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "onIceCandidate: " + iceCandidate);
                if (listener != null) {
                    listener.onIceCandidate(iceCandidate);
                }
            }
            
            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved");
            }
            
            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.getId());
                if (listener != null) {
                    listener.onAddRemoteStream(mediaStream);
                }
                
                // 显示远程视频
                if (mediaStream.videoTracks.size() > 0) {
                    VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                    remoteVideoTrack.setEnabled(true);
                    remoteVideoTrack.addSink(remoteRenderer);
                }
            }
            
            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream: " + mediaStream.getId());
            }
            
            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel: " + dataChannel.label());
            }
            
            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded");
            }
            
            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(TAG, "onAddTrack");
            }
        });
        
        // 添加本地媒体流
        if (localMediaStream != null) {
            peerConnection.addStream(localMediaStream);
        }
    }
    
    /**
     * 创建提议
     */
    public void createOffer() {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection未初始化，无法创建提议");
            return;
        }
        
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        
        if (videoEnabled) {
            mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        } else {
            mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        }
        
        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "创建提议成功");
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                    }
                    
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "设置本地描述成功");
                        if (listener != null) {
                            listener.onLocalDescription(sessionDescription);
                        }
                    }
                    
                    @Override
                    public void onCreateFailure(String s) {
                    }
                    
                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "设置本地描述失败: " + s);
                    }
                }, sessionDescription);
            }
            
            @Override
            public void onSetSuccess() {
            }
            
            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "创建提议失败: " + s);
            }
            
            @Override
            public void onSetFailure(String s) {
            }
        }, mediaConstraints);
    }
    
    /**
     * 创建应答
     */
    public void createAnswer() {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection未初始化，无法创建应答");
            return;
        }
        
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        
        if (videoEnabled) {
            mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        } else {
            mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        }
        
        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "创建应答成功");
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                    }
                    
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "设置本地描述成功");
                        if (listener != null) {
                            listener.onLocalDescription(sessionDescription);
                        }
                    }
                    
                    @Override
                    public void onCreateFailure(String s) {
                    }
                    
                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "设置本地描述失败: " + s);
                    }
                }, sessionDescription);
            }
            
            @Override
            public void onSetSuccess() {
            }
            
            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "创建应答失败: " + s);
            }
            
            @Override
            public void onSetFailure(String s) {
            }
        }, mediaConstraints);
    }
    
    /**
     * 设置远程描述
     */
    public void setRemoteDescription(SessionDescription sessionDescription) {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection未初始化，无法设置远程描述");
            return;
        }
        
        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
            }
            
            @Override
            public void onSetSuccess() {
                Log.d(TAG, "设置远程描述成功");
            }
            
            @Override
            public void onCreateFailure(String s) {
            }
            
            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "设置远程描述失败: " + s);
            }
        }, sessionDescription);
    }
    
    /**
     * 添加远程ICE候选
     */
    public void addRemoteIceCandidate(IceCandidate iceCandidate) {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection未初始化，无法添加远程ICE候选");
            return;
        }
        
        peerConnection.addIceCandidate(iceCandidate);
    }
    
    /**
     * 添加ICE候选
     */
    public void addIceCandidate(IceCandidate iceCandidate) {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection未初始化，无法添加ICE候选");
            return;
        }
        
        peerConnection.addIceCandidate(iceCandidate);
    }
    
    /**
     * 关闭WebRTC连接
     */
    public void close() {
        // 停止视频捕获
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.e(TAG, "停止视频捕获失败: " + e.getMessage());
            }
        }
        
        // 关闭PeerConnection
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        
        Log.d(TAG, "WebRTC连接已关闭");
    }
    
    /**
     * 设置启用视频
     */
    public void setVideoEnabled(boolean enabled) {
        this.videoEnabled = enabled;
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(enabled);
        }
    }
    
    /**
     * 设置静音
     */
    public void setMicEnabled(boolean enabled) {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(enabled);
        }
    }
    
    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            ((CameraVideoCapturer) videoCapturer).switchCamera(null);
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        // 停止视频捕获
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.e(TAG, "停止视频捕获失败: " + e.getMessage());
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }
        
        // 释放视频轨道
        if (localVideoTrack != null) {
            localVideoTrack.removeSink(localRenderer);
            localVideoTrack.dispose();
            localVideoTrack = null;
        }
        
        // 释放音频轨道
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }
        
        // 释放音频源
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        
        // 释放视频源
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        
        // 关闭对等连接
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        
        // 释放PeerConnectionFactory
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        
        // 释放渲染器
        if (localRenderer != null) {
            localRenderer.release();
            localRenderer = null;
        }
        
        if (remoteRenderer != null) {
            remoteRenderer.release();
            remoteRenderer = null;
        }
        
        // 释放EglBase
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
        
        Log.d(TAG, "WebRTC资源已释放");
    }
    
    /**
     * WebRTC辅助监听器接口
     */
    public interface WebRTCHelperListener {
        void onLocalDescription(SessionDescription sessionDescription);
        void onIceCandidate(IceCandidate iceCandidate);
        void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState);
        void onAddRemoteStream(MediaStream mediaStream);
    }
}