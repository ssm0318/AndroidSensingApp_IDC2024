package com.example.drg2023.model.sensors

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class NetworkStatsModel (
    override var time: Long?,
    val networkType: Int,
    val startTimeStamp: Long,
    val endTimeStamp: Long,
    val defaultNetworkStatus: Int?,
    val metered: Int,
    val roaming: Int,
    val rxBytes: Long,
    val rxPackets: Long,
    val state: Int,
    val tag: Int,
    val txBytes: Long,
    val txPackets: Long,
    val uid: Int
)
    :ModelInterface{

}

object NetworkStatsListAdapter:
        ModelAdapterInterface<NetworkStatsModel> {
    override var queue: Queue<NetworkStatsModel> = ConcurrentLinkedQueue()

    override var mModelInterface: Class<NetworkStatsModel> = NetworkStatsModel::class.java

}