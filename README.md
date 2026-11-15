<p align="center"><img src="docs/icon.svg" width="120" alt="Biểu tượng Báo Tam Nương"></p>

# Báo Tam Nương

Ứng dụng Android báo thức vào **ngày Tam Nương** (mùng 3, 7, 13, 18, 22, 27 âm lịch), tính theo **âm lịch Việt Nam (UTC+7)**. Chuông dùng bộ hẹn giờ báo thức của hệ thống, nên **vẫn kêu khi tắt màn hình, khoá máy hoặc sau khi khởi động lại máy**.

## Tải về

**Link tải thẳng (Android bấm vào là tải):**

https://github.com/vin371/TamNuong-App/releases/latest/download/BaoTamNuong.apk

Cài đặt:
1. Mở file `BaoTamNuong.apk` vừa tải.
2. Cho phép **"Cài từ nguồn này"**. Nếu Play Protect cảnh báo, chọn **"Vẫn cài đặt"**.
3. Mở ứng dụng, cho phép thông báo, rồi bấm từng nút trong mục **"Cài đặt để chuông kêu chắc chắn"** cho đến khi đều ✅.
4. Bấm **"Báo thử sau 10 giây"** và khoá màn hình để kiểm tra.

Máy Xiaomi, Oppo, Vivo, Realme: vào Cài đặt › Ứng dụng › Báo Tam Nương, bật **Tự khởi động** và chọn pin **Không giới hạn**.

## Tính năng

| Tính năng | Chi tiết |
|---|---|
| Báo 7:00 | Vào ngày Tam Nương |
| Báo 17:00 | Vào ngày Tam Nương và chiều hôm trước ngày Tam Nương ("Ngày mai là ngày Tam Nương") |
| Chế độ test | Công tắc bật/tắt, báo mỗi 2 phút, có đếm ngược |
| Báo thử sau 10 giây | Để thử khi màn hình tắt |
| Màn hình báo thức | Tự bật sáng, hiện đè màn hình khoá, chuông kêu lặp đến khi bấm **Tắt chuông** |
| Tờ lịch hôm nay | Dương lịch, âm lịch, 10 ngày Tam Nương sắp tới |
| Không cần mạng | Toàn bộ tính toán trên máy |

Mỗi bản phát hành có kèm file lịch `Ngay_Tam_Nuong_7h_17h.ics` (5 năm tới) để nhập vào Google Calendar hoặc lịch iPhone. Tự tạo: `python3 tools/tao_lich_ics.py lich.ics 5`.

## CI/CD (GitHub Actions)

Workflow [`.github/workflows/android.yml`](.github/workflows/android.yml):

| Khi nào | Việc chạy |
|---|---|
| Mỗi lần push lên `main` hoặc mở pull request | Unit test âm lịch và giờ báo → lint → dựng APK release → kiểm tra chữ ký → lưu APK ở mục **Artifacts** của lần chạy |
| Push tag `v*` (ví dụ `v1.0.1`) | Như trên, rồi **tạo GitHub Release** kèm `BaoTamNuong.apk`, mã SHA-256 và file lịch `.ics` do CI tự tạo. Link tải thẳng ở trên luôn trỏ tới bản mới nhất |
| Hằng tháng | Dependabot đề xuất cập nhật các action và thư viện |

Phát hành bản mới:

```bash
git tag v1.0.1
git push origin v1.0.1
```

### Cài secrets (bắt buộc để phát hành)

Vào **Settings › Secrets and variables › Actions › New repository secret** và tạo 4 secret:

| Tên | Giá trị |
|---|---|
| `KEYSTORE_BASE64` | Nội dung file khoá `.jks` mã hoá base64 (`base64 -w0 release.jks`) |
| `KEYSTORE_PASSWORD` | Mật khẩu kho khoá |
| `KEY_ALIAS` | Bí danh khoá |
| `KEY_PASSWORD` | Mật khẩu khoá |

Mọi bản phát hành phải ký cùng một khoá thì điện thoại mới cập nhật đè lên được. Không đưa file `.jks` lên repo.

## Dựng trên máy

Mở thư mục này bằng **Android Studio** rồi chạy, hoặc dùng Gradle 8.9 với JDK 17:

```bash
gradle :app:testDebugUnitTest   # chạy test
gradle :app:assembleRelease     # APK ở app/build/outputs/apk/release/
```

## Cấu trúc

```
app/src/main/java/vn/tamnuong/baothuc/
  Lunar.java          Âm lịch Việt Nam (thuật toán Hồ Ngọc Đức) và lịch báo
  Sched.java          Hẹn giờ AlarmManager.setAlarmClock, kênh thông báo, đổ chuông
  AlarmReceiver.java  Nhận báo thức, đổ chuông, hẹn lần kế tiếp
  BootReceiver.java   Hẹn lại sau khởi động lại, cập nhật, đổi giờ
  AlarmActivity.java  Màn hình báo thức đè màn hình khoá
  MainActivity.java   Màn hình chính
tools/tao_lich_ics.py Tạo file lịch .ics
app/src/test/...      Unit test (Tết 2026/2027/2030, tháng nhuận, 307 ngày Tam Nương, giờ báo)
```
