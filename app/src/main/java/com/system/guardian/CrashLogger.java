package com.system.guardian;

import android.content.Context;
import android.util.Log;

import com.system.guardian.core.LogUploader;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class CrashLogger {

    public static void log(Context context, String tag, String msg) {
        log(context, tag, msg, Log.INFO); // Default
    }

    public static void log(Context context, String tag, String msg, int level) {
        String fullMsg = "[" + levelToText(level) + "] " + tag + ": " + msg + "\n";

        Log.println(level, "GuardianLogger", fullMsg.trim());

        new Thread(() -> {
            try {
                FileOutputStream fos = context.openFileOutput("crashlog.txt", Context.MODE_APPEND);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(fullMsg);
                osw.close();

                LogUploader.uploadLog(context, fullMsg);

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
