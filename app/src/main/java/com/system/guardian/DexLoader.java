package com.system.guardian;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dalvik.system.DexClassLoader;

public class DexLoader {
    private static final long MAX_DEX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final String TAG = "DexLoader";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void schedulePatchLoad(Context context, File dexFile) {
        schedulePatchLoad(context, dexFile, false); // default non-forced
    }

    public static void schedulePatchLoad(Context context, File dexFile, boolean force) {
        executor.execute(() -> loadAndPatch(context, dexFile, force));
    }

    private static void loadAndPatch(Context context, File dexFile, boolean force) {
        File safeDexFile = null;

        try {
            if (dexFile == null || !dexFile.exists()) {
                CrashLogger.log(context, TAG, "❌ Skipped: dexFile missing or null.");
                return;
            }

            String patchId = computeSHA256(dexFile);
            SharedPreferences prefs = context.getSharedPreferences("dex_patch", Context.MODE_PRIVATE);
            String lastApplied = prefs.getString("last_patch_id", null);
            if (!force && patchId.equals(lastApplied)) {
                CrashLogger.log(context, TAG, "🔁 Patch already applied — skipping.");
                return;
            }

            CrashLogger.log(context, TAG, "📍 Starting patch load: " + dexFile.getAbsolutePath());
            CrashLogger.log(context, TAG, "📏 JAR size: " + dexFile.length());

            if (dexFile.length() > MAX_DEX_SIZE_BYTES) {
                CrashLogger.log(context, TAG, "❌ Patch too large (" + dexFile.length() + " bytes) — aborting.");
                return;
            }

            File dexSecureDir = context.getDir("dex_patch_secure", Context.MODE_PRIVATE);
            safeDexFile = new File(dexSecureDir, "patch.jar");

            copyFile(dexFile, safeDexFile);

            if (!safeDexFile.setWritable(false)) {
                CrashLogger.log(context, TAG, "⚠️ Failed to make patch file read-only.");
            }

            File optimizedDir = context.getDir("opt_dex", Context.MODE_PRIVATE);
            DexClassLoader classLoader = new DexClassLoader(
                    safeDexFile.getAbsolutePath(),
                    optimizedDir.getAbsolutePath(),
                    null,
                    context.getClassLoader()
            );

            Class<?> patchClass = classLoader.loadClass("com.system.guardian.dex_patch_build.PatchOverride");
            Method patchMethod = patchClass.getMethod("applyPatch", Context.class);
            patchMethod.invoke(null, context);

            CrashLogger.log(context, TAG, "✅ PatchOverride applied successfully.");
            prefs.edit().putString("last_patch_id", patchId).apply();

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            CrashLogger.log(context, TAG, "❌ Reflection error: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assert cause != null;
            CrashLogger.log(context, TAG, "❌ Patch method threw: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        } catch (Throwable t) {
            CrashLogger.log(context, TAG, "❌ Runtime error during patch: " + t.getMessage());
        } finally {
            if (safeDexFile != null && safeDexFile.exists()) {
                boolean deleted = safeDexFile.delete();
                CrashLogger.log(context, TAG, deleted ? "🧹 Patch file deleted post-load." : "⚠️ Patch deletion failed.");
            }
            System.gc();
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }

    private static String computeSHA256(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return file.getName() + "_" + file.length(); // fallback
        }
    }
}
