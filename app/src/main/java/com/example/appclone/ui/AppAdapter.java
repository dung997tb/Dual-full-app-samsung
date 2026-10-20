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

/** Adapter cho tab "Đã cài đặt". */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

    public interface OnCloneClick {
        void onClone(AppEntry app);
    }

    private final List<AppEntry> apps;
    private final OnCloneClick listener;

    public AppAdapter(List<AppEntry> apps, OnCloneClick listener) {
        this.apps = apps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        AppEntry app = apps.get(position);
        h.name.setText(app.label);
        if (app.icon != null) {
            h.icon.setImageDrawable(app.icon);
        } else {
            h.icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        h.btnAction.setOnClickListener(v -> listener.onClone(app));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        Button btnAction;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            name = v.findViewById(R.id.name);
            btnAction = v.findViewById(R.id.btnAction);
        }
    }
}
