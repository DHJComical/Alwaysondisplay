package com.dhj.always_on_display;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class AppSelectorStore {
    private static final String PREFS_NAME = "overlay_compat";
    private static final String KEY_SELECTED_PACKAGES = "selected_packages";

    private AppSelectorStore() {
    }

    static Set<String> readSelectedPackages(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> packages = preferences.getStringSet(KEY_SELECTED_PACKAGES, Collections.emptySet());
        return new HashSet<>(packages);
    }

    static void writeSelectedPackages(Context context, Set<String> packages) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putStringSet(KEY_SELECTED_PACKAGES, new HashSet<>(packages)).apply();
    }
}
