package com.system.guardian;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.AppOpsManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends Activity {

    private boolean guardianLocallyEnabled = true;
    private TextView logTextView;
    private boolean overlayActive = false;
    private static final int MAX_LOG_LINES = 100;
    private static final int LOG_SIZE_LIMIT = 1024 * 1024; // 1MB
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static boolean initialized = false;

    // Add this helper method to your MainActivity class:
    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void setInitialized(boolean initialized) {
        MainActivity.initialized = initialized;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityInterceptor.check(this);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());

        Button refreshButton = findViewById(R.id.refreshButton);
        Button toggleOverlay = findViewById(R.id.toggleOverlay);
        Button toggleGuardianButton = findViewById(R.id.toggleGuardianButton);

        // ✅ Firebase init off UI thread
        new Thread(() -> FirebaseApp.initializeApp(getApplicationContext())).start();

        // ✅ Consolidated delayed service startup
        handler.postDelayed(() -> {
            Intent svc = new Intent(this, WatchdogForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
            Intent pollIntent = new Intent(this, ControlPollerService.class);
            ControlPollerService.enqueueWork(this, pollIntent);
        }, 4000);

        // ✅ Load logs after UI ready
        handler.postDelayed(this::loadLogs, 6000);

        // ✅ Overlay permission single-time prompt
        handler.postDelayed(() -> {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                overlayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(overlayIntent);
            }
        }, 2000);

        // 🔍 Modular check: Prompt for Usage Stats permission if not granted
        handler.postDelayed(() -> {
            if (!hasUsageStatsPermission(this)) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }, 2500);

        // Safe DevicePolicyManager check
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName compName = new ComponentName(this, AdminReceiver.class);
        if (dpm != null && !dpm.isAdminActive(compName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin for secure operations.");
            startActivityForResult(intent, 1);
        }

        refreshButton.setOnClickListener(v -> {
            String msg = "\uD83C\uDFC1 App started up cleanly";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            CrashLogger.log(this, "MainActivity", msg);
            loadLogs();
            isWatuRunning();
        });

        toggleOverlay.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                overlayActive = !overlayActive;
                String msg;
                if (overlayActive) {
                    OverlayBlocker.show(this);
                    msg = "Overlay Activated";
                } else {
                    OverlayBlocker.hide(this);
                    msg = "Overlay Deactivated";
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                CrashLogger.log(this, "MainActivity", msg);
            } else {
                String msg = "Overlay permission not granted";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                CrashLogger.log(this, "MainActivity", msg);
            }
        });

        toggleGuardianButton.setOnClickListener(v -> {
            guardianLocallyEnabled = !guardianLocallyEnabled;
            String status = guardianLocallyEnabled ? "ENABLED" : "DISABLED";
            String msg = "Guardian manually set to " + status;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            CrashLogger.log(this, "MainActivity", msg);
            toggleGuardianButton.setText("Guardian: " + status);

            // 🧬 Sync GhostMode Status
            GuardianStateCache.isGhostModeEnabled = !guardianLocallyEnabled;

            // 👻 Start/Stop GhostModeService based on Guardian status
            if (GuardianStateCache.isGhostModeEnabled) {
                GhostModeService.start(this);
            } else {
                stopService(new Intent(this, GhostModeService.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ No heavy init here now
    }

    @SuppressLint("SetTextI18n")
    private void loadLogs() {
        new Thread(() -> {
            try {
                File file = new File(getFilesDir(), "crashlog.txt");
                if (file.length() > LOG_SIZE_LIMIT) {
                    runOnUiThread(() -> logTextView.setText("Log too large to load."));
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput("crashlog.txt")));
                LinkedList<String> lines = new LinkedList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    lines.addFirst(line);
                    if (lines.size() > MAX_LOG_LINES) lines.removeLast();
                }
                reader.close();

                StringBuilder builder = new StringBuilder();
                for (String s : lines) builder.append(s).append("\n");
                runOnUiThread(() -> logTextView.setText(builder.toString()));
            } catch (Exception e) {
                String msg = "No logs or error: " + e.getMessage();
                runOnUiThread(() -> logTextView.setText(msg));
                CrashLogger.log(this, "MainActivity", msg);
            }
        }).start();
    }

    private void isWatuRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.processName.equals("com.watuke.app")) {
                    CrashLogger.log(this, "MainActivity", "⚠️ Watu is currently RUNNING");
                    return;
                }
            }
        }
        CrashLogger.log(this, "MainActivity", "✅ Watu not running");
    }
}
