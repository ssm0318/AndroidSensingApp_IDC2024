package com.example.drg2023.tracker

import android.os.Build
import android.service.notification.NotificationListenerService
import com.example.drg2023.Utils
import com.example.drg2023.model.sensors.NotificationRankingListAdapter
import com.example.drg2023.model.sensors.NotificationRankingModel
import com.example.drg2023.model.sensors.Ranking


class NotificationRankingTracker: Tracker() {
    override val TAG: String
        get() = super.TAG

    override fun start() {
        super.start()
        val notificationRankingModel =
            NotificationRankingModel(Utils.getUnixNow())
        NotificationListener.mCurrentRanking?.run {
            NotificationListener.mCurrentOrderedKeys?.forEach {
                val tmpRanking = NotificationListenerService.Ranking()
                val sentiment = if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.P) tmpRanking.userSentiment else null
                this.getRanking(it, tmpRanking)
                notificationRankingModel.add(
                    Ranking(
                        it,
                        tmpRanking.rank,
                        sentiment
                    )
                )
            }
        }
        NotificationRankingListAdapter.add(
            notificationRankingModel
        )
    }

    override fun stop() {
        super.stop()
        NotificationRankingListAdapter.clear()
    }
}