package com.movtery.angkorlauncher.feature.unpack

import android.content.Context
import android.content.res.AssetManager
import com.movtery.angkorlauncher.feature.log.Logging
import com.movtery.angkorlauncher.launch.RuntimeFiles
import com.movtery.angkorlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import java.io.FilterInputStream
import java.io.File
import java.io.InputStream

class UnpackJreTask(val context: Context, val jre: Jre) : AbstractUnpackTask() {
    private lateinit var assetManager: AssetManager
    private lateinit var launcherRuntimeVersion: String
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            assetManager = context.assets
            launcherRuntimeVersion = Tools.read(assetManager.open(jre.jrePath + "/version"))
        }.getOrElse {
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false

        runCatching {
            val installedRuntimeVersion = MultiRTUtils.readInternalRuntimeVersion(jre.jreName)
            val runtimeHome = File(PathManager.DIR_MULTIRT_HOME, jre.jreName)
            return launcherRuntimeVersion != installedRuntimeVersion ||
                !RuntimeFiles.hasRequiredStructure(runtimeHome, jre.javaVersion)
        }.getOrElse { e ->
            Logging.e("CheckInternalRuntime", Tools.printToString(e))
            return false
        }
    }

    override fun run() {
        listener?.onTaskStart()
        runCatching {
            val universalPath = jre.jrePath + "/universal.tar.xz"
            val platformPath = jre.jrePath + "/bin-" + Architecture.archAsString(
                Tools.DEVICE_ARCHITECTURE
            ) + ".tar.xz"

            val universalInput = assetManager.open(universalPath)
            val platformInput = assetManager.open(platformPath)
            val totalBytes = universalInput.available().toLong() + platformInput.available().toLong()
            var universalBytes = 0L
            var platformBytes = 0L

            val trackedUniversalInput = ProgressInputStream(universalInput) { bytesRead ->
                universalBytes = bytesRead
                if (totalBytes > 0L) {
                    listener?.onTaskProgress((((universalBytes + platformBytes) * 90L) / totalBytes).toInt())
                }
            }
            val trackedPlatformInput = ProgressInputStream(platformInput) { bytesRead ->
                platformBytes = bytesRead
                if (totalBytes > 0L) {
                    listener?.onTaskProgress((((universalBytes + platformBytes) * 90L) / totalBytes).toInt())
                }
            }

            MultiRTUtils.installRuntimeNamedBinpack(
                trackedUniversalInput,
                trackedPlatformInput,
                jre.jreName, launcherRuntimeVersion
            )
            listener?.onTaskProgress(94)
            MultiRTUtils.postPrepare(jre.jreName)
            listener?.onTaskProgress(99)
        }.getOrElse { e -> Logging.e("UnpackJREAuto", "Internal JRE unpack failed", e) }
        listener?.onTaskEnd()
    }

    private class ProgressInputStream(
        inputStream: InputStream,
        private val progressCallback: (Long) -> Unit
    ) : FilterInputStream(inputStream) {
        private var bytesRead = 0L
        private var lastReportedPercent = -1
        private val totalBytes = inputStream.available().toLong().coerceAtLeast(1L)

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) reportProgress(1)
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val count = super.read(buffer, offset, length)
            if (count > 0) reportProgress(count.toLong())
            return count
        }

        private fun reportProgress(count: Long) {
            bytesRead += count
            val percent = ((bytesRead * 100L) / totalBytes).toInt().coerceIn(0, 100)
            if (percent != lastReportedPercent) {
                lastReportedPercent = percent
                progressCallback(bytesRead)
            }
        }
    }
}
