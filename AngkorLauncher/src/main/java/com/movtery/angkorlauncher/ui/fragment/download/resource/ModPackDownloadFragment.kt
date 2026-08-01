package com.movtery.angkorlauncher.ui.fragment.download.resource

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.movtery.anim.animations.Animations
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.event.value.InstallLocalModpackEvent
import com.movtery.angkorlauncher.feature.download.enums.Classify
import com.movtery.angkorlauncher.feature.download.utils.CategoryUtils
import com.movtery.angkorlauncher.feature.mod.modpack.install.InstallExtra
import com.movtery.angkorlauncher.task.Task
import com.movtery.angkorlauncher.task.TaskExecutors
import com.movtery.angkorlauncher.utils.path.PathManager
import com.movtery.angkorlauncher.utils.ZHTools
import com.movtery.angkorlauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim
import com.movtery.angkorlauncher.utils.file.FileTools
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import org.greenrobot.eventbus.EventBus

class ModPackDownloadFragment(parentFragment: Fragment? = null) : AbstractResourceDownloadFragment(
    parentFragment,
    Classify.MODPACK,
    CategoryUtils.getModPackCategory(),
    true
) {
    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension(null)) { uris: List<Uri>? ->
            uris?.let { uriList ->
                uriList[0].let { result ->
                    if (!isTaskRunning()) {
                        val dialog = ZHTools.showTaskRunningDialog(requireContext())
                        Task.runTask {
                            FileTools.copyFileInBackground(requireContext(), result, PathManager.DIR_CACHE.absolutePath)
                        }.ended(TaskExecutors.getAndroidUI()) { modPackFile ->
                            modPackFile?.let {
                                EventBus.getDefault().post(InstallLocalModpackEvent(InstallExtra(true, it.absolutePath)))
                            }
                        }.onThrowable { e ->
                            Tools.showErrorRemote(e)
                        }.finallyTask(TaskExecutors.getAndroidUI()) {
                            dialog.dismiss()
                        }.execute()
                    }
                }
            }
        }
    }

    override fun initInstallButton(installButton: Button) {
        installButton.setOnClickListener {
            if (!isTaskRunning()) {
                Toast.makeText(requireActivity(), getString(R.string.select_modpack_local_tip), Toast.LENGTH_SHORT).show()
                openDocumentLauncher?.launch(null)
            } else {
                setViewAnim(installButton, Animations.Shake)
                Toast.makeText(requireActivity(), getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show()
            }
        }
    }
}