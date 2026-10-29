package com.example.appclone.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.appclone.R;
import com.example.appclone.model.AppEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet tuỳ chọn cho 1 bản sao (chạm giữ trong lưới):
 * mở / chạy song song / ghim / xoá.
 * Bản sao trong Dual của Samsung (user 95) có thêm hộp cảnh báo
 * nguy cơ bị Samsung tự xoá ở đầu sheet.
 * Kết quả trả về qua FragmentResult cho ClonesFragment xử lý.
 */
public class CloneOptionsDialog extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "clone_options";
    public static final String KEY_ACTION = "action";
    public static final String KEY_PKG = "pkg";
    public static final String KEY_LABEL = "label";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_PROFILE_LABEL = "profileLabel";

    public static final int ACTION_OPEN = 1;
    public static final int ACTION_PARALLEL = 2;
    public static final int ACTION_PIN = 3;
    public static final int ACTION_DELETE = 4;

    private static final String ARG_PKG = "pkg";
    private static final String ARG_LABEL = "label";
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_PROFILE_LABEL = "profileLabel";
    private static final String ARG_SAMSUNG_DUAL = "samsungDual";

    public static CloneOptionsDialog newInstance(AppEntry app) {
        CloneOptionsDialog d = new CloneOptionsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PKG, app.packageName);
        args.putString(ARG_LABEL, app.label);
        args.putInt(ARG_USER_ID, app.userId);
        args.putString(ARG_PROFILE_LABEL, app.profileLabel);
        args.putBoolean(ARG_SAMSUNG_DUAL, app.samsungDual);
        d.setArguments(args);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_clone_options, container, false);
        Bundle a = requireArguments();

        TextView title = v.findViewById(R.id.sheetTitle);
        title.setText(a.getString(ARG_LABEL));
        TextView sub = v.findViewById(R.id.sheetSub);
        sub.setText("· " + shortProfile(a.getString(ARG_PROFILE_LABEL))
                + " · user " + a.getInt(ARG_USER_ID));

        if (a.getBoolean(ARG_SAMSUNG_DUAL)) {
            v.findViewById(R.id.warnBox).setVisibility(View.VISIBLE);
            ((TextView) v.findViewById(R.id.warnTitle))
                    .setText("⚠ " + getString(R.string.warn_dual_title));
        }

        v.findViewById(R.id.rowOpen).setOnClickListener(x -> deliver(ACTION_OPEN));
        v.findViewById(R.id.rowParallel).setOnClickListener(x -> deliver(ACTION_PARALLEL));
        v.findViewById(R.id.rowPin).setOnClickListener(x -> deliver(ACTION_PIN));
        v.findViewById(R.id.rowDelete).setOnClickListener(x -> deliver(ACTION_DELETE));
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d instanceof BottomSheetDialog) {
            View sheet = ((BottomSheetDialog) d)
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.sheet_bg));
            }
        }
    }

    private void deliver(int action) {
        Bundle a = requireArguments();
        Bundle result = new Bundle();
        result.putInt(KEY_ACTION, action);
        result.putString(KEY_PKG, a.getString(ARG_PKG));
        result.putString(KEY_LABEL, a.getString(ARG_LABEL));
        result.putInt(KEY_USER_ID, a.getInt(ARG_USER_ID));
        result.putString(KEY_PROFILE_LABEL, a.getString(ARG_PROFILE_LABEL));
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    /** Rút gọn nhãn profile giống pill trong lưới. */
    private static String shortProfile(String profileLabel) {
        if (profileLabel == null || profileLabel.isEmpty()) return "Profile";
        if (profileLabel.contains("Dual")) return "Dual";
        if (profileLabel.contains("Công việc")) return "Công việc";
        if (profileLabel.contains("Riêng")) return "Riêng tư";
        return profileLabel;
    }
}
