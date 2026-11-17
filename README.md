# Lớp học nhí — BTL quản lý lớp học lập trình cho trẻ

Ứng dụng Android dùng **Kotlin + Jetpack Compose + SQLiteOpenHelper**, lưu dữ liệu trên thiết bị và hoạt động không cần mạng.

## Mở và chạy trong Android Studio

1. Mở thư mục `BTL-MAD` bằng Android Studio, đợi Gradle Sync hoàn tất.
2. Chọn điện thoại hoặc emulator Android API 24 trở lên.
3. Chọn cấu hình `app`, nhấn **Run**.

Project hiện dùng compile/target SDK 37, Gradle 9.6.0, Android Gradle Plugin 9.4.1 và daemon JDK 25 theo cấu hình đã có. `local.properties` chứa đường dẫn SDK riêng của máy; cấu hình SDK trong Android Studio nếu mở trên máy khác.

## Chức năng theo đề bài

| Yêu cầu | Cách sử dụng |
| --- | --- |
| Thêm, sửa, xóa học viên | Mục **Học viên**, chọn **+ Thêm mới** hoặc **Sửa / Xóa** trên hồ sơ |
| Thêm, sửa, xóa khóa học | Mục **Khóa học**, chọn **+ Thêm mới** hoặc **Sửa / Xóa** |
| Gán học viên vào khóa | Mục **Ghi danh**, chọn khóa, chọn **+ Gán học viên vào khóa**, chọn học viên rồi lưu |
| Hủy ghi danh | Mục **Ghi danh**, chọn **Hủy ghi danh** trên học viên |
| Liệt kê học viên theo khóa | Mục **Báo cáo**, chọn **Theo khóa học** rồi chọn khóa |
| Tìm học viên 10–12 tuổi học Python cơ bản | Mục **Báo cáo**, chọn **10–12 tuổi · Python cơ bản** |

Tạo học viên và khóa học trước khi ghi danh. Lần chạy đầu không tự thêm dữ liệu mẫu. Xóa cần xác nhận; hủy hộp thoại không thay đổi dữ liệu.

## Quy ước triển khai bổ sung

Các lựa chọn sau bổ sung cho bảng đề bài và có thể điều chỉnh nếu giảng viên có quy định khác:

- Một học viên có thể học nhiều khóa; mỗi cặp học viên–khóa chỉ có một lượt ghi danh.
- Tuổi là số nguyên từ 1 đến 17. Báo cáo lấy cả tuổi **10 và 12**.
- Mã bắt buộc, tối đa 30 ký tự, chỉ dùng chữ không dấu, số, dấu gạch nối hoặc gạch dưới; duy nhất trong từng loại đối tượng, không phân biệt hoa/thường ASCII. Tên bắt buộc, tối đa 100 ký tự và phải có chữ cái. Tên học viên không chấp nhận chữ số. Mã và tên được cắt khoảng trắng đầu/cuối.
- Trình độ Scratch: Chưa học, Cơ bản, Trung cấp, Nâng cao.
- Ngôn ngữ khóa: Scratch hoặc Python. Cấp độ: Cơ bản, Trung cấp, Nâng cao.
- Điều kiện “Python cơ bản” dựa trên hai trường **ngôn ngữ** và **cấp độ**, không dựa vào tên khóa.
- Học viên học nhiều khóa Python cơ bản chỉ xuất hiện một lần trong báo cáo tuổi.
- Xóa học viên/khóa sẽ xóa lượt ghi danh liên quan; xóa khóa không xóa hồ sơ học viên.
- Thống kê “Ghi danh” là số lượt ghi danh, có thể lớn hơn số học viên.

## Thiết kế SQLite

File dữ liệu: `programming_school.db`, schema version 1; SQLite nằm trong vùng lưu trữ riêng của ứng dụng.

```text
students 1 ─── N enrollments N ─── 1 courses

students(id PK, code UNIQUE, name, age, scratch_level)
courses(id PK, code UNIQUE, name, language, level)
enrollments(student_id FK, course_id FK, PK(student_id, course_id))
```

Khóa ngoại bật trong `onConfigure`, dùng `ON DELETE CASCADE`. ID nội bộ không thay đổi khi sửa mã hiển thị. Các truy vấn có tham số, dữ liệu nhập không được ghép vào chuỗi SQL. SQLite chạy trên `Dispatchers.IO`.

### Truy vấn theo khóa

```sql
SELECT s.*
FROM students s
JOIN enrollments e ON e.student_id = s.id
WHERE e.course_id = ?
ORDER BY s.name COLLATE NOCASE, s.id;
```

### Truy vấn tuổi 10–12 trong khóa Python cơ bản

```sql
SELECT DISTINCT s.*
FROM students s
JOIN enrollments e ON e.student_id = s.id
JOIN courses c ON c.id = e.course_id
WHERE s.age BETWEEN 10 AND 12
  AND c.language = ? -- Python
  AND c.level = ?    -- Cơ bản
ORDER BY s.name COLLATE NOCASE, s.id;
```

## Cấu trúc mã nguồn

- `data/Models.kt`: model, danh mục lựa chọn và kiểm tra dữ liệu nhập.
- `data/SchoolDatabase.kt`: schema, CRUD, ghi danh và hai truy vấn báo cáo.
- `ui/SchoolApp.kt`: bốn màn hình, trạng thái, điều phối thao tác SQLite.
- `ui/Editors.kt`: biểu mẫu học viên, khóa học, ghi danh.
- `ui/Components.kt`: thành phần giao diện dùng chung.
- `ui/theme/Theme.kt`: màu giao diện sáng/tối.
- `MainActivity.kt`: điểm vào ứng dụng.

Các đường dẫn trên tương đối với `app/src/main/java/com/example/btl/`.

## Build và kiểm thử

Trong terminal Windows, dùng JDK đi kèm Android Studio nếu cần:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Cần emulator hoặc điện thoại đã kết nối:
.\gradlew.bat :app:connectedDebugAndroidTest
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

Kiểm thử nghiệp vụ dùng database riêng, không sửa database của ứng dụng. Bao gồm validation, CRUD và lưu qua lần mở lại DB, mã trùng, khóa ngoại, ghi danh trùng, hủy ghi danh, xóa cascade, truy vấn tuổi ở các mốc 9/10/11/12/13, loại khóa sai và loại bản ghi trùng. Kiểm thử Compose dùng thao tác chạm để kiểm tra form, thêm/sửa/xóa, tìm kiếm, ghi danh, hủy ghi danh và cả hai báo cáo.

Thư viện kiểm thử Espresso đã nâng lên 3.7.0 và AndroidX Test JUnit lên 1.3.0. Bản Espresso cũ trong project gặp lỗi gọi `InputManager.getInstance` trên emulator hiện tại; AndroidX đã sửa cách lấy InputManager, xem [ghi chú phát hành AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test).

Kết quả lần kiểm tra lại: build APK debug thành công, 7/7 unit test, 12/12 kiểm thử Android trên máy ảo API 37 và mở ứng dụng thành công. Android Lint ghi nhận 0 lỗi và 14 cảnh báo. Chưa kiểm thử trên điện thoại thật.

Báo cáo PTIT: [PDF để in một mặt](docs/Bao-cao-MAD-NguyenMinhHien-B23DTCN178.pdf) · [Bản Word](docs/Bao-cao-MAD-NguyenMinhHien-B23DTCN178.docx).
