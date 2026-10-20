package com.example.appclone.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.appclone.R;

/** Dialog tiến trình nhân bản (không cho tắt giữa chừng). */
public class CloneProgressDialog extends DialogFragment {

    private static final String ARG_PKG = "pkg";
    private static final String ARG_LABEL = "label";

    private TextView tvStatus;

    public static CloneProgressDialog newInstance(String pkg, String label) {
        CloneProgressDialog d = new CloneProgressDialog();
        Bundle b = new Bundle();
        b.putString(ARG_PKG, pkg);
        b.putString(ARG_LABEL, label);
        d.setArguments(b);
        return d;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_clone_progress, container, false);
        Bundle b = getArguments();
        String label = b != null ? b.getString(ARG_LABEL, "") : "";
        String pkg = b != null ? b.getString(ARG_PKG, "") : "";
        ImageView icon = v.findViewById(R.id.icon);
        try {
            icon.setImageDrawable(requireContext().getPackageManager().getApplicationIcon(pkg));
        } catch (Exception e) {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        ((TextView) v.findViewById(R.id.title)).setText(getString(R.string.cloning_title, label));
        tvStatus = v.findViewById(R.id.status);
        tvStatus.setText(R.string.cloning_step_space);
        return v;
    }

    /** Đổi dòng trạng thái (gọi từ UI thread). */
    public void setStatus(String s) {
        if (tvStatus != null) tvStatus.setText(s);
    }

    public void setStatus(int resId) {
        if (tvStatus != null) tvStatus.setText(resId);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null && d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
