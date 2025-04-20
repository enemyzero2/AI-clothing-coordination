package com.example.aioutfitapp.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.util.Log;

import com.example.aioutfitapp.CallActivity;

/**
 * 来电广播接收器
 * 
 * 用于接收SIP来电广播，并启动通话界面
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    
    private static final String TAG = "IncomingCallReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            SipAudioCall sipAudioCall = null;
            
            // 尝试获取SIP管理器
            SipManager sipManager = SipManager.newInstance(context);
            if (sipManager == null) {
                Log.e(TAG, "无法创建SIP管理器");
                return;
            }
            
            // 从Intent中获取SIP来电
            sipAudioCall = sipManager.takeAudioCall(intent, null);
            
            if (sipAudioCall == null) {
                Log.e(TAG, "无法获取SIP音频通话");
                return;
            }
            
            // 获取来电者信息
            SipProfile callerProfile = sipAudioCall.getPeerProfile();
            String callerId = callerProfile.getDisplayName();
            if (callerId == null || callerId.isEmpty()) {
                callerId = callerProfile.getUserName() + "@" + callerProfile.getSipDomain();
            }
            
            // 启动来电界面
            Intent callIntent = CallActivity.createIncomingCallIntent(
                    context, 
                    CallManager.CALL_TYPE_AUDIO, 
                    callerId, 
                    null
            );
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
            
            // 将通话交给CallManager处理
            CallManager.getInstance(context).answerCall();
            
        } catch (SipException e) {
            Log.e(TAG, "处理SIP来电出错: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "处理来电广播出错: " + e.getMessage());
        }
    }
}