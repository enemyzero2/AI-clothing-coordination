package com.example.aioutfitapp.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 来电广播接收器 - 已禁用
 * 
 * 此类已被禁用，因为它使用的是Android原生SIP系统，与Linphone系统冲突。
 * 所有SIP通话功能现在完全由Linphone SDK处理。
 * 
 * 此类保留仅作为参考，但不再在AndroidManifest中注册，因此不会被系统调用。
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    
    private static final String TAG = "IncomingCallReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // 此方法已被禁用，不再处理任何SIP来电
        Log.i(TAG, "IncomingCallReceiver已被禁用，所有SIP通话由Linphone处理");
    }
}