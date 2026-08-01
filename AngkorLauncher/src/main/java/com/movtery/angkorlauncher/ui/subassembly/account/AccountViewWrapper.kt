package com.movtery.angkorlauncher.ui.subassembly.account

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.databinding.ViewAccountBinding
import com.movtery.angkorlauncher.databinding.ViewLauncherAccountBinding
import com.movtery.angkorlauncher.feature.accounts.AccountUtils
import com.movtery.angkorlauncher.feature.accounts.AccountsManager
import com.movtery.angkorlauncher.feature.log.Logging
import com.movtery.angkorlauncher.ui.fragment.AccountFragment
import com.movtery.angkorlauncher.ui.fragment.FragmentWithAnim
import com.movtery.angkorlauncher.utils.ZHTools
import com.movtery.angkorlauncher.utils.skin.SkinLoader
import net.kdt.pojavlaunch.Tools

/**
 * Keeps the existing account navigation and data source while allowing the launcher dock to use
 * its own horizontal presentation. The standard account screen continues to use [ViewAccountBinding].
 */
class AccountViewWrapper private constructor(
    private val parentFragment: FragmentWithAnim?,
    private val root: View,
    private val userIcon: ImageView,
    private val userName: TextView,
    private val accountType: TextView,
    private val accountStatusIcon: ImageView?,
    private val useLauncherBrandIcon: Boolean
) {
    private val mContext: Context = root.context

    constructor(
        parentFragment: FragmentWithAnim? = null,
        binding: ViewAccountBinding
    ) : this(
        parentFragment,
        binding.root,
        binding.userIcon,
        binding.userName,
        binding.accountType,
        null,
        false
    )

    constructor(
        parentFragment: FragmentWithAnim? = null,
        binding: ViewLauncherAccountBinding
    ) : this(
        parentFragment,
        binding.root,
        binding.userIcon,
        binding.userName,
        binding.accountType,
        binding.accountStatusIcon,
        true
    )

    init {
        parentFragment?.let { fragment ->
            root.setOnClickListener {
                ZHTools.swapFragmentWithAnim(fragment, AccountFragment::class.java, AccountFragment.TAG, null)
            }
        }
    }

    fun refreshAccountInfo() {
        val account = AccountsManager.currentAccount
        if (account == null) {
            accountStatusIcon?.visibility = View.GONE
            accountType.visibility = View.GONE

            if (useLauncherBrandIcon) {
                userIcon.clearColorFilter()
                userIcon.setImageResource(R.drawable.angkor_launcher_logo)
                userName.setText(R.string.account_add)
            } else if (parentFragment == null) {
                userIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_help))
                userName.text = null
            } else {
                userIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_add))
                userIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.download_text_primary))
                userName.setText(R.string.account_add)
            }
            return
        }

        if (useLauncherBrandIcon) {
            userIcon.clearColorFilter()
            userIcon.setImageResource(R.drawable.angkor_launcher_logo)
        } else {
            runCatching {
                userIcon.clearColorFilter()
                userIcon.setImageDrawable(
                    SkinLoader.getAvatarDrawable(
                        mContext,
                        account,
                        Tools.dpToPx(
                            mContext.resources.getDimensionPixelSize(R.dimen._52sdp).toFloat()
                        ).toInt()
                    )
                )
            }.onFailure { error ->
                Logging.e("AccountViewWrapper", "Failed to load avatar.", error)
            }
        }

        userName.text = account.username
        accountType.text = AccountUtils.getAccountTypeName(mContext, account)
        accountType.visibility = if (
            useLauncherBrandIcon && mContext.resources.configuration.screenWidthDp < 720
        ) View.GONE else View.VISIBLE
        accountStatusIcon?.visibility = if (AccountUtils.isMicrosoftAccount(account)) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}
