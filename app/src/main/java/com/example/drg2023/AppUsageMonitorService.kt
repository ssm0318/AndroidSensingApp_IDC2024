package com.example.drg2023

import android.annotation.SuppressLint
import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class AppUsageMonitorService : Service() {

    private lateinit var chosenAppPackageName: String
    private var usageStartMillis: Long = 0L
    private var totalUsageMillis: Long = 0
    private var notificationCount: Int = 0
    private var lastNotificationTime: Long = 0

    private val maxNotificationsPerDay = 4
    private val minTimeBetweenNotifications = 2 * 60 * 60 * 1000 // 2 hours
    private val intervalMillis: Long = 1 * 60_000 // 1 minutes
    private val notificationId = 100

    private val handler = Handler(Looper.getMainLooper())

    // using queryUsageStats()
    private val checkAppUsageTask = object : Runnable {
        override fun run() {
            println("MY_TAG CHECKAPPUSAGETASK RUNNING!!!!!!!")
            val usageStatsManager =
                getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - intervalMillis
            println("MY_TAG endtime $endTime begintime $beginTime")
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                beginTime,
                endTime
            )

            val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            chosenAppPackageName = sharedPref.getString("chosenApp", "") ?: ""

            val chosenAppStats = stats.find { it.packageName == chosenAppPackageName }
            println("MY_TAG hello..?")
            println(chosenAppStats)
            println("MY_TAG chosenAppStats.totalTimeInForeground: " + chosenAppStats?.totalTimeInForeground)
            val foregroundAppStats = stats.maxByOrNull { it.lastTimeUsed }
            val foregroundAppPackageName = foregroundAppStats?.packageName
            println("MY_TAG Foreground App: $foregroundAppPackageName")
            println("MY_TAG foreground app time: " + foregroundAppStats?.totalTimeInForeground)
            println(chosenAppPackageName)
            println("MY_TAG usageStartMillis " + usageStartMillis)

            if (foregroundAppStats?.packageName == chosenAppPackageName) {
                println("MY_TAG foreground == chosenapp")
                if (usageStartMillis == 0L) {
                    usageStartMillis = System.currentTimeMillis()
                }

                if (chosenAppStats != null && chosenAppStats.totalTimeInForeground > 0) {
                    val usageMillis = System.currentTimeMillis() - usageStartMillis
                    totalUsageMillis += usageMillis

                    println("MY_TAG usageMillis $usageMillis")
                    println("MY_TAG totalUsageMils $totalUsageMillis")

                    if (usageMillis >= 2 * 60 * 1000 &&
                        shouldSendNotification()) {
                        println("MY_TAG here1")
                        sendNotification()
                    }

                    if (totalUsageMillis >= 3 * 60 * 60 * 1000 &&
                        shouldSendNotification()) {
                        println("MY_TAG here2")
                        sendNotification()
                    }
                }
            } else {
                usageStartMillis = 0L
            }

            handler.postDelayed(this, intervalMillis)
        }
    }

    // using queryEvents()
//    private val checkAppUsageTask = object : Runnable {
//        override fun run() {
//            println("MY_TAG CHECKAPPUSAGETASK RUNNING!!!!!!!")
//            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
//            val endTime = System.currentTimeMillis()
//            val beginTime = endTime - intervalMillis
//            println("MY_TAG endtime $endTime begintime $beginTime")
//            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
//
//            val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//            chosenAppPackageName = sharedPref.getString("chosenApp", "") ?: ""
//
//            var chosenAppForegroundTime: Long = 0
//            var lastEventTimeStamp: Long = 0
//            var foregroundAppPackageName: String? = null
//            var foregroundAppTime: Long = 0
//
//            val event = UsageEvents.Event()
//            while (usageEvents.hasNextEvent()) {
//                usageEvents.getNextEvent(event)
//                when (event.eventType) {
//                    UsageEvents.Event.ACTIVITY_RESUMED -> {
//                        if (event.packageName == chosenAppPackageName) {
//                            lastEventTimeStamp = event.timeStamp
//                        }
//                        foregroundAppPackageName = event.packageName
//                        foregroundAppTime = 0
//                    }
//                    UsageEvents.Event.ACTIVITY_PAUSED -> {
//                        if (event.packageName == chosenAppPackageName) {
//                            chosenAppForegroundTime += event.timeStamp - lastEventTimeStamp
//                        }
//                        if (event.packageName == foregroundAppPackageName) {
//                            foregroundAppTime += event.timeStamp - lastEventTimeStamp
//                        }
//                    }
//                }
//            }
//
//            if (foregroundAppPackageName == chosenAppPackageName) {
//                chosenAppForegroundTime += System.currentTimeMillis() - lastEventTimeStamp
//                foregroundAppTime += System.currentTimeMillis() - lastEventTimeStamp
//            }
//
//            println("MY_TAG chosenAppStats.totalTimeInForeground: $chosenAppForegroundTime")
//            println("MY_TAG Foreground App: $foregroundAppPackageName")
//            println("MY_TAG foreground app time: $foregroundAppTime")
//            println(chosenAppPackageName)
//            println("MY_TAG usageStartMillis " + usageStartMillis)
//
//            if (foregroundAppPackageName == chosenAppPackageName) {
//                println("MY_TAG foreground == chosenapp")
//                if (usageStartMillis == 0L) {
//                    usageStartMillis = System.currentTimeMillis()
//                }
//
//                if (chosenAppForegroundTime > 0) {
//                    val usageMillis = System.currentTimeMillis() - usageStartMillis
//                    totalUsageMillis += usageMillis
//
//                    println("MY_TAG usageMillis $usageMillis")
//                    println("MY_TAG totalUsageMils $totalUsageMillis")
//
//                    if (usageMillis >= 2.5 * 60 * 1000 && shouldSendNotification()) {
//                        println("MY_TAG here1")
//                        sendNotification()
//                    }
//
//                    if (totalUsageMillis >= 3 * 60 * 60 * 1000 && shouldSendNotification()) {
//                        println("MY_TAG here2")
//                        sendNotification()
//                    }
//                }
//            } else {
//                usageStartMillis = 0L
//            }
//
//            handler.postDelayed(this, intervalMillis)
//        }
//    }


    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForegroundService()

        handler.post(checkAppUsageTask)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForegroundService() {
        val CHANNEL_ID = getString(R.string.channel_id)
        val notificationId = 2
        val textTitle = "AppMinder is Running!"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.uw_logo)
            .setContentTitle(textTitle)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        startForeground(notificationId, builder.build())
    }

    private fun shouldSendNotification(): Boolean {
        val currentTimeMillis = System.currentTimeMillis()

        println("noti count$notificationCount")

        // Check if the maximum number of notifications per day has been reached
        if (notificationCount >= maxNotificationsPerDay) {
            return false
        }

        // Check if enough time has passed since the last notification
        if (lastNotificationTime + minTimeBetweenNotifications > currentTimeMillis) {
            println("should not send noti")
            return false
        }

        return true
    }

//    @SuppressLint("MissingPermission")
//    private fun sendNotification() {
//        val textTitle = "Please open the AppMinder and answer our survey :)"
//
//        val intent = Intent(this, MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            0,
//            intent,
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
//        )
//
//        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.channel_id))
//            .setSmallIcon(R.drawable.uw_logo)
//            .setContentTitle(textTitle)
//            .setContentIntent(pendingIntent)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .setAutoCancel(true)
//
//        val notificationManager = NotificationManagerCompat.from(this)
//        notificationManager.notify(notificationId, notificationBuilder.build())
//
//        notificationCount++
//        lastNotificationTime = System.currentTimeMillis()
//    }

    @SuppressLint("MissingPermission")
    private fun sendNotification() {
        println("at send notification")
//        val popupIntent = Intent(this, PopUpActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            0,
//            popupIntent,
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
//        )
//
//        try {
//            pendingIntent.send()
//        } catch (e: PendingIntent.CanceledException) {
//            e.printStackTrace()
//        }

        val intent = Intent(this, DialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)

        notificationCount++
        lastNotificationTime = System.currentTimeMillis()
        println("successfully sent notification")
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(getString(R.string.channel_id), name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkAppUsageTask)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
