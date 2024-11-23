package com.example.drg2023

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import java.util.*


class NotificationService : Service() {
    private lateinit var pendingIntent: PendingIntent

    override fun onBind(intent: Intent?): IBinder? {
            return null
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            scheduleNotification()
            return START_STICKY
        }

        private fun scheduleNotification() {
            // Set up the alarm to trigger at 8:30 pm
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val notificationIntent = Intent(this, NotificationReceiver::class.java)
            pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 20) // 8 pm
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

