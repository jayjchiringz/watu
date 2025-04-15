package com.system.guardian;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends Activity {

    private TextView logTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);
        Button refreshButton = findViewById(R.id.refreshButton);
        Button toggleOverlay = findViewById(R.id.toggleOverlay);

        CrashLogger.log(this, "MainActivity", "TEST LOG: MainActivity started successfully");

        // Prompt overlay permission if needed
        if (!Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(overlayIntent);
        }

        // Trigger Device Admin prompt if not active
        ComponentName compName = new ComponentName(this, AdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (!dpm.isAdminActive(compName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin for secure operations.");
            startActivityForResult(intent, 1);
        }

        // Load logs on start
        loadLogs();

        // Refresh logs
        refreshButton.setOnClickListener(v -> {
            loadLogs();
            isWatuRunning();
        });

        // Manual overlay test button
        toggleOverlay.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    OverlayBlocker.show(this);
                }
                CrashLogger.log(this, "Overlay", "Overlay manually activated from UI");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadLogs() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput("crashlog.txt")));
            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

            logTextView.setText(builder.toString());
            reader.close();
        } catch (Exception e) {
            logTextView.setText("No logs found or error: " + e.getMessage());
        }
    }

    private void isWatuRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();

            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.processName.equals("com.watuke.app")) {
                    CrashLogger.log(this, "ProcessCheck", "✅ Watu app process is RUNNING");
                    return;
                }
            }
        }

        CrashLogger.log(this, "ProcessCheck", "❌ Watu app process is NOT running");
    }
}
