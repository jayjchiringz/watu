package com.system.guardian.firebase;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import com.system.guardian.CrashLogger;

@SuppressLint("SpecifyJobSchedulerIdRange")
public class HijackedFirebaseJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("Hijack", "🔥 Native JobService spoofed execution");
        CrashLogger.log(this, "Hijack", "🚀 Hijacked native job triggered");

        // Optionally emulate stolen behavior here
        // e.g., LogUploaderService.startIfNeeded();

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
