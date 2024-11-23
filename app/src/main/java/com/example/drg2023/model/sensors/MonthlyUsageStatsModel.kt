package com.example.drg2023.model.sensors

import android.app.usage.UsageStats
import android.os.Build
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class MonthlyUsageStatsModel(
    override var time: Long?,
    val packageName: String,
    val firstTimeStamp: Long?,
    val lastTimeStamp: Long?,
    val totalTimeForeground: Long,
    val totalTimeVisible: Long?
): ModelInterface {
    fun getStringKey(): String{
        return this.packageName+this.firstTimeStamp+this.lastTimeStamp+this.totalTimeForeground+this.totalTimeVisible
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MonthlyUsageStatsModel) return false
        return this.getStringKey()==other.getStringKey()
    }
}

object MonthlyUsageStatsListAdapter:
    ModelAdapterInterface<MonthlyUsageStatsModel> {
    override var queue: Queue<MonthlyUsageStatsModel> = ConcurrentLinkedQueue()
    override var mModelInterface: Class<MonthlyUsageStatsModel> = MonthlyUsageStatsModel::class.java

    fun add(time: Long, list: List<UsageStats>) {
        for (item in list) {
            val usageStatsModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MonthlyUsageStatsModel(
                    time,
                    item.packageName,
                    item.firstTimeStamp,
                    item.lastTimeStamp,
                    item.totalTimeInForeground,
                    item.totalTimeVisible
                )
            } else {
                MonthlyUsageStatsModel(
                    time,
                    item.packageName,
                    item.firstTimeStamp,
                    item.lastTimeStamp,
                    item.totalTimeInForeground,
                    null
                )
            }
            if (!queue.contains(usageStatsModel))
                this.add(usageStatsModel)
        }
    }

}