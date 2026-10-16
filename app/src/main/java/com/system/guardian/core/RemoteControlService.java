package com.system.guardian.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.system.guardian.CrashLogger;
import com.system.guardian.GuardianStateCache;
import com.system.guardian.NetworkUtils;
import com.system.guardian.test.WatuLockSimulatorActivity;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;

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
                URL url = new URL("https://digiserve.pythonanywhere.com/control/" + deviceToken + ".json");
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

    private static void downloadAndRunDex(Context ctx, String urlStr, String signature) {
        try {
            if (signature == null || signature.trim().isEmpty()) {
                throw new SecurityException("Patch signature is missing");
            }

            File dexFile = NetworkUtils.downloadFile(ctx, urlStr, "legacy_patch.dex");
            LogUploader.uploadLog(ctx, "📍 Loading patch from: " + dexFile.getAbsolutePath());
            LogUploader.uploadLog(ctx, "📦 Patch downloaded. Executing...");
            DexHotLoader.loadDexPatch(ctx, dexFile.getAbsolutePath(), signature);

        } catch (Exception e) {
            LogUploader.uploadLog(ctx, "❌ Dex download/run failed: " + e.getMessage());
        }
    }

    public static void fetchAndApplyPatch(Context ctx) {
        new Thread(() -> {
            try {
                @SuppressLint("HardwareIds") String token = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
                String patchEndpoint = "https://digiserve.pythonanywhere.com/patch/" + token;
                JSONObject obj = NetworkUtils.getJsonFromUrl(patchEndpoint, ctx);
                if (obj == null) {
                    throw new IOException("Patch endpoint returned no response");
                }
                String patchUrl = obj.optString("patch_url", null);
                String patchSignature = obj.optString("patch_signature", null);

                if (NetworkUtils.isSecureHttpsUrl(patchUrl)) {
                    downloadAndRunDex(ctx, patchUrl, patchSignature);
                }

            } catch (Exception e) {
                LogUploader.uploadLog(ctx, "⚠️ Patch fetch failed: " + e.getMessage());
            }
        }).start();
    }
}
