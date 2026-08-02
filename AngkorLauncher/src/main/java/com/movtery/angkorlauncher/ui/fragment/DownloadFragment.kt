package com.movtery.angkorlauncher.ui.fragment

import android.os.Bundle
import android.os.SystemClock
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
import com.movtery.angkorlauncher.databinding.FragmentDownloadBinding
import com.movtery.angkorlauncher.event.value.DownloadPageEvent
import com.movtery.angkorlauncher.event.value.DownloadPageEvent.PageSwapEvent.Companion.IN
import com.movtery.angkorlauncher.event.value.DownloadPageEvent.PageSwapEvent.Companion.OUT
import com.movtery.angkorlauncher.ui.fragment.download.resource.ModDownloadFragment
import com.movtery.angkorlauncher.ui.fragment.download.resource.ModPackDownloadFragment
import com.movtery.angkorlauncher.ui.fragment.download.resource.ResourcePackDownloadFragment
import com.movtery.angkorlauncher.ui.fragment.download.resource.ShaderPackDownloadFragment
import com.movtery.angkorlauncher.ui.fragment.download.resource.WorldDownloadFragment
import com.movtery.angkorlauncher.utils.ZHTools
import org.greenrobot.eventbus.EventBus

class DownloadFragment : FragmentWithAnim(R.layout.fragment_download) {
    companion object {
        const val TAG = "DownloadFragment"
        private const val STATE_SELECTED_PAGE = "download_selected_page"
        private const val NAVIGATION_DEBOUNCE_MS = 320L
    }

    private lateinit var binding: FragmentDownloadBinding
    private var lastNavigationTarget = -1
    private var lastNavigationTapUptime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val initialPage = savedInstanceState?.getInt(STATE_SELECTED_PAGE, 0) ?: 0
        setupCategoryDock()
        initViewPager(initialPage)
        onFragmentSelect(initialPage)

        ZHTools.setTooltipText(
            binding.modImage,
            binding.modpackImage,
            binding.resourcePackImage,
            binding.worldImage,
            binding.shaderPackImage
        )
    }

    /**
     * Each complete tile owns exactly one click listener. The icon children are decorative,
     * so taps on their centers and edges follow the same authoritative navigation path.
     */
    private fun setupCategoryDock() {
        categoryIcons().forEach { icon ->
            icon.isClickable = false
            icon.isFocusable = false
        }

        categoryItems().forEachIndexed { index, item ->
            item.isClickable = true
            item.isFocusable = true
            item.setOnClickListener { navigateToCategory(index) }
        }
    }

    private fun navigateToCategory(targetPage: Int) {
        val safeTarget = targetPage.coerceIn(0, categoryItems().lastIndex)
        if (safeTarget == binding.downloadViewpager.currentItem) return

        val now = SystemClock.elapsedRealtime()
        if (safeTarget == lastNavigationTarget &&
            now - lastNavigationTapUptime < NAVIGATION_DEBOUNCE_MS
        ) {
            return
        }

        lastNavigationTarget = safeTarget
        lastNavigationTapUptime = now
        onFragmentSelect(safeTarget)
        binding.downloadViewpager.setCurrentItem(safeTarget, false)
    }

    private fun categoryItems(): List<View> = listOf(
        binding.modTab,
        binding.modpackTab,
        binding.resourcePackTab,
        binding.worldTab,
        binding.shaderPackTab
    )

    private fun categoryIcons(): List<View> = listOf(
        binding.modImage,
        binding.modpackImage,
        binding.resourcePackImage,
        binding.worldImage,
        binding.shaderPackImage
    )

    private fun initViewPager(initialPage: Int) {
        binding.downloadViewpager.apply {
            adapter = ViewPagerAdapter(this@DownloadFragment)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 1
            isUserInputEnabled = false
            registerOnPageChangeCallback(object: OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    onFragmentSelect(position)
                    EventBus.getDefault().post(DownloadPageEvent.PageSwapEvent(position, IN))
                }
            })
            setCurrentItem(initialPage.coerceIn(0, 4), false)
        }
    }

    private fun onFragmentSelect(position: Int) {
        val safePosition = position.coerceIn(0, categoryItems().lastIndex)
        categoryItems().forEachIndexed { index, item ->
            item.isSelected = index == safePosition
        }
        binding.classifyTab.onPageSelected(safePosition)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::binding.isInitialized) {
            outState.putInt(STATE_SELECTED_PAGE, binding.downloadViewpager.currentItem)
        }
        super.onSaveInstanceState(outState)
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.classifyLayout, Animations.BounceInRight))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.classifyLayout, Animations.FadeOutLeft))
        EventBus.getDefault().post(DownloadPageEvent.PageSwapEvent(binding.classifyTab.currentItemIndex, OUT))
    }

    override fun onDestroyView() {
        EventBus.getDefault().post(DownloadPageEvent.PageDestroyEvent())
        super.onDestroyView()
    }

    private class ViewPagerAdapter(private val fragment: Fragment): FragmentStateAdapter(fragment.requireActivity()) {
        override fun getItemCount(): Int = 5
        override fun createFragment(position: Int): Fragment {
            return when(position) {
                1 -> ModPackDownloadFragment(fragment)
                2 -> ResourcePackDownloadFragment(fragment)
                3 -> WorldDownloadFragment(fragment)
                4 -> ShaderPackDownloadFragment(fragment)
                else -> ModDownloadFragment(fragment)
            }
        }
    }
}
