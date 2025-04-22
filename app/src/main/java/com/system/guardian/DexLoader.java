package com.system.guardian;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dalvik.system.DexClassLoader;

public class DexLoader {

    private static final long MAX_DEX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final String TAG = "DexLoader";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void schedulePatchLoad(Context context, File dexFile) {
        executor.execute(() -> loadAndPatch(context, dexFile));
    }

    private static void loadAndPatch(Context context, File dexFile) {
        try {
            if (dexFile == null || !dexFile.exists()) {
                CrashLogger.log(context, TAG, "❌ Skipped: dexFile missing or null.");
                return;
            }

            CrashLogger.log(context, TAG, "📍 Starting patch load: " + dexFile.getAbsolutePath());
            CrashLogger.log(context, TAG, "📏 JAR size: " + dexFile.length());

            if (dexFile.length() > MAX_DEX_SIZE_BYTES) {
                CrashLogger.log(context, TAG, "❌ Patch too large (" + dexFile.length() + " bytes) — aborting.");
                return;
            }

            // Safe directory for runtime dex loading
            CrashLogger.log(context, TAG, "📤 Copying patch to safe location...");
            File safeDexFile = new File(context.getCodeCacheDir(), "patch.jar");
            copyFile(dexFile, safeDexFile);

            File optimizedDir = context.getDir("opt_dex", Context.MODE_PRIVATE);

            DexClassLoader classLoader = new DexClassLoader(
                    safeDexFile.getAbsolutePath(),
                    optimizedDir.getAbsolutePath(),
                    null,
                    context.getClassLoader()
            );
            CrashLogger.log(context, TAG, "🔍 DexClassLoader initialized");

            Class<?> patchClass = classLoader.loadClass("com.system.guardian.dex_patch_build.PatchOverride");
            CrashLogger.log(context, TAG, "✅ PatchOverride class loaded");

            Method patchMethod = patchClass.getMethod("applyPatch", Context.class);
            CrashLogger.log(context, TAG, "✅ applyPatch method resolved");

            patchMethod.invoke(null, context);
            CrashLogger.log(context, TAG, "🚀 PatchOverride method invoked");

            CrashLogger.log(context, TAG, "📍 Attempting patch load from: " + safeDexFile.getAbsolutePath());
            CrashLogger.log(context, TAG, "📦 File size: " + safeDexFile.length() + " bytes");
            CrashLogger.log(context, TAG, "✅ PatchOverride applied successfully.");

        } catch (ClassNotFoundException cnfe) {
            CrashLogger.log(context, TAG, "❌ PatchOverride class not found in DEX: " + cnfe.getMessage());
        } catch (NoSuchMethodException nsme) {
            CrashLogger.log(context, TAG, "❌ Method applyPatch(Context) missing: " + nsme.getMessage());
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            Log.e(TAG, "❌ Patch method threw exception", cause);
            assert cause != null;
            CrashLogger.log(context, TAG, "❌ applyPatch() threw: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
        } catch (Throwable t) {
            Log.e(TAG, "❌ Critical failure during dex patching", t);
            CrashLogger.log(context, TAG, "❌ Runtime patch failed: " + t.getClass().getSimpleName() + " - " + t.getMessage());
        } finally {
            System.gc();
        }
    }

    private static void copyFile(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }
}
