package com.example.drg2023

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startForegroundService
import com.example.drg2023.model.sensors.BootListAdapter
import com.example.drg2023.model.sensors.BootModel

class BootBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        BootListAdapter.add(
            BootModel(
                Utils.getUnixNow(),
                true
            )
        )
        Intent(context, TrackerService::class.java).also {
            context?.startForegroundService(it)
        }
        Intent(context, FloatingButtonService::class.java).also {
            context?.startService(it)
        }
    }
}