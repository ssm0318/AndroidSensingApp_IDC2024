package com.example.drg2023.model.sensors

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class InstalledAppModel(
    override var time: Long?,
    val pkgName: String?,
    val uid: Int
): ModelInterface {

}
object InstalledAppListAdapter: ModelAdapterInterface<InstalledAppModel> {
    override var queue: Queue<InstalledAppModel> = ConcurrentLinkedQueue()
    override var mModelInterface: Class<InstalledAppModel> = InstalledAppModel::class.java

}