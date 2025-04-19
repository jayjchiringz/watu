package com.system.guardian.test;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.system.guardian.AdminReceiver;
import com.system.guardian.CrashLogger;

public class WatuLockSimulatorActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button lockBtn = new Button(this);
        lockBtn.setText("Simulate Watu Lock");
        setContentView(lockBtn);

        lockBtn.setOnClickListener(v -> {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);

            if (dpm.isAdminActive(adminReceiver)) {
                dpm.lockNow();
                Toast.makeText(this, "🔒 Simulated Watu lock triggered", Toast.LENGTH_SHORT).show();
                CrashLogger.log(this, "TestLock", "🔒 Simulated Watu lock triggered via UI");
            } else {
                Toast.makeText(this, "Admin inactive. Lock failed.", Toast.LENGTH_LONG).show();
                CrashLogger.log(this, "TestLock", "⚠️ Admin inactive, lock simulation failed");
            }
        });
    }
}
