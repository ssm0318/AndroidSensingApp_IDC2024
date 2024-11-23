package com.example.drg2023.model.sensors

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class UsageStatsEventModel(
    override var time: Long?,
    val timeStamp: Long,
    val className: String?,
    val eventType: Int,
    val packageName: String,
    val shortcutId: String?,
    val configuration: ConfigurationModel?

    ):ModelInterface {
}
data class ConfigurationModel(
    val colorMode: Int,
    val densityDpi: Int,
    val fontScale: Float,
    val hartKeyboardHidden: Int,
    val isScreenHdr: Boolean,
    val isScreenRound: Boolean,
    val isScreenWideColorGamut: Boolean,
    val keyboard: Int,
    val keyboardHidden: Int,
    val layoutDirection: Int,
//    TODO(make another model? -> language // locales return nothing)
//    val locales: LocaleList,
    val mcc: Int,
    val mnc: Int,
    val navigation: Int,
    val navigationHidden: Int,
    val orientation: Int,
    val screenHeightDp: Int,
    val screenLayout: Int,
    val screenWidthDp: Int,
    val smallestScreenWidthDp: Int,
    val touchscreen: Int
)
object UsageEventListAdapter:
        ModelAdapterInterface<UsageStatsEventModel> {
    override var mModelInterface: Class<UsageStatsEventModel> = UsageStatsEventModel::class.java
    override var queue: Queue<UsageStatsEventModel> = ConcurrentLinkedQueue()
}