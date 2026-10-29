package com.example.appclone.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.appclone.R;
import com.example.appclone.clone.CloneManager;
import com.example.appclone.shizuku.ShizukuHelper;

/** Tab "Cài đặt": hướng dẫn Shizuku, trạng thái quyền, giới thiệu. */
public class SettingsFragment extends Fragment {

    private TextView tvPermStatus;
    private View permDot;
    private CloneManager clones;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        tvPermStatus = v.findViewById(R.id.tvPermStatus);
        permDot = v.findViewById(R.id.permDot);
        clones = new CloneManager(requireContext());

        MainActivity host = (MainActivity) requireActivity();
        v.findViewById(R.id.rowGuide).setOnClickListener(x -> host.showShizukuGuide());
        v.findViewById(R.id.rowPerm).setOnClickListener(x -> onPermRowClicked());
        v.findViewById(R.id.rowMedia).setOnClickListener(x -> onMediaRowClicked());
        v.findViewById(R.id.rowDiag).setOnClickListener(x -> onDiagRowClicked());
        v.findViewById(R.id.rowNative).setOnClickListener(x ->
                infoDialog(getString(R.string.row_native_title), getString(R.string.row_native_desc)));
        v.findViewById(R.id.rowPerf).setOnClickListener(x ->
                infoDialog(getString(R.string.row_perf_title), getString(R.string.row_perf_desc)));
        v.findViewById(R.id.rowPrivacy).setOnClickListener(x ->
                infoDialog(getString(R.string.row_privacy_title), getString(R.string.row_privacy_desc)));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPermRow();
    }

    private void refreshPermRow() {
        int dot;
        int text;
        if (!ShizukuHelper.isBinderAlive()) {
            dot = R.drawable.dot_red;
            text = R.string.perm_nobinder;
        } else if (!ShizukuHelper.hasPermission()) {
            dot = R.drawable.dot_yellow;
            text = R.string.perm_denied;
        } else {
            dot = R.drawable.dot_green;
            text = R.string.perm_granted;
        }
        tvPermStatus.setText(text);
        permDot.setBackground(ContextCompat.getDrawable(requireContext(), dot));
    }

    private void onPermRowClicked() {
        ((MainActivity) requireActivity()).requestShizukuPermission();
    }

    /** Cấp runtime permission ảnh/file/camera/mic cho các app nhân bản. */
    private void onMediaRowClicked() {
        if (!ShizukuHelper.hasPermission()) {
            Toast.makeText(getContext(), R.string.toast_need_shizuku, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getContext(), "Đang cấp quyền…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                int userId = clones.requireCloneUser();
                int n = clones.grantAllMediaPermissions(userId);
                uiToast(getString(R.string.toast_media_done, n));
            } catch (Exception e) {
                uiToast(getString(R.string.toast_error, e.getMessage()));
            }
        }).start();
    }

    /** Mở hộp thoại chẩn đoán Samsung Dual (lệnh chỉ đọc qua Shizuku). */
    private void onDiagRowClicked() {
        if (!ShizukuHelper.hasPermission()) {
            Toast.makeText(getContext(), R.string.toast_need_shizuku, Toast.LENGTH_SHORT).show();
            return;
        }
        new DiagnosticsDialog().show(getParentFragmentManager(), "diagnostics");
    }

    private void infoDialog(String title, String msg) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(R.string.dialog_close, null)
                .show();
    }

    private void uiToast(String msg) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show());
    }
}
