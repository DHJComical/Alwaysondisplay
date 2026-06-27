package com.dhj.always_on_display;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class KeepAwakeOverlayService extends Service {
    public static final String ACTION_START = "com.dhj.always_on_display.action.START_KEEP_AWAKE_OVERLAY";
    public static final String ACTION_STOP = "com.dhj.always_on_display.action.STOP_KEEP_AWAKE_OVERLAY";

    private WindowManager windowManager;
    private View overlayView;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            removeOverlay();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!Settings.canDrawOverlays(this)) {
            setOverlayActive(false);
            stopSelf();
            return START_NOT_STICKY;
        }

        showOverlay();
        setOverlayActive(overlayView != null);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        setOverlayActive(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showOverlay() {
        if (overlayView != null) {
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) {
            return;
        }

        overlayView = new View(this);
        overlayView.setBackgroundColor(Color.TRANSPARENT);
        overlayView.setKeepScreenOn(true);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1,
                1,
                getOverlayWindowType(),
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.START | Gravity.TOP;
        params.alpha = 0.01f;

        try {
            windowManager.addView(overlayView, params);
        } catch (RuntimeException e) {
            overlayView = null;
        }
    }

    private void removeOverlay() {
        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (RuntimeException ignored) {
            }
        }
        overlayView = null;
    }

    @SuppressWarnings("deprecation")
    private int getOverlayWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void setOverlayActive(boolean active) {
        SharedPreferences preferences = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putBoolean(MainActivity.KEY_OVERLAY_ACTIVE, active).apply();
    }
}
