package com.example.btl.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** All calls are made on Dispatchers.IO. IDs remain stable when user-facing codes change. */
class SchoolDatabase(context: Context, name: String = "programming_school.db") :
    SQLiteOpenHelper(context.applicationContext, name, null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(code)) BETWEEN 1 AND 30),
                name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 100),
                age INTEGER NOT NULL CHECK(age BETWEEN 1 AND 17),
                scratch_level TEXT NOT NULL CHECK(scratch_level IN ('Chưa học','Cơ bản','Trung cấp','Nâng cao'))
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE courses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(code)) BETWEEN 1 AND 30),
                name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 100),
                language TEXT NOT NULL CHECK(language IN ('Scratch','Python')),
                level TEXT NOT NULL CHECK(level IN ('Cơ bản','Trung cấp','Nâng cao'))
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE enrollments (
                student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE,
                course_id INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
                PRIMARY KEY(student_id, course_id)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX enrollments_course_idx ON enrollments(course_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add non-destructive migrations here when the schema version increases.
        error("Chưa có migration từ $oldVersion lên $newVersion")
    }

    fun snapshot(): SchoolData {
        val db = readableDatabase
        db.beginTransaction()
        try {
            val students = db.rawQuery("SELECT * FROM students ORDER BY name COLLATE NOCASE, id", null)
                .use { c -> buildList { while (c.moveToNext()) add(c.student()) } }
            val courses = db.rawQuery("SELECT * FROM courses ORDER BY name COLLATE NOCASE, id", null)
                .use { c -> buildList { while (c.moveToNext()) add(c.course()) } }
            val enrollments = db.rawQuery("SELECT student_id, course_id FROM enrollments", null)
                .use { c -> buildList { while (c.moveToNext()) add(Enrollment(c.getLong(0), c.getLong(1))) } }
            db.setTransactionSuccessful()
            return SchoolData(students, courses, enrollments)
        } finally {
            db.endTransaction()
        }
    }

    fun saveStudent(id: Long?, code: String, name: String, age: String, level: String) {
        require(Validation.student(code, name, age, level) == null)
        val values = ContentValues().apply {
            put("code", code.trim()); put("name", name.trim())
            put("age", age.trim().toInt()); put("scratch_level", level)
        }
        if (id == null) writableDatabase.insertOrThrow("students", null, values)
        else check(writableDatabase.update("students", values, "id = ?", arrayOf(id.toString())) == 1)
    }

    fun saveCourse(id: Long?, code: String, name: String, language: String, level: String) {
        require(Validation.course(code, name, language, level) == null)
        val values = ContentValues().apply {
            put("code", code.trim()); put("name", name.trim())
            put("language", language); put("level", level)
        }
        if (id == null) writableDatabase.insertOrThrow("courses", null, values)
        else check(writableDatabase.update("courses", values, "id = ?", arrayOf(id.toString())) == 1)
    }

    fun deleteStudent(id: Long) {
        check(writableDatabase.delete("students", "id = ?", arrayOf(id.toString())) == 1)
    }

    fun deleteCourse(id: Long) {
        check(writableDatabase.delete("courses", "id = ?", arrayOf(id.toString())) == 1)
    }

    fun enroll(studentId: Long, courseId: Long) {
        writableDatabase.insertOrThrow("enrollments", null, ContentValues().apply {
            put("student_id", studentId); put("course_id", courseId)
        })
    }

    fun unenroll(studentId: Long, courseId: Long) {
        check(writableDatabase.delete("enrollments", "student_id = ? AND course_id = ?",
            arrayOf(studentId.toString(), courseId.toString())) == 1)
    }

    fun studentsInCourse(courseId: Long): List<Student> = readableDatabase.rawQuery("""
        SELECT s.* FROM students s JOIN enrollments e ON e.student_id = s.id
        WHERE e.course_id = ? ORDER BY s.name COLLATE NOCASE, s.id
    """.trimIndent(), arrayOf(courseId.toString())).use { c ->
        buildList { while (c.moveToNext()) add(c.student()) }
    }

    fun pythonBasicStudents(): List<Student> = readableDatabase.rawQuery("""
        SELECT DISTINCT s.* FROM students s
        JOIN enrollments e ON e.student_id = s.id
        JOIN courses c ON c.id = e.course_id
        WHERE s.age BETWEEN 10 AND 12 AND c.language = ? AND c.level = ?
        ORDER BY s.name COLLATE NOCASE, s.id
    """.trimIndent(), arrayOf("Python", "Cơ bản")).use { c ->
        buildList { while (c.moveToNext()) add(c.student()) }
    }

    private fun Cursor.student() = Student(
        getLong(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("code")),
        getString(getColumnIndexOrThrow("name")), getInt(getColumnIndexOrThrow("age")),
        getString(getColumnIndexOrThrow("scratch_level"))
    )

    private fun Cursor.course() = Course(
        getLong(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("code")),
        getString(getColumnIndexOrThrow("name")), getString(getColumnIndexOrThrow("language")),
        getString(getColumnIndexOrThrow("level"))
    )

    companion object {
        @Volatile private var instance: SchoolDatabase? = null
        fun getInstance(context: Context): SchoolDatabase = instance ?: synchronized(this) {
            instance ?: SchoolDatabase(context).also { instance = it }
        }
    }
}
