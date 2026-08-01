package com.movtery.angkorlauncher.feature.download

import androidx.lifecycle.ViewModel
import com.movtery.angkorlauncher.feature.download.item.InfoItem
import com.movtery.angkorlauncher.feature.download.platform.AbstractPlatformHelper

class InfoViewModel : ViewModel() {
    var platformHelper: AbstractPlatformHelper? = null
    var infoItem: InfoItem? = null
}