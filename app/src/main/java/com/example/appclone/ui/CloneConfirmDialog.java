package com.example.appclone.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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

/**
 * Dialog xác nhận trước khi nhân bản.
 * Kết quả trả về qua FragmentResult ("clone_confirm_request",
 * bundle chứa "pkg" và "label") — an toàn khi Fragment bị tái tạo.
 */
public class CloneConfirmDialog extends DialogFragment {

    public static final String REQUEST_KEY = "clone_confirm_request";
    public static final String ARG_PKG = "pkg";
    public static final String ARG_LABEL = "label";

    public static CloneConfirmDialog newInstance(String pkg, String label) {
        CloneConfirmDialog d = new CloneConfirmDialog();
        Bundle b = new Bundle();
        b.putString(ARG_PKG, pkg);
        b.putString(ARG_LABEL, label);
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_clone_confirm, container, false);
        Bundle b = getArguments();
        String label = b != null ? b.getString(ARG_LABEL, "") : "";
        String pkg = b != null ? b.getString(ARG_PKG, "") : "";

        ImageView icon = v.findViewById(R.id.icon);
        try {
            Drawable d = requireContext().getPackageManager().getApplicationIcon(pkg);
            icon.setImageDrawable(d);
        } catch (Exception e) {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        ((TextView) v.findViewById(R.id.title)).setText(getString(R.string.clone_confirm_title, label));
        ((TextView) v.findViewById(R.id.desc)).setText(getString(R.string.clone_confirm_desc, label));

        v.findViewById(R.id.btnCancel).setOnClickListener(x -> dismiss());
        v.findViewById(R.id.btnConfirm).setOnClickListener(x -> {
            Bundle result = new Bundle();
            result.putString(ARG_PKG, pkg);
            result.putString(ARG_LABEL, label);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismiss();
        });
        return v;
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
