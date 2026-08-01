package com.movtery.angkorlauncher.feature.unpack

interface OnTaskRunningListener {
    fun onTaskStart()
    fun onTaskProgress(progress: Int)
    fun onTaskEnd()
}
