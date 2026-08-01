package com.movtery.angkorlauncher.ui.fragment.download.addon

import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.feature.mod.modloader.FabricLikeUtils

class DownloadQuiltFragment : DownloadFabricLikeFragment(FabricLikeUtils.QUILT_UTILS, R.drawable.ic_quilt) {
    companion object {
        const val TAG: String = "DownloadQuiltFragment"
    }
}