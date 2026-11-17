package com.example.btl

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.btl.data.SchoolDatabase
import com.example.btl.ui.SchoolApp
import com.example.btl.ui.theme.BTLTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SchoolUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: SchoolDatabase
    @Before fun setUp() {
        context.deleteDatabase("school-ui-test.db")
        db = SchoolDatabase(context, "school-ui-test.db")
        compose.setContent { BTLTheme { SchoolApp(db) } }
        waitFor("Chưa có học viên")
    }
    @After fun tearDown() { db.close(); context.deleteDatabase("school-ui-test.db") }
    private fun waitFor(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun click(text: String) {
        compose.onNode(hasText(text) and hasClickAction()).performTouchInput { click() }
    }
    private fun replace(label: String, value: String) {
        compose.onNode(hasText(label) and hasSetTextAction()).performTextReplacement(value)
    }
    private fun screenshot(name: String, dialog: Boolean = false) {
        compose.waitForIdle()
        val image = if (dialog) {
            requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        } else compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun addStudentCourseEnrollAndQueryFromUi() {
        click("+ Thêm mới")
        click("Lưu học viên")
        compose.onNodeWithText("Vui lòng nhập mã học viên.").assertExists()
        compose.onNodeWithText("Mã học viên").performTextInput("HV01")
        compose.onNodeWithText("Tên học viên").performTextInput("Nguyễn An")
        compose.onNodeWithText("Tuổi (1–17)").performTextInput("11")
        click("Lưu học viên")
        waitFor("Nguyễn An")
        screenshot("qa-students.png")
        click("Khóa học")
        waitFor("Chưa có khóa học")
        click("+ Thêm mới")
        compose.onNodeWithText("Mã khóa học").performTextInput("PY01")
        compose.onNodeWithText("Tên khóa học").performTextInput("Python nhập môn")
        click("Scratch")
        click("Python")
        click("Lưu khóa học")
        waitFor("Python nhập môn")
        screenshot("qa-courses.png")
        compose.onAllNodesWithText("Ghi danh").filter(hasClickAction()).onFirst().performTouchInput { click() }
        waitFor("+ Gán học viên vào khóa")
        click("+ Gán học viên vào khóa")
        click("Chọn Học viên")
        click("HV01 · Nguyễn An")
        click("Gán vào khóa")
        waitFor("Danh sách ghi danh (1)")
        screenshot("qa-enrollments.png")
        click("Báo cáo")
        waitFor("Kết quả: 1 học viên")
        click("10–12 tuổi · Python cơ bản")
        waitFor("Kết quả: 1 học viên")
        compose.onNodeWithText("Nguyễn An").assertExists()
        screenshot("qa-report.png")

        click("Ghi danh")
        click("Hủy ghi danh")
        click("Xác nhận")
        waitFor("Danh sách ghi danh (0)")
        click("Báo cáo")
        waitFor("Kết quả: 0 học viên")
        click("Theo khóa học")
        waitFor("Khóa học")
        waitFor("Kết quả: 0 học viên")
    }

    @Test fun studentValidationDuplicateEditSearchAndDelete() {
        click("+ Thêm mới")
        replace("Mã học viên", "HV 01")
        replace("Tên học viên", "234567")
        replace("Tuổi (1–17)", "18")
        click("Lưu học viên")
        compose.onNodeWithText("Mã chỉ dùng chữ không dấu, số, dấu gạch nối hoặc gạch dưới.").assertIsDisplayed()
        compose.onNodeWithText("Tên học viên phải có chữ cái.").assertIsDisplayed()
        compose.onNodeWithText("Tuổi học viên phải là số nguyên từ 1 đến 17.").assertIsDisplayed()
        screenshot("qa-validation.png", dialog = true)
        org.junit.Assert.assertTrue(db.snapshot().students.isEmpty())

        replace("Mã học viên", "HV01")
        replace("Tên học viên", "Nguyễn An")
        replace("Tuổi (1–17)", "11")
        click("Lưu học viên")
        waitFor("Nguyễn An")
        click("+ Thêm mới")
        replace("Mã học viên", "hv01")
        replace("Tên học viên", "Nguyễn Bình")
        replace("Tuổi (1–17)", "12")
        click("Lưu học viên")
        waitFor("Mã học viên đã tồn tại. Hãy nhập mã khác.")
        click("Hủy")
        click("Sửa")
        replace("Tên học viên", "Nguyễn Minh An")
        click("Lưu học viên")
        waitFor("Nguyễn Minh An")
        replace("Tìm tên hoặc mã học viên", "absent")
        waitFor("Không tìm thấy học viên")
        replace("Tìm tên hoặc mã học viên", "hv01")
        waitFor("Nguyễn Minh An")
        click("Xóa")
        click("Hủy")
        compose.onNodeWithText("Nguyễn Minh An").assertExists()
        click("Xóa")
        click("Xác nhận")
        waitFor("Chưa có học viên")
        org.junit.Assert.assertTrue(db.snapshot().students.isEmpty())
    }

    @Test fun courseValidationDuplicateEditAndDelete() {
        click("Khóa học")
        waitFor("Chưa có khóa học")
        click("+ Thêm mới")
        replace("Mã khóa học", "PY01")
        replace("Tên khóa học", "12345")
        click("Lưu khóa học")
        compose.onNodeWithText("Tên khóa học phải có chữ cái.").assertIsDisplayed()
        org.junit.Assert.assertTrue(db.snapshot().courses.isEmpty())
        replace("Tên khóa học", "Python 101")
        click("Scratch")
        click("Python")
        click("Lưu khóa học")
        waitFor("Python 101")
        click("+ Thêm mới")
        replace("Mã khóa học", "py01")
        replace("Tên khóa học", "Python nâng cao")
        click("Lưu khóa học")
        waitFor("Mã khóa học đã tồn tại. Hãy nhập mã khác.")
        click("Hủy")
        click("Sửa")
        replace("Tên khóa học", "Python nhập môn")
        click("Lưu khóa học")
        waitFor("Python nhập môn")
        click("Xóa")
        click("Xác nhận")
        waitFor("Chưa có khóa học")
        click("Ghi danh")
        waitFor("Cần có khóa học")
    }
}
