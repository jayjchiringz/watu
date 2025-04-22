package com.system.guardian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.system.guardian.core.LogUploader;

public class OverlayBlocker {
    @SuppressLint("StaticFieldLeak")
    private static View overlayView;

    // ✅ Track internal state
    private static boolean isShowing = false;

    @SuppressLint("ObsoleteSdkInt")
    public static void show(Context context) {
        if (isShowing || overlayView != null) return;

        // ✅ Prevent crash: Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !android.provider.Settings.canDrawOverlays(context)) {
            CrashLogger.log(context, "OverlayBlocker", "❌ Missing overlay permission. Skipping draw.");
            return;
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        overlayView = new FrameLayout(context);
        overlayView.setBackgroundColor(0x00000000); // transparent

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;

        try {
            wm.addView(overlayView, params);
            isShowing = true;
            CrashLogger.log(context, "OverlayBlocker", "🛡️ Shield overlay deployed");
            LogUploader.uploadLog(context, "🛡️ OverlayBlocker activated");
        } catch (Exception e) {
            CrashLogger.log(context, "OverlayBlocker", "❌ Failed to add overlay: " + e.getMessage());
            isShowing = false;
        }
    }

    public static void hide(Context context) {
        if (overlayView != null && isShowing) {
            try {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                wm.removeView(overlayView);
            } catch (Exception e) {
                CrashLogger.log(context, "OverlayBlocker", "⚠️ Failed to remove overlay: " + e.getMessage());
            }
            overlayView = null;
            isShowing = false;
            CrashLogger.log(context, "OverlayBlocker", "🧯 Shield overlay removed");
            LogUploader.uploadLog(context, "🧯 OverlayBlocker deactivated");
        }
    }

    // ✅ New method
    public static boolean isShowing() {
        return isShowing;
    }
}
