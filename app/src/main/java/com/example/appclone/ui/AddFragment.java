package com.example.appclone.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appclone.R;
import com.example.appclone.clone.CloneManager;
import com.example.appclone.model.AppEntry;
import com.example.appclone.shizuku.ShizukuHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tab "Thêm mới": tìm kiếm + liệt kê app + nút Nhân bản. */
public class AddFragment extends Fragment {

    private EditText etSearch;
    private TextView btnClear;
    private TextView tvHeader;
    private RecyclerView rv;
    private ProgressBar progress;
    private TextView tvEmpty;
    private CloneManager clones;

    private final List<AppEntry> allApps = new ArrayList<>();
    private final List<AppEntry> shown = new ArrayList<>();
    private AppAdapter adapter;
    private CloneProgressDialog progressDlg;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Nhận kết quả từ dialog xác nhận (an toàn khi Fragment tái tạo)
        getChildFragmentManager().setFragmentResultListener(
                CloneConfirmDialog.REQUEST_KEY, this, (key, result) -> {
                    String pkg = result.getString(CloneConfirmDialog.ARG_PKG, "");
                    String label = result.getString(CloneConfirmDialog.ARG_LABEL, "");
                    if (!pkg.isEmpty()) onConfirmClone(pkg, label);
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add, container, false);
        etSearch = v.findViewById(R.id.etSearch);
        btnClear = v.findViewById(R.id.btnClear);
        tvHeader = v.findViewById(R.id.tvHeader);
        rv = v.findViewById(R.id.rv);
        progress = v.findViewById(R.id.progress);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppAdapter(shown, this::onCloneClicked);
        rv.setAdapter(adapter);
        clones = new CloneManager(requireContext());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filter(s.toString());
            }
        });
        btnClear.setOnClickListener(x -> etSearch.setText(""));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Nối lại dialog tiến trình nếu nó sống sót qua xoay màn hình
        // (FragmentManager tự khôi phục DialogFragment theo tag)
        Fragment f = getChildFragmentManager().findFragmentByTag("progress");
        progressDlg = (f instanceof CloneProgressDialog) ? (CloneProgressDialog) f : null;
        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        String self = requireContext().getPackageName();
        new Thread(() -> {
            // Dựng list trên background thread, gán vào UI thread -> không race
            List<AppEntry> fresh = new ArrayList<>();
            for (AppEntry a : clones.getInstalledApps()) {
                if (!a.packageName.equals(self)) fresh.add(a);
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return; // check trong UI thread, sát lúc dùng view
                progress.setVisibility(View.GONE);
                allApps.clear();
                allApps.addAll(fresh);
                filter(etSearch.getText().toString());
            });
        }).start();
    }

    private void filter(String q) {
        shown.clear();
        String needle = q.trim().toLowerCase(Locale.ROOT);
        for (AppEntry a : allApps) {
            if (needle.isEmpty()
                    || a.label.toLowerCase(Locale.ROOT).contains(needle)
                    || a.packageName.toLowerCase(Locale.ROOT).contains(needle)) {
                shown.add(a);
            }
        }
        tvHeader.setText(getString(R.string.all_apps, shown.size()));
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(R.string.empty_installed);
        adapter.notifyDataSetChanged();
    }

    private void onCloneClicked(AppEntry app) {
        if (!ShizukuHelper.hasPermission()) {
            Toast.makeText(getContext(), R.string.toast_need_shizuku, Toast.LENGTH_SHORT).show();
            return;
        }
        CloneConfirmDialog.newInstance(app.packageName, app.label)
                .show(getChildFragmentManager(), "confirm");
    }

    private void onConfirmClone(String pkg, String label) {
        progressDlg = CloneProgressDialog.newInstance(pkg, label);
        progressDlg.show(getChildFragmentManager(), "progress");
        new Thread(() -> {
            try {
                int userId = clones.getOrCreateCloneUser();
                setProgressStatus(R.string.cloning_step_setup);
                clones.cloneApp(userId, pkg);
                // Cấp quyền camera/mic/ảnh ngay sau khi nhân bản (best-effort)
                clones.grantMediaPermissions(userId, pkg);
                uiRun(() -> {
                    dismissProgress();
                    if (isAdded()) {
                        CloneSuccessDialog.newInstance(label)
                                .show(getChildFragmentManager(), "success");
                    }
                });
            } catch (Exception e) {
                uiRun(() -> {
                    dismissProgress();
                    Toast.makeText(getContext(),
                            getString(R.string.toast_error, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /** Lấy dialog tiến trình hiện tại (kể cả sau khi xoay màn hình). */
    private CloneProgressDialog getProgress() {
        if (progressDlg != null) return progressDlg;
        Fragment f = isAdded()
                ? getChildFragmentManager().findFragmentByTag("progress") : null;
        return (f instanceof CloneProgressDialog) ? (CloneProgressDialog) f : null;
    }

    private void setProgressStatus(int resId) {
        uiRun(() -> {
            CloneProgressDialog d = getProgress();
            if (d != null) d.setStatus(resId);
        });
    }

    private void dismissProgress() {
        CloneProgressDialog d = getProgress();
        if (d != null) {
            try {
                d.dismissAllowingStateLoss();
            } catch (Exception ignored) {
            }
        }
        progressDlg = null;
    }

    private void uiRun(Runnable r) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(r);
    }
}
