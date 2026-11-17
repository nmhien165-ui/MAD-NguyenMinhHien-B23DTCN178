# Lớp học nhí

Đây là bài tập lớn môn Phát triển ứng dụng cho các thiết bị di động của mình. App dùng Java và giao diện XML/Android Views để quản lý học viên và các lớp lập trình Scratch, Python. Dữ liệu lưu trong SQLite trên máy nên có thể dùng khi không có mạng.

## Cách chạy

1. Mở thư mục dự án bằng Android Studio và đợi Gradle Sync xong. Nếu IDE yêu cầu cài SDK, chọn Android SDK 37.
2. Chọn máy ảo hoặc điện thoại chạy Android 7.0 (API 24) trở lên.
3. Chọn cấu hình `app` rồi bấm **Run**.

Lần mở đầu chưa có dữ liệu. Bạn có thể tạo học viên, tạo một khóa rồi vào **Ghi danh** để thêm học viên vào khóa.

## Trong app có gì?

- Thêm, sửa, xóa và tìm học viên hoặc khóa học theo tên, mã.
- Ghi danh hoặc hủy ghi danh. Mỗi học viên chỉ ghi danh một lần trong cùng một khóa.
- Xem danh sách theo khóa hoặc lọc học viên 10–12 tuổi đã ghi danh khóa Python cơ bản.

Phần giao diện nằm ở `MainActivity.java` và `activity_main.xml`. Các thao tác với SQLite và hai truy vấn báo cáo nằm trong `SchoolDatabase.java`.

Nếu muốn chạy test, có thể chạy `.gradlew.bat :app:testDebugUnitTest` trong Terminal của Android Studio. Để chạy các ca SQLite và thao tác giao diện, kết nối thiết bị rồi chạy `.gradlew.bat :app:connectedDebugAndroidTest`.
