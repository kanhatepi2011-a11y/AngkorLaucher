package com.movtery.angkorlauncher.feature.download.install

import java.io.File

fun interface OnFileDownloadedListener {
    fun onEnded(file: File)
}