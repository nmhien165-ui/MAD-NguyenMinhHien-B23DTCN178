package com.example.btl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.btl.data.*

@Composable
private fun EditorField(label: String, value: String, onChange: (String) -> Unit,
                        busy: Boolean, error: String?, type: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type), enabled = !busy,
        isError = error != null, supportingText = error?.let { message -> { Text(message) } },
        modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun StudentEditor(student: Student?, busy: Boolean, serverError: String?, onDismiss: () -> Unit,
                           onSave: (String, String, String, String) -> Unit) {
    var code by rememberSaveable { mutableStateOf(student?.code ?: "") }
    var name by rememberSaveable { mutableStateOf(student?.name ?: "") }
    var age by rememberSaveable { mutableStateOf(student?.age?.toString() ?: "") }
    var level by rememberSaveable { mutableStateOf(student?.scratchLevel ?: "Chưa học") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (student == null) "Thêm học viên" else "Sửa học viên") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ErrorText(serverError)
                EditorField("Mã học viên", code, { code = it }, busy, if (submitted) Validation.code(code, "học viên") else null)
                EditorField("Tên học viên", name, { name = it }, busy, if (submitted) Validation.name(name, student = true) else null)
                EditorField("Tuổi (1–17)", age, { age = it }, busy, if (submitted) Validation.age(age) else null, KeyboardType.Number)
                ChoiceField("Trình độ Scratch", SchoolOptions.scratchLevels.map { it to it }, level, !busy) { level = it }
            }
        },
        confirmButton = { Button(enabled = !busy, onClick = {
            submitted = true
            focus.clearFocus()
            keyboard?.hide()
            if (Validation.student(code, name, age, level) == null) onSave(code, name, age, level)
        }) { Text(if (busy) "Đang lưu…" else "Lưu học viên") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Hủy") } })
}

@Composable
internal fun CourseEditor(course: Course?, busy: Boolean, serverError: String?, onDismiss: () -> Unit,
                          onSave: (String, String, String, String) -> Unit) {
    var code by rememberSaveable { mutableStateOf(course?.code ?: "") }
    var name by rememberSaveable { mutableStateOf(course?.name ?: "") }
    var language by rememberSaveable { mutableStateOf(course?.language ?: "Scratch") }
    var level by rememberSaveable { mutableStateOf(course?.level ?: "Cơ bản") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (course == null) "Thêm khóa học" else "Sửa khóa học") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ErrorText(serverError)
                EditorField("Mã khóa học", code, { code = it }, busy, if (submitted) Validation.code(code, "khóa học") else null)
                EditorField("Tên khóa học", name, { name = it }, busy, if (submitted) Validation.name(name, student = false) else null)
                ChoiceField("Ngôn ngữ", SchoolOptions.languages.map { it to it }, language, !busy) { language = it }
                ChoiceField("Cấp độ", SchoolOptions.levels.map { it to it }, level, !busy) { level = it }
            }
        },
        confirmButton = { Button(enabled = !busy, onClick = {
            submitted = true
            focus.clearFocus()
            keyboard?.hide()
            if (Validation.course(code, name, language, level) == null) onSave(code, name, language, level)
        }) { Text(if (busy) "Đang lưu…" else "Lưu khóa học") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Hủy") } })
}

@Composable
internal fun EnrollmentEditor(course: Course, students: List<Student>, busy: Boolean, error: String?,
                              onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = students.filter { it.name.contains(query, true) || it.code.contains(query, true) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Gán học viên vào khóa") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${course.code} · ${course.name}")
                if (students.isEmpty()) Text("Không còn học viên chưa ghi danh vào khóa này. Hãy thêm học viên ở mục Học viên.")
                else {
                    OutlinedTextField(query, { query = it }, label = { Text("Tìm tên / mã học viên") },
                        enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                    ChoiceField("Học viên", filtered.map { it.id.toString() to "${it.code} · ${it.name}" },
                        selected, !busy) { selected = it }
                    students.firstOrNull { it.id.toString() == selected }?.let {
                        Text("Đã chọn: ${it.name} (${it.code})", style = MaterialTheme.typography.bodySmall)
                    }
                    if (filtered.isEmpty()) Text("Không tìm thấy học viên phù hợp.")
                }
                ErrorText(error)
            }
        },
        confirmButton = { Button(enabled = !busy && selected != null && students.any { it.id.toString() == selected },
            onClick = { selected?.toLongOrNull()?.let(onSave) }) { Text("Gán vào khóa") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Hủy") } })
}
