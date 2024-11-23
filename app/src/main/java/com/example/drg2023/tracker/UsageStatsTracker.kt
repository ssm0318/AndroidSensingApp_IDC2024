package com.example.drg2023.tracker

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.drg2023.ApiManager
import com.example.drg2023.Utils
import com.example.drg2023.model.sensors.UsageStatsListAdapter
import java.text.SimpleDateFormat
import java.util.*

//AppUsageTracker(this, UsageStatsManager.INTERVAL_MONTHLY, listOf(Utils.getUnixByMonth(-1), Utils.getUnixNow())).start()

//TODO(ActivityNum, RunningNum- task, process? / Activity Manager)
class UsageStatsTracker(val context: Context, val days: Int): Tracker() {
    init {
        if (days<0) throw RuntimeException("days should be positive")
    }
    override val TAG: String
        get() = UsageStatsTracker::class.java.simpleName

    private val usageStatsManager: UsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @SuppressLint("SimpleDateFormat")
    override fun start() {
        super.start()
        for (day in 0 until days) {
            val start = Utils.getUnixByDay(-day-1)
            val end = Utils.getUnixByDay(-day)
            val query = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            UsageStatsListAdapter.add(Utils.getUnixNow(), query)

            val date = Date(end)
            var format = SimpleDateFormat("yyyy-MM-dd HH:mm")
            val dateTimeKey = format.format(date)
            ApiManager.writeDataInJson(context, UsageStatsListAdapter, dateTimeKey)
        }
    }

    override fun stop() {
        super.stop()
    }

}