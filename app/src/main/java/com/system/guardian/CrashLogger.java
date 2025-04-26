package com.system.guardian;

import android.content.Context;
import android.util.Log;

import com.system.guardian.core.LogUploader;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class CrashLogger {

    public static void log(Context context, String tag, String msg) {
        String fullMsg = tag + ": " + msg + "\n";

        // ✅ Log to local logcat for debug visibility
        Log.i("GuardianLogger", fullMsg.trim());

        new Thread(() -> {
            try {
                FileOutputStream fos = context.openFileOutput("crashlog.txt", Context.MODE_APPEND);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(fullMsg);
                osw.close();

                // ✅ Upload attempt — if fails, it'll be queued
                LogUploader.uploadLog(context, fullMsg);

            } catch (Exception e) {
                Log.e("GuardianLogger", "❌ Logging failed", e);
            }
        }).start();
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
