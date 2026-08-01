package com.movtery.angkorlauncher.feature.download.enums

import com.movtery.angkorlauncher.feature.download.platform.AbstractPlatformHelper
import com.movtery.angkorlauncher.feature.download.platform.curseforge.CurseForgeHelper
import com.movtery.angkorlauncher.feature.download.platform.modrinth.ModrinthHelper

enum class Platform(val pName: String, val helper: AbstractPlatformHelper) {
    MODRINTH("Modrinth", ModrinthHelper()),
    CURSEFORGE("CurseForge", CurseForgeHelper())
}