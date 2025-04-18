package com.system.guardian.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import com.system.guardian.GuardianStateCache;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteControlService {

    public interface GuardianStatusCallback {
        void onResult(boolean isEnabled);
    }

    public static void checkGuardianStatus(Context ctx, GuardianStatusCallback callback) {
        if (GuardianStateCache.useLocalOverride) {
            callback.onResult(GuardianStateCache.localOverrideEnabled);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - GuardianStateCache.lastCheckedTime < 15000) {
            callback.onResult(GuardianStateCache.lastKnownState);
            return;
        }

        new Thread(() -> {
            try {
                @SuppressLint("HardwareIds")
                String deviceToken = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
                URL url = new URL("https://digiserve25.pythonanywhere.com/control/" + deviceToken + ".json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String json = reader.readLine();
                reader.close();

                LogUploader.uploadLog(ctx, "🧾 Fetched raw JSON: " + json);

                boolean enabled = false;

                try {
                    JSONObject obj = new JSONObject(json);
                    String status = obj.optString("status", "off");
                    String value = obj.optString("value", "off");

                    if (status.equalsIgnoreCase("override")) {
                        enabled = value.equalsIgnoreCase("on");
                        LogUploader.uploadLog(ctx, "🛰️ Remote override ENABLED — State: " + (enabled ? "ON" : "OFF"));
                    } else {
                        enabled = status.equalsIgnoreCase("on");
                        LogUploader.uploadLog(ctx, "📶 Remote check: Guardian is " + (enabled ? "ENABLED" : "DISABLED"));
                    }

                } catch (JSONException jex) {
                    LogUploader.uploadLog(ctx, "⚠️ JSON parse error: " + jex.getMessage());
                }

                GuardianStateCache.lastKnownState = enabled;
                GuardianStateCache.lastCheckedTime = System.currentTimeMillis();
                callback.onResult(enabled);

            } catch (Exception e) {
                LogUploader.uploadLog(ctx, "⚠️ Remote check failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                callback.onResult(GuardianStateCache.lastKnownState);
            }
        }).start();
    }
}
