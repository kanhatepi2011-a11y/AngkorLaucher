package com.movtery.angkorlauncher.task

fun interface OnTaskEndedListener<V> {
    @Throws(Throwable::class)
    fun onEnded(result: V?)
}