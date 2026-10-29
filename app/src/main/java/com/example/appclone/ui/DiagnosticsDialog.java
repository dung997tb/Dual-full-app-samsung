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
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.appclone.R;
import com.example.appclone.shizuku.ShizukuHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Chẩn đoán Samsung Dual": chạy một loạt lệnh CHỈ ĐỌC qua Shizuku để
 * soi không gian Dual (user 95) và agent của Samsung
 * (com.samsung.android.da.daagent) — danh sách app trong Dual,
 * installer của từng app, settings liên quan tới Dual, và đặc biệt là
 * DualAppProvider của daagent (nơi được cho là giữ danh sách app
 * được phép nhân bản). Có thêm ô gõ lệnh tự do để soi tiếp mà không
 * cần build lại app.
 *
 * Kết quả hiện dạng text thô để người dùng sao chép gửi lại phân tích.
 * Các lệnh mặc định không ghi/sửa bất cứ thứ gì trên máy.
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
            {"DualAppProvider: cau hinh quyen cua provider",
             "dumpsys package com.samsung.android.da.daagent | grep -B1 -A10 'DualAppProvider'"},
            {"Thu query DualAppProvider (authority goc)",
             "content query --uri content://com.samsung.android.da.daagent.provider 2>&1 | head -30"},
            {"Thu query DualAppProvider (cac duong con thuong gap)",
             "content query --uri content://com.samsung.android.da.daagent.provider/dual_app 2>&1 | head -15; "
                     + "content query --uri content://com.samsung.android.da.daagent.provider/apps 2>&1 | head -15; "
                     + "content query --uri content://com.samsung.android.da.daagent.provider/dualapp 2>&1 | head -15"},
            {"App trong Cong viec (user 10) de doi chieu",
             "pm list packages --user 10; echo '--- installer:'; "
                     + "pm list packages -i --user 10 | grep -vE 'installer=null'"},
    };

    private static final int MAX_SECTION_CHARS = 6000;

    private TextView tvText;
    private ScrollView scroll;
    private EditText etCmd;
    private final StringBuilder report = new StringBuilder();
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

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
        etCmd = view.findViewById(R.id.diagCmdInput);
        view.findViewById(R.id.btnDiagClose).setOnClickListener(x -> dismiss());
        view.findViewById(R.id.btnDiagCopy).setOnClickListener(x -> copyReport());
        view.findViewById(R.id.btnDiagRun).setOnClickListener(x -> runCustom());
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        exec.shutdownNow();
    }

    private void runProbes() {
        report.append("=== CHAN DOAN SAMSUNG DUAL v2 — AppClone ===\n");
        showReport();
        exec.execute(() -> {
            for (String[] probe : PROBES) {
                runOne(probe[0], probe[1]);
            }
            report.append("\n=== HET PHAN TU DONG ===\n");
            showReport();
        });
    }

    private void runCustom() {
        if (etCmd == null) return;
        String cmd = etCmd.getText().toString().trim();
        if (cmd.isEmpty()) return;
        etCmd.setText("");
        exec.execute(() -> runOne("Lenh tuy y", cmd));
    }

    /** Chạy 1 lệnh và nối kết quả vào báo cáo. Phải gọi trên exec thread. */
    private void runOne(String title, String cmd) {
        report.append("\n### ").append(title).append('\n');
        report.append("$ ").append(cmd).append('\n');
        try {
            ShizukuHelper.CommandResult r = ShizukuHelper.runCommand(cmd);
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
