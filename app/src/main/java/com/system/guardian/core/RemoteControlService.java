package com.system.guardian.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.system.guardian.CrashLogger;
import com.system.guardian.GuardianStateCache;
import com.system.guardian.test.WatuLockSimulatorActivity;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

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
                    boolean simulateWatu = obj.optBoolean("simulate_watu", false);

                    if (simulateWatu) {
                        Intent intent = new Intent(ctx, WatuLockSimulatorActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(intent);
                        CrashLogger.log(ctx, "SimTrigger", "🚨 Simulated Watu lock triggered from backend");
                        LogUploader.uploadLog(ctx, "🚨 Simulated Watu lock launched via control JSON");
                    }

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

    private static void downloadAndRunDex(Context ctx, String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            FileOutputStream out = ctx.openFileOutput("patch.dex", Context.MODE_PRIVATE);
            File dexFile = ctx.getFileStreamPath("patch.dex");
            InputStream in = conn.getInputStream();

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            out.close();
            in.close();

            LogUploader.uploadLog(ctx, "📍 Loading patch from: " + dexFile.getAbsolutePath());
            LogUploader.uploadLog(ctx, "📦 Patch downloaded. Executing...");
            DexHotLoader.loadDexPatch(ctx, dexFile.getAbsolutePath());

        } catch (Exception e) {
            LogUploader.uploadLog(ctx, "❌ Dex download/run failed: " + e.getMessage());
        }
    }

    public static void fetchAndApplyPatch(Context ctx) {
        new Thread(() -> {
            try {
                @SuppressLint("HardwareIds") String token = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
                URL url = new URL("https://your-backend.com/patch/" + token);  // ✅ Change to your real endpoint
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String json = reader.readLine();
                reader.close();

                JSONObject obj = new JSONObject(json);
                String patchUrl = obj.optString("patch_url", null);

                if (!patchUrl.equals("null")) {
                    downloadAndRunDex(ctx, patchUrl);
                }

            } catch (Exception e) {
                LogUploader.uploadLog(ctx, "⚠️ Patch fetch failed: " + e.getMessage());
            }
        }).start();
    }
}
