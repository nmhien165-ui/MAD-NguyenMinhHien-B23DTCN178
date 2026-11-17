package com.example.btl.data;

import java.util.Collections;
import java.util.List;

public final class Models {
    private Models() {}

    public static final class Student {
        public final long id;
        public final String code, name, scratchLevel;
        public final int age;
        public Student(long id, String code, String name, int age, String scratchLevel) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.age = age;
            this.scratchLevel = scratchLevel;
        }
    }

    public static final class Course {
        public final long id;
        public final String code, name, language, level;
        public Course(long id, String code, String name, String language, String level) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.language = language;
            this.level = level;
        }
    }

    public static final class Enrollment {
        public final long studentId, courseId;
        public Enrollment(long studentId, long courseId) {
            this.studentId = studentId;
            this.courseId = courseId;
        }
    }

    public static final class SchoolData {
        public final List<Student> students;
        public final List<Course> courses;
        public final List<Enrollment> enrollments;
        public SchoolData(List<Student> students, List<Course> courses, List<Enrollment> enrollments) {
            this.students = Collections.unmodifiableList(students);
            this.courses = Collections.unmodifiableList(courses);
            this.enrollments = Collections.unmodifiableList(enrollments);
        }
    }
}
