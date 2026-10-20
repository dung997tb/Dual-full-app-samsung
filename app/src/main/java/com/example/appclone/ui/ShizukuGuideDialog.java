package com.example.appclone.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.appclone.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/** Bottom sheet hướng dẫn kết nối Shizuku (4 bước). */
public class ShizukuGuideDialog extends BottomSheetDialogFragment {

    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_shizuku_guide, container, false);
        v.findViewById(R.id.btnClose).setOnClickListener(x -> dismiss());
        v.findViewById(R.id.btnOpenSettings).setOnClickListener(x -> openDevSettings());
        v.findViewById(R.id.btnOpenShizuku).setOnClickListener(x -> openShizuku());
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Nền sheet tối bo góc (macOS + Ubuntu dark)
        Dialog d = getDialog();
        if (d instanceof BottomSheetDialog) {
            View sheet = ((BottomSheetDialog) d)
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.sheet_bg));
            }
        }
    }

    private void openDevSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
            }
        }
    }

    private void openShizuku() {
        try {
            Intent li = requireContext().getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
            if (li != null) {
                startActivity(li);
                return;
            }
            // Chưa cài -> mở Play Store
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + SHIZUKU_PKG)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + SHIZUKU_PKG)));
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.toast_no_shizuku_app, Toast.LENGTH_SHORT).show();
        }
    }
}
