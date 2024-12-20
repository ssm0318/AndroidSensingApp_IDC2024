package com.example.drg2023.tracker

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.drg2023.ApiManager
import com.example.drg2023.Utils
import com.example.drg2023.model.sensors.MonthlyUsageStatsListAdapter
import java.text.SimpleDateFormat
import java.util.*

//AppUsageTracker(this, UsageStatsManager.INTERVAL_MONTHLY, listOf(Utils.getUnixByMonth(-1), Utils.getUnixNow())).start()

//TODO(ActivityNum, RunningNum- task, process? / Activity Manager)
class MonthlyUsageStatsTracker(val context: Context, val months: Int): Tracker() {
    init {
        if (months<0) throw RuntimeException("months should be positive")
    }
    override val TAG: String
        get() = UsageStatsTracker::class.java.simpleName

    private val usageStatsManager: UsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @SuppressLint("SimpleDateFormat")
    override fun start() {
        super.start()
        for (month in 0 until months) {
            val start = Utils.getUnixByMonthAtFirst(-month-1)
            val end = Utils.getUnixByMonthAtFirst(-month)
            val query = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, start, end)
            MonthlyUsageStatsListAdapter.add(Utils.getUnixNow(), query)

            val date = Date(start)
            var format = SimpleDateFormat("yyyy-MM-dd HH:mm")
            val dateTimeKey = format.format(date)
            ApiManager.writeDataInJson(context, MonthlyUsageStatsListAdapter, dateTimeKey)
        }
    }

    override fun stop() {
        super.stop()
    }

}