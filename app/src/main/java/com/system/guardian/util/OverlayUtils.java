package com.system.guardian.util;

import android.content.Context;

import com.system.guardian.OverlayBlocker;

public class OverlayUtils {

    public static void ensureOverlayVisible(Context context) {
        if (!OverlayBlocker.isShowing()) {
            OverlayBlocker.show(context);
        }
    }
}
