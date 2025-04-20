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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
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

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());

        Button refreshButton = findViewById(R.id.refreshButton);
        Button toggleOverlay = findViewById(R.id.toggleOverlay);
        Button toggleGuardianButton = findViewById(R.id.toggleGuardianButton);

        // 🔄 Delayed initializers to avoid cold boot ANR
        handler.postDelayed(() -> FirebaseApp.initializeApp(getApplicationContext()), 3000);

        handler.postDelayed(() -> {
            Intent svc = new Intent(this, WatchdogForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
        }, 4000);

        handler.postDelayed(this::loadLogs, 6000);

        handler.postDelayed(() -> {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                overlayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(overlayIntent);
            }
        }, 2000);

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
            Toast.makeText(this, "🏁 App started up cleanly", Toast.LENGTH_SHORT).show();
            loadLogs();
            isWatuRunning();
        });

        toggleOverlay.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                overlayActive = !overlayActive;
                if (overlayActive) {
                    OverlayBlocker.show(this);
                    Toast.makeText(this, "Overlay Activated", Toast.LENGTH_SHORT).show();
                } else {
                    OverlayBlocker.hide(this);
                    Toast.makeText(this, "Overlay Deactivated", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Overlay permission not granted", Toast.LENGTH_LONG).show();
            }
        });

        toggleGuardianButton.setOnClickListener(v -> {
            guardianLocallyEnabled = !guardianLocallyEnabled;
            String status = guardianLocallyEnabled ? "ENABLED" : "DISABLED";
            Toast.makeText(this, "Guardian manually set to " + status, Toast.LENGTH_SHORT).show();
            toggleGuardianButton.setText("Guardian: " + status);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Run after UI settles
        getWindow().getDecorView().post(() -> {
            new Handler().postDelayed(() -> {
                FirebaseApp.initializeApp(getApplicationContext());

                Intent svc = new Intent(this, WatchdogForegroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(svc);
                } else {
                    startService(svc);
                }

                Intent pollIntent = new Intent(MainActivity.this, ControlPollerService.class);
                ControlPollerService.enqueueWork(MainActivity.this, pollIntent);

                loadLogs();

                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    overlayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(overlayIntent);
                }
            }, 2000); // Delay more if needed
        });
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
                    if (!line.toLowerCase().contains("guardian")) continue;
                    lines.addFirst(line);
                    if (lines.size() > MAX_LOG_LINES) lines.removeLast();
                }
                reader.close();

                StringBuilder builder = new StringBuilder();
                for (String s : lines) builder.append(s).append("\n");
                runOnUiThread(() -> logTextView.setText(builder.toString()));
            } catch (Exception e) {
                runOnUiThread(() -> logTextView.setText("No logs or error: " + e.getMessage()));
            }
        }).start();
    }

    private boolean isWatuRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.processName.equals("com.watuke.app")) {
                    return true;
                }
            }
        }
        return false;
    }
}
