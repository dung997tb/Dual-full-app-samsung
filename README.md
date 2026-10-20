# AppClone — App nhân bản ứng dụng

Nhân bản app để chạy 2 tài khoản song song, dùng Shizuku (quyền ADB, không cần root).
Mở full tính năng miễn phí, không giới hạn số app.

## Tác giả

- **Đào Mạnh Dũng** — người tạo
- **Muse AI** — đồng tạo

## Tải app

- **[Tải APK mới nhất](https://github.com/dung997tb/Dual-full-app-samsung/releases/latest/download/app-debug.apk)** — bản build tự động mới nhất từ GitHub Releases
- [Xem tất cả các bản phát hành](https://github.com/dung997tb/Dual-full-app-samsung/releases)

> APK là bản debug (ký bằng debug key) nên Android/Play Protect có thể hiện cảnh báo khi cài — bấm vẫn cài tiếp là được.

## Tính năng

- Tab **Bản sao**: thẻ cảnh báo Shizuku, danh sách app đã nhân bản
  (mở bản sao / chạy song song / xóa), nút **+ Nhân bản**
- Tab **Thêm mới**: liệt kê app trên máy, bấm **Nhân bản**
- Tab **Cài đặt**: thẻ thương hiệu, hướng dẫn kết nối Shizuku (bottom sheet
  4 bước), trạng thái quyền, khôi phục quyền ảnh/file/camera,
  giới thiệu công nghệ
- Dialog hướng dẫn Shizuku: 4 bước + nút **Mở Cài đặt** / **Mở Shizuku**

## Thiết kế: macOS + Ubuntu, dark theme

- Thanh tiêu đề kiểu cửa sổ macOS: đèn giao thông đỏ/vàng/xanh + đèn trạng thái Shizuku
- Header tối + nút **cam Ubuntu #E95420**, chữ trắng/xám
- Thẻ card bo góc lớn, bottom navigation 3 tab

## Cơ chế

| Việc | Lệnh chạy qua Shizuku |
|---|---|
| Tìm user | `pm list users` |
| Tạo user | `pm create-user --profileOf 0 --user-type android.os.usertype.profile.CLONE "AppClone"` (thử `--type` nếu máy cũ) |
| Nhân bản | `pm install-existing --user <id> <package>` |
| Mở bản sao | `am start --user <id> -n <package>/<activity>` |
| Chạy song song | mở bản gốc thường + mở bản sao |
| Xóa bản sao | `pm uninstall --user <id> <package>` |
| Cấp quyền ảnh/file | `appops set --user <id> <package> CAMERA/RECORD_AUDIO/READ_MEDIA_* allow` |

## Yêu cầu trước khi dùng

1. Cài app **Shizuku** (github.com/RikkaApps/Shizuku)
2. Mở Shizuku → Start qua **Wireless debugging** (ghép nối 1 lần, không cần máy tính)
3. Mở AppClone → bấm **Cấp quyền** → cho phép

## Build trên điện thoại (Termux + RTK toolchain)

```
source /path/to/RTK_ANDROID_TOOLCHAIN/env.sh
cd appclone
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

> Font Inter được nhúng sẵn trong repo (dạng base64 ở `app/fonts-src/`),
> Gradle tự giải mã khi build — không cần làm gì thêm.

## Build trên máy tính

Mở thư mục `appclone` bằng Android Studio → Sync → Run.
Hoặc cài Gradle rồi chạy: `gradle assembleDebug`

> Repo không kèm `gradle-wrapper.jar` (file binary), nên `./gradlew`
> sẽ không chạy được từ bản clone nguyên trạng — dùng `gradle` hệ thống.

## Lưu ý trung thực

- **Chưa kiểm chứng trên máy thật**: toàn bộ mới ở mức code review.
  Trên Samsung cần test thực tế.
- **Rủi ro Samsung dọn app**: có ý kiến cho rằng One UI 8+ (Dual App Agent)
  có thể gỡ app cài lén vào Dual Messenger sau reboot — chưa có nguồn kiểm
  chứng độc lập. Đừng nhân bản tài khoản quan trọng cho đến khi mày tự test.
- **"Chạy song song"** chỉ là mở bản gốc + bản sao cùng lúc, không phải
  chia đôi màn hình (Android không có API công khai để ép split-screen).
- Quyền `QUERY_ALL_PACKAGES` chỉ để liệt kê app; nếu sau này đăng Play Store
  thì Google yêu cầu khai báo lý do sử dụng.

## Cấu hình

- compileSdk 35, minSdk 26, targetSdk 35
- AGP 8.7.3 + Gradle 8.10.2, Java 17
- Shizuku API 13.1.5 (`dev.rikka.shizuku:api` + `:provider`)
