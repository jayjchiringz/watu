package com.system.guardian.background;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.system.guardian.CrashLogger;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LogUploadWorker extends Worker {

    private static final String TAG = "LogUploadWorker";

    public LogUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "🚀 Upload task started");

        Context context = getApplicationContext();
        File logDir = new File(context.getFilesDir(), "logs"); // Adjust if CrashLogger uses another path

        try {
            // Flush in-memory logs to disk
            CrashLogger.flush(context);

            if (!logDir.exists() || logDir.listFiles() == null) {
                Log.w(TAG, "📭 No log files to upload");
                return Result.success();  // Nothing to do
            }

            for (File logFile : Objects.requireNonNull(logDir.listFiles())) {
                if (!logFile.getName().endsWith(".log")) continue;

                Log.i(TAG, "📤 Uploading: " + logFile.getName());

                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.MediaType mediaType = okhttp3.MediaType.parse("text/plain");

                okhttp3.RequestBody body = okhttp3.RequestBody.create(logFile, mediaType);
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url("https://digiserve25.pythonanywhere.com/upload-logs/") // Update if different
                        .addHeader("X-DEVICE-TOKEN", getDeviceToken(context))
                        .post(body)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                int code = response.code();

                if (code >= 200 && code < 300) {
                    Log.i(TAG, "✅ Uploaded successfully: " + logFile.getName());
                    if (!logFile.delete()) {
                        Log.w(TAG, "⚠️ Failed to delete uploaded log file: " + logFile.getName());
                    }

                } else {
                    Log.e(TAG, "❌ Upload failed [" + code + "]: " + response.message());
                    return Result.retry();
                }
            }

            // Reschedule this worker after 2 minutes
            OneTimeWorkRequest retryWork = new OneTimeWorkRequest.Builder(LogUploadWorker.class)
                    .setInitialDelay(2, TimeUnit.MINUTES)
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .build();

            WorkManager.getInstance(getApplicationContext()).enqueue(retryWork);

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "❌ Log upload failed", e);
            return Result.retry();
        }
    }

    @SuppressLint("HardwareIds")
    private static String getDeviceToken(Context context) {
        return android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
        );
    }
}
