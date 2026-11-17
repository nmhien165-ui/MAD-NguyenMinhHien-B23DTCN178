package com.example.btl.data

data class Student(val id: Long, val code: String, val name: String, val age: Int, val scratchLevel: String)
data class Course(val id: Long, val code: String, val name: String, val language: String, val level: String)
data class Enrollment(val studentId: Long, val courseId: Long)
data class SchoolData(
    val students: List<Student> = emptyList(),
    val courses: List<Course> = emptyList(),
    val enrollments: List<Enrollment> = emptyList()
)

object SchoolOptions {
    val languages = listOf("Scratch", "Python")
    val levels = listOf("Cơ bản", "Trung cấp", "Nâng cao")
    val scratchLevels = listOf("Chưa học", "Cơ bản", "Trung cấp", "Nâng cao")
}

object Validation {
    fun student(code: String, name: String, age: String, level: String): String? = when {
        code.isBlank() -> "Vui lòng nhập mã học viên."
        code.trim().length > 30 -> "Mã học viên tối đa 30 ký tự."
        name.isBlank() -> "Vui lòng nhập tên học viên."
        name.trim().length > 100 -> "Tên học viên tối đa 100 ký tự."
        age.toIntOrNull() !in 1..17 -> "Tuổi học viên phải là số nguyên từ 1 đến 17."
        level !in SchoolOptions.scratchLevels -> "Vui lòng chọn trình độ Scratch."
        else -> null
    }

    fun course(code: String, name: String, language: String, level: String): String? = when {
        code.isBlank() -> "Vui lòng nhập mã khóa học."
        code.trim().length > 30 -> "Mã khóa học tối đa 30 ký tự."
        name.isBlank() -> "Vui lòng nhập tên khóa học."
        name.trim().length > 100 -> "Tên khóa học tối đa 100 ký tự."
        language !in SchoolOptions.languages -> "Vui lòng chọn Scratch hoặc Python."
        level !in SchoolOptions.levels -> "Vui lòng chọn cấp độ khóa học."
        else -> null
    }
}
