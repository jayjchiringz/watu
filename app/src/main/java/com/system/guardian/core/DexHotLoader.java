package com.system.guardian.core;

import android.content.Context;
import android.util.Log;
import dalvik.system.DexClassLoader;

import java.io.File;

public class DexHotLoader {

    private static final String TAG = "DexHotLoader";

    public static void loadDexPatch(Context ctx, String dexPath) {
        try {
            File optimizedDir = ctx.getDir("dexopt", Context.MODE_PRIVATE);
            Log.d(TAG, "Dex path: " + dexPath);
            Log.d(TAG, "Optimized dir: " + optimizedDir.getAbsolutePath());

            DexClassLoader loader = new DexClassLoader(
                    dexPath,
                    optimizedDir.getAbsolutePath(),
                    null,
                    ctx.getClassLoader()
            );

            Class<?> patchClass = loader.loadClass("com.patch.PatchExecutor");

            Object instance = patchClass.getDeclaredConstructor().newInstance();

            if (instance instanceof Runnable) {
                ((Runnable) instance).run();
                Log.d(TAG, "PatchExecutor executed successfully.");
            } else {
                Log.e(TAG, "Loaded class is not Runnable.");
            }

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "PatchExecutor class not found in dex: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to load dex patch", e);
        }
    }
}
