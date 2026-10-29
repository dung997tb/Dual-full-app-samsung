package com.example.appclone.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appclone.R;
import com.example.appclone.clone.CloneManager;
import com.example.appclone.model.AppEntry;
import com.example.appclone.shizuku.ShizukuHelper;

import java.util.Collections;
import java.util.List;

/** Tab "Bản sao": danh sách app đã nhân bản. */
public class ClonesFragment extends Fragment {

    private View warnCard;
    private Button btnGuide;
    private Button btnGrant;
    private TextView tvSectionTitle;
    private TextView tvSectionSub;
    private RecyclerView rv;
    private ProgressBar progress;
    private TextView tvEmpty;
    private View fab;
    private View btnRefresh;
    private CloneManager clones;
    private int loadGeneration;
    private boolean refreshScheduled;
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refreshTask = () -> {
        refreshScheduled = false;
        if (isAdded() && getView() != null && !isHidden()) load();
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_clones, container, false);
        warnCard = v.findViewById(R.id.warnCard);
        btnGuide = v.findViewById(R.id.btnGuide);
        btnGrant = v.findViewById(R.id.btnGrant);
        tvSectionTitle = v.findViewById(R.id.tvSectionTitle);
        tvSectionSub = v.findViewById(R.id.tvSectionSub);
        rv = v.findViewById(R.id.rv);
        progress = v.findViewById(R.id.progress);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        fab = v.findViewById(R.id.fab);
        btnRefresh = v.findViewById(R.id.btnRefresh);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 4));
        clones = new CloneManager(requireContext());

        MainActivity host = (MainActivity) requireActivity();
        btnGuide.setOnClickListener(x -> host.showShizukuGuide());
        btnGrant.setOnClickListener(x -> host.requestShizukuPermission());
        fab.setOnClickListener(x -> host.goToAddTab());
        btnRefresh.setOnClickListener(x -> refresh());

        // Nhận lựa chọn từ bottom sheet tuỳ chọn (chạm giữ 1 bản sao)
        getChildFragmentManager().setFragmentResultListener(
                CloneOptionsDialog.REQUEST_KEY, this, (requestKey, result) -> {
                    AppEntry app = new AppEntry(
                            result.getString(CloneOptionsDialog.KEY_PKG),
                            result.getString(CloneOptionsDialog.KEY_LABEL),
                            null,
                            result.getInt(CloneOptionsDialog.KEY_USER_ID),
                            result.getString(CloneOptionsDialog.KEY_PROFILE_LABEL));
                    switch (result.getInt(CloneOptionsDialog.KEY_ACTION)) {
                        case CloneOptionsDialog.ACTION_OPEN:
                            onOpenClicked(app);
                            break;
                        case CloneOptionsDialog.ACTION_PARALLEL:
                            onParallelClicked(app);
                            break;
                        case CloneOptionsDialog.ACTION_PIN:
                            onPinClicked(app);
                            break;
                        default:
                            confirmDelete(app);
                            break;
                    }
                });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refresh();
        else ++loadGeneration;
    }

    public void refresh() {
        if (!isAdded() || getView() == null || isHidden()) return;
        ++loadGeneration; // Invalidate old scans immediately, before scheduling another one.
        if (refreshScheduled) return;
        refreshScheduled = true;
        main.post(refreshTask);
    }

    @Override
    public void onDestroyView() {
        ++loadGeneration;
        main.removeCallbacks(refreshTask);
        refreshScheduled = false;
        super.onDestroyView();
    }

    private void load() {
        final int generation = ++loadGeneration;
        final android.content.Context context = requireContext().getApplicationContext();
        final CloneManager manager = clones;
        boolean ready = ShizukuHelper.hasPermission();
        warnCard.setVisibility(ready ? View.GONE : View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        new Thread(() -> {
            List<AppEntry> list;
            String emptyMsg;
            try {
                if (!ready) {
                    list = Collections.emptyList();
                    emptyMsg = context.getString(R.string.toast_need_shizuku);
                } else {
                    list = manager.getAllClones();
                    emptyMsg = manager.getScanWarning().isEmpty()
                            ? context.getString(R.string.empty_clones) : manager.getScanWarning();
                }
            } catch (Exception e) {
                list = Collections.emptyList();
                emptyMsg = context.getString(R.string.toast_error, e.getMessage());
            }
            List<AppEntry> finalList = list;
            String finalMsg = emptyMsg;
            main.post(() -> {
                if (!isAdded() || getView() == null || isHidden() || generation != loadGeneration) return;
                progress.setVisibility(View.GONE);
                tvSectionSub.setVisibility(View.VISIBLE);
                tvSectionSub.setText(manager.getScanWarning().isEmpty()
                        ? getString(R.string.clones_hint) : manager.getScanWarning());
                tvSectionTitle.setText(getString(R.string.cloned_title, finalList.size()));
                if (finalList.isEmpty()) {
                    tvEmpty.setText(finalMsg);
                    tvEmpty.setVisibility(View.VISIBLE);
                }
                rv.setAdapter(new CloneAdapter(finalList,
                        this::onOpenClicked, this::onParallelClicked, this::onMenuClicked));
            });
        }).start();
    }

    private void onOpenClicked(AppEntry app) {
        if (!ShizukuHelper.hasPermission()) {
            toast(R.string.toast_need_shizuku);
            return;
        }
        new Thread(() -> {
            try {
                int userId = app.userId;
                clones.launchClone(userId, app.packageName);
                uiToast(getString(R.string.toast_opened, app.label));
            } catch (Exception e) {
                uiToast(getString(R.string.toast_error, e.getMessage()));
            }
        }).start();
    }

    private void onParallelClicked(AppEntry app) {
        if (!ShizukuHelper.hasPermission()) {
            toast(R.string.toast_need_shizuku);
            return;
        }
        new Thread(() -> {
            try {
                int userId = app.userId;
                clones.launchParallel(app.packageName, userId, requireContext());
                uiToast(getString(R.string.toast_parallel, app.label));
            } catch (Exception e) {
                uiToast(getString(R.string.toast_error, e.getMessage()));
            }
        }).start();
    }

    private void onMenuClicked(AppEntry app, View anchor) {
        CloneOptionsDialog.newInstance(app).show(getChildFragmentManager(), "clone_options");
    }

    private void onPinClicked(AppEntry app) {
        boolean ok = ShortcutHelper.pinCloneShortcut(requireContext(), app.packageName, app.label, app.userId);
        Toast.makeText(getContext(),
                ok ? getString(R.string.toast_pinned, app.label) : getString(R.string.toast_pin_failed),
                Toast.LENGTH_LONG).show();
    }

    private void confirmDelete(AppEntry app) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_msg, app.label)
                        + "\n" + app.profileLabel + " · user " + app.userId)
                .setPositiveButton(R.string.dialog_ok, (d, w) -> doDelete(app))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void doDelete(AppEntry app) {
        new Thread(() -> {
            try {
                int userId = app.userId;
                clones.removeClone(userId, app.packageName);
                uiToast(getString(R.string.toast_deleted, app.label));
                main.post(this::refresh);
            } catch (Exception e) {
                uiToast(getString(R.string.toast_error, e.getMessage()));
            }
        }).start();
    }

    private void toast(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }

    private void uiToast(String msg) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show());
    }
}
