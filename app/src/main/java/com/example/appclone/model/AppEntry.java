package com.example.appclone.model;

import android.graphics.drawable.Drawable;

/** Một app: gói + tên hiển thị + icon (icon có thể null). */
public class AppEntry {
    public final String packageName;
    public final String label;
    public final Drawable icon;
    public final int userId;
    public final String profileLabel;
    /** Nằm trong không gian Dual của Samsung (user 95) — có nguy cơ bị Samsung tự xoá. */
    public final boolean samsungDual;

    public AppEntry(String packageName, String label, Drawable icon) {
        this(packageName, label, icon, -1, "");
    }

    public AppEntry(String packageName, String label, Drawable icon, int userId, String profileLabel) {
        this(packageName, label, icon, userId, profileLabel, false);
    }

    public AppEntry(String packageName, String label, Drawable icon, int userId, String profileLabel,
                    boolean samsungDual) {
        this.userId = userId;
        this.profileLabel = profileLabel;
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
        this.samsungDual = samsungDual;
    }
}
