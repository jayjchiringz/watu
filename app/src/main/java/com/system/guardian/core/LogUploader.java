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
                URL url = new URL("https://DigiServe25.pythonanywhere.com/upload-log");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");

                // Include device token for backend linking
                @SuppressLint("HardwareIds") String deviceToken = Settings.Secure.getString(
                        context.getContentResolver(), Settings.Secure.ANDROID_ID);
                conn.setRequestProperty("X-DEVICE-TOKEN", deviceToken);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(logText.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                conn.getInputStream().close(); // Trigger complete
                Log.d("LogUploader", "✅ Log uploaded: " + logText);

            } catch (Exception e) {
                Log.e("LogUploader", "❌ Failed to upload log", e);
            }
        }).start();
    }
}
