package com.movtery.angkorlauncher.ui.fragment

import com.movtery.anim.AnimPlayer
import com.movtery.angkorlauncher.setting.AllSettings
import com.movtery.angkorlauncher.utils.anim.SlideAnimation

abstract class FragmentWithAnim : BaseFragment, SlideAnimation {
    private var animPlayer: AnimPlayer = AnimPlayer()

    constructor() : super()

    constructor(contentLayoutId: Int) : super(contentLayoutId)

    override fun onStart() {
        super.onStart()
        slideIn()
    }

    fun slideIn() {
        playAnimation { slideIn(it) }
    }

    fun slideOut() {
        playAnimation { slideOut(it) }
    }

    private fun playAnimation(animationAction: (AnimPlayer) -> Unit) {
        if (AllSettings.animation.getValue()) {
            animPlayer.clearEntries()
            animPlayer.apply {
                animationAction(this)
                start()
            }
        }
    }
}
