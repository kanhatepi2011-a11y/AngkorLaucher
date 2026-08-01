package com.movtery.angkorlauncher.ui.fragment.download.resource

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.feature.download.enums.Classify
import com.movtery.angkorlauncher.feature.download.enums.Platform
import com.movtery.angkorlauncher.feature.download.platform.AbstractPlatformHelper.Companion.getShaderPackPath
import com.movtery.angkorlauncher.feature.download.utils.CategoryUtils
import com.movtery.angkorlauncher.task.Task
import com.movtery.angkorlauncher.task.TaskExecutors
import com.movtery.angkorlauncher.utils.ZHTools
import com.movtery.angkorlauncher.utils.file.FileTools
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension

class ShaderPackDownloadFragment(parentFragment: Fragment? = null) : AbstractResourceDownloadFragment(
    parentFragment,
    Classify.SHADER_PACK,
    CategoryUtils.getShaderPackCategory(),
    false,
    Platform.MODRINTH
) {
    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension("zip", true)) { uris: List<Uri>? ->
            uris?.let { uriList ->
                val dialog = ZHTools.showTaskRunningDialog(requireContext())
                Task.runTask {
                    uriList.forEach { uri ->
                        FileTools.copyFileInBackground(requireActivity(), uri, getShaderPackPath().absolutePath)
                    }
                }.onThrowable { e ->
                    Tools.showErrorRemote(e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                }.execute()
            }
        }
    }

    override fun initInstallButton(installButton: Button) {
        installButton.setOnClickListener {
            val suffix = ".zip"
            Toast.makeText(
                requireActivity(),
                String.format(getString(R.string.file_add_file_tip), suffix),
                Toast.LENGTH_SHORT
            ).show()
            openDocumentLauncher?.launch(suffix)
        }
    }
}