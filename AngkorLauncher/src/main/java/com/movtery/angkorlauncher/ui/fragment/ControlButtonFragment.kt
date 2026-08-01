package com.movtery.angkorlauncher.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.constraintlayout.widget.ConstraintSet
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.databinding.FragmentControlManagerBinding
import com.movtery.angkorlauncher.event.sticky.FileSelectorEvent
import com.movtery.angkorlauncher.setting.AllSettings
import com.movtery.angkorlauncher.task.Task
import com.movtery.angkorlauncher.task.TaskExecutors
import com.movtery.angkorlauncher.ui.dialog.DeleteDialog
import com.movtery.angkorlauncher.ui.dialog.EditControlInfoDialog
import com.movtery.angkorlauncher.ui.dialog.FilesDialog
import com.movtery.angkorlauncher.ui.dialog.FilesDialog.FilesButton
import com.movtery.angkorlauncher.ui.dialog.SelectControlsDialog
import com.movtery.angkorlauncher.ui.dialog.TipDialog
import com.movtery.angkorlauncher.ui.subassembly.customcontrols.ControlInfoData
import com.movtery.angkorlauncher.ui.subassembly.customcontrols.ControlSelectedListener
import com.movtery.angkorlauncher.ui.subassembly.customcontrols.ControlsListViewCreator
import com.movtery.angkorlauncher.ui.subassembly.customcontrols.EditControlData
import com.movtery.angkorlauncher.ui.subassembly.customcontrols.EditControlData.Companion.createNewControlFile
import com.movtery.angkorlauncher.utils.NewbieGuideUtils
import com.movtery.angkorlauncher.utils.ZHTools
import com.movtery.angkorlauncher.utils.anim.AnimUtils.Companion.setVisibilityAnim
import com.movtery.angkorlauncher.utils.file.FileTools
import com.movtery.angkorlauncher.utils.file.PasteFile
import com.movtery.angkorlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.CustomControlsActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import org.greenrobot.eventbus.EventBus
import java.io.File

class ControlButtonFragment : FragmentWithAnim(R.layout.fragment_control_manager) {
    companion object {
        const val TAG: String = "ControlButtonFragment"
        const val BUNDLE_SELECT_CONTROL: String = "bundle_select_control"

        private const val STATE_SELECTED_PROFILE = "state_selected_control_profile"
        private const val STATE_SEARCH_QUERY = "state_control_search_query"
        private const val SEARCH_DELAY_MS = 180L
        private const val ACTION_DEBOUNCE_MS = 500L
    }

    private lateinit var binding: FragmentControlManagerBinding
    private lateinit var controlsListViewCreator: ControlsListViewCreator
    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null
    private var mSelectControl = false
    private var selectedControlFile: File? = null
    private var restoredSelectionPath: String? = null
    private var pendingSearch: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(
            OpenDocumentWithExtension("json", true)
        ) { uris: List<Uri>? ->
            uris?.let { uriList ->
                val dialog = ZHTools.showTaskRunningDialog(requireContext())
                Task.runTask {
                    uriList.forEach { uri ->
                        FileTools.copyFileInBackground(
                            requireContext(),
                            uri,
                            File(PathManager.DIR_CTRLMAP_PATH).absolutePath
                        )
                    }
                }.ended(TaskExecutors.getAndroidUI()) {
                    Toast.makeText(requireContext(), R.string.file_added, Toast.LENGTH_SHORT).show()
                    controlsListViewCreator.refresh()
                }.onThrowable { error ->
                    Tools.showErrorRemote(error)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                }.execute()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentControlManagerBinding.inflate(inflater, container, false)
        controlsListViewCreator = ControlsListViewCreator(requireContext(), binding.recyclerView)
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        parseBundle()
        restoredSelectionPath = savedInstanceState?.getString(STATE_SELECTED_PROFILE)
        applyResponsiveConstraints()
        initList()
        initSearch(savedInstanceState?.getString(STATE_SEARCH_QUERY).orEmpty())
        initActions()
        renderNoSelection()
        controlsListViewCreator.listAtPath()
        startNewbieGuide()
    }

    private fun initList() {
        controlsListViewCreator.apply {
            setShowSearchResultsOnly(true)
            setSelectedListener(object : ControlSelectedListener() {
                override fun onItemSelected(file: File) {
                    handleProfileSelection(file)
                }

                override fun onItemLongClick(file: File) {
                    TipDialog.Builder(requireContext())
                        .setTitle(R.string.pedit_control)
                        .setMessage(R.string.controls_set_default_message)
                        .setConfirmClickListener {
                            AllSettings.defaultCtrl.put(file.absolutePath).save()
                        }.showDialog()
                }
            })

            setRefreshListener {
                val count = itemCount
                binding.profileCountText.text = getString(R.string.controls_profile_count, count)
                binding.nothingText.setText(
                    if (binding.searchEditText.text.isNullOrBlank()) {
                        R.string.controls_nothing
                    } else {
                        R.string.controls_no_search_results
                    }
                )
                setVisibilityAnim(binding.nothingText, count == 0, 200)
                resolveSelectionAfterRefresh()
                syncImportActionState()
            }
        }
    }

    private fun initSearch(restoredQuery: String) {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                pendingSearch?.let(binding.searchEditText::removeCallbacks)
                pendingSearch = Runnable {
                    controlsListViewCreator.searchControls(
                        binding.searchResultCount,
                        text?.toString().orEmpty(),
                        false
                    )
                }.also { binding.searchEditText.postDelayed(it, SEARCH_DELAY_MS) }
            }

            override fun afterTextChanged(editable: Editable?) = Unit
        })

        if (restoredQuery.isNotEmpty()) binding.searchEditText.setText(restoredQuery)
    }

    private fun initActions() {
        binding.refreshProfileButton.setOnClickListener { button ->
            debounceAction(button) { controlsListViewCreator.refresh() }
        }

        binding.createProfileButton.setOnClickListener { button ->
            debounceAction(button) { showCreateProfileDialog() }
        }

        binding.importProfileButton.setOnClickListener { button ->
            debounceAction(button) {
                if (PasteFile.getInstance().pasteType != null) pastePendingProfiles()
                else launchProfileImport()
            }
        }

        binding.controlSelectorButton.setOnClickListener { button ->
            debounceAction(button) { showProfileSelector() }
        }

        binding.deleteProfileButton.setOnClickListener { button ->
            debounceAction(button) { deleteSelectedProfile() }
        }

        binding.editProfileButton.setOnClickListener { button ->
            debounceAction(button) { selectedControlFile?.let(::openProfileEditor) }
        }

        binding.profileOperationsButton.setOnClickListener { button ->
            debounceAction(button) { selectedControlFile?.let(::showFileActions) }
        }

        binding.returnProfileButton.setOnClickListener { button ->
            debounceAction(button) { ZHTools.onBackPressed(requireActivity()) }
        }

        ZHTools.setTooltipText(
            binding.refreshProfileButton,
            binding.deleteProfileButton
        )
        ZHTools.setTooltipText(binding.createProfileButton, binding.createProfileButton.contentDescription)
        ZHTools.setTooltipText(binding.importProfileButton, binding.importProfileButton.contentDescription)
        ZHTools.setTooltipText(binding.returnProfileButton, binding.returnProfileButton.contentDescription)
        ZHTools.setTooltipText(binding.controlSelectorButton, binding.controlSelectorButton.contentDescription)
        ZHTools.setTooltipText(
            binding.editProfileButton,
            binding.editProfileButton.contentDescription
        )
        ZHTools.setTooltipText(
            binding.profileOperationsButton,
            binding.profileOperationsButton.contentDescription
        )
        syncImportActionState()
    }

    private fun applyResponsiveConstraints() {
        val resources = resources
        ConstraintSet().apply {
            clone(binding.root)
            setGuidelinePercent(
                R.id.control_left_start_guide,
                resources.getFraction(R.fraction.control_left_start, 1, 1)
            )
            setGuidelinePercent(
                R.id.control_left_end_guide,
                resources.getFraction(R.fraction.control_left_end, 1, 1)
            )
            setGuidelinePercent(
                R.id.control_right_start_guide,
                resources.getFraction(R.fraction.control_right_start, 1, 1)
            )
            setGuidelinePercent(
                R.id.control_right_end_guide,
                resources.getFraction(R.fraction.control_right_end, 1, 1)
            )
            applyTo(binding.root)
        }
    }

    private fun handleProfileSelection(file: File) {
        if (mSelectControl) {
            EventBus.getDefault().postSticky(
                FileSelectorEvent(removeLockPath(file.absolutePath))
            )
            Tools.removeCurrentFragment(requireActivity())
        } else if (file.isFile) {
            selectProfile(file)
        }
    }

    private fun showProfileSelector() {
        SelectControlsDialog(
            requireContext(),
            object : SelectControlsDialog.SelectedListener {
                override fun onSelected(file: File) {
                    handleProfileSelection(file)
                }
            }
        ).show()
    }

    private fun resolveSelectionAfterRefresh() {
        selectedControlFile?.let { current ->
            if (current.exists()) {
                controlsListViewCreator.setSelectedFile(current)
                return
            }
            selectedControlFile = null
            renderNoSelection()
        }

        val restored = restoredSelectionPath?.let(::File)?.takeIf(File::exists)
        val configured = File(AllSettings.defaultCtrl.getValue()).takeIf(File::exists)
        val candidate = restored ?: configured ?: controlsListViewCreator.getFirstValidFile()
        restoredSelectionPath = null
        candidate?.let(::selectProfile)
    }

    private fun selectProfile(file: File) {
        selectedControlFile = file
        controlsListViewCreator.setSelectedFile(file)
        binding.controlSelectorText.text = file.name
        binding.profilePathText.text = file.absolutePath
        binding.profileDetailName.text = file.name
        binding.profileDetailDescription.setText(R.string.generic_waiting)
        setSelectionActionsEnabled(false)

        val selectedPath = file.absolutePath
        val appContext = requireContext().applicationContext
        Task.runTask {
            EditControlData.loadFormFile(appContext, file)
        }.ended(TaskExecutors.getAndroidUI()) { info ->
            if (!isAdded || view == null) return@ended
            if (selectedControlFile?.absolutePath != selectedPath) return@ended
            renderSelectedProfile(file, info)
        }.onThrowable(TaskExecutors.getAndroidUI()) { error ->
            if (!isAdded || view == null) return@onThrowable
            if (selectedControlFile?.absolutePath == selectedPath) {
                Tools.showErrorRemote(error)
                renderSelectedProfile(file, null)
            }
        }.execute()
    }

    private fun renderSelectedProfile(file: File, info: ControlInfoData?) {
        binding.profileDetailName.text = file.name
        binding.profileDetailDescription.text = when (val description = info?.desc) {
            "control.default.desc.text" -> getString(R.string.controls_info_default_desc)
            null, "", "null" -> getString(R.string.controls_info_no_info)
            else -> description
        }
        binding.profileOperationsButton.alpha = 1f
        setSelectionActionsEnabled(true)
    }

    private fun renderNoSelection() {
        binding.controlSelectorText.setText(R.string.controls_no_profile_selected)
        binding.profilePathText.text = File(PathManager.DIR_CTRLMAP_PATH).absolutePath
        binding.profileDetailName.setText(R.string.controls_no_profile_selected)
        binding.profileDetailDescription.setText(R.string.controls_tip_set_default)
        binding.profileOperationsButton.alpha = 0.45f
        setSelectionActionsEnabled(false)
    }

    private fun setSelectionActionsEnabled(enabled: Boolean) {
        binding.profileOperationsButton.isEnabled = enabled
        binding.editProfileButton.isEnabled = enabled
        binding.deleteProfileButton.isEnabled = enabled
        binding.editProfileButton.alpha = if (enabled) 1f else 0.5f
        binding.deleteProfileButton.alpha = if (enabled) 1f else 0.5f
    }

    private fun showCreateProfileDialog() {
        if (isTaskRunning()) {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = EditControlInfoDialog(requireContext(), true, null, ControlInfoData())
        dialog.setTitle(getString(R.string.controls_create_new))
        dialog.setOnConfirmClickListener { fileName, controlInfoData ->
            val file = File(PathManager.DIR_CTRLMAP_PATH, "$fileName.json")
            if (file.exists()) {
                dialog.fileNameEditBox.error = getString(R.string.file_rename_exitis)
                return@setOnConfirmClickListener
            }

            createNewControlFile(requireContext(), file, controlInfoData)
            selectProfile(file)
            controlsListViewCreator.refresh()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun launchProfileImport() {
        val suffix = ".json"
        Toast.makeText(
            requireContext(),
            getString(R.string.file_add_file_tip, suffix),
            Toast.LENGTH_SHORT
        ).show()
        openDocumentLauncher?.launch(suffix)
    }

    private fun pastePendingProfiles() {
        PasteFile.getInstance().pasteFiles(
            requireActivity(),
            File(PathManager.DIR_CTRLMAP_PATH),
            null,
            Task.runTask(TaskExecutors.getAndroidUI()) {
                controlsListViewCreator.refresh()
                syncImportActionState()
            }
        )
    }

    private fun syncImportActionState() {
        val pastePending = PasteFile.getInstance().pasteType != null
        binding.importProfileIcon.setImageResource(
            if (pastePending) R.drawable.ic_paste else R.drawable.ic_download
        )
        binding.importProfileText.setText(
            if (pastePending) R.string.generic_paste else R.string.controls_import_control
        )
        binding.importProfileButton.contentDescription = getString(
            if (pastePending) R.string.generic_paste else R.string.controls_import_control
        )
    }

    private fun deleteSelectedProfile() {
        val file = selectedControlFile?.takeIf(File::exists) ?: return
        DeleteDialog(
            requireContext(),
            Task.runTask(TaskExecutors.getAndroidUI()) {
                if (selectedControlFile?.absolutePath == file.absolutePath) {
                    selectedControlFile = null
                    renderNoSelection()
                }
                controlsListViewCreator.refresh()
            },
            listOf(file)
        ).show()
    }

    private fun showFileActions(file: File) {
        val buttons = FilesButton().apply {
            setButtonVisibility(true, true, true, true, true, true)
            setMessageText(getString(R.string.file_message))
            setMoreButtonText(getString(R.string.generic_edit))
        }
        val refreshTask = Task.runTask(TaskExecutors.getAndroidUI()) {
            controlsListViewCreator.refresh()
        }
        val dialog = FilesDialog(
            requireContext(),
            buttons,
            refreshTask,
            controlsListViewCreator.fullPath,
            file
        )
        dialog.setCopyButtonClick { syncImportActionState() }
        dialog.setMoreButtonClick { openProfileEditor(file) }
        dialog.show()
    }

    private fun openProfileEditor(file: File) {
        if (isTaskRunning()) {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), CustomControlsActivity::class.java).apply {
            putExtra(CustomControlsActivity.BUNDLE_CONTROL_PATH, file.absolutePath)
        }
        startActivity(intent)
    }

    private fun debounceAction(view: View, action: () -> Unit) {
        if (!view.isEnabled) return
        view.isEnabled = false
        action()
        view.postDelayed({
            if (!isAdded) return@postDelayed
            view.isEnabled = when (view.id) {
                R.id.edit_profile_button,
                R.id.delete_profile_button,
                R.id.profile_operations_button -> selectedControlFile?.exists() == true
                else -> true
            }
        }, ACTION_DEBOUNCE_MS)
    }

    private fun removeLockPath(path: String): String {
        return path.replace(PathManager.DIR_CTRLMAP_PATH, ".")
    }

    private fun parseBundle() {
        mSelectControl = arguments?.getBoolean(BUNDLE_SELECT_CONTROL, mSelectControl) ?: mSelectControl
    }

    private fun startNewbieGuide() {
        if (NewbieGuideUtils.showOnlyOne(TAG)) return
        val activity = requireActivity()
        TapTargetSequence(activity)
            .targets(
                NewbieGuideUtils.getSimpleTarget(
                    activity,
                    binding.refreshProfileButton,
                    getString(R.string.generic_refresh),
                    getString(R.string.newbie_guide_general_refresh)
                ),
                NewbieGuideUtils.getSimpleTarget(
                    activity,
                    binding.searchEditText,
                    getString(R.string.generic_search),
                    getString(R.string.newbie_guide_control_search)
                ),
                NewbieGuideUtils.getSimpleTarget(
                    activity,
                    binding.importProfileButton,
                    getString(R.string.controls_import_control),
                    getString(R.string.newbie_guide_control_import)
                ),
                NewbieGuideUtils.getSimpleTarget(
                    activity,
                    binding.createProfileButton,
                    getString(R.string.controls_create_new),
                    getString(R.string.newbie_guide_control_create)
                )
            ).start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedControlFile?.absolutePath?.let {
            outState.putString(STATE_SELECTED_PROFILE, it)
        }
        outState.putString(STATE_SEARCH_QUERY, binding.searchEditText.text?.toString().orEmpty())
    }

    override fun onDestroyView() {
        controlsListViewCreator.setRefreshListener(null)
        controlsListViewCreator.setSelectedListener(null)
        pendingSearch?.let(binding.searchEditText::removeCallbacks)
        pendingSearch = null
        super.onDestroyView()
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.controlLayout, Animations.BounceInDown))
            .apply(AnimPlayer.Entry(binding.operateLayout, Animations.BounceInLeft))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.controlLayout, Animations.FadeOutUp))
            .apply(AnimPlayer.Entry(binding.operateLayout, Animations.FadeOutRight))
    }
}
