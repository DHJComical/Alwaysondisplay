package com.dhj.always_on_display;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class HookEntry extends XposedModule {
    private static final String TAG = "AlwaysOnDisplay";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        super.onModuleLoaded(param);
        log(android.util.Log.INFO, TAG, "Module loaded in process: " + param.getProcessName());
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        super.onPackageLoaded(param);
        log(android.util.Log.INFO, TAG, "Target package loaded: " + param.getPackageName());
    }
}
