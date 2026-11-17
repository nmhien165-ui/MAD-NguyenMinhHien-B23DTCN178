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
    private fun click(text: String) { compose.onNodeWithText(text).performClick() }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
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
        compose.onAllNodesWithText("Khóa học").filter(hasClickAction()).onFirst().performClick()
        waitFor("Chưa có khóa học")
        click("+ Thêm mới")
        compose.onNodeWithText("Mã khóa học").performTextInput("PY01")
        compose.onNodeWithText("Tên khóa học").performTextInput("Python nhập môn")
        click("Scratch")
        click("Python")
        click("Lưu khóa học")
        waitFor("Python nhập môn")
        screenshot("qa-courses.png")
        compose.onAllNodesWithText("Ghi danh").filter(hasClickAction()).onFirst().performClick()
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
    }
}
