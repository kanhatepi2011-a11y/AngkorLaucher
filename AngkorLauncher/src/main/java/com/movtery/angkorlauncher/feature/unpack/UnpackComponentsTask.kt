package com.movtery.angkorlauncher.feature.unpack

import android.content.Context
import android.content.res.AssetManager
import com.movtery.angkorlauncher.feature.log.Logging.i
import com.movtery.angkorlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class UnpackComponentsTask(val context: Context, val component: Components) : AbstractUnpackTask() {
    private lateinit var am: AssetManager
    private lateinit var rootDir: String
    private lateinit var versionFile: File
    private lateinit var input: InputStream
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            am = context.assets
            rootDir = if (component.privateDirectory) PathManager.DIR_DATA else PathManager.DIR_GAME_HOME
            versionFile = File("$rootDir/${component.component}/version")
            input = am.open("components/${component.component}/version")
        }.getOrElse {
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false

        if (!versionFile.exists()) {
            requestEmptyParentDir(versionFile)
            i("Unpack Components", "${component.component}: Pack was installed manually, or does not exist...")
            return true
        } else {
            val fis = FileInputStream(versionFile)
            val release1 = Tools.read(input)
            val release2 = Tools.read(fis)
            if (release1 != release2) {
                requestEmptyParentDir(versionFile)
                return true
            } else {
                i("UnpackPrep", "${component.component}: Pack is up-to-date with the launcher, continuing...")
                return false
            }
        }
    }

    override fun run() {
        listener?.onTaskStart()
        val fileList = am.list("components/${component.component}")
        val totalFiles = fileList?.size ?: 0
        if (totalFiles == 0) {
            listener?.onTaskProgress(100)
        }
        fileList?.forEachIndexed { index, fileName ->
            Tools.copyAssetFile(context, "components/${component.component}/$fileName", "$rootDir/${component.component}", true)
            listener?.onTaskProgress(((index + 1) * 100) / totalFiles)
        }
        listener?.onTaskEnd()
    }

    private fun requestEmptyParentDir(file: File) {
        file.parentFile!!.apply {
            if (exists() and isDirectory) {
                FileUtils.deleteDirectory(this)
            }
            mkdirs()
        }
    }
}
