package com.example.btl.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.btl.data.Models.Course;
import com.example.btl.data.Models.Enrollment;
import com.example.btl.data.Models.SchoolData;
import com.example.btl.data.Models.Student;

import java.util.ArrayList;
import java.util.List;

/** The database name, version, tables and constraints match the previous application. */
public final class SchoolDatabase extends SQLiteOpenHelper {
    private static final String ORDER = " ORDER BY name COLLATE NOCASE, id";
    private static SchoolDatabase instance;

    public static synchronized SchoolDatabase getInstance(Context context) {
        if (instance == null) instance = new SchoolDatabase(context, "programming_school.db");
        return instance;
    }

    public SchoolDatabase(Context context, String name) {
        super(context.getApplicationContext(), name, null, 1);
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "code TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(code)) BETWEEN 1 AND 30), " +
                "name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 100), " +
                "age INTEGER NOT NULL CHECK(age BETWEEN 1 AND 17), " +
                "scratch_level TEXT NOT NULL CHECK(scratch_level IN ('Chưa học','Cơ bản','Trung cấp','Nâng cao')))");
        db.execSQL("CREATE TABLE courses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "code TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(code)) BETWEEN 1 AND 30), " +
                "name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 100), " +
                "language TEXT NOT NULL CHECK(language IN ('Scratch','Python')), " +
                "level TEXT NOT NULL CHECK(level IN ('Cơ bản','Trung cấp','Nâng cao')))");
        db.execSQL("CREATE TABLE enrollments (" +
                "student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE, " +
                "course_id INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE, " +
                "PRIMARY KEY(student_id, course_id))");
        db.execSQL("CREATE INDEX enrollments_course_idx ON enrollments(course_id)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Chưa có migration từ " + oldVersion + " lên " + newVersion);
    }

    public SchoolData snapshot() {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try {
            List<Student> students = students(db, "SELECT * FROM students" + ORDER, null);
            List<Course> courses = new ArrayList<>();
            List<Enrollment> enrollments = new ArrayList<>();
            try (Cursor row = db.rawQuery("SELECT id, code, name, language, level FROM courses" + ORDER, null)) {
                while (row.moveToNext()) courses.add(new Course(row.getLong(0), row.getString(1),
                        row.getString(2), row.getString(3), row.getString(4)));
            }
            try (Cursor row = db.rawQuery("SELECT student_id, course_id FROM enrollments", null)) {
                while (row.moveToNext()) enrollments.add(new Enrollment(row.getLong(0), row.getLong(1)));
            }
            db.setTransactionSuccessful();
            return new SchoolData(students, courses, enrollments);
        } finally {
            db.endTransaction();
        }
    }

    public void saveStudent(Long id, String code, String name, String age, String level) {
        Validation.student(code, name, age, level);
        ContentValues values = commonValues(code, name);
        values.put("age", Integer.parseInt(Validation.trim(age)));
        values.put("scratch_level", level);
        save("students", id, values);
    }

    public void saveCourse(Long id, String code, String name, String language, String level) {
        Validation.course(code, name, language, level);
        ContentValues values = commonValues(code, name);
        values.put("language", language);
        values.put("level", level);
        save("courses", id, values);
    }

    public void deleteStudent(long id) {
        ensureChanged(getWritableDatabase().delete("students", "id = ?", args(id)));
    }

    public void deleteCourse(long id) {
        ensureChanged(getWritableDatabase().delete("courses", "id = ?", args(id)));
    }

    public void enroll(long studentId, long courseId) {
        ContentValues values = new ContentValues();
        values.put("student_id", studentId);
        values.put("course_id", courseId);
        getWritableDatabase().insertOrThrow("enrollments", null, values);
    }

    public void unenroll(long studentId, long courseId) {
        ensureChanged(getWritableDatabase().delete("enrollments", "student_id = ? AND course_id = ?",
                new String[]{Long.toString(studentId), Long.toString(courseId)}));
    }

    public List<Student> studentsInCourse(long courseId) {
        return students(getReadableDatabase(), "SELECT s.* FROM students s JOIN enrollments e ON e.student_id = s.id" +
                " WHERE e.course_id = ? ORDER BY s.name COLLATE NOCASE, s.id", args(courseId));
    }

    public List<Student> pythonBasicStudents() {
        return students(getReadableDatabase(), "SELECT DISTINCT s.* FROM students s" +
                " JOIN enrollments e ON e.student_id = s.id JOIN courses c ON c.id = e.course_id" +
                " WHERE s.age BETWEEN 10 AND 12 AND c.language = ? AND c.level = ?" +
                " ORDER BY s.name COLLATE NOCASE, s.id", new String[]{"Python", "Cơ bản"});
    }

    private void save(String table, Long id, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        if (id == null) db.insertOrThrow(table, null, values);
        else ensureChanged(db.update(table, values, "id = ?", args(id)));
    }

    private static void ensureChanged(int rows) {
        if (rows != 1) throw new IllegalStateException("Bản ghi không còn tồn tại.");
    }

    private static String[] args(long id) { return new String[]{Long.toString(id)}; }

    private static ContentValues commonValues(String code, String name) {
        ContentValues values = new ContentValues();
        values.put("code", Validation.trim(code));
        values.put("name", Validation.trim(name));
        return values;
    }

    private static List<Student> students(SQLiteDatabase db, String sql, String[] parameters) {
        List<Student> result = new ArrayList<>();
        try (Cursor row = db.rawQuery(sql, parameters)) {
            int id = row.getColumnIndexOrThrow("id"), code = row.getColumnIndexOrThrow("code");
            int name = row.getColumnIndexOrThrow("name"), age = row.getColumnIndexOrThrow("age");
            int level = row.getColumnIndexOrThrow("scratch_level");
            while (row.moveToNext()) {
                result.add(new Student(row.getLong(id), row.getString(code), row.getString(name),
                        row.getInt(age), row.getString(level)));
            }
        }
        return result;
    }
}
