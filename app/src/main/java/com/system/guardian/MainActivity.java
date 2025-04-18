package com.system.guardian;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.RemoteMessage;
import com.system.guardian.core.LogUploader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private boolean guardianLocallyEnabled = true;
    private TextView logTextView;
    private boolean overlayActive = false;
    private static final int MAX_LOG_LINES = 100;

    private final BroadcastReceiver firebaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CrashLogger.log(context, "FirebaseSniffer", timestamp() + " 📡 Firebase message or event detected: " + intent.getAction());
        }
    };

    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());

        Button refreshButton = findViewById(R.id.refreshButton);
        Button toggleOverlay = findViewById(R.id.toggleOverlay);

        // Register Firebase Receiver safely for Android 13+
        IntentFilter firebaseFilter = new IntentFilter("com.google.firebase.MESSAGING_EVENT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(firebaseReceiver, firebaseFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(firebaseReceiver, firebaseFilter, null, null);
        }

        CrashLogger.log(this, "MainActivity", timestamp() + " TEST LOG: MainActivity started successfully");
        LogUploader.uploadLog(this, "🚀 App launched: MainActivity");

        if (!Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(overlayIntent);
        }

        ComponentName compName = new ComponentName(this, AdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (!dpm.isAdminActive(compName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin for secure operations.");
            startActivityForResult(intent, 1);
        }

        loadLogs();

        refreshButton.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing logs...", Toast.LENGTH_SHORT).show();
            loadLogs();
            isWatuRunning();
            auditDevicePolicy();
            LogUploader.uploadLog(this, "🔄 Log refresh + audit triggered from UI");
        });

        toggleOverlay.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                overlayActive = !overlayActive;
                if (overlayActive) {
                    OverlayBlocker.show(this);
                    CrashLogger.log(this, "Overlay", timestamp() + " Overlay manually activated from UI");
                    Toast.makeText(this, "Overlay Activated", Toast.LENGTH_SHORT).show();
                    LogUploader.uploadLog(this, "🛡️ Overlay manually activated via UI");
                } else {
                    OverlayBlocker.hide(this);
                    CrashLogger.log(this, "Overlay", timestamp() + " Overlay manually deactivated from UI");
                    Toast.makeText(this, "Overlay Deactivated", Toast.LENGTH_SHORT).show();
                    LogUploader.uploadLog(this, "🧯 Overlay manually deactivated via UI");
                }
            } else {
                Toast.makeText(this, "Overlay permission not granted", Toast.LENGTH_LONG).show();
            }
        });

        Button toggleGuardianButton = findViewById(R.id.toggleGuardianButton);

        toggleGuardianButton.setOnClickListener(v -> {
            guardianLocallyEnabled = !guardianLocallyEnabled;

            // Commented out: allow remote control unless manual override is required
            // GuardianStateCache.useLocalOverride = true;
            // GuardianStateCache.localOverrideEnabled = guardianLocallyEnabled;

            String status = guardianLocallyEnabled ? "ENABLED" : "DISABLED";
            Toast.makeText(this, "Guardian manually set to " + status, Toast.LENGTH_SHORT).show();

            CrashLogger.log(this, "ManualToggle", timestamp() + " Guardian manually set to " + status);
            LogUploader.uploadLog(this, "⚙️ Guardian manually set to " + status + " via app UI");

            // Update toggle button label
            toggleGuardianButton.setText("Guardian: " + status);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLogs();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(firebaseReceiver);
        super.onDestroy();
    }

    @SuppressLint("SetTextI18n")
    private void loadLogs() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput("crashlog.txt")));
            LinkedList<String> lines = new LinkedList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.addFirst(line);
                if (lines.size() > MAX_LOG_LINES) {
                    lines.removeLast();
                }
            }
            StringBuilder builder = new StringBuilder();
            for (String s : lines) {
                builder.append(s).append("\n");
            }
            logTextView.setText(builder.toString());
            reader.close();
        } catch (Exception e) {
            logTextView.setText("No logs found or error: " + e.getMessage());
        }
    }

    private boolean isWatuRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.processName.equals("com.watuke.app")) {
                    CrashLogger.log(this, "ProcessCheck", timestamp() + " ✅ Watu app process is RUNNING");
                    return true;
                }
            }
        }
        CrashLogger.log(this, "ProcessCheck", timestamp() + " ❌ Watu app process is NOT running");
        return false;
    }

    private void auditDevicePolicy() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName compName = new ComponentName(this, AdminReceiver.class);

        boolean isAdmin = dpm.isAdminActive(compName);
        CrashLogger.log(this, "Audit", timestamp() + " App Admin Active: " + isAdmin);

        boolean isLockTaskPermitted = dpm.isLockTaskPermitted(getPackageName());
        CrashLogger.log(this, "Audit", timestamp() + " LockTask Permitted: " + isLockTaskPermitted);
    }

    private String timestamp() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
