package com.movtery.angkorlauncher.ui.subassembly.customcontrols;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.movtery.angkorlauncher.R;
import com.movtery.angkorlauncher.task.Task;
import com.movtery.angkorlauncher.task.TaskExecutors;
import com.movtery.angkorlauncher.ui.dialog.DeleteDialog;
import com.movtery.angkorlauncher.ui.subassembly.filelist.RefreshListener;
import com.movtery.angkorlauncher.utils.file.FileTools;
import com.movtery.angkorlauncher.utils.path.PathManager;
import com.movtery.angkorlauncher.utils.stringutils.StringFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ControlsListViewCreator {
    private final Context context;
    private final RecyclerView mainListView;
    private final AtomicInteger refreshGeneration = new AtomicInteger(0);

    private ControlListAdapter controlListAdapter;
    private ControlSelectedListener selectedListener;
    private RefreshListener refreshListener;
    private File fullPath = new File(PathManager.DIR_CTRLMAP_PATH);
    private String filterString = "";
    private boolean showSearchResultsOnly = false;
    private boolean caseSensitive = false;
    private TextView searchCountText;

    public ControlsListViewCreator(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.mainListView = recyclerView;
        init();
    }

    public void init() {
        controlListAdapter = new ControlListAdapter();
        controlListAdapter.setOnItemClickListener(new ControlListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String name) {
                File file = new File(fullPath, name);
                if (selectedListener != null) selectedListener.onItemSelected(file);
            }

            @Override
            public void onLongClick(String name) {
                File file = new File(fullPath, name);
                if (selectedListener != null) selectedListener.onItemLongClick(file);
            }

            @Override
            public void onInvalidItemClick(String name) {
                File file = new File(fullPath, name);
                List<File> files = new ArrayList<>();
                files.add(file);
                new DeleteDialog(
                        context,
                        Task.runTask(TaskExecutors.getAndroidUI(), () -> {
                            refresh();
                            return null;
                        }),
                        files
                ).show();
            }
        });

        mainListView.setLayoutManager(new LinearLayoutManager(context));
        mainListView.setLayoutAnimation(new LayoutAnimationController(
                AnimationUtils.loadAnimation(context, R.anim.fade_downwards)
        ));
        mainListView.setAdapter(controlListAdapter);
    }

    public void setSelectedListener(ControlSelectedListener listener) {
        selectedListener = listener;
    }

    public void setRefreshListener(RefreshListener listener) {
        refreshListener = listener;
    }

    public void setShowSearchResultsOnly(boolean showSearchResultsOnly) {
        this.showSearchResultsOnly = showSearchResultsOnly;
    }

    public int getItemCount() {
        return controlListAdapter.getItemCount();
    }

    public void setSelectedFile(@Nullable File file) {
        controlListAdapter.setSelectedFileName(file == null ? null : file.getName());
    }

    @Nullable
    public File getFirstValidFile() {
        String fileName = controlListAdapter.getFirstValidFileName();
        return fileName == null ? null : new File(fullPath, fileName);
    }

    public boolean containsValidFile(@Nullable File file) {
        return file != null && controlListAdapter.containsValidFileName(file.getName());
    }

    private LoadResult loadInfoData(
            File path,
            String currentFilter,
            boolean currentShowOnly,
            boolean currentCaseSensitive
    ) {
        List<ControlItemBean> data = new ArrayList<>();
        int matchCount = 0;

        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isFile()) continue;

                ControlInfoData controlInfoData = null;
                if (file.getName().endsWith(".json")) {
                    controlInfoData = EditControlData.loadFormFile(context, file);
                }

                ControlItemBean item;
                if (controlInfoData == null) {
                    ControlInfoData invalidInfoData = new ControlInfoData();
                    invalidInfoData.fileName = file.getName();
                    item = new ControlItemBean(invalidInfoData);
                    item.isInvalid = true;
                    if (!currentFilter.isEmpty()) {
                        if (!StringFilter.containsSubstring(file.getName(), currentFilter, currentCaseSensitive)) {
                            if (currentShowOnly) continue;
                        } else {
                            item.isHighlighted = true;
                            matchCount++;
                        }
                    }
                } else {
                    item = new ControlItemBean(controlInfoData);
                    if (shouldHighlight(controlInfoData, file, currentFilter, currentCaseSensitive)) {
                        item.isHighlighted = true;
                        matchCount++;
                    } else if (currentShowOnly && !currentFilter.isEmpty()) {
                        continue;
                    }
                }
                data.add(item);
            }
        }
        return new LoadResult(data, matchCount);
    }

    private boolean shouldHighlight(
            ControlInfoData controlInfoData,
            File file,
            String currentFilter,
            boolean currentCaseSensitive
    ) {
        if (currentFilter.isEmpty()) return false;

        String name = controlInfoData.name;
        String searchString = name != null && !name.isEmpty() && !name.equals("null")
                ? name
                : file.getName();

        return StringFilter.containsSubstring(searchString, currentFilter, currentCaseSensitive) ||
                StringFilter.containsSubstring(file.getName(), currentFilter, currentCaseSensitive);
    }

    public void listAtPath() {
        fullPath = controlPath();
        refresh();
    }

    public File getFullPath() {
        return fullPath;
    }

    public void searchControls(TextView searchCountText, String filterString, boolean caseSensitive) {
        this.filterString = filterString == null ? "" : filterString.trim();
        this.caseSensitive = caseSensitive;
        this.searchCountText = searchCountText;
        refresh();
    }

    private File controlPath() {
        File ctrlPath = new File(PathManager.DIR_CTRLMAP_PATH);
        if (!ctrlPath.exists()) FileTools.mkdirs(ctrlPath);
        return ctrlPath;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh() {
        final int generation = refreshGeneration.incrementAndGet();
        final String currentFilter = filterString;
        final boolean currentShowOnly = showSearchResultsOnly;
        final boolean currentCaseSensitive = caseSensitive;

        Task.runTask(() -> loadInfoData(
                fullPath,
                currentFilter,
                currentShowOnly,
                currentCaseSensitive
        )).ended(TaskExecutors.getAndroidUI(), result -> {
            if (generation != refreshGeneration.get()) return;

            controlListAdapter.updateItems(result.items);
            mainListView.scheduleLayoutAnimation();

            if (searchCountText != null) {
                searchCountText.setText(searchCountText.getContext().getString(
                        R.string.search_count,
                        result.matchCount
                ));
                searchCountText.setVisibility(currentFilter.isEmpty() ? View.GONE : View.VISIBLE);
            }

            if (refreshListener != null) refreshListener.onRefresh();
        }).execute();
    }

    private static final class LoadResult {
        final List<ControlItemBean> items;
        final int matchCount;

        LoadResult(List<ControlItemBean> items, int matchCount) {
            this.items = items;
            this.matchCount = matchCount;
        }
    }
}
