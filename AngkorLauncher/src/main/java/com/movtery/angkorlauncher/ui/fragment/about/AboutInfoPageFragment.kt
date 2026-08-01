package com.movtery.angkorlauncher.ui.fragment.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.movtery.angkorlauncher.InfoCenter
import com.movtery.angkorlauncher.InfoDistributor
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.databinding.FragmentAboutInfoPageBinding
import com.movtery.angkorlauncher.ui.dialog.TipDialog
import com.movtery.angkorlauncher.utils.ZHTools
import com.movtery.angkorlauncher.utils.path.UrlManager

class AboutInfoPageFragment : Fragment(R.layout.fragment_about_info_page) {
    private lateinit var binding: FragmentAboutInfoPageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutInfoPageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireActivity()

        binding.apply {
            dec1.text = InfoCenter.replaceName(context, R.string.about_dec1)
            dec2.text = InfoCenter.replaceName(context, R.string.about_dec2)
            dec3.text = InfoCenter.replaceName(context, R.string.about_dec3)

            githubButton.setOnClickListener { ZHTools.openLink(requireActivity(), UrlManager.URL_HOME) }
            licenseButton.setOnClickListener { ZHTools.openLink(requireActivity(), "https://www.gnu.org/licenses/gpl-3.0.html") }

            if (ZHTools.isChinese(requireActivity())) {
                qqGroupButton.visibility = View.VISIBLE
                qqGroupButton.setOnClickListener {
                    TipDialog.Builder(context)
                        .setTitle("QQ")
                        .setMessage("欢迎加入 ${InfoDistributor.APP_NAME} 官方 QQ 交流群（群号：${InfoCenter.QQ_GROUP}）！由于群人数有限，加入群聊前需要赞助 5元 或以上金额，请点击右侧“赞助开发”按钮访问爱发电。")
                        .setSelectable(true)
                        .setConfirm(R.string.generic_confirm)
                        .setShowCancel(false)
                        .showDialog()
                }
            } else {
                qqGroupButton.visibility = View.GONE
            }

            discordButton.setOnClickListener { ZHTools.openLink(requireActivity(), "https://discord.gg/yDDkTHp4cJ") }
        }
    }
}
