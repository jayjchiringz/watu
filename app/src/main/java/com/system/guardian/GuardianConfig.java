package com.system.guardian;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Single source of truth for which packages Guardian suppresses.
 * Ships with defaults; can be overridden at runtime via the control JSON
 * by writing to SharedPreferences under the "guardian_config" file.
 */
public final class GuardianConfig {

    private static final String PREFS = "guardian_config";
    private static final String KEY_TARGETS = "target_packages";

    /**
     * Default targets. Add entries here for new client apps.
     * com.watuke.app     -> Watu credit phones
     * com.asses.onfon    -> OnfonMobile credit phones
     */
    public static final Set<String> DEFAULT_TARGETS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "com.watuke.app",
                    "com.asses.onfon"
            ))
    );

    private GuardianConfig() {}

    /** Returns the currently active target set (cached in prefs or defaults). */
    public static Set<String> getTargets(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_TARGETS, null);
        if (raw == null || raw.trim().isEmpty()) {
            return DEFAULT_TARGETS;
        }
        Set<String> set = new HashSet<>();
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) set.add(t);
        }
        return set.isEmpty() ? DEFAULT_TARGETS : set;
    }

    /** Persist a new target set (called from server-driven config updates). */
    public static void setTargets(Context context, Set<String> targets) {
        if (targets == null || targets.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String s : targets) {
            if (sb.length() > 0) sb.append(",");
            sb.append(s);
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TARGETS, sb.toString())
                .apply();
    }

    public static boolean isTarget(Context context, String packageName) {
        return packageName != null && getTargets(context).contains(packageName);
    }
}