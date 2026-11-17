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
    private val codeFormat = Regex("[A-Za-z0-9_-]+")
    private val studentNameFormat = Regex("[\\p{L}\\p{M} .’'-]+")

    fun code(value: String, entity: String): String? = when {
        value.isBlank() -> "Vui lòng nhập mã $entity."
        value.trim().length > 30 -> "Mã $entity tối đa 30 ký tự."
        !codeFormat.matches(value.trim()) || value.none { it.isLetterOrDigit() } ->
            "Mã chỉ dùng chữ không dấu, số, dấu gạch nối hoặc gạch dưới."
        else -> null
    }

    fun name(value: String, student: Boolean): String? {
        val entity = if (student) "học viên" else "khóa học"
        return when {
            value.isBlank() -> "Vui lòng nhập tên $entity."
            value.trim().length > 100 -> "Tên $entity tối đa 100 ký tự."
            value.none { it.isLetter() } -> "Tên $entity phải có chữ cái."
            student && !studentNameFormat.matches(value.trim()) -> "Tên học viên không được chứa số hoặc ký tự đặc biệt."
            value.any { it.isISOControl() } -> "Tên $entity phải nằm trên một dòng."
            else -> null
        }
    }

    fun age(value: String): String? =
        if (value.trim().toIntOrNull() in 1..17 && value.trim().all { it in '0'..'9' }) null
        else "Tuổi học viên phải là số nguyên từ 1 đến 17."

    fun student(code: String, name: String, age: String, level: String): String? =
        code(code, "học viên") ?: name(name, student = true) ?: age(age)
            ?: "Vui lòng chọn trình độ Scratch.".takeIf { level !in SchoolOptions.scratchLevels }

    fun course(code: String, name: String, language: String, level: String): String? =
        code(code, "khóa học") ?: name(name, student = false)
            ?: "Vui lòng chọn Scratch hoặc Python.".takeIf { language !in SchoolOptions.languages }
            ?: "Vui lòng chọn cấp độ khóa học.".takeIf { level !in SchoolOptions.levels }
}
