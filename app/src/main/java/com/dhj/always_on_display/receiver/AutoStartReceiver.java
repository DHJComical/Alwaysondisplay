package com.dhj.always_on_display.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dhj.always_on_display.logging.DebugLog;
import com.dhj.always_on_display.service.KeepAwakeServiceController;

public class AutoStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "<null>" : intent.getAction();
        DebugLog.i(context, "Auto-start receiver invoked: action=" + action);
        KeepAwakeServiceController.syncService(context, "broadcast:" + action);
    }
}
