package com.example.appclone.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.appclone.R;

/** Dialog báo nhân bản thành công, tự tắt sau 1.6s. */
public class CloneSuccessDialog extends DialogFragment {

    private static final String ARG_LABEL = "label";

    public static CloneSuccessDialog newInstance(String label) {
        CloneSuccessDialog d = new CloneSuccessDialog();
        Bundle b = new Bundle();
        b.putString(ARG_LABEL, label);
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_clone_success, container, false);
        Bundle b = getArguments();
        String label = b != null ? b.getString(ARG_LABEL, "") : "";
        ((TextView) v.findViewById(R.id.desc)).setText(getString(R.string.clone_success_desc, label));
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) dismiss();
        }, 1600);
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
