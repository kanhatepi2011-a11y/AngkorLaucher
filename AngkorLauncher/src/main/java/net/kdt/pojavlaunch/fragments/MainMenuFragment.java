package net.kdt.pojavlaunch.fragments;

import static com.movtery.angkorlauncher.event.single.RefreshVersionsEvent.MODE.END;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.movtery.anim.AnimPlayer;
import com.movtery.anim.animations.Animations;
import com.movtery.angkorlauncher.InfoCenter;
import com.movtery.angkorlauncher.R;
import com.movtery.angkorlauncher.databinding.FragmentLauncherBinding;
import com.movtery.angkorlauncher.event.single.AccountUpdateEvent;
import com.movtery.angkorlauncher.event.single.LaunchGameEvent;
import com.movtery.angkorlauncher.event.single.RefreshVersionsEvent;
import com.movtery.angkorlauncher.event.single.SwapToLoginEvent;
import com.movtery.angkorlauncher.feature.version.Version;
import com.movtery.angkorlauncher.feature.version.utils.VersionIconUtils;
import com.movtery.angkorlauncher.feature.version.VersionInfo;
import com.movtery.angkorlauncher.feature.version.VersionsManager;
import com.movtery.angkorlauncher.task.TaskExecutors;
import com.movtery.angkorlauncher.ui.fragment.AboutFragment;
import com.movtery.angkorlauncher.ui.fragment.ControlButtonFragment;
import com.movtery.angkorlauncher.ui.fragment.FilesFragment;
import com.movtery.angkorlauncher.ui.fragment.FragmentWithAnim;
import com.movtery.angkorlauncher.ui.fragment.VersionManagerFragment;
import com.movtery.angkorlauncher.ui.fragment.VersionsListFragment;
import com.movtery.angkorlauncher.ui.subassembly.account.AccountViewWrapper;
import com.movtery.angkorlauncher.utils.path.PathManager;
import com.movtery.angkorlauncher.utils.ZHTools;
import com.movtery.angkorlauncher.utils.anim.ViewAnimUtils;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainMenuFragment extends FragmentWithAnim {
    public static final String TAG = "MainMenuFragment";
    private FragmentLauncherBinding binding;
    private AccountViewWrapper accountViewWrapper;
    private boolean launchDebounce;

    public MainMenuFragment() {
        super(R.layout.fragment_launcher);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLauncherBinding.inflate(getLayoutInflater());
        accountViewWrapper = new AccountViewWrapper(this, binding.viewAccount);
        accountViewWrapper.refreshAccountInfo();
        binding.viewAccount.userName.setTextColor(ContextCompat.getColor(requireContext(), R.color.download_text_primary));
        binding.viewAccount.accountType.setTextColor(ContextCompat.getColor(requireContext(), R.color.download_text_secondary));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.aboutText.setText(InfoCenter.replaceName(requireActivity(), R.string.about_tab));
        binding.aboutButton.setContentDescription(binding.aboutText.getText());
        binding.aboutButton.setOnClickListener(v -> ZHTools.swapFragmentWithAnim(this, AboutFragment.class, AboutFragment.TAG, null));
        binding.customControlButton.setOnClickListener(v -> ZHTools.swapFragmentWithAnim(this, ControlButtonFragment.class, ControlButtonFragment.TAG, null));
        binding.openMainDirButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(FilesFragment.BUNDLE_LIST_PATH, PathManager.DIR_GAME_HOME);
            ZHTools.swapFragmentWithAnim(this, FilesFragment.class, FilesFragment.TAG, bundle);
        });
        binding.installJarButton.setOnClickListener(v -> runInstallerWithConfirmation(false));
        binding.installJarButton.setOnLongClickListener(v -> {
            runInstallerWithConfirmation(true);
            return true;
        });
        binding.shareLogsButton.setOnClickListener(v -> ZHTools.shareLogs(requireActivity()));

        bindUtilityTooltip(binding.aboutButton, binding.aboutText);
        bindUtilityTooltip(binding.customControlButton, binding.customControlText);
        bindUtilityTooltip(binding.openMainDirButton, binding.openMainDirText);
        bindUtilityTooltip(binding.installJarButton, binding.installJarText);
        bindUtilityTooltip(binding.shareLogsButton, binding.shareLogsText);

        binding.addAccountButton.setOnClickListener(v -> EventBus.getDefault().post(new SwapToLoginEvent()));

        binding.version.setOnClickListener(v -> {
            if (!isTaskRunning()) {
                ZHTools.swapFragmentWithAnim(this, VersionsListFragment.class, VersionsListFragment.TAG, null);
            } else {
                ViewAnimUtils.setViewAnim(binding.version, Animations.Shake);
                TaskExecutors.runInUIThread(() -> Toast.makeText(requireContext(), R.string.version_manager_task_in_progress, Toast.LENGTH_SHORT).show());
            }
        });
        binding.managerProfileButton.setOnClickListener(v -> {
            if (!isTaskRunning()) {
                ViewAnimUtils.setViewAnim(binding.managerProfileButton, Animations.Pulse);
                ZHTools.swapFragmentWithAnim(this, VersionManagerFragment.class, VersionManagerFragment.TAG, null);
            } else {
                ViewAnimUtils.setViewAnim(binding.managerProfileButton, Animations.Shake);
                TaskExecutors.runInUIThread(() -> Toast.makeText(requireContext(), R.string.version_manager_task_in_progress, Toast.LENGTH_SHORT).show());
            }
        });

        binding.playButton.setOnClickListener(v -> {
            if (!binding.playButton.isEnabled() || launchDebounce) return;

            launchDebounce = true;
            setPlayState(R.string.launcher_play_launching, true);
            EventBus.getDefault().post(new LaunchGameEvent());

            // If validation stops the launch before a task is created, safely restore the button.
            binding.playButton.postDelayed(() -> {
                if (binding != null && ProgressKeeper.getTaskCount() == 0) {
                    launchDebounce = false;
                    updateLauncherState(0);
                }
            }, 1200L);
        });

        binding.versionName.setSelected(true);
        binding.versionInfo.setSelected(true);

        refreshCurrentVersion();
        updateLauncherState(ProgressKeeper.getTaskCount());
    }

    private void refreshCurrentVersion() {
        Version version = VersionsManager.INSTANCE.getCurrentVersion();

        int versionInfoVisibility;
        if (version != null) {
            binding.versionName.setText(version.getVersionName());
            VersionInfo versionInfo = version.getVersionInfo();
            if (versionInfo != null) {
                binding.versionInfo.setText(versionInfo.getInfoString());
                versionInfoVisibility = View.VISIBLE;
            } else versionInfoVisibility = View.GONE;

            new VersionIconUtils(version).start(binding.versionIcon);
            binding.managerProfileButton.setVisibility(View.VISIBLE);
        } else {
            binding.versionName.setText(R.string.version_no_versions);
            binding.managerProfileButton.setVisibility(View.GONE);
            versionInfoVisibility = View.GONE;
        }
        binding.versionInfo.setVisibility(versionInfoVisibility);
        updateLauncherState(ProgressKeeper.getTaskCount());
    }

    private void bindUtilityTooltip(@NonNull View button, @NonNull View tooltip) {
        button.setOnFocusChangeListener((view, hasFocus) -> setTooltipVisible(tooltip, hasFocus));
        button.setOnHoverListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER) {
                setTooltipVisible(tooltip, true);
            } else if (event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT && !view.hasFocus()) {
                setTooltipVisible(tooltip, false);
            }
            return false;
        });
        button.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                setTooltipVisible(tooltip, true);
            } else if ((event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL) && !view.hasFocus()) {
                setTooltipVisible(tooltip, false);
            }
            return false;
        });
    }

    private void setTooltipVisible(@NonNull View tooltip, boolean visible) {
        tooltip.animate().cancel();
        tooltip.animate()
                .alpha(visible ? 1f : 0f)
                .translationX(visible ? 0f : requireContext().getResources().getDimension(R.dimen._4sdp))
                .setDuration(180L)
                .start();
    }

    private void updateLauncherState(int taskCount) {
        if (binding == null) return;

        boolean hasTasks = taskCount > 0;
        binding.version.setEnabled(!hasTasks);
        binding.version.setAlpha(hasTasks ? 0.72f : 1f);
        binding.managerProfileButton.setEnabled(!hasTasks);
        binding.managerProfileButton.setAlpha(hasTasks ? 0.45f : 1f);
        binding.managerProfileButton.setVisibility(
                !hasTasks && VersionsManager.INSTANCE.getCurrentVersion() != null ? View.VISIBLE : View.GONE
        );
        binding.versionLoading.setVisibility(hasTasks ? View.VISIBLE : View.GONE);

        if (hasTasks) {
            launchDebounce = true;
            if (ProgressKeeper.containsProgress(ProgressLayout.DOWNLOAD_MINECRAFT) ||
                    ProgressKeeper.containsProgress(ProgressLayout.DOWNLOAD_VERSION_LIST)) {
                setPlayState(R.string.launcher_play_downloading, true);
            } else if (ProgressKeeper.containsProgress(ProgressLayout.INSTALL_RESOURCE) ||
                    ProgressKeeper.containsProgress(ProgressLayout.UNPACK_RUNTIME)) {
                setPlayState(R.string.launcher_play_installing, true);
            } else if (ProgressKeeper.containsProgress(ProgressLayout.CHECKING_MODS)) {
                setPlayState(R.string.launcher_play_launching, true);
            } else {
                setPlayState(R.string.launcher_play_loading, true);
            }
            binding.playButton.setEnabled(false);
            binding.playButton.setAlpha(0.78f);
            return;
        }

        launchDebounce = false;
        boolean hasVersion = VersionsManager.INSTANCE.getCurrentVersion() != null;
        binding.playButton.setEnabled(hasVersion);
        binding.playButton.setAlpha(hasVersion ? 1f : 0.55f);
        setPlayState(
                hasVersion ? R.string.launcher_play_ready : R.string.launcher_play_disabled,
                false
        );
    }

    private void setPlayState(int contentDescription, boolean showProgress) {
        if (binding == null) return;
        binding.playButton.setContentDescription(getString(contentDescription));
        binding.playButtonProgress.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        binding.playButtonIcon.setVisibility(showProgress ? View.GONE : View.VISIBLE);
    }

    @Subscribe()
    public void event(RefreshVersionsEvent event) {
        if (event.getMode() == END) {
            TaskExecutors.runInUIThread(this::refreshCurrentVersion);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(AccountUpdateEvent event) {
        if (accountViewWrapper != null) accountViewWrapper.refreshAccountInfo();
    }

    @Override
    public void onUpdateTaskCount(int taskCount) {
        super.onUpdateTaskCount(taskCount);
        TaskExecutors.runInUIThread(() -> updateLauncherState(taskCount));
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        accountViewWrapper = null;
        binding = null;
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    @Override
    public void slideIn(AnimPlayer animPlayer) {
        animPlayer.apply(new AnimPlayer.Entry(binding.launcherMenu, Animations.BounceInDown))
                .apply(new AnimPlayer.Entry(binding.playLayout, Animations.BounceInLeft))
                .apply(new AnimPlayer.Entry(binding.playButtonsLayout, Animations.BounceEnlarge));
    }

    @Override
    public void slideOut(AnimPlayer animPlayer) {
        animPlayer.apply(new AnimPlayer.Entry(binding.launcherMenu, Animations.FadeOutUp))
                .apply(new AnimPlayer.Entry(binding.playLayout, Animations.FadeOutRight))
                .apply(new AnimPlayer.Entry(binding.playButtonsLayout, Animations.BounceShrink));
    }
}
