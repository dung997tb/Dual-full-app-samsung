
package com.example.appclone.clone;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import com.example.appclone.model.AppEntry;
import com.example.appclone.shizuku.ShizukuHelper;
import com.example.appclone.shizuku.ShizukuHelper.CommandResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloneManager {

    private static final String PREFS = "appclone_clones";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_USER_ID = "clone_user_id";

    private static final Pattern USER_PATTERN =
            Pattern.compile("UserInfo\\{(\\d+):([^}:]+):");

    private final Context ctx;
    private final PackageManager pm;

    public CloneManager(Context context) {
        ctx = context.getApplicationContext();
        pm = ctx.getPackageManager();
    }

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
    }

    private synchronized Set<String> loadEntries() {
        return new HashSet<>(
                prefs().getStringSet(
                        KEY_ENTRIES, new HashSet<String>()));
    }

    private synchronized void saveEntries(Set<String> entries) {
        prefs().edit()
                .putStringSet(
                        KEY_ENTRIES, new HashSet<>(entries))
                .commit();
    }

    public synchronized void track(String pkg, int userId) {
        Set<String> entries = loadEntries();
        entries.add(pkg + "|" + userId);
        saveEntries(entries);
    }

    public synchronized void untrack(String pkg, int userId) {
        Set<String> entries = loadEntries();
        entries.remove(pkg + "|" + userId);
        saveEntries(entries);
    }

    public boolean isTracked(String pkg, int userId) {
        return loadEntries().contains(pkg + "|" + userId);
    }

    public Set<String> getTrackedForUser(int userId) {
        Set<String> result = new HashSet<>();
        String suffix = "|" + userId;

        for (String entry : loadEntries()) {
            if (entry.endsWith(suffix)) {
                result.add(entry.substring(
                        0,
                        entry.length() - suffix.length()));
            }
        }
        return result;
    }

    private void saveCloneUserId(int id) {
        prefs().edit().putInt(KEY_USER_ID, id).apply();
    }

    private int loadCloneUserId() {
        return prefs().getInt(KEY_USER_ID, -1);
    }

    private void validatePackage(String pkg) {
        if (pkg == null || !pkg.matches(
                "[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) {
            throw new IllegalArgumentException(
                    "Tên package không hợp lệ");
        }
    }

    // -------- ỨNG DỤNG TRÊN MÁY --------

    public List<AppEntry> getInstalledApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> ris =
                pm.queryIntentActivities(intent, 0);

        List<AppEntry> out = new ArrayList<>();
        Set<String> added = new HashSet<>();

        for (ResolveInfo ri : ris) {
            if (ri.activityInfo == null) continue;

            String pkg = ri.activityInfo.packageName;

            if (!added.add(pkg)) continue;

            CharSequence label = ri.loadLabel(pm);
            Drawable icon = null;

            try {
                icon = pm.getApplicationIcon(pkg);
            } catch (Exception ignored) {}

            out.add(new AppEntry(
                    pkg,
                    label != null ? label.toString() : pkg,
                    icon
            ));
        }

        out.sort((a, b) ->
                a.label.compareToIgnoreCase(b.label));

        return out;
    }

    private Set<String> getDeclaredPermissions(String pkg) {
        Set<String> result = new HashSet<>();

        try {
            PackageInfo pi = pm.getPackageInfo(
                    pkg, PackageManager.GET_PERMISSIONS);

            if (pi.requestedPermissions != null) {
                for (String perm : pi.requestedPermissions) {
                    result.add(perm);
                }
            }
        } catch (Exception ignored) {}

        return result;
    }

    // -------- NHẬN DIỆN CLONE PROFILE --------

    private String scanWarning = "";
    public String getScanWarning() { return scanWarning; }

    private List<ProfileParser.Profile> profiles() throws Exception {
        CommandResult result = ShizukuHelper.runCommand("dumpsys user");
        if (!result.isSuccess()) throw new Exception("Không đọc được profiles: " + result.allOutput());
        return ProfileParser.parse(result.stdout);
    }

    public int findCloneUser() throws Exception {
        int found = -1;
        for (ProfileParser.Profile profile : profiles()) {
            if (!profile.type.endsWith(".CLONE")) continue;
            if (found != -1) throw new Exception("Có nhiều clone profile. Cần chọn profile để tạo bản sao.");
            found = profile.id;
        }
        return found;
    }

    public List<AppEntry> getAllClones() throws Exception {
        List<AppEntry> all = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (ProfileParser.Profile profile : profiles()) {
            try { all.addAll(getClones(profile.id)); }
            catch (Exception e) { warnings.add("User " + profile.id + ": " + e.getMessage()); }
        }
        scanWarning = android.text.TextUtils.join("; ", warnings);
        all.sort((a, b) -> {
            int byUser = Integer.compare(a.userId, b.userId);
            return byUser != 0 ? byUser : a.label.compareToIgnoreCase(b.label);
        });
        return all;
    }

    private boolean userExists(int userId) throws Exception {
        CommandResult r =
                ShizukuHelper.runCommand("pm list users");

        if (!r.isSuccess()) {
            throw new Exception(
                    "Không kiểm tra được user: "
                            + r.allOutput().trim());
        }

        return Pattern.compile(
                "UserInfo\\{" + userId + ":")
                .matcher(r.stdout).find();
    }

    /**
     * Chỉ tạo Android CLONE profile.
     * Không fallback sang managed/full user.
     */
    public int createCloneUser() throws Exception {
        String cmd =
                "pm create-user --profileOf 0 "
                + "--user-type "
                + "android.os.usertype.profile.CLONE "
                + "\"AppClone\"";

        CommandResult r = ShizukuHelper.runCommand(cmd);

        Matcher m = Pattern.compile(
                "created user id (\\d+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(r.allOutput());

        if (!r.isSuccess() || !m.find()) {
            throw new Exception(
                    "Không tạo được clone profile: "
                            + r.allOutput().trim());
        }

        int id = Integer.parseInt(m.group(1));

        // Xác minh profile tạo ra thật sự được
        // nhận diện trước khi sử dụng.
        int found = findCloneUser();

        if (found != id) {
            throw new Exception(
                    "Đã tạo user " + id
                    + " nhưng chưa xác minh được "
                    + "clone profile hợp lệ. "
                    + "Không thực hiện cài đặt.");
        }

        saveCloneUserId(id);
        return id;
    }

    public int getOrCreateCloneUser() throws Exception {
        int id = findCloneUser();

        if (id == -1) {
            id = createCloneUser();
        } else {
            saveCloneUserId(id);
        }

        return id;
    }

    /**
     * Chỉ lấy profile hiện có.
     * KHÔNG tạo mới khi mở hoặc xóa ứng dụng.
     */
    public int requireCloneUser() throws Exception {
        int id = findCloneUser();

        if (id == -1 || !userExists(id)) {
            throw new Exception(
                    "Không tìm thấy clone profile hợp lệ");
        }

        saveCloneUserId(id);
        return id;
    }

    private void requireCorrectUser(int userId)
            throws Exception {
        for (ProfileParser.Profile profile : profiles()) {
            if (profile.id == userId) return;
        }
        throw new Exception("User " + userId + " không phải profile được xác minh của user 0");
    }

    // -------- ĐỌC PACKAGE TRONG PROFILE --------

    private Set<String> listPackages(
            int userId, boolean thirdPartyOnly)
            throws Exception {

        String cmd = "pm list packages "
                + (thirdPartyOnly ? "-3 " : "")
                + "--user " + userId;

        CommandResult r = ShizukuHelper.runCommand(cmd);

        if (!r.isSuccess()) {
            throw new Exception(
                    "Không đọc được package của user "
                    + userId + ": " + r.allOutput().trim());
        }

        Set<String> installed = new HashSet<>();

        for (String line : r.stdout.split("\\r?\\n")) {
            line = line.trim();

            if (line.startsWith("package:")) {
                String pkg = line.substring(8).trim();

                if (pkg.matches(
                        "[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) {
                    installed.add(pkg);
                }
            }
        }

        return installed;
    }

    public boolean isInstalledForUser(
            int userId, String pkg) throws Exception {

        validatePackage(pkg);
        requireCorrectUser(userId);
        return listPackages(userId, false).contains(pkg);
    }

    /**
     * Quét ứng dụng thực tế trong clone profile.
     *
     * Bao gồm:
     * - Ứng dụng bên thứ ba đang cài trong user.
     * - AppClone đã track và còn tồn tại.
     *
     * Không tự nhận sở hữu app từ công cụ khác.
     */
    public List<AppEntry> getClones(int userId)
            throws Exception {

        requireCorrectUser(userId);

        Set<String> allInstalled =
                listPackages(userId, false);

        Set<String> candidates =
                listPackages(userId, true);

        Set<String> tracked =
                getTrackedForUser(userId);

        candidates.addAll(tracked);
        candidates.retainAll(allInstalled);

        candidates.remove(ctx.getPackageName());

        List<AppEntry> result = new ArrayList<>();
        String profileLabel = "Profile";
        for (ProfileParser.Profile profile : profiles()) {
            if (profile.id == userId) { profileLabel = profile.label(); break; }
        }

        // Nhãn/icon đúng theo từng profile (app chỉ cài trong profile khác
        // thì PackageManager của user 0 không lấy được -> hiện package name).
        Map<String, Object[]> launcher = launcherEntries(userId);

        for (String pkg : candidates) {
            boolean managed = tracked.contains(pkg);

            // Loại package nền không có launcher.
            // Giữ lại app đã được AppClone quản lý.
            if (!managed
                    && resolveLauncher(userId, pkg) == null) {
                continue;
            }

            String label = pkg;
            Drawable icon = null;

            Object[] le = launcher.get(pkg);
            if (le != null) {
                if (le[0] != null) label = le[0].toString();
                icon = (Drawable) le[1];
            }

            if (icon == null || label.equals(pkg)) {
                try {
                    ApplicationInfo ai =
                            pm.getApplicationInfo(pkg, 0);

                    if (label.equals(pkg)) {
                        CharSequence text =
                                pm.getApplicationLabel(ai);

                        if (text != null) {
                            label = text.toString();
                        }
                    }

                    if (icon == null) {
                        icon = pm.getApplicationIcon(ai);
                    }
                } catch (Exception ignored) {}
            }

            result.add(new AppEntry(pkg, label, icon, userId, profileLabel));
        }

        result.sort((a, b) ->
                a.label.compareToIgnoreCase(b.label));

        return result;
    }

    // -------- NHÂN BẢN ỨNG DỤNG --------

    public void cloneApp(int userId, String pkg)
            throws Exception {

        validatePackage(pkg);
        requireCorrectUser(userId);

        if (isInstalledForUser(userId, pkg)) {
            if (isTracked(pkg, userId)) return;

            throw new Exception(
                    "Ứng dụng đã có bản sao từ trước. "
                    + "Không tự nhận quyền quản lý.");
        }

        CommandResult r = ShizukuHelper.runCommand(
                "pm install-existing --user "
                + userId + " " + pkg
        );

        if (r.timedOut) {
            throw new Exception(
                    "Lệnh nhân bản hết thời gian chờ. "
                    + "Cần kiểm tra trạng thái thực tế.");
        }

        if (!r.isSuccess()) {
            throw new Exception(
                    "Nhân bản thất bại: "
                            + r.allOutput().trim());
        }

        if (!isInstalledForUser(userId, pkg)) {
            throw new Exception(
                    "Lệnh chạy xong nhưng package "
                    + "chưa tồn tại trong clone profile");
        }

        track(pkg, userId);
    }

    // -------- XÓA BẢN SAO --------

    public void removeClone(int userId, String pkg)
            throws Exception {

        validatePackage(pkg);
        requireCorrectUser(userId);

        if (!isInstalledForUser(userId, pkg)) throw new Exception("Bản sao không còn tồn tại trong user " + userId);

        CommandResult r = ShizukuHelper.runCommand(
                "pm uninstall --user "
                + userId + " " + pkg
        );

        if (!r.isSuccess()) {
            throw new Exception(
                    "Xóa thất bại: "
                            + r.allOutput().trim());
        }

        if (isInstalledForUser(userId, pkg)) {
            throw new Exception(
                    "Lệnh gỡ chạy xong nhưng "
                    + "package vẫn còn trong profile");
        }

        untrack(pkg, userId);
    }

    // -------- MỞ ỨNG DỤNG --------

    public void launchClone(int userId, String pkg)
            throws Exception {

        validatePackage(pkg);
        requireCorrectUser(userId);

        if (!isInstalledForUser(userId, pkg)) {
            throw new Exception(
                    "App không tồn tại trong clone profile");
        }

        String component = resolveLauncher(userId, pkg);
        if (component == null) throw new Exception("Không tìm thấy launcher trong user " + userId);

        CommandResult r = ShizukuHelper.runCommand(
                "am start --user " + userId
                + " -n " + component
        );

        if (!r.isSuccess()
                || r.allOutput().contains("Error")) {
            throw new Exception(
                    "Mở bản sao thất bại: "
                            + r.allOutput().trim());
        }
    }

    /**
     * Nhãn + icon của app trong đúng profile đó, qua LauncherApps.
     * Trả về map package -> [label, icon]; rỗng nếu profile không thuộc
     * nhóm profile của user hiện tại hoặc đang bị khóa.
     */
    private Map<String, Object[]> launcherEntries(int userId) {
        Map<String, Object[]> map = new HashMap<>();
        try {
            android.content.pm.LauncherApps la = (android.content.pm.LauncherApps)
                    ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (la == null) return map;
            for (android.os.UserHandle handle : la.getProfiles()) {
                if (parseUserHandleId(handle) != userId) continue;
                for (android.content.pm.LauncherActivityInfo info
                        : la.getActivityList(null, handle)) {
                    String pkg = info.getComponentName().getPackageName();
                    if (map.containsKey(pkg)) continue;
                    Drawable icon = null;
                    try { icon = info.getIcon(0); } catch (Exception ignored) {}
                    map.put(pkg, new Object[]{info.getLabel(), icon});
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    /** UserHandle không có API công khai lấy id — parse từ dạng "UserHandle{95}". */
    private int parseUserHandleId(android.os.UserHandle handle) {
        try {
            String s = handle.toString();
            int a = s.indexOf('{');
            int b = s.indexOf('}', a);
            if (a >= 0 && b > a) {
                return Integer.parseInt(s.substring(a + 1, b));
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private String resolveLauncher(int userId, String pkg) throws Exception {
        validatePackage(pkg);
        CommandResult r = ShizukuHelper.runCommand("cmd package resolve-activity --brief --user " + userId
                + " -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p " + pkg);
        if (!r.isSuccess()) throw new Exception("Không đọc được launcher: " + r.allOutput());
        for (String line : r.stdout.split("\\r?\\n")) {
            String component = line.trim();
            if (component.matches("[A-Za-z0-9_.$]+/[A-Za-z0-9_.$]+")) return component;
        }
        return null;
    }

    public void launchOriginal(String pkg, Context c)
            throws Exception {

        Intent li = pm.getLaunchIntentForPackage(pkg);

        if (li == null) {
            throw new Exception(
                    "Không tìm thấy bản gốc");
        }

        li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(li);
    }

    public void launchParallel(
            String pkg, int userId, Context c)
            throws Exception {

        launchOriginal(pkg, c);
        launchClone(userId, pkg);
    }

    // -------- CẤP QUYỀN MEDIA --------

    public void grantMediaPermissions(int userId, String pkg) {
        Set<String> declared =
                getDeclaredPermissions(pkg);

        String[] perms = {
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_AUDIO"
        };

        for (String perm : perms) {
            if (!declared.contains(perm)) continue;

            try {
                CommandResult r =
                        ShizukuHelper.runCommand(
                                "pm grant --user "
                                + userId + " "
                                + pkg + " " + perm
                        );

                if (!r.isSuccess()) {
                    android.util.Log.w(
                            "AppClone",
                            "Không cấp được " + perm
                            + ": " + r.allOutput()
                    );
                }
            } catch (Exception e) {
                android.util.Log.w(
                        "AppClone",
                        "Lỗi cấp quyền " + perm,
                        e
                );
            }
        }
    }

    public int grantAllMediaPermissions(int userId)
            throws Exception {

        List<AppEntry> list = getClones(userId);

        int processed = 0;
        for (AppEntry app : list) {
            // Chỉ xử lý app do AppClone quản lý.
            if (isTracked(app.packageName, userId)) {
                grantMediaPermissions(
                        userId, app.packageName);
                processed++;
            }
        }

        return processed;
    }
}
