package com.dhj.always_on_display;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    static final String PREFS_NAME = "overlay_compat";
    static final String KEY_OVERLAY_ACTIVE = "overlay_active";

    private final List<AppInfo> allApps = new ArrayList<>();

    private SharedPreferences preferences;
    private AppListAdapter adapter;
    private TextView overlayPermissionStatus;
    private TextView usagePermissionStatus;
    private TextView compatibilityStatus;
    private TextView selectionCount;
    private TextView introSelectionCount;
    private TextView introCompatibilityStatus;
    private TextView emptyState;
    private MaterialButton startOverlayButton;
    private MaterialButton stopOverlayButton;
    private TextInputEditText searchInput;
    private View introPage;
    private View appsPage;
    private View settingsPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        setContentView(R.layout.activity_main);

        bindViews();
        setupRecyclerView();
        setupActions();
        loadInstalledApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        filterApps(getSearchQuery());
    }

    private void bindViews() {
        introPage = findViewById(R.id.introPage);
        appsPage = findViewById(R.id.appsPage);
        settingsPage = findViewById(R.id.settingsPage);
        overlayPermissionStatus = findViewById(R.id.overlayPermissionStatus);
        usagePermissionStatus = findViewById(R.id.usagePermissionStatus);
        compatibilityStatus = findViewById(R.id.compatibilityStatus);
        selectionCount = findViewById(R.id.selectionCount);
        introSelectionCount = findViewById(R.id.introSelectionCount);
        introCompatibilityStatus = findViewById(R.id.introCompatibilityStatus);
        emptyState = findViewById(R.id.emptyState);
        startOverlayButton = findViewById(R.id.startOverlayButton);
        stopOverlayButton = findViewById(R.id.stopOverlayButton);
        searchInput = findViewById(R.id.searchInput);

        MaterialButton overlayPermissionButton = findViewById(R.id.overlayPermissionButton);
        MaterialButton usagePermissionButton = findViewById(R.id.usagePermissionButton);

        overlayPermissionButton.setOnClickListener(view -> requestOverlayPermission());
        usagePermissionButton.setOnClickListener(view -> requestUsageAccessPermission());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_apps) {
                showPage(appsPage);
            } else if (item.getItemId() == R.id.nav_settings) {
                showPage(settingsPage);
            } else {
                showPage(introPage);
            }
            return true;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_intro);
    }

    private void setupRecyclerView() {
        Set<String> selectedPackages = AppSelectorStore.readSelectedPackages(this);
        adapter = new AppListAdapter(selectedPackages, updatedSelection -> {
            AppSelectorStore.writeSelectedPackages(this, updatedSelection);
            updateSelectionCount(updatedSelection.size());
        });

        RecyclerView recyclerView = findViewById(R.id.appsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        updateSelectionCount(selectedPackages.size());
    }

    private void setupActions() {
        startOverlayButton.setOnClickListener(view -> startOverlayCompatibilityMode());
        stopOverlayButton.setOnClickListener(view -> stopOverlayCompatibilityMode());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadInstalledApps() {
        emptyState.setText(R.string.loading_apps);
        emptyState.setVisibility(android.view.View.VISIBLE);

        new Thread(() -> {
            PackageManager packageManager = getPackageManager();
            List<AppInfo> loadedApps = new ArrayList<>();

            List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0));
            for (ApplicationInfo applicationInfo : installedApplications) {
                String packageName = applicationInfo.packageName;
                if (getPackageName().equals(packageName)) {
                    continue;
                }

                CharSequence label = applicationInfo.loadLabel(packageManager);
                loadedApps.add(new AppInfo(
                        label == null ? packageName : label.toString(),
                        packageName,
                        applicationInfo.loadIcon(packageManager)
                ));
            }

            Collator collator = Collator.getInstance(Locale.getDefault());
            loadedApps.sort(Comparator.comparing(appInfo -> appInfo.appName, collator));

            runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(loadedApps);
                filterApps(getSearchQuery());
            });
        }).start();
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private void requestUsageAccessPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }

    private void startOverlayCompatibilityMode() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }
        if (!ForegroundAppMonitor.hasUsageAccess(this)) {
            requestUsageAccessPermission();
            return;
        }
        if (adapter.getSelectedCount() == 0) {
            Toast.makeText(this, R.string.toast_select_apps_first, Toast.LENGTH_SHORT).show();
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
        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean usageGranted = ForegroundAppMonitor.hasUsageAccess(this);
        boolean compatibilityActive = preferences.getBoolean(KEY_OVERLAY_ACTIVE, false);

        overlayPermissionStatus.setText(overlayGranted
                ? getString(R.string.permission_granted)
                : getString(R.string.permission_required));
        usagePermissionStatus.setText(usageGranted
                ? getString(R.string.permission_granted)
                : getString(R.string.permission_required));
        compatibilityStatus.setText(getString(
                R.string.compat_status,
                getString(compatibilityActive ? R.string.compat_enabled : R.string.compat_disabled)
        ));
        introCompatibilityStatus.setText(getString(compatibilityActive ? R.string.compat_enabled : R.string.compat_disabled));

        startOverlayButton.setEnabled(overlayGranted && usageGranted && adapter.getSelectedCount() > 0 && !compatibilityActive);
        stopOverlayButton.setEnabled(compatibilityActive);
    }

    private void updateSelectionCount(int count) {
        if (count == 0) {
            selectionCount.setText(R.string.selection_none);
            introSelectionCount.setText(R.string.selection_none);
        } else {
            String text = getString(R.string.selection_count, count);
            selectionCount.setText(text);
            introSelectionCount.setText(text);
        }
        updateStatus();
    }

    private void filterApps(@NonNull String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        List<AppInfo> filteredApps = new ArrayList<>();

        for (AppInfo appInfo : allApps) {
            if (normalizedQuery.isEmpty()
                    || appInfo.appName.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || appInfo.packageName.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                filteredApps.add(appInfo);
            }
        }

        adapter.submitList(filteredApps);
        emptyState.setVisibility(filteredApps.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        if (filteredApps.isEmpty()) {
            emptyState.setText(allApps.isEmpty() ? R.string.loading_apps : R.string.empty_apps);
        }
    }

    private String getSearchQuery() {
        Editable editable = searchInput.getText();
        return editable == null ? "" : editable.toString();
    }

    private void showPage(View pageToShow) {
        introPage.setVisibility(pageToShow == introPage ? View.VISIBLE : View.GONE);
        appsPage.setVisibility(pageToShow == appsPage ? View.VISIBLE : View.GONE);
        settingsPage.setVisibility(pageToShow == settingsPage ? View.VISIBLE : View.GONE);
    }
}
