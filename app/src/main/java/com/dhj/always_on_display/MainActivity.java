package com.dhj.always_on_display;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    static final String PREFS_NAME = "overlay_compat";
    static final String KEY_OVERLAY_ACTIVE = "overlay_active";

    private SharedPreferences preferences;
    private TextView permissionStatus;
    private TextView compatibilityStatus;
    private Button enableCompatibilityButton;
    private Button stopCompatibilityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        int gap = padding / 2;

        LinearLayout root = new LinearLayout(this);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView title = new TextView(this);
        title.setText(R.string.module_status_title);
        title.setTextSize(22);
        title.setGravity(Gravity.START);

        TextView body = new TextView(this);
        body.setText(R.string.module_status_body);
        body.setTextSize(16);
        body.setGravity(Gravity.START);
        body.setLineSpacing(0, 1.15f);

        permissionStatus = createStatusText();
        compatibilityStatus = createStatusText();

        Button permissionButton = new Button(this);
        permissionButton.setText(R.string.permission_button);
        permissionButton.setAllCaps(false);
        permissionButton.setOnClickListener(view -> requestOverlayPermission());

        enableCompatibilityButton = new Button(this);
        enableCompatibilityButton.setText(R.string.enable_compat_button);
        enableCompatibilityButton.setAllCaps(false);
        enableCompatibilityButton.setOnClickListener(view -> startOverlayCompatibilityMode());

        stopCompatibilityButton = new Button(this);
        stopCompatibilityButton.setText(R.string.stop_compat_button);
        stopCompatibilityButton.setAllCaps(false);
        stopCompatibilityButton.setOnClickListener(view -> stopOverlayCompatibilityMode());

        root.addView(title);
        root.addView(body, withTopMargin(gap));
        root.addView(permissionStatus, withTopMargin(gap));
        root.addView(permissionButton, withTopMargin(gap));
        root.addView(compatibilityStatus, withTopMargin(gap));
        root.addView(enableCompatibilityButton, withTopMargin(gap));
        root.addView(stopCompatibilityButton, withTopMargin(gap));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private TextView createStatusText() {
        TextView textView = new TextView(this);
        textView.setTextSize(14);
        textView.setGravity(Gravity.START);
        textView.setLineSpacing(0, 1.15f);
        return textView;
    }

    private LinearLayout.LayoutParams withTopMargin(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = margin;
        return params;
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private void startOverlayCompatibilityMode() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }

        startService(new Intent(this, KeepAwakeOverlayService.class)
                .setAction(KeepAwakeOverlayService.ACTION_START));
        preferences.edit().putBoolean(KEY_OVERLAY_ACTIVE, true).apply();
        Toast.makeText(this, R.string.toast_compat_enabled, Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void stopOverlayCompatibilityMode() {
        startService(new Intent(this, KeepAwakeOverlayService.class)
                .setAction(KeepAwakeOverlayService.ACTION_STOP));
        preferences.edit().putBoolean(KEY_OVERLAY_ACTIVE, false).apply();
        Toast.makeText(this, R.string.toast_compat_stopped, Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void updateStatus() {
        boolean canDrawOverlays = Settings.canDrawOverlays(this);
        boolean compatibilityActive = preferences.getBoolean(KEY_OVERLAY_ACTIVE, false);

        permissionStatus.setText(canDrawOverlays
                ? getString(R.string.permission_granted)
                : getString(R.string.permission_required));

        compatibilityStatus.setText(getString(
                R.string.compat_status,
                getString(compatibilityActive ? R.string.compat_enabled : R.string.compat_disabled)
        ));

        enableCompatibilityButton.setEnabled(canDrawOverlays && !compatibilityActive);
        stopCompatibilityButton.setEnabled(compatibilityActive);
    }
}
