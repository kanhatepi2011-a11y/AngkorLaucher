package com.movtery.angkorlauncher.ui.fragment.settings.wrapper

import android.content.Context
import android.view.View
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.ui.dialog.TipDialog
import com.movtery.angkorlauncher.utils.ZHTools

abstract class AbstractSettingsWrapper(
    private val mainView: View
) {
    private var isRequiresReboot = false

    fun setRequiresReboot(): AbstractSettingsWrapper {
        isRequiresReboot = true
        return this
    }

    fun checkShowRebootDialog(context: Context) {
        if (isRequiresReboot) {
            TipDialog.Builder(context)
                .setTitle(R.string.generic_tip)
                .setMessage(R.string.setting_reboot_tip)
                .setConfirmClickListener { ZHTools.killProcess() }
                .showDialog()
        }
    }

    fun setGone() {
        mainView.visibility = View.GONE
    }
}