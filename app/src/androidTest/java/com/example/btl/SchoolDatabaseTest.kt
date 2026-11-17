package com.example.btl

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.btl.data.SchoolDatabase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SchoolDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "school-test.db"
    private lateinit var db: SchoolDatabase

    @Before fun setUp() { context.deleteDatabase(dbName); db = SchoolDatabase(context, dbName) }
    @After fun tearDown() { db.close(); context.deleteDatabase(dbName) }

    private fun student(code: String, age: Int = 10): Long {
        db.saveStudent(null, code, "Học viên $code", age.toString(), "Cơ bản")
        return db.snapshot().students.first { it.code == code }.id
    }
    private fun course(code: String, language: String = "Python", level: String = "Cơ bản"): Long {
        db.saveCourse(null, code, "Khóa $code", language, level)
        return db.snapshot().courses.first { it.code == code }.id
    }
    private fun constraint(action: () -> Unit) {
        try { action(); fail("Expected SQLite constraint") } catch (_: SQLiteConstraintException) { }
    }

    @Test fun crudPersistsAcrossReopenAndEditingPreservesEnrollment() {
        val s = student("HV01"); val c = course("PY01"); db.enroll(s, c)
        db.saveStudent(s, "HV02", "Nguyễn An", "12", "Nâng cao")
        db.saveCourse(c, "PY02", "Python cho trẻ", "Python", "Trung cấp")
        db.close(); db = SchoolDatabase(context, dbName)
        val snapshot = db.snapshot()
        assertEquals("Nguyễn An", snapshot.students.single().name)
        assertEquals(12, snapshot.students.single().age)
        assertEquals("Nâng cao", snapshot.students.single().scratchLevel)
        assertEquals("HV02", snapshot.students.single().code)
        assertEquals("PY02", snapshot.courses.single().code)
        assertEquals("Trung cấp", snapshot.courses.single().level)
        assertEquals(s, db.studentsInCourse(c).single().id)
        assertTrue(db.pythonBasicStudents().isEmpty())
    }

    @Test fun duplicateCodesAreRejectedWithoutOverwritingOriginal() {
        val s = student("HV01"); course("PY01")
        constraint { db.saveStudent(null, "hv01", "Khác", "11", "Chưa học") }
        constraint { db.saveCourse(null, "py01", "Khác", "Scratch", "Nâng cao") }
        assertEquals(s, db.snapshot().students.single().id)
        assertEquals("Khóa PY01", db.snapshot().courses.single().name)
    }

    @Test fun duplicateEnrollmentAndMissingForeignKeysAreRejected() {
        val s = student("HV01"); val c = course("PY01"); db.enroll(s, c)
        constraint { db.enroll(s, c) }
        constraint { db.enroll(9999, c) }
        constraint { db.enroll(s, 9999) }
        assertEquals(1, db.snapshot().enrollments.size)
    }

    @Test fun courseReportIsScopedAndUnenrollmentPreservesStudent() {
        val a = student("A"); val b = student("B"); val c = course("PY01"); val d = course("SC01", "Scratch")
        db.enroll(a, c); db.enroll(b, d)
        assertEquals(listOf(a), db.studentsInCourse(c).map { it.id })
        db.unenroll(a, c)
        assertTrue(db.studentsInCourse(c).isEmpty())
        assertEquals(2, db.snapshot().students.size)
        assertEquals(1, db.snapshot().enrollments.size)
    }

    @Test fun pythonReportIncludesBothAgeLimitsAndDeduplicatesAcrossCourses() {
        val c = course("PY01"); val d = course("PY02")
        val expected = (9..13).map { age -> student("HV$age", age).also { db.enroll(it, c); db.enroll(it, d) } }
            .slice(1..3).toSet()
        assertEquals(expected, db.pythonBasicStudents().map { it.id }.toSet())
        assertEquals(3, db.pythonBasicStudents().size)
    }

    @Test fun pythonReportExcludesScratchAdvancedAndNotEnrolled() {
        val c = course("SC", "Scratch"); val d = course("PY", "Python", "Nâng cao")
        db.enroll(student("Scratch"), c); db.enroll(student("Advanced"), d); student("NotEnrolled")
        assertTrue(db.pythonBasicStudents().isEmpty())
    }

    @Test fun deleteCourseCascadesOnlyItsEnrollments() {
        val s = student("HV01"); val c = course("PY01"); val d = course("PY02")
        db.enroll(s, c); db.enroll(s, d); db.deleteCourse(c)
        assertEquals(1, db.snapshot().students.size)
        assertEquals(d, db.snapshot().enrollments.single().courseId)
    }

    @Test fun deleteStudentCascadesOnlyItsEnrollments() {
        val a = student("A"); val b = student("B"); val c = course("PY01")
        db.enroll(a, c); db.enroll(b, c); db.deleteStudent(a)
        assertEquals(1, db.snapshot().courses.size)
        assertEquals(b, db.snapshot().enrollments.single().studentId)
        assertEquals(b, db.studentsInCourse(c).single().id)
    }
}
