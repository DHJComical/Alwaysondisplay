package com.dhj.always_on_display.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dhj.always_on_display.R;
import com.dhj.always_on_display.data.AppSelectorStore;
import com.dhj.always_on_display.logging.DebugLog;
import com.dhj.always_on_display.monitor.ForegroundAppMonitor;
import com.dhj.always_on_display.service.KeepAwakeServiceController;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {
    private TextView overlayPermissionStatus;
    private TextView usagePermissionStatus;
    private TextView notificationPermissionStatus;
    private TextView monitorStatus;
    private MaterialSwitch debugLoggingSwitch;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                KeepAwakeServiceController.syncService(requireContext(), "notification_permission_result");
                updateStatus();
            });

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupActions(view);
        KeepAwakeServiceController.syncService(requireContext(), "settings_view_created");
        updateStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        KeepAwakeServiceController.syncService(requireContext(), "settings_resume");
        updateStatus();
    }

    private void bindViews(@NonNull View view) {
        overlayPermissionStatus = view.findViewById(R.id.overlayPermissionStatus);
        usagePermissionStatus = view.findViewById(R.id.usagePermissionStatus);
        notificationPermissionStatus = view.findViewById(R.id.notificationPermissionStatus);
        monitorStatus = view.findViewById(R.id.monitorStatus);
        debugLoggingSwitch = view.findViewById(R.id.debugLoggingSwitch);
    }

    private void setupActions(@NonNull View view) {
        MaterialButton overlayPermissionButton = view.findViewById(R.id.overlayPermissionButton);
        MaterialButton usagePermissionButton = view.findViewById(R.id.usagePermissionButton);
        MaterialButton notificationPermissionButton = view.findViewById(R.id.notificationPermissionButton);

        overlayPermissionButton.setOnClickListener(v -> requestOverlayPermission());
        usagePermissionButton.setOnClickListener(v -> requestUsageAccessPermission());
        notificationPermissionButton.setOnClickListener(v -> requestNotificationPermission());
        debugLoggingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onDebugLoggingChanged(isChecked));
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + requireContext().getPackageName())
        );
        startActivity(intent);
    }

    private void requestUsageAccessPermission() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        openNotificationSettings();
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        startActivity(intent);
    }

    private void updateStatus() {
        boolean overlayGranted = Settings.canDrawOverlays(requireContext());
        boolean usageGranted = ForegroundAppMonitor.hasUsageAccess(requireContext());
        boolean notificationsGranted = hasNotificationPermission();

        overlayPermissionStatus.setText(overlayGranted
                ? getString(R.string.permission_granted)
                : getString(R.string.permission_required));
        usagePermissionStatus.setText(usageGranted
                ? getString(R.string.permission_granted)
                : getString(R.string.permission_required));
        notificationPermissionStatus.setText(getString(
                notificationsGranted
                        ? R.string.permission_granted
                        : (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? R.string.permission_recommended
                        : R.string.permission_not_required)
        ));
        monitorStatus.setText(getString(
                R.string.monitor_status,
                getString(KeepAwakeServiceController.getStatusLabelResId(requireContext()))
        ));
        debugLoggingSwitch.setChecked(AppSelectorStore.isDebugLoggingEnabled(requireContext()));
    }

    private void onDebugLoggingChanged(boolean enabled) {
        boolean current = AppSelectorStore.isDebugLoggingEnabled(requireContext());
        if (current == enabled) {
            return;
        }

        if (!enabled) {
            DebugLog.i(requireContext(), "Debug logging disabled by user");
        }
        AppSelectorStore.setDebugLoggingEnabled(requireContext(), enabled);
        if (enabled) {
            DebugLog.i(requireContext(), "Debug logging enabled by user");
        }
        Toast.makeText(
                requireContext(),
                enabled ? R.string.toast_debug_logging_enabled : R.string.toast_debug_logging_disabled,
                Toast.LENGTH_SHORT
        ).show();
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }
}
