package com.dhj.always_on_display;

import android.graphics.drawable.Drawable;

final class AppInfo {
    final String appName;
    final String packageName;
    final Drawable icon;

    AppInfo(String appName, String packageName, Drawable icon) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
    }
}
