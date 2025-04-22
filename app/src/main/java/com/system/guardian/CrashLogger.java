package com.system.guardian;

import android.content.Context;
import android.util.Log;

import com.system.guardian.core.LogUploader;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class CrashLogger {

    public static void log(Context context, String tag, String msg) {
        new Thread(() -> {
            try {
                FileOutputStream fos = context.openFileOutput("crashlog.txt", Context.MODE_APPEND);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                String fullMsg = tag + ": " + msg + "\n";
                osw.write(fullMsg);
                osw.close();

                LogUploader.uploadLog(context, fullMsg);

            } catch (Exception e) {
                Log.e("CrashLogger", "❌ Log write or upload failed", e);
            }
        }).start();
    }

    // ✅ NEW: Force sync unsent logs from disk (e.g., queued)
    public static void flush(Context context) {
        new Thread(() -> {
            try {
                LogUploader.processQueue(context);
                Log.d("CrashLogger", "📤 CrashLogger.flush() triggered");
            } catch (Exception e) {
                Log.e("CrashLogger", "❌ flush() failed", e);
            }
        }).start();
    }
}
