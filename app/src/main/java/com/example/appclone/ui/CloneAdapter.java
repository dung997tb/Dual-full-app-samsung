package com.example.appclone.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appclone.R;
import com.example.appclone.model.AppEntry;

import java.util.List;

/** Adapter cho tab "Bản sao". */
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
        h.name.setText(app.label + " " + h.itemView.getContext().getString(R.string.clone_suffix));
        if (app.icon != null) {
            h.icon.setImageDrawable(app.icon);
        } else {
            h.icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        h.btnOpen.setOnClickListener(v -> openListener.onOpen(app));
        h.btnParallel.setOnClickListener(v -> parallelListener.onParallel(app));
        h.btnMenu.setOnClickListener(v -> menuListener.onMenu(app, v));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        Button btnOpen;
        Button btnParallel;
        ImageView btnMenu;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            name = v.findViewById(R.id.name);
            btnOpen = v.findViewById(R.id.btnOpen);
            btnParallel = v.findViewById(R.id.btnParallel);
            btnMenu = v.findViewById(R.id.btnMenu);
        }
    }
}
