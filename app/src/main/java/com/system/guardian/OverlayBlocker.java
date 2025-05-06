package com.system.guardian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class OverlayBlocker {

    @SuppressLint("StaticFieldLeak")
    private static View overlayView;
    private static boolean isShowing = false;
    private static boolean overlayLoggedMissing = false;

    public static void ensureOverlay(Context context) {
        if (!Settings.canDrawOverlays(context)) {
            if (!overlayLoggedMissing) {
                CrashLogger.log(context, "OverlayBlocker", "❌ Missing overlay permission. Aborting overlay.", Log.WARN);
                overlayLoggedMissing = true;
            }
        } else {
            overlayLoggedMissing = false; // Reset once permission granted
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void show(Context context) {
        show(context, false);
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void show(Context context, boolean wakeAndUnlock) {
        if (isShowing || overlayView != null) {
            CrashLogger.log(context, "OverlayBlocker", "⚠️ Overlay already shown, skipping.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(context)) {
            CrashLogger.log(context, "OverlayBlocker", "❌ Missing overlay permission. Aborting overlay.");
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                overlayView = new FrameLayout(context);
                overlayView.setBackgroundColor(0x00000000); // Fully transparent

                int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE;

                int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

                if (wakeAndUnlock) {
                    flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                }

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        overlayType,
                        flags,
                        PixelFormat.TRANSLUCENT
                );

                params.gravity = Gravity.TOP | Gravity.START;
                wm.addView(overlayView, params);
                isShowing = true;

                CrashLogger.log(context, "OverlayBlocker", wakeAndUnlock
                        ? "🔓 Wake+Unlock overlay deployed"
                        : "🛡️ Standard shield overlay deployed");

            } catch (Exception e) {
                CrashLogger.log(context, "OverlayBlocker", "❌ Overlay add failed: " + e.getMessage());
                isShowing = false;
            }
        });
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
        }
    }

    public static boolean isShowing() {
        return isShowing;
    }
}
