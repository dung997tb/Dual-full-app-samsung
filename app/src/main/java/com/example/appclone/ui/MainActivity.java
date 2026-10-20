package com.example.appclone.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.appclone.R;
import com.example.appclone.shizuku.ShizukuHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_SHIZUKU = 1001;

    /** Action mở bản sao từ shortcut ghim ngoài màn hình chính. */
    public static final String ACTION_LAUNCH_CLONE = "com.example.appclone.LAUNCH_CLONE";
    public static final String EXTRA_PKG = "pkg";

    public static final String CLONES_CHANGED = "appclone.clones_changed";

    private View statusDot;
    private BottomNavigationView bottomNav;

    private Fragment clonesFragment;
    private Fragment addFragment;
    private Fragment settingsFragment;
    private Fragment active;

    private final Shizuku.OnBinderReceivedListener binderReceivedListener =
            () -> runOnUiThread(this::refreshStatus);
    private final Shizuku.OnBinderDeadListener binderDeadListener =
            () -> runOnUiThread(this::refreshStatus);
    private final Shizuku.OnRequestPermissionResultListener permissionResultListener =
            (requestCode, grantResult) -> {
                if (requestCode == REQ_SHIZUKU) {
                    runOnUiThread(() -> {
                        refreshStatus();
                        Toast.makeText(this,
                                grantResult == PackageManager.PERMISSION_GRANTED
                                        ? "Đã cấp quyền Shizuku"
                                        : "Chưa được cấp quyền",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusDot = findViewById(R.id.statusDot);
        bottomNav = findViewById(R.id.bottomNav);

        // Android 15+ (targetSdk 35): edge-to-edge mặc định, header bị
        // thanh trạng thái che -> đệm thêm theo system bar inset.
        View headerBar = findViewById(R.id.headerBar);
        final int basePad = (int) (14 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(headerBar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top + basePad,
                    v.getPaddingRight(), basePad);
            return insets;
        });

        // Chỉ add Fragment lần đầu; khi xoay màn hình / khôi phục thì
        // FragmentManager đã giữ lại các Fragment cũ.
        if (savedInstanceState == null) {
            clonesFragment = new ClonesFragment();
            addFragment = new AddFragment();
            settingsFragment = new SettingsFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, settingsFragment, "settings").hide(settingsFragment)
                    .add(R.id.container, addFragment, "add").hide(addFragment)
                    .add(R.id.container, clonesFragment, "clones")
                    .commit();
            active = clonesFragment;
        } else {
            clonesFragment = getSupportFragmentManager().findFragmentByTag("clones");
            addFragment = getSupportFragmentManager().findFragmentByTag("add");
            settingsFragment = getSupportFragmentManager().findFragmentByTag("settings");
            active = clonesFragment;
            // Khôi phục tab đang hiển thị
            for (Fragment f : getSupportFragmentManager().getFragments()) {
                if (f != null && f.isVisible()) active = f;
            }
        }

        getSupportFragmentManager().setFragmentResultListener(CLONES_CHANGED, this,
                (key, result) -> refreshClones());

        bottomNav.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.nav_clones) refreshClones();
        });
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_clones) {
                switchTo(clonesFragment);
            } else if (id == R.id.nav_add) {
                switchTo(addFragment);
            } else if (id == R.id.nav_settings) {
                switchTo(settingsFragment);
            } else {
                return false;
            }
            return true;
        });

        Shizuku.addBinderReceivedListener(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        Shizuku.addRequestPermissionResultListener(permissionResultListener);

        refreshStatus();
        handleShortcutIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        Shizuku.removeRequestPermissionResultListener(permissionResultListener);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShortcutIntent(intent);
    }

    /**
     * Shortcut ghim ngoài launcher bấm vào sẽ tới đây.
     * Chống confused deputy: chỉ mở package do AppClone quản lý và đang
     * tồn tại trong đúng profile — KHÔNG tự tạo profile mới.
     */
    private void handleShortcutIntent(Intent intent) {
        if (intent == null || !ACTION_LAUNCH_CLONE.equals(intent.getAction())) return;
        String pkg = intent.getStringExtra(EXTRA_PKG);
        if (pkg == null || pkg.isEmpty()) return;
        if (!ShizukuHelper.hasPermission()) {
            Toast.makeText(this, R.string.toast_need_shizuku, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                com.example.appclone.clone.CloneManager cm =
                        new com.example.appclone.clone.CloneManager(this);
                int userId = cm.requireCloneUser(); // không tự tạo
                if (!cm.isTracked(pkg, userId) || !cm.isInstalledForUser(userId, pkg)) {
                    throw new Exception("Bản sao không còn hợp lệ");
                }
                cm.launchClone(userId, pkg);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.toast_error, e.getMessage()),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void switchTo(Fragment f) {
        if (f == active) {
            if (f == clonesFragment) refreshClones();
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .hide(active).show(f)
                .runOnCommit(() -> {
                    if (f == clonesFragment) refreshClones();
                }).commit();
        active = f;
    }

    private void refreshClones() {
        if (clonesFragment instanceof ClonesFragment) {
            ((ClonesFragment) clonesFragment).refresh();
        }
    }

    /** Cho ClonesFragment gọi khi bấm FAB "+ Nhân bản". */
    public void goToAddTab() {
        bottomNav.setSelectedItemId(R.id.nav_add);
    }

    /** Hiện dialog hướng dẫn Shizuku (dùng chung cho các tab). */
    public void showShizukuGuide() {
        new ShizukuGuideDialog().show(getSupportFragmentManager(), "guide");
    }

    public void requestShizukuPermission() {
        if (!ShizukuHelper.isBinderAlive()) {
            Toast.makeText(this, "Mở app Shizuku và start nó trước đã", Toast.LENGTH_LONG).show();
            return;
        }
        ShizukuHelper.requestPermission(REQ_SHIZUKU);
    }

    /** Đèn trạng thái trên thanh tiêu đề: xanh/vàng/đỏ. */
    public void refreshStatus() {
        int dot;
        if (!ShizukuHelper.isBinderAlive()) {
            dot = R.drawable.dot_red;
        } else if (!ShizukuHelper.hasPermission()) {
            dot = R.drawable.dot_yellow;
        } else {
            dot = R.drawable.dot_green;
        }
        statusDot.setBackground(ContextCompat.getDrawable(this, dot));
        refreshClones();
    }
}
