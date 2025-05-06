package com.system.guardian.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public class LogUploader {

    private static final String TAG = "LogUploader";
    private static final String QUEUE_FILENAME = "upload_queue.txt";

    public static void uploadLog(Context context, String logText) {
        new Thread(() -> {
            try {
                boolean uploaded = attemptUpload(context, logText);
                if (!uploaded) {
                    queueLog(context, logText);
                    Log.w(TAG, "⚠️ Log upload failed — queued for retry.");
                } else {
                    Log.d(TAG, "✅ Log uploaded: " + logText);
                }

                processQueue(context);
            } catch (Exception e) {
                Log.e(TAG, "❌ Unexpected failure in uploadLog", e);
            }
        }).start();
    }

    private static boolean attemptUpload(Context context, String logText) {
        try {
            if (context.checkCallingOrSelfPermission(android.Manifest.permission.INTERNET)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "⚠️ INTERNET permission not granted.");
                return false;
            }

            URL url = new URL("https://digiserve25.pythonanywhere.com/upload-log");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");

            @SuppressLint("HardwareIds")
            String deviceToken = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceToken == null) deviceToken = "UNKNOWN";
            conn.setRequestProperty("X-DEVICE-TOKEN", deviceToken);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(logText.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            conn.getInputStream().close();
            return code >= 200 && code < 300;

        } catch (Exception e) {
            Log.e(TAG, "❌ Upload attempt failed", e);
            return false;
        }
    }

    private static void queueLog(Context context, String logText) {
        File logDir = new File(context.getFilesDir(), "logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            Log.w("CrashLogger", "⚠️ Failed to create logs directory at: " + logDir.getAbsolutePath());
        }

        File queueFile = new File(logDir, QUEUE_FILENAME);

        try (FileWriter writer = new FileWriter(queueFile, true)) {
            writer.write(logText.replace("\n", "␤") + "\n");
        } catch (IOException e) {
            Log.e(TAG, "❌ Failed to queue log", e);
        }
    }

    public static void processQueue(Context context) {
        File logDir = new File(context.getFilesDir(), "logs");
        File queueFile = new File(logDir, QUEUE_FILENAME);

        if (!queueFile.exists()) return;

        Queue<String> lines = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(queueFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.replace("␤", "\n"));
            }
        } catch (IOException e) {
            Log.e(TAG, "❌ Failed to read upload queue", e);
            return;
        }

        boolean allUploaded = true;
        for (String logLine : lines) {
            if (!attemptUpload(context, logLine)) {
                allUploaded = false;
                break;
            }
        }

        if (allUploaded && queueFile.delete()) {
            Log.d(TAG, "✅ Cleared upload queue after successful sync.");
        } else {
            Log.w(TAG, "⚠️ Queue partially uploaded. Retaining remaining logs.");
        }
    }
}
