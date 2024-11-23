package com.example.drg2023

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.drg2023.tracker.UsageStatsTracker
import com.example.drg2023.tracker.NetworkStatsTracker
import com.example.drg2023.tracker.UsageEventTracker

class AppTrackWorker(val context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {
    val TAG = AppTrackWorker::class.java.simpleName
    override fun doWork(): Result {
        UsageStatsTracker(context, 2).start()
        NetworkStatsTracker(context).start()
        UsageEventTracker(context).start()
        return Result.success()
    }
}