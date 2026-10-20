package com.example.appclone.model;

import android.graphics.drawable.Drawable;

/** Một app: gói + tên hiển thị + icon (icon có thể null). */
public class AppEntry {
    public final String packageName;
    public final String label;
    public final Drawable icon;

    public AppEntry(String packageName, String label, Drawable icon) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
    }
}
