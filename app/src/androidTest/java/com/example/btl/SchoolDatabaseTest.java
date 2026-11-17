package com.example.btl;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.btl.data.Models.Student;
import com.example.btl.data.SchoolDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SchoolDatabaseTest {
    private static final String DB = "java-db-test.db";
    private Context context;
    private SchoolDatabase db;

    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DB);
        db = new SchoolDatabase(context, DB);
    }

    @After public void teardown() {
        db.close();
        context.deleteDatabase(DB);
    }

    private long student(String code, int age) {
        db.saveStudent(null, code, "Nguyễn An", Integer.toString(age), "Cơ bản");
        return db.snapshot().students.stream().filter(s -> s.code.equals(code)).findFirst().get().id;
    }

    private long course(String code, String language, String level) {
        db.saveCourse(null, code, "Khóa " + code, language, level);
        return db.snapshot().courses.stream().filter(c -> c.code.equals(code)).findFirst().get().id;
    }

    @Test public void databasePersistsEditsAndEnrollmentAfterReopen() {
        long student = student("HV1", 11), course = course("KH1", "Python", "Cơ bản");
        db.enroll(student, course);
        db.saveStudent(student, "HV2", "Nguyễn Ánh", "12", "Nâng cao");
        db.saveCourse(course, "KH2", "Python nhập môn", "Python", "Cơ bản");
        db.close();
        db = new SchoolDatabase(context, DB);
        assertEquals("HV2", db.snapshot().students.get(0).code);
        assertEquals("KH2", db.snapshot().courses.get(0).code);
        assertEquals(student, db.studentsInCourse(course).get(0).id);
        assertEquals(12, db.pythonBasicStudents().get(0).age);
    }

    @Test public void duplicateCodesAreCaseInsensitive() {
        student("HV1", 11);
        course("KH1", "Scratch", "Cơ bản");
        assertThrows(SQLiteConstraintException.class, () -> student("hv1", 12));
        assertThrows(SQLiteConstraintException.class, () -> course("kh1", "Python", "Cơ bản"));
        assertEquals(1, db.snapshot().students.size());
        assertEquals(1, db.snapshot().courses.size());
    }

    @Test public void enrollmentRejectsDuplicatesAndMissingForeignKeys() {
        long s = student("HV1", 11), c = course("KH1", "Python", "Cơ bản");
        db.enroll(s, c);
        assertThrows(SQLiteConstraintException.class, () -> db.enroll(s, c));
        assertThrows(SQLiteConstraintException.class, () -> db.enroll(999, c));
        assertThrows(SQLiteConstraintException.class, () -> db.enroll(s, 999));
        assertEquals(1, db.snapshot().enrollments.size());
    }

    @Test public void courseReportsAndUnenrollmentAreScoped() {
        long a = student("HV1", 10), b = student("HV2", 11);
        long c = course("KH1", "Python", "Cơ bản"), other = course("KH2", "Python", "Cơ bản");
        db.enroll(a, c);
        db.enroll(b, other);
        assertEquals(a, db.studentsInCourse(c).get(0).id);
        assertEquals(1, db.studentsInCourse(c).size());
        db.unenroll(a, c);
        assertTrue(db.studentsInCourse(c).isEmpty());
        assertEquals(1, db.studentsInCourse(other).size());
        assertEquals(2, db.snapshot().students.size());
    }

    @Test public void pythonReportIncludesBoundariesAndRemovesDuplicates() {
        long p = course("P", "Python", "Cơ bản"), q = course("Q", "Python", "Cơ bản");
        for (int age : new int[]{9, 10, 11, 12, 13}) {
            long s = student("HV" + age, age);
            db.enroll(s, p);
            db.enroll(s, q);
        }
        List<Integer> ages = db.pythonBasicStudents().stream().map(s -> s.age).sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList(10, 11, 12), ages);
    }

    @Test public void pythonReportExcludesWrongLanguageLevelAndUnenrolled() {
        db.enroll(student("HV1", 11), course("KH1", "Scratch", "Cơ bản"));
        db.enroll(student("HV2", 11), course("KH2", "Python", "Trung cấp"));
        student("HV3", 11);
        assertTrue(db.pythonBasicStudents().isEmpty());
    }

    @Test public void deletingParentsCascadesOnlyRelatedEnrollments() {
        long s = student("HV1", 11), other = student("HV2", 12);
        long a = course("KH1", "Python", "Cơ bản"), b = course("KH2", "Scratch", "Cơ bản");
        db.enroll(s, a);
        db.enroll(s, b);
        db.enroll(other, a);
        db.deleteCourse(b);
        assertEquals(2, db.snapshot().enrollments.size());
        assertEquals(2, db.snapshot().students.size());
        db.deleteStudent(s);
        assertEquals(1, db.snapshot().enrollments.size());
        assertEquals(other, db.studentsInCourse(a).get(0).id);
    }

    @Test public void javaImplementationReadsAnExistingVersionOneFile() {
        db.close();
        try (SQLiteDatabase old = context.openOrCreateDatabase(DB, Context.MODE_PRIVATE, null)) {
            old.execSQL("CREATE TABLE students(id INTEGER PRIMARY KEY, code TEXT, name TEXT, age INTEGER, scratch_level TEXT)");
            old.execSQL("CREATE TABLE courses(id INTEGER PRIMARY KEY, code TEXT, name TEXT, language TEXT, level TEXT)");
            old.execSQL("CREATE TABLE enrollments(student_id INTEGER, course_id INTEGER)");
            old.execSQL("INSERT INTO students VALUES (7, 'OLD', 'Nguyễn An', 10, 'Cơ bản')");
            old.execSQL("INSERT INTO courses VALUES (8, 'PY', 'Python nhập môn', 'Python', 'Cơ bản')");
            old.execSQL("INSERT INTO enrollments VALUES (7, 8)");
            old.setVersion(1);
        }
        db = new SchoolDatabase(context, DB);
        Student stored = db.pythonBasicStudents().get(0);
        assertEquals(7, stored.id);
        assertEquals("OLD", stored.code);
        assertEquals(1, db.snapshot().enrollments.size());
        assertEquals(1, db.getReadableDatabase().getVersion());
    }
}
