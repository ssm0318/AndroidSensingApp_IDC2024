package com.example.drg2023

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Create the notification channel with the given channel ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("channel_id", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT)
                .apply {
                    description = "Channel Description"
                }
            val notificationManager = context?.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
        // Create the intent that will open the link
        val websiteIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))

        // Create the pending intent that will open the link when the notification is clicked
        val pendingIntent = PendingIntent.getActivity(context, 0, websiteIntent, PendingIntent.FLAG_IMMUTABLE)
        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context!!, "channel_id")
            .setSmallIcon(R.drawable.uw_logo)
            .setContentTitle("Time for daily review")
            .setContentText("press me!")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show the notification
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }
}
