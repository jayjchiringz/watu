package com.system.guardian.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LogUploader {

    public static void uploadLog(Context context, String logText) {
        new Thread(() -> {
            try {
                // Safety: Check internet permission
                if (context.checkCallingOrSelfPermission(android.Manifest.permission.INTERNET)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w("LogUploader", "⚠️ INTERNET permission not granted. Skipping log upload.");
                    return;
                }

                URL url = new URL("https://digiserve25.pythonanywhere.com/upload-log");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");

                @SuppressLint("HardwareIds")
                String deviceToken = Settings.Secure.getString(
                        context.getContentResolver(), Settings.Secure.ANDROID_ID);
                if (deviceToken == null) deviceToken = "UNKNOWN";
                conn.setRequestProperty("X-DEVICE-TOKEN", deviceToken);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(logText.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                conn.getInputStream().close(); // ensure completion
                Log.d("LogUploader", "✅ Log uploaded: " + logText);

            } catch (Exception e) {
                Log.e("LogUploader", "❌ Failed to upload log", e);
            }
        }).start();
    }
}
