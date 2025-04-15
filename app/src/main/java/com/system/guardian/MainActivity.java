package com.system.guardian;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import android.app.ActivityManager;

public class MainActivity extends Activity {

    private TextView logTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);
        Button refreshButton = findViewById(R.id.refreshButton);

        // 🔹 Force a test log entry
        CrashLogger.log(this, "MainActivity", "TEST LOG: MainActivity started successfully");

        // Trigger Device Admin prompt
        ComponentName compName = new ComponentName(this, AdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin for secure operations.");
        startActivityForResult(intent, 1);

        // Load logs on start
        loadLogs();

        // Refresh logs
        refreshButton.setOnClickListener(v -> {
            loadLogs();         // Refresh crashlog.txt display
            isWatuRunning();    // 🔍 Check if Watu app process is alive
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

    private boolean isWatuRunning() {
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);

        if (am != null) {
            List<android.app.ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();

            for (android.app.ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.processName.equals("com.watuke.app")) {
                    CrashLogger.log(this, "ProcessCheck", "✅ Watu app process is RUNNING");
                    return true;
                }
            }
        }

        CrashLogger.log(this, "ProcessCheck", "❌ Watu app process is NOT running");
        return false;
    }
}
