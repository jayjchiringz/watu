package com.system.guardian;

import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class CrashLogger {
    public static void log(Context context, String tag, String msg) {
        try {
            FileOutputStream fos = context.openFileOutput("crashlog.txt", Context.MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(tag + ": " + msg + "\n");
            osw.close();
        } catch (Exception e) {
            Log.e("CrashLogger", "Log write failed", e);
        }
    }
}
