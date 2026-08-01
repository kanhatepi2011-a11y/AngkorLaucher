package com.movtery.angkorlauncher.ui.dialog

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.movtery.angkorlauncher.R
import com.movtery.angkorlauncher.feature.version.favorites.FavoritesVersionUtils
import com.movtery.angkorlauncher.task.Task
import com.movtery.angkorlauncher.task.TaskExecutors
import com.movtery.angkorlauncher.ui.subassembly.version.FavoritesVersionAdapter

class FavoritesVersionDialog(
    context: Context,
    private val versionName: String,
    private val favoritesChanged: () -> Unit
) : AbstractSelectDialog(context) {
    private val mFavoritesAdapter = FavoritesVersionAdapter(versionName)

    override fun initDialog(recyclerView: RecyclerView) {
        setTitleText(R.string.version_manager_favorites_dialog_title)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = mFavoritesAdapter
    }

    override fun dismiss() {
        super.dismiss()
        Task.runTask {
            FavoritesVersionUtils.updateVersionFolders(versionName, mFavoritesAdapter.getSelectedCategorySet())
        }.ended(TaskExecutors.getAndroidUI()) {
            favoritesChanged()
        }.execute()
    }
}
