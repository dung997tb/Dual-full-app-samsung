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
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
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
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        clones = new CloneManager(requireContext());

        MainActivity host = (MainActivity) requireActivity();
        btnGuide.setOnClickListener(x -> host.showShizukuGuide());
        btnGrant.setOnClickListener(x -> host.requestShizukuPermission());
        fab.setOnClickListener(x -> host.goToAddTab());
        btnRefresh.setOnClickListener(x -> load());
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
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
                    emptyMsg = getString(R.string.toast_need_shizuku);
                } else {
                    int userId = clones.findCloneUser();
                    if (userId == -1) {
                        list = Collections.emptyList();
                        emptyMsg = getString(R.string.empty_clones);
                    } else {
                        list = clones.getClones(userId);
                        emptyMsg = getString(R.string.empty_clones);
                    }
                }
            } catch (Exception e) {
                list = Collections.emptyList();
                emptyMsg = getString(R.string.toast_error, e.getMessage());
            }
            if (!isAdded()) return;
            List<AppEntry> finalList = list;
            String finalMsg = emptyMsg;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
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
                int userId = clones.requireCloneUser();
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
                int userId = clones.requireCloneUser();
                clones.launchParallel(app.packageName, userId, requireContext());
                uiToast(getString(R.string.toast_parallel, app.label));
            } catch (Exception e) {
                uiToast(getString(R.string.toast_error, e.getMessage()));
            }
        }).start();
    }

    private void onMenuClicked(AppEntry app, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, getString(R.string.menu_pin));
        menu.getMenu().add(0, 2, 1, getString(R.string.menu_delete));
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                onPinClicked(app);
            } else {
                confirmDelete(app);
            }
            return true;
        });
        menu.show();
    }

    private void onPinClicked(AppEntry app) {
        boolean ok = ShortcutHelper.pinCloneShortcut(requireContext(), app.packageName, app.label);
        Toast.makeText(getContext(),
                ok ? getString(R.string.toast_pinned, app.label) : getString(R.string.toast_pin_failed),
                Toast.LENGTH_LONG).show();
    }

    private void confirmDelete(AppEntry app) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_msg, app.label))
                .setPositiveButton(R.string.dialog_ok, (d, w) -> doDelete(app))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void doDelete(AppEntry app) {
        new Thread(() -> {
            try {
                int userId = clones.requireCloneUser();
                clones.removeClone(userId, app.packageName);
                uiToast(getString(R.string.toast_deleted, app.label));
                if (isAdded()) requireActivity().runOnUiThread(this::load);
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
