package com.movtery.angkorlauncher.ui.subassembly.customcontrols;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.movtery.angkorlauncher.R;
import com.movtery.angkorlauncher.databinding.ItemControlListViewBinding;
import com.movtery.angkorlauncher.databinding.ItemFileListViewBinding;
import com.movtery.angkorlauncher.task.Task;
import com.movtery.angkorlauncher.task.TaskExecutors;
import com.movtery.angkorlauncher.ui.dialog.ControlInfoDialog;
import com.movtery.angkorlauncher.utils.stringutils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ControlListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_VALID = 0;
    private static final int VIEW_TYPE_INVALID = 1;

    private final List<ControlItemBean> mData = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;
    private String mSelectedFileName;

    public ControlListAdapter() {
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_VALID) {
            return new ValidViewHolder(ItemControlListViewBinding.inflate(inflater, parent, false));
        }
        return new InvalidViewHolder(ItemFileListViewBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ControlItemBean item = mData.get(position);
        if (getItemViewType(position) == VIEW_TYPE_VALID) {
            String fileName = item.controlInfoData.fileName;
            boolean selected = fileName != null && fileName.equals(mSelectedFileName);
            ((ValidViewHolder) holder).setData(item, selected);
            holder.itemView.setOnClickListener(view -> {
                if (mOnItemClickListener != null) mOnItemClickListener.onItemClick(fileName);
            });
            holder.itemView.setOnLongClickListener(view -> {
                if (mOnItemClickListener == null) return false;
                mOnItemClickListener.onLongClick(fileName);
                return true;
            });
        } else {
            ((InvalidViewHolder) holder).setData(item);
            holder.itemView.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onInvalidItemClick(item.controlInfoData.fileName);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).isInvalid ? VIEW_TYPE_INVALID : VIEW_TYPE_VALID;
    }

    @Override
    public long getItemId(int position) {
        ControlItemBean item = mData.get(position);
        String fileName = item.controlInfoData.fileName;
        String key = (item.isInvalid ? "invalid:" : "valid:") + (fileName == null ? "" : fileName);
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < key.length(); i++) {
            hash ^= key.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    public void updateItems(List<ControlItemBean> items) {
        List<ControlItemBean> oldItems = new ArrayList<>(mData);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldItems.size();
            }

            @Override
            public int getNewListSize() {
                return items.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                ControlItemBean oldItem = oldItems.get(oldItemPosition);
                ControlItemBean newItem = items.get(newItemPosition);
                return oldItem.isInvalid == newItem.isInvalid && Objects.equals(
                        oldItem.controlInfoData.fileName,
                        newItem.controlInfoData.fileName
                );
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ControlItemBean oldItem = oldItems.get(oldItemPosition);
                ControlItemBean newItem = items.get(newItemPosition);
                ControlInfoData oldInfo = oldItem.controlInfoData;
                ControlInfoData newInfo = newItem.controlInfoData;
                return oldItem.isHighlighted == newItem.isHighlighted &&
                        oldItem.isInvalid == newItem.isInvalid &&
                        Objects.equals(oldInfo.fileName, newInfo.fileName) &&
                        Objects.equals(oldInfo.name, newInfo.name) &&
                        Objects.equals(oldInfo.version, newInfo.version) &&
                        Objects.equals(oldInfo.author, newInfo.author) &&
                        Objects.equals(oldInfo.desc, newInfo.desc);
            }
        });
        mData.clear();
        mData.addAll(items);
        result.dispatchUpdatesTo(this);
    }

    public void setSelectedFileName(@Nullable String selectedFileName) {
        if (selectedFileName == null ? mSelectedFileName == null : selectedFileName.equals(mSelectedFileName)) {
            return;
        }
        int previousPosition = findPositionByFileName(mSelectedFileName);
        mSelectedFileName = selectedFileName;
        int selectedPosition = findPositionByFileName(mSelectedFileName);
        if (previousPosition != RecyclerView.NO_POSITION) notifyItemChanged(previousPosition);
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition != previousPosition) {
            notifyItemChanged(selectedPosition);
        }
    }

    private int findPositionByFileName(@Nullable String fileName) {
        if (fileName == null) return RecyclerView.NO_POSITION;
        for (int index = 0; index < mData.size(); index++) {
            if (fileName.equals(mData.get(index).controlInfoData.fileName)) return index;
        }
        return RecyclerView.NO_POSITION;
    }

    @Nullable
    public String getFirstValidFileName() {
        for (ControlItemBean item : mData) {
            if (!item.isInvalid && item.controlInfoData.fileName != null) {
                return item.controlInfoData.fileName;
            }
        }
        return null;
    }

    public boolean containsValidFileName(@Nullable String fileName) {
        if (fileName == null) return false;
        for (ControlItemBean item : mData) {
            if (!item.isInvalid && fileName.equals(item.controlInfoData.fileName)) return true;
        }
        return false;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(String name);

        void onLongClick(String name);

        void onInvalidItemClick(String name);
    }

    public static class InvalidViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final ItemFileListViewBinding binding;

        public InvalidViewHolder(@NonNull ItemFileListViewBinding binding) {
            super(binding.getRoot());
            context = binding.getRoot().getContext();
            this.binding = binding;
            binding.check.setVisibility(View.GONE);
            binding.image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_disabled));
        }

        public void setData(ControlItemBean item) {
            String text = StringUtils.insertSpace(
                    context.getString(R.string.controls_info_invalid),
                    item.controlInfoData.fileName
            );
            binding.name.setText(text);
            binding.name.setTextColor(Color.rgb(255, 90, 90));
            binding.name.setTypeface(null, Typeface.BOLD);
        }
    }

    public class ValidViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final ItemControlListViewBinding binding;

        public ValidViewHolder(@NonNull ItemControlListViewBinding binding) {
            super(binding.getRoot());
            context = binding.getRoot().getContext();
            this.binding = binding;
        }

        /** Keeps the original public binding API available for existing callers. */
        public void setData(ControlItemBean item) {
            String fileName = item.controlInfoData.fileName;
            setData(item, fileName != null && fileName.equals(mSelectedFileName));
        }

        public void setData(ControlItemBean item, boolean selected) {
            ControlInfoData info = item.controlInfoData;
            binding.getRoot().setActivated(selected);
            binding.infoButton.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            binding.infoButton.setOnClickListener(view -> {
                ControlInfoDialog dialog = new ControlInfoDialog(
                        context,
                        info,
                        Task.runTask(TaskExecutors.getAndroidUI(), () -> {
                            int position = getBindingAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) notifyItemChanged(position);
                            return null;
                        })
                );
                dialog.show();
            });

            // The redesigned list intentionally shows the exact JSON filename.
            binding.title.setText(info.fileName);
            binding.title.setTextColor(ContextCompat.getColor(
                    context,
                    item.isHighlighted ? R.color.launcher_accent_gold : R.color.launcher_text_primary
            ));

            String description = info.desc;
            if ("control.default.desc.text".equals(description)) {
                description = context.getString(R.string.controls_info_default_desc);
            }
            if (description == null || description.isEmpty() || "null".equals(description)) {
                description = context.getString(R.string.controls_info_no_info);
            }
            binding.desc.setText(description);
            binding.getRoot().setContentDescription(
                    selected
                            ? context.getString(R.string.controls_selected_profile) + ": " + info.fileName
                            : info.fileName
            );
        }
    }
}
