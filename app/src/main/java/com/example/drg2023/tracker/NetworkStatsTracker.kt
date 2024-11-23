package com.example.drg2023.tracker

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.example.drg2023.Utils
import com.example.drg2023.model.sensors.NetworkStatsListAdapter
import com.example.drg2023.model.sensors.NetworkStatsModel


class NetworkStatsTracker(val context: Context): Tracker() {


    override val TAG: String
        get() = NetworkStatsTracker::class.java.simpleName

    private val networkStatsManager: NetworkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    override fun start() {
        super.start()

        val subscriberId = null
        val HOUR = 3600000
        val network_type = arrayOf(ConnectivityManager.TYPE_MOBILE, ConnectivityManager.TYPE_WIFI)

        for (type in network_type) {
            var start = Utils.getUnixToday(-1)
            var bucket = NetworkStats.Bucket()
            for (hour in 1..24) {
                var end = start+HOUR

//        Mobile
                val networkStats = networkStatsManager.querySummary(type, subscriberId, start, end)
                do {
                    networkStats.getNextBucket(bucket)
                    bucket.apply{
                        val defaultNetworkStatus = if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.P) this.defaultNetworkStatus else null
                        NetworkStatsListAdapter.add(NetworkStatsModel(
                            Utils.getUnixNow(), type, this.startTimeStamp, this.endTimeStamp,
                            defaultNetworkStatus, this.metered, this.roaming, this.rxBytes,
                            this.rxPackets, this.state, this.tag, this.txBytes, this.txPackets, this.uid
                        ))
                    }
                } while (networkStats.hasNextBucket())
                start += HOUR
            }
        }
    }

    override fun stop() {
        super.stop()
    }
}