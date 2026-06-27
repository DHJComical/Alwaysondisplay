package com.dhj.always_on_display.service;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.dhj.always_on_display.R;
import com.dhj.always_on_display.data.AppSelectorStore;
import com.dhj.always_on_display.logging.DebugLog;
import com.dhj.always_on_display.monitor.ForegroundAppMonitor;

public final class KeepAwakeServiceController {
    private KeepAwakeServiceController() {
    }

    public static void syncService(Context context, String reason) {
        Context appContext = context.getApplicationContext();
        boolean overlayGranted = Settings.canDrawOverlays(appContext);
        boolean usageGranted = ForegroundAppMonitor.hasUsageAccess(appContext);
        int selectedCount = AppSelectorStore.readSelectedPackages(appContext).size();

        DebugLog.i(appContext, "Sync keep-awake monitor: reason="
                + reason
                + ", overlay="
                + overlayGranted
                + ", usageAccess="
                + usageGranted
                + ", selectedCount="
                + selectedCount
                + ", active="
                + AppSelectorStore.isOverlayActive(appContext));

        if (overlayGranted && usageGranted && selectedCount > 0) {
            requestStart(appContext, reason);
        } else {
            requestStop(appContext, reason);
        }
    }

    public static int getStatusLabelResId(Context context) {
        Context appContext = context.getApplicationContext();
        if (!Settings.canDrawOverlays(appContext)) {
            return R.string.monitor_status_overlay_required;
        }
        if (!ForegroundAppMonitor.hasUsageAccess(appContext)) {
            return R.string.monitor_status_usage_required;
        }
        if (AppSelectorStore.readSelectedPackages(appContext).isEmpty()) {
            return R.string.monitor_status_no_apps;
        }
        return AppSelectorStore.isOverlayActive(appContext)
                ? R.string.monitor_status_running
                : R.string.monitor_status_starting;
    }

    private static void requestStart(Context context, String reason) {
        try {
            ContextCompat.startForegroundService(
                    context,
                    new Intent(context, KeepAwakeOverlayService.class)
                            .setAction(KeepAwakeOverlayService.ACTION_START)
            );
            AppSelectorStore.setOverlayActive(context, true);
            DebugLog.i(context, "Start request sent: reason=" + reason);
        } catch (RuntimeException e) {
            AppSelectorStore.setOverlayActive(context, false);
            DebugLog.e(context, "Failed to start keep-awake monitor: reason=" + reason, e);
        }
    }

    private static void requestStop(Context context, String reason) {
        boolean stopped = context.stopService(new Intent(context, KeepAwakeOverlayService.class));
        AppSelectorStore.setOverlayActive(context, false);
        DebugLog.i(context, "Stop request processed: reason=" + reason + ", serviceStopped=" + stopped);
    }
}
