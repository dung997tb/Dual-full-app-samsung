package com.example.appclone.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;

import com.example.appclone.R;

/** Ghim shortcut mở bản sao ra màn hình chính. */
public final class ShortcutHelper {

    private ShortcutHelper() {
    }

    /**
     * Ghim shortcut "Mở bản sao" của app ra launcher.
     * Bấm vào shortcut -> mở MainActivity kèm action LAUNCH_CLONE,
     * MainActivity sẽ tự mở bản sao tương ứng.
     */
    public static boolean pinCloneShortcut(Context ctx, String pkg, String label) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false;
        try {
            ShortcutManager sm = ctx.getSystemService(ShortcutManager.class);
            if (sm == null || !sm.isRequestPinShortcutSupported()) return false;

            PackageManager pm = ctx.getPackageManager();
            Drawable d;
            try {
                d = pm.getApplicationIcon(pkg);
            } catch (Exception e) {
                d = pm.getDefaultActivityIcon();
            }
            Bitmap bmp = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, 192, 192);
            d.draw(c);

            Intent intent = new Intent(ctx, MainActivity.class);
            intent.setAction(MainActivity.ACTION_LAUNCH_CLONE);
            intent.putExtra(MainActivity.EXTRA_PKG, pkg);

            ShortcutInfo info = new ShortcutInfo.Builder(ctx, "clone_" + pkg)
                    .setShortLabel(label + " " + ctx.getString(R.string.clone_suffix))
                    .setLongLabel(label + " " + ctx.getString(R.string.clone_suffix))
                    .setIcon(Icon.createWithBitmap(bmp))
                    .setIntent(intent)
                    .build();
            return sm.requestPinShortcut(info, null);
        } catch (Exception e) {
            return false;
        }
    }
}
