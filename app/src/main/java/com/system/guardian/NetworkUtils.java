package com.system.guardian;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility for basic HTTP and file operations
 */
public class NetworkUtils {

    public static JSONObject getJsonFromUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            conn.connect();

            InputStream in = new BufferedInputStream(conn.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();
            in.close();
            return new JSONObject(result.toString());

        } catch (Exception e) {
            Log.e("NetworkUtils", "Failed to fetch JSON: " + e.getMessage(), e);
            return null;
        }
    }

    public static File downloadFile(Context context, String fileUrl, String filename) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();

        File file = new File(context.getExternalFilesDir(null), filename);
        FileOutputStream output = new FileOutputStream(file);

        InputStream input = conn.getInputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }

        output.close();
        input.close();
        return file;
    }
}
