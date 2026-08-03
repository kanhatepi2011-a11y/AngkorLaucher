package com.movtery.angkorlauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.databinding.FragmentSettingsBinding
import com.movtery.angkorlauncher.event.value.SettingsPageSwapEvent
import com.movtery.angkorlauncher.feature.log.Logging
import com.movtery.angkorlauncher.setting.Settings
import com.movtery.angkorlauncher.ui.fragment.settings.ControlSettingsFragment
import com.movtery.angkorlauncher.ui.fragment.settings.ExperimentalSettingsFragment
import com.movtery.angkorlauncher.ui.fragment.settings.GameSettingsFragment
import com.movtery.angkorlauncher.ui.fragment.settings.LauncherSettingsFragment
import com.movtery.angkorlauncher.ui.fragment.settings.VideoSettingsFragment
import com.movtery.angkorlauncher.ui.navigation.PageNavigation
import com.movtery.angkorlauncher.utils.ZHTools
import org.greenrobot.eventbus.EventBus

class SettingsFragment : FragmentWithAnim(R.layout.fragment_settings) {
    companion object {
        const val TAG: String = "SettingsFragment"
        private const val STATE_SELECTED_PAGE = "settings_selected_page"
        private val CATEGORY_LABELS = intArrayOf(
            R.string.setting_category_video,
            R.string.setting_category_control,
            R.string.setting_category_game,
            R.string.setting_category_launcher,
            R.string.setting_category_experimental
        )
    }

    private lateinit var binding: FragmentSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val initialPage = savedInstanceState?.getInt(STATE_SELECTED_PAGE, 0) ?: 0
        setupNavigationRail()
        initViewPager(initialPage)
        onFragmentSelect(initialPage)

        ZHTools.setTooltipText(
            binding.videoImage,
            binding.controlsImage,
            binding.gameImage,
            binding.launcherImage,
            binding.experimentalImage
        )

        binding.settingsTooltip.visibility = View.GONE
    }

    /** The complete rail tile owns the only click listener; icons are decorative children. */
    private fun setupNavigationRail() {
        navigationIcons().forEach { icon ->
            icon.isClickable = false
            icon.isFocusable = false
        }

        navigationItems().forEachIndexed { index, item ->
            item.isClickable = true
            item.isFocusable = true
            item.setOnClickListener { navigateToSettingsPage(index) }
        }
    }

    private fun navigateToSettingsPage(targetPage: Int) {
        val items = navigationItems()
        val currentPage = binding.settingsViewpager.currentItem
        val safeTarget = PageNavigation.targetOrNone(currentPage, targetPage, items.size)
        val shouldNavigate = safeTarget != PageNavigation.NO_DESTINATION
        val logTarget = if (shouldNavigate) safeTarget else currentPage
        Logging.i(
            "NavigationTap",
            "bar=settings item=${getString(CATEGORY_LABELS[logTarget])} " +
                "currentPage=$currentPage targetPage=$logTarget " +
                "selectedBefore=${items[logTarget].isSelected} " +
                "navigateCalled=$shouldNavigate thread=${Thread.currentThread().name}"
        )
        if (!shouldNavigate) return

        onFragmentSelect(safeTarget)
        try {
            binding.settingsViewpager.setCurrentItem(safeTarget, false)
        } catch (error: RuntimeException) {
            onFragmentSelect(currentPage)
            throw error
        }
    }

    private fun navigationItems(): List<View> = listOf(
        binding.videoSettings,
        binding.controlsSettings,
        binding.gameSettings,
        binding.launcherSettings,
        binding.experimentalSettings
    )

    private fun navigationIcons(): List<View> = listOf(
        binding.videoImage,
        binding.controlsImage,
        binding.gameImage,
        binding.launcherImage,
        binding.experimentalImage
    )

    override fun onResume() {
        super.onResume()
        Settings.refreshSettings()
    }

    private fun initViewPager(initialPage: Int) {
        binding.settingsViewpager.apply {
            adapter = ViewPagerAdapter(this@SettingsFragment)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 1
            isUserInputEnabled = false
            registerOnPageChangeCallback(object: OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    onFragmentSelect(position)
                    updateCategoryTooltip(position, true)
                    EventBus.getDefault().post(SettingsPageSwapEvent(position))
                }
            })
            setCurrentItem(initialPage.coerceIn(0, 4), false)
        }
    }

    private fun onFragmentSelect(position: Int) {
        val safePosition = position.coerceIn(0, navigationItems().lastIndex)
        navigationItems().forEachIndexed { index, item ->
            item.isSelected = index == safePosition
        }
    }

    private fun updateCategoryTooltip(position: Int, animate: Boolean) {
        val safePosition = position.coerceIn(CATEGORY_LABELS.indices)
        val itemStep = resources.getDimensionPixelSize(R.dimen.settings_toolbar_item_size) +
            resources.getDimensionPixelSize(R.dimen.settings_toolbar_item_gap)

        val applyState = {
            binding.settingsTooltip.setText(CATEGORY_LABELS[safePosition])
            binding.settingsTooltip.translationY = (itemStep * safePosition).toFloat()
        }

        binding.settingsTooltip.animate().cancel()
        if (!animate || !binding.settingsTooltip.isLaidOut) {
            applyState()
            binding.settingsTooltip.alpha = 1f
            return
        }

        binding.settingsTooltip.animate()
            .alpha(0f)
            .setDuration(90L)
            .withEndAction {
                applyState()
                binding.settingsTooltip.animate()
                    .alpha(1f)
                    .setDuration(180L)
                    .start()
            }
            .start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::binding.isInitialized) {
            outState.putInt(STATE_SELECTED_PAGE, binding.settingsViewpager.currentItem)
        }
        super.onSaveInstanceState(outState)
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.settingsLayout, Animations.BounceInLeft))
            .apply(AnimPlayer.Entry(binding.settingsViewpager, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.settingsLayout, Animations.FadeOutRight))
            .apply(AnimPlayer.Entry(binding.settingsViewpager, Animations.FadeOutUp))
    }

    private class ViewPagerAdapter(val fragment: FragmentWithAnim): FragmentStateAdapter(fragment.requireActivity()) {
        override fun getItemCount(): Int = 5
        override fun createFragment(position: Int): Fragment {
            return when(position) {
                1 -> ControlSettingsFragment(fragment)
                2 -> GameSettingsFragment()
                3 -> LauncherSettingsFragment(fragment)
                4 -> ExperimentalSettingsFragment()
                else -> VideoSettingsFragment()
            }
        }
    }

}
