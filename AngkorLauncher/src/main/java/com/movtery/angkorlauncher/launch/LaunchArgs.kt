package com.movtery.angkorlauncher.launch

import com.movtery.angkorlauncher.BuildConfig
import com.movtery.angkorlauncher.InfoDistributor
import com.movtery.angkorlauncher.feature.accounts.AccountUtils
import com.movtery.angkorlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.angkorlauncher.feature.customprofilepath.ProfilePathHome.Companion.getLibrariesHome
import com.movtery.angkorlauncher.feature.version.Version
import com.movtery.angkorlauncher.utils.path.LibPath
import com.movtery.angkorlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.AWTCanvasView
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.authenticator.microsoft.MicrosoftAuthConfig
import net.kdt.pojavlaunch.value.MinecraftAccount
import org.jackhuang.hmcl.util.versioning.VersionNumber
import java.io.File
import java.util.HashMap

class LaunchArgs(
    private val account: MinecraftAccount,
    private val gameDirPath: File,
    private val minecraftVersion: Version,
    private val versionInfo: JMinecraftVersionList.Version,
    private val versionFileName: String,
    private val runtime: Runtime,
    private val launchClassPath: String,
    private val nativeWorkDirectories: NativeWorkDirectories
) {
    fun getAllArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        argsList.addAll(getJavaArgs())
        argsList.addAll(getMinecraftJVMArgs())
        argsList.add("-cp")
        argsList.add("${Tools.getLWJGL3ClassPath()}${File.pathSeparator}$launchClassPath")

        if (runtime.javaVersion > 8) {
            argsList.add("--add-exports")
            val pkg: String = versionInfo.mainClass.substring(0, versionInfo.mainClass.lastIndexOf("."))
            argsList.add("$pkg/$pkg=ALL-UNNAMED")
        }

        argsList.add(versionInfo.mainClass)
        argsList.addAll(getMinecraftClientArgs())

        JvmArgumentSanitizer.validateNoUnresolvedPlaceholders(
            argsList,
            account.accountType ?: "Unknown",
            minecraftVersion.getVersionName(),
            "Minecraft version arguments"
        )
        return argsList
    }

    private fun getJavaArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        if (AccountUtils.isOtherLoginAccount(account)) {
            if (account.otherBaseUrl.contains("auth.mc-user.com")) {
                argsList.add("-javaagent:${LibPath.NIDE_8_AUTH.absolutePath}=${account.otherBaseUrl.replace("https://auth.mc-user.com:233/", "")}")
                argsList.add("-Dnide8auth.client=true")
            } else {
                argsList.add("-javaagent:${LibPath.AUTHLIB_INJECTOR.absolutePath}=${account.otherBaseUrl}")
            }
        }

        argsList.addAll(getCacioJavaArgs(runtime.javaVersion == 8))

        val is7 = VersionNumber.compare(VersionNumber.asVersion(versionInfo.id ?: "0.0").canonical, "1.12") < 0
        val configFilePath = if (is7) LibPath.LOG4J_XML_1_7 else LibPath.LOG4J_XML_1_12
        argsList.add("-Dlog4j.configurationFile=${configFilePath.absolutePath}")

        val nativeSearchPaths = nativeSearchPaths().joinToString(File.pathSeparator)
        argsList.add("-Djava.library.path=$nativeSearchPaths")
        argsList.add("-Dorg.lwjgl.librarypath=$nativeSearchPaths")
        argsList.add("-Djna.boot.library.path=$nativeSearchPaths")
        argsList.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=${nativeWorkDirectories.lwjgl.absolutePath}")
        argsList.add("-Djna.tmpdir=${nativeWorkDirectories.jna.absolutePath}")
        argsList.add("-Dio.netty.native.workdir=${nativeWorkDirectories.netty.absolutePath}")

        return argsList
    }

    private fun getMinecraftJVMArgs(): Array<String> {
        val versionInfo = Tools.getVersionInfo(minecraftVersion, true)

//        // Parse Forge 1.17+ additional JVM Arguments
//        if (versionInfo.inheritsFrom == null || versionInfo.arguments == null || versionInfo.arguments.jvm == null) {
//            return emptyArray()
//        }

        val minecraftArgs: MutableList<String> = java.util.ArrayList()
        versionInfo.arguments?.let {
            fun String.addIgnoreListIfHas(): String {
                if (startsWith("-DignoreList=")) return "$this,$versionFileName.jar"
                return this
            }
            it.jvm?.forEach { arg ->
                if (arg is String) {
                    minecraftArgs.add(arg.addIgnoreListIfHas())
                }
            }
        }
        val resolvedArguments = LaunchArgumentResolver.resolve(
            minecraftArgs,
            placeholderValues(),
            AccountUtils.isMicrosoftAccount(account)
        )
        return JvmArgumentSanitizer.removeLauncherOwnedNativeProperties(
            JvmArgumentSanitizer.removeClasspathArguments(resolvedArguments)
        ).toTypedArray()
    }

    private fun getMinecraftClientArgs(): Array<String> {
        val minecraftArgs: MutableList<String> = ArrayList()
        versionInfo.arguments?.apply {
            // Support Minecraft 1.13+
            game.forEach { if (it is String) minecraftArgs.add(it) }
        }

        return LaunchArgumentResolver.resolve(
            splitAndFilterEmpty(
                versionInfo.minecraftArguments ?:
                Tools.fromStringArray(minecraftArgs.toTypedArray())
            ).toList(),
            placeholderValues(),
            AccountUtils.isMicrosoftAccount(account)
        ).toTypedArray()
    }

    private fun placeholderValues(): Map<String, String> = HashMap<String, String>().apply {
        fun putValue(key: String, value: String?) {
            value?.takeIf { it.isNotBlank() }?.let { put(key, it) }
        }

        putValue("auth_session", account.accessToken)
        putValue("auth_access_token", account.accessToken)
        putValue("auth_player_name", account.username)
        putValue("auth_uuid", account.profileId?.replace("-", ""))
        putValue("auth_xuid", account.xuid)
        putValue("assets_root", ProfilePathHome.getAssetsHome())
        putValue("assets_index_name", versionInfo.assets)
        putValue("game_assets", ProfilePathHome.getAssetsHome())
        putValue("game_directory", gameDirPath.absolutePath)
        putValue("user_properties", "{}")
        putValue("user_type", if (AccountUtils.isMicrosoftAccount(account)) "msa" else "legacy")
        putValue("version_name", versionInfo.inheritsFrom ?: versionInfo.id)
        putValue("version_type", minecraftVersion.getCustomInfo().takeIf { it.isNotBlank() } ?: versionInfo.type)
        putValue("classpath_separator", File.pathSeparator)
        putValue("library_directory", getLibrariesHome())
        putValue("natives_directory", versionSpecificNativesDir().absolutePath)
        putValue("classpath", "${Tools.getLWJGL3ClassPath()}${File.pathSeparator}$launchClassPath")
        putValue("launcher_name", InfoDistributor.LAUNCHER_NAME)
        putValue("launcher_version", BuildConfig.VERSION_NAME)

        if (AccountUtils.isMicrosoftAccount(account)) {
            LaunchArgumentResolver.addClientIdAliases(this, MicrosoftAuthConfig.CLIENT_ID)
        }
    }

    private fun versionSpecificNativesDir() =
        File(PathManager.DIR_CACHE, "natives/${minecraftVersion.getVersionName()}")

    private fun nativeSearchPaths(): List<String> = linkedSetOf<String>().apply {
        versionSpecificNativesDir().takeIf { it.isDirectory && it.canRead() }
            ?.let { add(it.absolutePath) }
        File(PathManager.DIR_NATIVE_LIB).takeIf { it.isDirectory && it.canRead() }
            ?.let { add(it.absolutePath) }
    }.toList().also { paths ->
        if (paths.isEmpty()) {
            throw LaunchPreparationException("No readable native library search directory is available")
        }
        if (paths.none { isReadableNativeLibrary(File(it, "liblwjgl.so")) }) {
            throw LaunchPreparationException(
                "The packaged LWJGL native library liblwjgl.so is missing or unreadable"
            )
        }
    }

    private fun isReadableNativeLibrary(file: File) =
        file.isFile && file.canRead() && file.length() > 0L

    private fun splitAndFilterEmpty(arg: String): Array<String> {
        val list: MutableList<String> = ArrayList()
        arg.split(" ").forEach {
            if (it.isNotEmpty()) list.add(it)
        }
        return list.toTypedArray()
    }

    companion object {
        @JvmStatic
        fun getCacioJavaArgs(isJava8: Boolean): List<String> {
            val argsList: MutableList<String> = ArrayList()

            // Caciocavallo config AWT-enabled version
            argsList.add("-Djava.awt.headless=false")
            argsList.add("-Dcacio.managed.screensize=" + AWTCanvasView.AWT_CANVAS_WIDTH + "x" + AWTCanvasView.AWT_CANVAS_HEIGHT)
            argsList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager")
            argsList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler")
            argsList.add("-Dswing.defaultlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel")
            if (isJava8) {
                argsList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit")
                argsList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment")
            } else {
                argsList.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit")
                argsList.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment")
                argsList.add("-javaagent:" + LibPath.CACIO_17_AGENT.getAbsolutePath())
                argsList.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED")
                argsList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED")
                argsList.add("--add-opens=java.base/java.util=ALL-UNNAMED")
                argsList.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
                argsList.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED")
                argsList.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")
                argsList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")

                // Opens the java.net package to Arc DNS injector on Java 9+
                argsList.add("--add-opens=java.base/java.net=ALL-UNNAMED")
            }

            argsList.add(CacioFiles.buildBootClasspath(
                isJava8,
                LibPath.CACIO_8,
                LibPath.CACIO_17,
                LibPath.CACIO_17_AGENT
            ))

            return argsList
        }
    }
}
