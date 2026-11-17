package com.example.btl;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.btl.data.SchoolDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SchoolUiTest {
    private static final String TEST_DB = "java-ui-test.db";
    private Instrumentation instrumentation;
    private Context context;
    private SchoolDatabase db;
    private ActivityScenario<MainActivity> activity;

    @Before public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        context.deleteDatabase(TEST_DB);
        db = new SchoolDatabase(context, TEST_DB);
        MainActivity.testDatabase = db;
    }

    @After public void teardown() {
        if (activity != null) activity.close();
        MainActivity.testDatabase = null;
        db.close();
        context.deleteDatabase(TEST_DB);
    }

    private void open() { activity = ActivityScenario.launch(MainActivity.class); }

    private void tap(String text) {
        onView(allOf(withText(text), isAssignableFrom(Button.class), isDisplayed())).perform(click());
    }

    private void tab(int index) {
        onView(allOf(isAssignableFrom(Button.class), withParent(withId(R.id.tabs)),
                withText(new String[]{"Học viên", "Khóa học", "Ghi danh", "Báo cáo"}[index]))).perform(click());
    }

    private void enter(String hint, String value) {
        onView(withHint(hint)).perform(replaceText(value));
        androidx.test.espresso.Espresso.closeSoftKeyboard();
    }

    private void choice(String description, String value) {
        onView(allOf(withContentDescription(description), isAssignableFrom(android.widget.Spinner.class)))
                .perform(click());
        onView(withText(value)).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click());
    }

    private void waitFor(String text) {
        long deadline = SystemClock.uptimeMillis() + 8000;
        do {
            try {
                onView(allOf(withText(text), isDisplayed())).check(matches(isDisplayed()));
                return;
            } catch (androidx.test.espresso.NoMatchingViewException | AssertionError missing) {
                if (SystemClock.uptimeMillis() >= deadline) throw missing;
                SystemClock.sleep(100);
            }
        } while (true);
    }

    private void screenshot(String name) throws IOException {
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        try (FileOutputStream stream = new FileOutputStream(new File(context.getExternalFilesDir(null), name))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally {
            bitmap.recycle();
        }
    }

    @Test public void studentValidationDuplicateEditSearchAndDelete() throws IOException {
        open();
        waitFor("Chưa có học viên");
        tap("+ Thêm mới");
        enter("Mã học viên", "HV 01");
        enter("Tên học viên", "12345");
        enter("Tuổi (1–17)", "18");
        tap("Lưu học viên");
        waitFor("Mã chỉ dùng chữ không dấu, số, dấu gạch nối hoặc gạch dưới.");
        waitFor("Tên học viên phải có chữ cái.");
        waitFor("Tuổi học viên phải là số nguyên từ 1 đến 17.");
        screenshot("java-validation.png");
        assertTrue(db.snapshot().students.isEmpty());

        enter("Mã học viên", "HV01");
        enter("Tên học viên", "Nguyễn An");
        enter("Tuổi (1–17)", "11");
        tap("Lưu học viên");
        waitFor("Nguyễn An");
        screenshot("java-students.png");
        tap("+ Thêm mới");
        enter("Mã học viên", "hv01");
        enter("Tên học viên", "Nguyễn Minh");
        enter("Tuổi (1–17)", "12");
        tap("Lưu học viên");
        waitFor("Mã học viên đã tồn tại. Hãy nhập mã khác.");
        tap("Hủy");
        tap("Sửa");
        enter("Tên học viên", "Nguyễn Ánh");
        tap("Lưu học viên");
        waitFor("Nguyễn Ánh");
        enter("Tìm tên hoặc mã học viên", "absent");
        waitFor("Không tìm thấy học viên");
        enter("Tìm tên hoặc mã học viên", "hv01");
        waitFor("Nguyễn Ánh");
        tap("Xóa");
        tap("Hủy");
        waitFor("Nguyễn Ánh");
        tap("Xóa");
        tap("Xác nhận");
        waitFor("Chưa có học viên");
        assertTrue(db.snapshot().students.isEmpty());
    }

    @Test public void courseValidationDuplicateEditAndDelete() {
        open();
        tab(1);
        waitFor("Chưa có khóa học");
        tap("+ Thêm mới");
        enter("Mã khóa học", "PY01");
        enter("Tên khóa học", "12345");
        tap("Lưu khóa học");
        waitFor("Tên khóa học phải có chữ cái.");
        assertTrue(db.snapshot().courses.isEmpty());
        enter("Tên khóa học", "Python 101");
        choice("Ngôn ngữ", "Python");
        tap("Lưu khóa học");
        waitFor("Python 101");
        tap("+ Thêm mới");
        enter("Mã khóa học", "py01");
        enter("Tên khóa học", "Python khác");
        tap("Lưu khóa học");
        waitFor("Mã khóa học đã tồn tại. Hãy nhập mã khác.");
        tap("Hủy");
        tap("Sửa");
        enter("Tên khóa học", "Python nhập môn");
        tap("Lưu khóa học");
        waitFor("Python nhập môn");
        tap("Xóa");
        tap("Xác nhận");
        waitFor("Chưa có khóa học");
        tab(2);
        waitFor("Cần có khóa học");
    }

    @Test public void enrollmentAndBothReportsUpdateAfterRemoval() throws IOException {
        db.saveStudent(null, "HV01", "Nguyễn An", "11", "Chưa học");
        db.saveStudent(null, "HV02", "Nguyễn Minh", "13", "Cơ bản");
        db.saveCourse(null, "PY01", "Python nhập môn", "Python", "Cơ bản");
        open();
        waitFor("Nguyễn An");
        tab(1);
        onView(allOf(withText("Ghi danh"), isAssignableFrom(Button.class), not(withParent(withId(R.id.tabs))))).perform(click());
        tap("+ Gán học viên vào khóa");
        choice("Chọn học viên", "HV01 · Nguyễn An");
        tap("Gán vào khóa");
        waitFor("Danh sách ghi danh (1)");
        screenshot("java-enrollments.png");
        tap("+ Gán học viên vào khóa");
        choice("Chọn học viên", "HV02 · Nguyễn Minh");
        tap("Gán vào khóa");
        waitFor("Danh sách ghi danh (2)");
        tab(3);
        waitFor("Kết quả: 2 học viên");
        tap("10–12 tuổi · Python cơ bản");
        waitFor("Kết quả: 1 học viên");
        waitFor("Nguyễn An");
        screenshot("java-report.png");
        tab(2);
        long student = db.snapshot().students.stream().filter(s -> s.code.equals("HV01")).findFirst().get().id;
        onView(allOf(withText("Hủy ghi danh"), withParent(withParent(hasDescendant(withText("Nguyễn An"))))))
                .perform(scrollTo(), click());
        tap("Xác nhận");
        waitFor("Danh sách ghi danh (1)");
        assertNotNull(db.snapshot().students.stream().filter(s -> s.id == student).findFirst().orElse(null));
        tab(3);
        waitFor("Kết quả: 0 học viên");
        tap("Theo khóa học");
        waitFor("Kết quả: 1 học viên");
        waitFor("Nguyễn Minh");
    }
}
