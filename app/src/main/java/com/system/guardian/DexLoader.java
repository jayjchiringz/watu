package com.system.guardian;

import android.content.Context;
import android.util.Log;

import java.io.File;
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

            if (dexFile.length() > MAX_DEX_SIZE_BYTES) {
                CrashLogger.log(context, TAG, "❌ Patch too large (" + dexFile.length() + " bytes) — aborting.");
                return;
            }

            File optimizedDir = context.getDir("opt_dex", Context.MODE_PRIVATE);

            DexClassLoader classLoader = new DexClassLoader(
                    dexFile.getAbsolutePath(),
                    optimizedDir.getAbsolutePath(),
                    null,
                    context.getClassLoader()
            );

            Class<?> patchClass = classLoader.loadClass("com.system.guardian.dex_patch_build.PatchOverride");
            Method patchMethod = patchClass.getMethod("applyPatch", Context.class);

            patchMethod.invoke(null, context);

            CrashLogger.log(context, TAG, "✅ PatchOverride applied successfully.");
        } catch (ClassNotFoundException cnfe) {
            CrashLogger.log(context, TAG, "❌ PatchOverride class not found in DEX.");
        } catch (NoSuchMethodException nsme) {
            CrashLogger.log(context, TAG, "❌ Method applyPatch(Context) missing.");
        } catch (Throwable t) {
            Log.e(TAG, "❌ Critical failure during dex patching", t);
            CrashLogger.log(context, TAG, "❌ Runtime patch failed: " + t.getClass().getSimpleName() + " - " + t.getMessage());
        } finally {
            // Optional GC nudge for low-end devices
            System.gc();
        }
    }
}
