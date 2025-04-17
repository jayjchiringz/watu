package com.system.guardian.core;

import android.content.Context;
import com.system.guardian.core.LogUploader; // ensure this is imported
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteControlService {
    public static boolean isGuardianEnabled(Context ctx) {
        try {
            URL url = new URL("https://DigiServe25.pythonanywhere.com/control.json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String json = reader.readLine();

            boolean enabled = json.contains("\"status\":\"on\"");
            LogUploader.uploadLog(ctx, "📶 Remote check: Guardian is " + (enabled ? "ENABLED" : "DISABLED"));

            return enabled;
        } catch (Exception e) {
            LogUploader.uploadLog(ctx, "⚠️ Remote check failed, defaulting to ENABLED");
            return true; // fail-safe
        }
    }
}
