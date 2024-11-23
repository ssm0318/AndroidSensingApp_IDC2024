package com.example.drg2023


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.drg2023.model.sensors.*
import com.example.drg2023.tracker.*
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit


class TrackerService : Service() {
    val TAG = TrackerService::class.java.simpleName
    companion object {
        const val trackingInterval: Long = 5*60*1000

        val TRACKERS: List<ModelAdapterInterface<out ModelInterface>> = listOf(
            NotificationListAdapter, NotificationRankingListAdapter, UsageEventListAdapter)

        val ONCE_TRACKERS: List<ModelAdapterInterface<out ModelInterface>> = listOf(
            InstalledAppListAdapter, BootListAdapter, ServiceTurnoffListAdapter, UsageStatsListAdapter
        )
        const val appUsageTrackInterval = 12*20
        var appUsageCount = 0
    }

    private lateinit var notificationRankingTracker: NotificationRankingTracker

    private lateinit var installedAppTracker: InstalledAppTracker

    private lateinit var packageTracker: PackageTracker

    private lateinit var shutdownTracker: ShutDownTracker

    private lateinit var usageEventTracker: UsageEventTracker

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    private var runTracker: Boolean? = null
    private val timer = Timer()
    private lateinit var timerFirstTime: Date
    private val timerTask = object: TimerTask() {
        override fun run() {
            notificationRankingTracker.start()
            appUsageCount++
        }
    }

    override fun onCreate() {
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
        notificationRankingTracker = NotificationRankingTracker()
        installedAppTracker = InstalledAppTracker(this)
        packageTracker = PackageTracker(this)
        shutdownTracker = ShutDownTracker(this)
        usageEventTracker = UsageEventTracker(this)

        startWorker()
        timerFirstTime = Date()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()

        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopWorker()
        stopTracker()

        ServiceTurnoffListAdapter.add(
            ServiceTurnoffModel(Utils.getUnixNow())
        )

        try {
            val dateTimeKey = Utils.getDateTimeKey()
            for (listAdapter in TRACKERS) {
                ApiManager.writeDataInJson(this, listAdapter, dateTimeKey)
                ApiManager.uploadDataInRealtime(this, listAdapter)
            }
            for (listAdapter in TrackerService.ONCE_TRACKERS) {
                if (listAdapter.queue.isEmpty()) continue
                ApiManager.writeDataInJson(this, listAdapter, dateTimeKey)
                ApiManager.uploadDataInRealtime(this, listAdapter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }

    private fun startForegroundService() {
        val CHANNEL_ID = resources.getString(R.string.CHANNEL_ID)
        val notificationId = 1
        val textTitle = "AppMinder: Univ. of Washington research"

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
//            .setSmallIcon(R.drawable.uw_logo)
//            .setContentTitle(textTitle)
            .setContentTitle("")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_NONE
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            mChannel.setSound(null, null)
            val notificationManager = getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        startForeground(notificationId, builder.build())
    }

    fun postToast(message: String) {
        val handler = Handler(Looper.getMainLooper()).post{
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    // Handler that receives messages from the thread
    inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            try {
                startTracker()
            } catch (e:InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun startTracker() {
        if (runTracker != null && runTracker==true) return
        shutdownTracker.start()
        usageEventTracker.start()

        UsageStatsTracker(this, 20).start()

        timer.schedule(timerTask, timerFirstTime, trackingInterval)
        runTracker = true
    }
    private fun stopTracker() {
        if (runTracker!=null && runTracker == false) return
        Log.d(TAG, "stop tracker")
        shutdownTracker.stop()

        timer.cancel()

        val dateTimeKey = Utils.getDateTimeKey()
        for (listAdapter in TRACKERS) {
            ApiManager.writeDataInJson(this, listAdapter, dateTimeKey)
            ApiManager.uploadDataInRealtime(this, listAdapter)
        }
        for (listAdapter in ONCE_TRACKERS) {
            if (listAdapter.queue.isEmpty()) continue
            ApiManager.writeDataInJson(this, listAdapter, dateTimeKey)
            ApiManager.uploadDataInRealtime(this, listAdapter)
        }
        runTracker = false
    }
    fun startWorker() {
    //  Save Json file to phone every hour
        Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build().apply {
                PeriodicWorkRequestBuilder<FileSaveWorker>(1, TimeUnit.HOURS)
                    .setConstraints(this)
                    .build().apply {
                        WorkManager.getInstance(this@TrackerService)
                            .enqueueUniquePeriodicWork("FileSaveWorker", ExistingPeriodicWorkPolicy.REPLACE, this)
                    }
            }

        //  Save Json file to server every hour
        Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build().apply {
                PeriodicWorkRequestBuilder<UploadWholeWorker>(1, TimeUnit.HOURS)
                    .setInitialDelay(10, TimeUnit.MINUTES)
                    .setConstraints(this)
                    .build().apply {
                        WorkManager.getInstance(this@TrackerService)
                            .enqueueUniquePeriodicWork("UploadWholeWorker", ExistingPeriodicWorkPolicy.REPLACE, this)
                    }
            }

        //  Run app tracker every day
        Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build().apply {
                PeriodicWorkRequestBuilder<AppTrackWorker>(12, TimeUnit.HOURS)
                    .setInitialDelay(10, TimeUnit.MINUTES)
                    .setConstraints(this)
                    .build().apply {
                        WorkManager.getInstance(this@TrackerService)
                            .enqueueUniquePeriodicWork("AppTrackWorker", ExistingPeriodicWorkPolicy.REPLACE, this)
                    }
            }
    }
    fun stopWorker() {
        WorkManager.getInstance(this).cancelAllWork()
    }
}