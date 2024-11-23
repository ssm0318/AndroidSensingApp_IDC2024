package com.example.drg2023.tracker

import android.content.Context
import android.content.pm.PackageManager
import com.example.drg2023.Utils
import com.example.drg2023.model.sensors.InstalledAppListAdapter
import com.example.drg2023.model.sensors.InstalledAppModel


class InstalledAppTracker(val context: Context): Tracker() {
    override val TAG: String = InstalledAppTracker::class.java.simpleName
    override fun start() {
        val pm: PackageManager = context.packageManager
        val packages =
            pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (packageInfo in packages) {
            InstalledAppListAdapter.add(
                InstalledAppModel(
                    Utils.getUnixNow(),
                    packageInfo.packageName,
                    packageInfo.uid
                )
            )
        }
    }
}