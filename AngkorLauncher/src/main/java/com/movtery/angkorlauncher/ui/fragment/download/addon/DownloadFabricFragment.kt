package com.movtery.angkorlauncher.ui.fragment.download.addon

import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.feature.mod.modloader.FabricLikeUtils

class DownloadFabricFragment : DownloadFabricLikeFragment(FabricLikeUtils.FABRIC_UTILS, R.drawable.ic_fabric) {
    companion object {
        const val TAG: String = "DownloadFabricFragment"
    }
}