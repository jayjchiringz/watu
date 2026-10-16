package com.system.guardian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";

    public static JSONObject getJsonFromUrl(String urlStr, Context context) {
        HttpURLConnection conn = null;
        InputStream in = null;

        try {
            if (!isSecureHttpsUrl(urlStr)) {
                throw new IllegalArgumentException("Control URL must use HTTPS");
            }
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");

            // 🔒 Optional: Comment this temporarily if server rejects custom headers
            String token = getDeviceToken(context);
            conn.setRequestProperty("X-DEVICE-TOKEN", token);

            // ✅ Add User-Agent to mimic browser
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");

            // 🌐 Log URL being requested
            Log.d(TAG, "🌐 Connecting to: " + urlStr);

            conn.connect();
            int responseCode = conn.getResponseCode();
            if (!"https".equalsIgnoreCase(conn.getURL().getProtocol())) {
                throw new IOException("Control request redirected away from HTTPS");
            }

            // ✅ Log response code
            Log.d(TAG, "🌐 HTTP Response Code: " + responseCode);

            // ❌ Log error response if any
            if (responseCode >= 400) {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    StringBuilder errorText = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorText.append(line);
                    }
                    errorReader.close();
                    Log.e(TAG, "❌ Server Error: " + errorText.toString());
                }
                return null;
            }

            // ✅ Proceed to parse JSON
            in = new BufferedInputStream(conn.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();
            return new JSONObject(result.toString());

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage(), e);

            StringBuilder fullStackTrace = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                fullStackTrace.append("\n\tat ").append(element.toString());
            }
            Log.e(TAG, "📛 Full Stack Trace: " + fullStackTrace);

            CrashLogger.log(context, TAG, "❌ Failed to fetch JSON: " + e.getMessage());
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    public static File downloadFile(Context context, String fileUrl, String filename) throws Exception {
        HttpURLConnection conn = null;
        InputStream input = null;
        FileOutputStream output = null;

        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid URL: fileUrl is null or empty");
        }

        if (!isSecureHttpsUrl(fileUrl)) {
            throw new IllegalArgumentException("Patch and update URLs must use HTTPS");
        }

        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);

            String token = getDeviceToken(context);
            conn.setRequestProperty("X-DEVICE-TOKEN", token);
            conn.connect();
            int responseCode = conn.getResponseCode();

            if (!"https".equalsIgnoreCase(conn.getURL().getProtocol())) {
                throw new IOException("Download redirected away from HTTPS");
            }
            if (responseCode >= 400) {
                throw new IOException("Download failed with HTTP status " + responseCode);
            }

            File file = new File(context.getCodeCacheDir(), filename);
            input = conn.getInputStream();
            output = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            return file;

        } catch (IOException e) {
            CrashLogger.log(context, TAG, "❌ Download failed: " + e.getMessage());
            throw e;

        } finally {
            if (input != null) try { input.close(); } catch (IOException ignored) {}
            if (output != null) try { output.close(); } catch (IOException ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    public static boolean isSecureHttpsUrl(String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            return false;
        }

        try {
            URL url = new URL(urlStr);
            return "https".equalsIgnoreCase(url.getProtocol())
                    && url.getHost() != null
                    && !url.getHost().isEmpty()
                    && url.getUserInfo() == null;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("HardwareIds")
    private static String getDeviceToken(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
