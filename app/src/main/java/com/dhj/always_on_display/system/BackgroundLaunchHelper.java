package com.dhj.always_on_display.system;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;

public final class BackgroundLaunchHelper {
    private BackgroundLaunchHelper() {
    }

    public static boolean canScheduleExactAlarms(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    public static boolean isIgnoringBatteryOptimizations(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        PowerManager powerManager = context.getSystemService(PowerManager.class);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }
}
