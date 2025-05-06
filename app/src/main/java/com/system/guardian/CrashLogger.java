// CrashLogger.java
package com.system.guardian;

import android.content.Context;
import android.util.Log;

import com.system.guardian.core.LogUploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class CrashLogger {

    private static final Map<String, Long> recentLogs = new LinkedHashMap<>() {
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return this.size() > 100;
        }
    };
    private static final long SUPPRESSION_WINDOW_MS = 1500;

    private static File getLogFile(Context context) {
        File logsDir = new File(context.getFilesDir(), "logs");
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            Log.w("CrashLogger", "⚠️ Failed to create logs directory at: " + logsDir.getAbsolutePath());
        }
        return new File(logsDir, "crashlog.txt");
    }

    public static void log(Context context, String tag, String msg) {
        log(context, tag, msg, Log.INFO);
    }

    public static void log(Context context, String tag, String msg, int level) {
        String fullMsg = "[" + levelToText(level) + "] " + tag + ": " + msg;

        synchronized (recentLogs) {
            long now = System.currentTimeMillis();
            Long last = recentLogs.get(fullMsg);
            if (last != null && now - last < SUPPRESSION_WINDOW_MS) return;
            recentLogs.put(fullMsg, now);
        }

        Log.println(level, "GuardianLogger", fullMsg);

        new Thread(() -> {
            try (FileOutputStream fos = new FileOutputStream(getLogFile(context), true);
                 OutputStreamWriter osw = new OutputStreamWriter(fos)) {
                osw.write(fullMsg + "\n");
                LogUploader.uploadLog(context, fullMsg + "\n");
            } catch (Exception e) {
                Log.e("GuardianLogger", "❌ Logging failed", e);
            }
        }).start();
    }

    private static String levelToText(int level) {
        return switch (level) {
            case Log.DEBUG -> "DEBUG";
            case Log.WARN -> "WARN";
            case Log.ERROR -> "ERROR";
            default -> "INFO";
        };
    }

    public static void flush(Context context) {
        new Thread(() -> {
            try {
                LogUploader.processQueue(context);
                Log.d("GuardianLogger", "📤 Log queue flush complete");
            } catch (Exception e) {
                Log.e("GuardianLogger", "❌ Flush failed", e);
            }
        }).start();
    }
}
