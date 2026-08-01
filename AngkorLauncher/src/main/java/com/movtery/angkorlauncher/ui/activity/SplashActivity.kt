package com.movtery.angkorlauncher.ui.activity

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.movtery.angkorlauncher.InfoCenter
import com.movtery.angkorlauncher.InfoDistributor
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.databinding.ActivitySplashBinding
import com.movtery.angkorlauncher.feature.unpack.Components
import com.movtery.angkorlauncher.feature.unpack.Jre
import com.movtery.angkorlauncher.feature.unpack.UnpackComponentsTask
import com.movtery.angkorlauncher.feature.unpack.UnpackJreTask
import com.movtery.angkorlauncher.feature.unpack.UnpackSingleFilesTask
import com.movtery.angkorlauncher.task.Task
import com.movtery.angkorlauncher.ui.dialog.TipDialog
import com.movtery.angkorlauncher.utils.StoragePermissionsUtils
import net.kdt.pojavlaunch.LauncherActivity
import net.kdt.pojavlaunch.MissingStorageActivity
import net.kdt.pojavlaunch.Tools

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private var isStarted: Boolean = false
    private var isNavigating: Boolean = false
    private lateinit var binding: ActivitySplashBinding
    private lateinit var installableAdapter: InstallableAdapter
    private val items: MutableList<InstallableItem> = ArrayList()
    private var installStartedAt = 0L
    private var installStartProgress = 0
    private var latestProgress = 0
    private var latestCompletedTasks = 0
    private var latestTotalTasks = 0
    private var estimatedFinishAt: Long? = null
    private var smoothedTotalDuration: Double? = null
    private var progressAnimator: ObjectAnimator? = null
    private var downloadAnimator: ObjectAnimator? = null

    private val timeUpdater = object : Runnable {
        override fun run() {
            updateTimeLabel()
            if (isStarted && latestProgress < 100 && !isFinishing) {
                binding.root.postDelayed(this, 1000L)
            }
        }
    }

    private val mainNavigation = Runnable { openMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initItems()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.titleText.text = InfoDistributor.APP_NAME
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SplashActivity)
            adapter = installableAdapter
        }

        binding.startButton.apply {
            setOnClickListener {
                if (isStarted) return@setOnClickListener
                isStarted = true
                installStartedAt = SystemClock.elapsedRealtime()
                installStartProgress = latestProgress
                smoothedTotalDuration = null
                estimatedFinishAt = null
                binding.splashText.setText(R.string.splash_screen_installing)
                showInstallProgress()
                installableAdapter.startAllTasks()
            }
            isClickable = false
        }

        if (!Tools.checkStorageRoot()) {
            startActivity(Intent(this, MissingStorageActivity::class.java))
            finish()
            return
        }

        //如果安卓版本小于等于9，则检查存储权限（不是管理所有文件权限），拥有存储权限会保证文件、文件夹正常创建
        //但是并不强制要求用户必须授予权限，如果用户拒绝，那么之后产生的问题将由用户承担
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !StoragePermissionsUtils.hasStoragePermissions(this)) {
            TipDialog.Builder(this)
                .setTitle(R.string.generic_warning)
                .setMessage(InfoCenter.replaceName(this, R.string.permissions_write_external_storage))
                .setWarning()
                .setConfirmClickListener { requestStoragePermissions() }
                .setCancelClickListener { checkEnd() } //用户取消，那就跟随用户的意愿
                .showDialog()
        } else {
            checkEnd()
        }
    }

    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            //无论用户是否授予了权限，都会完成检查，因为启动器并不强制要求权限
            //但是一旦因为存储权限出现了问题，那么将由用户自行承担后果
            checkEnd()
        }
    }

    private fun initItems() {
        Components.entries.forEach {
            val unpackComponentsTask = UnpackComponentsTask(this, it)
            if (!unpackComponentsTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.displayName,
                        it.summary?.let { it1 -> getString(it1) },
                        unpackComponentsTask
                    )
                )
            }
        }
        Jre.entries.forEach {
            val unpackJreTask = UnpackJreTask(this, it)
            if (!unpackJreTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.jreName,
                        getString(it.summary),
                        unpackJreTask
                    )
                )
            }
        }
        items.sort()
        installableAdapter = InstallableAdapter(
            items,
            InstallableAdapter.TaskCompletionListener { toMain() },
            InstallableAdapter.OverallProgressListener { progress, completed, total ->
                updateInstallProgress(progress, completed, total)
            }
        )
    }
    
    private fun checkEnd() {
        installableAdapter.checkAllTask()
        Task.runTask {
            UnpackSingleFilesTask(this).run()
        }.execute()

        binding.startButton.isClickable = true
    }

    private fun toMain() {
        if (isNavigating) return
        isNavigating = true

        if (isStarted) {
            latestProgress = 100
            renderInstallProgress()
            downloadAnimator?.cancel()
            binding.root.postDelayed(mainNavigation, 420L)
        } else {
            openMain()
        }
    }

    private fun openMain() {
        binding.root.removeCallbacks(timeUpdater)
        binding.root.removeCallbacks(mainNavigation)
        progressAnimator?.cancel()
        downloadAnimator?.cancel()
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }

    private fun showInstallProgress() {
        binding.startButton.animate()
            .alpha(0f)
            .setDuration(160L)
            .withEndAction {
                binding.startButton.visibility = View.GONE
                binding.startButton.alpha = 1f
            }
            .start()

        binding.installProgressContainer.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(240L).start()
        }

        val travel = resources.displayMetrics.density * 4f
        downloadAnimator?.cancel()
        downloadAnimator = ObjectAnimator.ofFloat(
            binding.installAnimationIcon,
            View.TRANSLATION_Y,
            -travel,
            travel
        ).apply {
            duration = 650L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        renderInstallProgress()
        binding.root.removeCallbacks(timeUpdater)
        binding.root.post(timeUpdater)
    }

    private fun updateInstallProgress(progress: Int, completedTasks: Int, totalTasks: Int) {
        latestProgress = progress.coerceIn(0, 100)
        latestCompletedTasks = completedTasks
        latestTotalTasks = totalTasks

        if (!isStarted) return

        val elapsed = SystemClock.elapsedRealtime() - installStartedAt
        val completedWork = latestProgress - installStartProgress
        val totalWork = 100 - installStartProgress
        if (elapsed > 0L && completedWork > 0 && totalWork > 0 && latestProgress < 100) {
            val rawTotalDuration = elapsed.toDouble() * totalWork.toDouble() / completedWork.toDouble()
            smoothedTotalDuration = smoothedTotalDuration?.let { previous ->
                (previous * 0.72) + (rawTotalDuration * 0.28)
            } ?: rawTotalDuration
            estimatedFinishAt = installStartedAt + smoothedTotalDuration!!.toLong()
        }

        renderInstallProgress()
    }

    private fun renderInstallProgress() {
        binding.installPercent.text = getString(R.string.percent_format, latestProgress)
        binding.installStatus.text = getString(
            R.string.splash_screen_progress_items,
            latestCompletedTasks,
            latestTotalTasks
        )

        progressAnimator?.cancel()
        progressAnimator = ObjectAnimator.ofInt(
            binding.installProgress,
            "progress",
            binding.installProgress.progress,
            latestProgress
        ).apply {
            duration = 320L
            start()
        }

        updateTimeLabel()
    }

    private fun updateTimeLabel() {
        binding.installTime.text = when {
            latestProgress >= 100 -> getString(R.string.splash_screen_finishing)
            estimatedFinishAt == null -> getString(R.string.splash_screen_time_estimating)
            else -> {
                val secondsRemaining = ((estimatedFinishAt!! - SystemClock.elapsedRealtime()) / 1000L)
                    .coerceAtLeast(1L)
                getString(
                    R.string.splash_screen_time_remaining,
                    DateUtils.formatElapsedTime(secondsRemaining)
                )
            }
        }
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.root.removeCallbacks(timeUpdater)
            binding.root.removeCallbacks(mainNavigation)
        }
        progressAnimator?.cancel()
        downloadAnimator?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE: Int = 100
    }
}
