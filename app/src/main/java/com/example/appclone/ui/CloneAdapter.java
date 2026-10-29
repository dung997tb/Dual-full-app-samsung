package com.example.appclone.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appclone.R;
import com.example.appclone.model.AppEntry;

import java.util.List;

/** Adapter cho tab "Bản sao" — lưới icon kiểu launcher: chạm để mở, chạm giữ để mở menu. */
public class CloneAdapter extends RecyclerView.Adapter<CloneAdapter.Holder> {

    public interface OnOpenClick {
        void onOpen(AppEntry app);
    }

    public interface OnParallelClick {
        void onParallel(AppEntry app);
    }

    public interface OnMenuClick {
        void onMenu(AppEntry app, View anchor);
    }

    private final List<AppEntry> apps;
    private final OnOpenClick openListener;
    private final OnParallelClick parallelListener;
    private final OnMenuClick menuListener;

    public CloneAdapter(List<AppEntry> apps, OnOpenClick openListener,
                        OnParallelClick parallelListener, OnMenuClick menuListener) {
        this.apps = apps;
        this.openListener = openListener;
        this.parallelListener = parallelListener;
        this.menuListener = menuListener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clone, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        AppEntry app = apps.get(position);
        h.name.setText(app.label);
        String pill = app.userId >= 0
                ? shortProfile(app.profileLabel) + " · " + app.userId
                : shortProfile(app.profileLabel);
        if (app.samsungDual) {
            // Bản sao trong Dual 95: pill đỏ + ⚠ — Samsung có thể tự xoá.
            h.sub.setText("⚠ " + pill);
            h.sub.setBackgroundResource(R.drawable.pill_bg_warn);
            h.sub.setTextColor(ContextCompat.getColor(
                    h.itemView.getContext(), R.color.danger_light));
        } else {
            h.sub.setText(pill);
            h.sub.setBackgroundResource(R.drawable.pill_bg);
            h.sub.setTextColor(ContextCompat.getColor(
                    h.itemView.getContext(), R.color.ubuntu_orange));
        }
        if (app.icon != null) {
            h.icon.setImageDrawable(app.icon);
        } else {
            h.icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        h.itemView.setOnClickListener(v -> openListener.onOpen(app));
        h.itemView.setOnLongClickListener(v -> {
            menuListener.onMenu(app, v);
            return true;
        });
    }

    /** Nhãn profile rút gọn để hiện dưới tên app trong ô lưới. */
    private static String shortProfile(String profileLabel) {
        if (profileLabel == null || profileLabel.isEmpty()) return "Profile";
        if (profileLabel.contains("Dual")) return "Dual";
        if (profileLabel.contains("Công việc")) return "Công việc";
        if (profileLabel.contains("Riêng")) return "Riêng tư";
        return profileLabel;
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView sub;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            name = v.findViewById(R.id.name);
            sub = v.findViewById(R.id.sub);
        }
    }
}
