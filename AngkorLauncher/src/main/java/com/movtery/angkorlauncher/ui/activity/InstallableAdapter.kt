package com.movtery.angkorlauncher.ui.activity

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.movtery.angkorlauncher.databinding.ItemInstallableBinding
import com.movtery.angkorlauncher.feature.unpack.OnTaskRunningListener

class InstallableAdapter(
    private val items: List<InstallableItem>,
    private val listener: TaskCompletionListener,
    private val progressListener: OverallProgressListener
) : RecyclerView.Adapter<InstallableAdapter.ViewHolder>() {
    @Volatile
    private var completedTasksCount = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInstallableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun checkAllTask() {
        items.forEachIndexed { index, item ->
            if (!item.task.isNeedUnpack()) {
                item.isFinished = true
                item.progress = 100
                updateTaskCount(index)
            }
        }
    }

    fun startAllTasks() {
        items.forEachIndexed { index, item ->
            if (!item.isFinished) {
                Thread {
                    item.task.apply {
                        setTaskRunningListener(object : OnTaskRunningListener {
                            override fun onTaskStart() {
                                item.isRunning = true
                                updateUI { notifyItemChanged(index) }
                                dispatchOverallProgress()
                            }

                            override fun onTaskProgress(progress: Int) {
                                updateTaskProgress(index, progress)
                            }

                            override fun onTaskEnd() {
                                item.isRunning = false
                                item.isFinished = true
                                item.progress = 100
                                updateTaskCount(index)
                            }
                        })
                    }
                    item.task.run()
                }.start()
            }
        }
    }

    @Synchronized
    private fun updateTaskCount(index: Int) {
        completedTasksCount++
        updateUI {
            notifyItemChanged(index)
            dispatchOverallProgress()
        }

        if (completedTasksCount >= itemCount) {
            updateUI { listener.onAllTasksCompleted() }
        }
    }

    @Synchronized
    private fun updateTaskProgress(index: Int, progress: Int) {
        items[index].progress = maxOf(items[index].progress, progress.coerceIn(0, 100))
        dispatchOverallProgress()
    }

    @Synchronized
    fun dispatchOverallProgress() {
        val total = itemCount
        val overallProgress = if (total == 0) {
            100
        } else {
            items.sumOf { it.progress } / total
        }
        val completed = items.count { it.isFinished }
        updateUI {
            progressListener.onProgress(overallProgress.coerceIn(0, 100), completed, total)
        }
    }

    private fun updateUI(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post { action() }
        }
    }

    class ViewHolder(
        private val binding: ItemInstallableBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun setData(item: InstallableItem) {
            binding.name.text = item.name

            if (item.summary.isNullOrEmpty()) {
                binding.summary.visibility = View.GONE
            } else {
                binding.summary.text = item.summary
                binding.summary.visibility = View.VISIBLE
            }

            binding.progress.visibility = if (item.isRunning) View.VISIBLE else View.GONE
            binding.finish.visibility = if (item.isFinished) View.VISIBLE else View.GONE
        }
    }

    fun interface TaskCompletionListener {
        fun onAllTasksCompleted()
    }

    fun interface OverallProgressListener {
        fun onProgress(progress: Int, completedTasks: Int, totalTasks: Int)
    }
}
