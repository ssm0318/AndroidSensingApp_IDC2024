package com.example.drg2023.tracker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.example.drg2023.Utils
import com.example.drg2023.model.sensors.ConfigurationModel
import com.example.drg2023.model.sensors.UsageEventListAdapter
import com.example.drg2023.model.sensors.UsageStatsEventModel

class UsageEventTracker(val context: Context): Tracker() {
    override val TAG: String
        get() = UsageEventTracker::class.java.simpleName

    private val usageStatsManager: UsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    override fun start() {
        super.start()
        val res = usageStatsManager.queryEvents(Utils.getUnixToday(-1), Utils.getUnixToday(0))
        val event = UsageEvents.Event()
        while (res.hasNextEvent()) {
            res.getNextEvent(event)
            UsageEventListAdapter.add(
                UsageStatsEventModel(
                    Utils.getUnixNow(), event.timeStamp, event.className,
                    event.eventType, event.packageName, event.shortcutId,
                    event.configuration?.let{
                        ConfigurationModel(
                            it.colorMode,
                            it.densityDpi,
                            it.fontScale,
                            it.hardKeyboardHidden,
                            it.isScreenHdr,
                            it.isScreenRound,
                            it.isScreenWideColorGamut,
                            it.keyboard,
                            it.keyboardHidden,
                            it.layoutDirection,
                            it.mcc,
                            it.mnc,
                            it.navigation,
                            it.navigationHidden,
                            it.orientation,
                            it.screenHeightDp,
                            it.screenLayout,
                            it.screenWidthDp,
                            it.smallestScreenWidthDp,
                            it.touchscreen
                        )
                    }
                )
            )
        }
    }

    override fun stop() {
        super.stop()
        Log.d(TAG, UsageEventListAdapter.queue.size.toString());
    }
}

