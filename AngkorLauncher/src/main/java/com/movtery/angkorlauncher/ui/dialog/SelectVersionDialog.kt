package com.movtery.angkorlauncher.ui.dialog

import android.content.Context
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.databinding.DialogSelectVersionBinding
import com.movtery.angkorlauncher.ui.subassembly.versionlist.VersionSelectedListener
import com.movtery.angkorlauncher.ui.subassembly.versionlist.VersionType

class SelectVersionDialog(context: Context) : FullScreenDialog(context) {
    private val binding: DialogSelectVersionBinding = DialogSelectVersionBinding.inflate(layoutInflater)
    private lateinit var releaseTab: TabLayout.Tab
    private lateinit var snapshotTab: TabLayout.Tab
    private lateinit var betaTab: TabLayout.Tab
    private lateinit var alphaTab: TabLayout.Tab
    private var versionType: VersionType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCancelable(false)
        setContentView(binding.root)

        binding.closeButton.setOnClickListener { dismiss() }

        bindTab()

        binding.apply {
            versionTab.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    refresh(tab)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                }
            })

            searchVersion.doAfterTextChanged { text ->
                val string = text?.toString() ?: ""
                version.setFilterString(string)
            }

            refresh(versionTab.getTabAt(versionTab.selectedTabPosition))
        }
    }

    fun setOnVersionSelectedListener(versionSelectedListener: VersionSelectedListener?) {
        binding.version.setVersionSelectedListener(versionSelectedListener)
    }

    private fun refresh(tab: TabLayout.Tab?) {
        setVersionType(tab)
        binding.version.setVersionType(versionType)
    }

    private fun setVersionType(tab: TabLayout.Tab?) {
        when (tab) {
            releaseTab -> versionType = VersionType.RELEASE
            snapshotTab -> versionType = VersionType.SNAPSHOT
            betaTab -> versionType = VersionType.BETA
            alphaTab -> versionType = VersionType.ALPHA
            else -> dismiss()
        }
    }

    private fun bindTab() {
        binding.versionTab.apply {
            fun TabLayout.addNewTab(textRes: Int): TabLayout.Tab {
                val tab = newTab().apply {
                    setText(textRes)
                }
                addTab(tab)
                return tab
            }

            releaseTab = addNewTab(R.string.generic_release)
            snapshotTab = addNewTab(R.string.version_snapshot)
            betaTab = addNewTab(R.string.version_beta)
            alphaTab = addNewTab(R.string.version_alpha)

            selectTab(releaseTab)
        }
    }
}
