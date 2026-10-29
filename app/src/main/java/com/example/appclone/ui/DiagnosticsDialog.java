package com.example.appclone.ui;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.appclone.R;
import com.example.appclone.shizuku.ShizukuHelper;

/**
 * "Chẩn đoán Samsung Dual": chạy một loạt lệnh CHỈ ĐỌC qua Shizuku để
 * soi không gian Dual (user 95) và agent của Samsung
 * (com.samsung.android.da.daagent) — danh sách app trong Dual,
 * installer của từng app, settings liên quan tới Dual, provider và
 * thư mục dữ liệu của agent. Mục đích: tìm điểm khác nhau giữa bản
 * sao "chính chủ" (bật trong Cài đặt Dual Messenger) và bản sao do
 * AppClone cài qua shell, xem có đường nào để Samsung không tự xoá.
 *
 * Kết quả hiện dạng text thô để người dùng sao chép gửi lại phân tích.
 * Không ghi/sửa bất cứ thứ gì trên máy.
 */
public class DiagnosticsDialog extends DialogFragment {

    private static final String[][] PROBES = {
            {"May & phien ban Android",
             "getprop ro.product.model; getprop ro.build.version.release; "
                     + "getprop ro.build.version.sdk; getprop ro.build.display.id"},
            {"Danh sach user / profile (pm list users)",
             "pm list users"},
            {"Chi tiet profile (dumpsys user, 200 dong dau)",
             "dumpsys user | head -200"},
            {"App dang co trong Dual (user 95)",
             "pm list packages --user 95"},
            {"Installer cua app trong Dual (user 95)",
             "pm list packages -i --user 95"},
            {"Settings lien quan toi Dual (global / secure / system)",
             "echo '--- GLOBAL'; settings list global | grep -iE 'dual|daagent|da_'; "
                     + "echo '--- SECURE'; settings list secure | grep -iE 'dual|daagent|da_'; "
                     + "echo '--- SYSTEM'; settings list system | grep -iE 'dual|daagent|da_'"},
            {"Dual agent cua Samsung (daagent): provider / dataDir / version",
             "dumpsys package com.samsung.android.da.daagent | grep -iE "
                     + "'versionName|versionCode|codePath|dataDir|resourcePath|provider|authorit|installer' "
                     + "| head -80"},
            {"Thu doc thu muc du lieu cua daagent (ky vong bi chan)",
             "ls -la /data/data/com.samsung.android.da.daagent/ 2>&1; echo '---'; "
                     + "ls /data/user/95/ 2>&1 | head -20"},
    };

    private static final int MAX_SECTION_CHARS = 6000;

    private TextView tvText;
    private ScrollView scroll;
    private final StringBuilder report = new StringBuilder();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_diagnostics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvText = view.findViewById(R.id.diagText);
        scroll = view.findViewById(R.id.diagScroll);
        view.findViewById(R.id.btnDiagClose).setOnClickListener(x -> dismiss());
        view.findViewById(R.id.btnDiagCopy).setOnClickListener(x -> copyReport());
        runProbes();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null && d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics dm = getResources().getDisplayMetrics();
            d.getWindow().setLayout((int) (dm.widthPixels * 0.94),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void runProbes() {
        report.append("=== CHAN DOAN SAMSUNG DUAL — AppClone ===\n");
        showReport();
        new Thread(() -> {
            for (String[] probe : PROBES) {
                report.append("\n### ").append(probe[0]).append('\n');
                report.append("$ ").append(probe[1]).append('\n');
                try {
                    ShizukuHelper.CommandResult r = ShizukuHelper.runCommand(probe[1]);
                    String out = r.allOutput().trim();
                    if (out.isEmpty()) {
                        out = "(khong co output)";
                    }
                    if (out.length() > MAX_SECTION_CHARS) {
                        out = out.substring(0, MAX_SECTION_CHARS) + "\n...(cat bot)";
                    }
                    report.append(out).append('\n');
                    if (!r.isSuccess()) {
                        report.append("(exit=").append(r.exitCode).append(")\n");
                    }
                } catch (Exception e) {
                    report.append("LOI: ").append(e.getMessage()).append('\n');
                }
                showReport();
            }
            report.append("\n=== HET ===\n");
            showReport();
        }).start();
    }

    private void showReport() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (tvText == null) return;
            tvText.setText(report.toString());
            if (scroll != null) {
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private void copyReport() {
        ClipboardManager cm = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("AppClone diagnostics", report.toString()));
            Toast.makeText(getContext(), R.string.diag_copied, Toast.LENGTH_LONG).show();
        }
    }
}
