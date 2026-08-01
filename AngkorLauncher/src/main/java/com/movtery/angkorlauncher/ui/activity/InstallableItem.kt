package com.movtery.angkorlauncher.ui.activity

import com.movtery.angkorlauncher.feature.unpack.AbstractUnpackTask

class InstallableItem(
    val name: String,
    val summary: String?,
    val task: AbstractUnpackTask,
    var isRunning: Boolean = false,
    var isFinished: Boolean = false,
    var progress: Int = 0
) : Comparable<InstallableItem> {

    override fun compareTo(other: InstallableItem): Int {
        return name.compareTo(other.name, ignoreCase = true)
    }
}
