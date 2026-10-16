package com.system.guardian.core;

import android.content.Context;
import android.util.Log;

import com.system.guardian.PatchSignatureVerifier;

import dalvik.system.DexClassLoader;

import java.io.File;
import java.security.GeneralSecurityException;
import java.io.IOException;

public class DexHotLoader {

    private static final String TAG = "DexHotLoader";
    private static final long MAX_PATCH_SIZE_BYTES = 5 * 1024 * 1024;

    public static void loadDexPatch(Context ctx, String dexPath, String signature) {
        try {
            File dexFile = new File(dexPath);
            if (signature == null || signature.trim().isEmpty()) {
                Log.e(TAG, "Patch rejected because its signature is missing.");
                return;
            }
            if (!dexFile.isFile() || dexFile.length() > MAX_PATCH_SIZE_BYTES) {
                Log.e(TAG, "Patch rejected because it is missing or exceeds the size limit.");
                return;
            }
            if (!PatchSignatureVerifier.verify(ctx, dexFile, signature)) {
                Log.e(TAG, "Patch rejected because its signature is invalid.");
                return;
            }

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
        } catch (IOException | GeneralSecurityException e) {
            Log.e(TAG, "Patch signature verification failed", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load dex patch", e);
        }
    }
}
