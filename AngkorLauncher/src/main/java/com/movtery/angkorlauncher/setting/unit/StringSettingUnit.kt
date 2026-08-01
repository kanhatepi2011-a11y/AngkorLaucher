package com.movtery.angkorlauncher.setting.unit

import com.movtery.angkorlauncher.setting.Settings.Manager

class StringSettingUnit(key: String, defaultValue: String) : AbstractSettingUnit<String>(key, defaultValue) {
    override fun getValue() = Manager.getValue(key, defaultValue) { it }
}