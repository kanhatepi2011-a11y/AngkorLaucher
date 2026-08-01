package com.movtery.angkorlauncher.context

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import com.movtery.angkorlauncher.setting.Settings
import com.movtery.angkorlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.util.Locale

class LocaleHelper(context: Context) : ContextWrapper(context) {
    companion object {
        private val KHMER_LOCALE: Locale = Locale.forLanguageTag("km-KH")

        fun setLocale(context: Context): ContextWrapper {
            //初始化路径
            PathManager.initContextConstants(context)
            //刷新启动器设置
            Settings.refreshSettings()

            LauncherPreferences.loadPreferences()

            // The launcher UI is intentionally Khmer-first. Applying the locale to
            // the wrapped resource configuration makes Android resolve values-km
            // even when the device's system language is English.
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(KHMER_LOCALE)
            return LocaleHelper(context.createConfigurationContext(configuration))
        }
    }
}
