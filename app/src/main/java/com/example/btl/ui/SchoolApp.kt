package com.example.btl.ui

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.btl.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen(val title: String, val symbol: String) {
    STUDENTS("Học viên", "HV"), COURSES("Khóa học", "KH"),
    ENROLLMENTS("Ghi danh", "GN"), REPORTS("Báo cáo", "BC")
}

private data class DeleteRequest(val title: String, val message: String, val action: () -> Unit)

@Composable
fun SchoolApp(database: SchoolDatabase = SchoolDatabase.getInstance(LocalContext.current)) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var data by remember { mutableStateOf(SchoolData()) }
    var screenName by rememberSaveable { mutableStateOf(Screen.STUDENTS.name) }
    val screen = Screen.valueOf(screenName)
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCourseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pythonReport by rememberSaveable { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var studentEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingStudentId by rememberSaveable { mutableStateOf<Long?>(null) }
    var courseEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingCourseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var enrollmentEditorOpen by rememberSaveable { mutableStateOf(false) }
    var deletion by remember { mutableStateOf<DeleteRequest?>(null) }

    LaunchedEffect(revision) {
        loading = true
        loadError = null
        try {
            data = withContext(Dispatchers.IO) { database.snapshot() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadError = "Không thể đọc dữ liệu SQLite. Hãy thử lại."
        } finally { loading = false }
    }

    // Keep the current course when possible; clear selection after the last course is deleted.
    val selectedCourse = data.courses.firstOrNull { it.id == selectedCourseId } ?: data.courses.firstOrNull()
    val courseId = selectedCourse?.id
    fun perform(success: String, action: () -> Unit, onSuccess: () -> Unit) {
        if (busy) return
        busy = true
        actionError = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                onSuccess()
                revision++
                scope.launch { snackbar.showSnackbar(success) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: SQLiteConstraintException) {
                actionError = "Mã đã tồn tại hoặc học viên đã được ghi danh. Vui lòng kiểm tra lại."
            } catch (_: SQLiteException) {
                actionError = "Không thể lưu dữ liệu SQLite. Hãy kiểm tra dung lượng máy rồi thử lại."
            } catch (_: Exception) {
                actionError = "Thao tác chưa hoàn thành. Vui lòng thử lại."
            } finally { busy = false }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Screen.entries.forEach { tab ->
                    NavigationBarItem(selected = screen == tab, enabled = !busy,
                        onClick = { screenName = tab.name; query = ""; actionError = null },
                        icon = { Text(tab.symbol, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge) }, label = { Text(tab.title) })
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("LỚP HỌC NHÍ", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("Cùng trẻ học lập trình", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Summary(data) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(screen.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (screen == Screen.STUDENTS || screen == Screen.COURSES) {
                        FilledTonalButton(enabled = !loading && !busy && loadError == null, onClick = {
                            actionError = null
                            if (screen == Screen.STUDENTS) { editingStudentId = null; studentEditorOpen = true }
                            else { editingCourseId = null; courseEditorOpen = true }
                        }) { Text("+ Thêm mới") }
                    }
                }
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (loadError != null) {
                item { Panel { ErrorText(loadError); Button(onClick = { revision++ }) { Text("Thử lại") } } }
            } else if (!loading) {
                if (actionError != null && !studentEditorOpen && !courseEditorOpen && !enrollmentEditorOpen && deletion == null) {
                    item { ErrorText(actionError) }
                }
                when (screen) {
                    Screen.STUDENTS -> {
                        item { SearchField(query, "Tìm tên hoặc mã học viên") { query = it } }
                        val students = data.students.filter { it.name.contains(query, true) || it.code.contains(query, true) }
                        if (students.isEmpty()) item {
                            EmptyState(if (data.students.isEmpty()) "Chưa có học viên" else "Không tìm thấy học viên",
                                if (data.students.isEmpty()) "Chọn + Thêm mới để tạo hồ sơ học viên đầu tiên." else "Thử tìm bằng tên hoặc mã khác.")
                        }
                        items(students, key = { it.id }) { student ->
                            val courses = data.enrollments.filter { it.studentId == student.id }.mapNotNull { e ->
                                data.courses.firstOrNull { it.id == e.courseId }?.name
                            }
                            StudentCard(student, if (courses.isEmpty()) "Chưa ghi danh khóa học" else "Khóa học: ${courses.joinToString()}") {
                                TextButton(enabled = !busy, onClick = {
                                    actionError = null; editingStudentId = student.id; studentEditorOpen = true
                                }) { Text("Sửa") }
                                TextButton(enabled = !busy, onClick = {
                                    actionError = null
                                    deletion = DeleteRequest("Xóa học viên?",
                                        "Xóa ${student.name} (${student.code}) và ${courses.size} lượt ghi danh liên quan? Thao tác này không thể hoàn tác.") {
                                        database.deleteStudent(student.id)
                                    }
                                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                    Screen.COURSES -> {
                        item { SearchField(query, "Tìm tên hoặc mã khóa học") { query = it } }
                        val courses = data.courses.filter { it.name.contains(query, true) || it.code.contains(query, true) }
                        if (courses.isEmpty()) item {
                            EmptyState(if (data.courses.isEmpty()) "Chưa có khóa học" else "Không tìm thấy khóa học",
                                if (data.courses.isEmpty()) "Tạo khóa học Scratch hoặc Python để bắt đầu ghi danh." else "Thử tìm bằng tên hoặc mã khác.")
                        }
                        items(courses, key = { it.id }) { course ->
                            val count = data.enrollments.count { it.courseId == course.id }
                            Panel {
                                Badge("${course.language} · ${course.level}")
                                Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${course.code}  ·  $count học viên", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(enabled = !busy, onClick = {
                                        selectedCourseId = course.id; screenName = Screen.ENROLLMENTS.name; actionError = null
                                    }) { Text("Ghi danh") }
                                    TextButton(enabled = !busy, onClick = {
                                        actionError = null; editingCourseId = course.id; courseEditorOpen = true
                                    }) { Text("Sửa") }
                                    TextButton(enabled = !busy, onClick = {
                                        actionError = null
                                        deletion = DeleteRequest("Xóa khóa học?",
                                            "Xóa ${course.name} (${course.code}) và $count lượt ghi danh liên quan? Hồ sơ học viên vẫn được giữ. Thao tác này không thể hoàn tác.") {
                                            database.deleteCourse(course.id)
                                        }
                                    }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                    Screen.ENROLLMENTS -> {
                        if (selectedCourse == null) item {
                            EmptyState("Cần có khóa học", "Hãy tạo khóa học trong mục Khóa học trước khi ghi danh.")
                        } else {
                            item {
                                Panel {
                                    CourseChoice(data.courses, courseId, !busy) { selectedCourseId = it }
                                    Text("${selectedCourse.language} · ${selectedCourse.level}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Button(enabled = !busy && data.students.isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
                                        actionError = null; enrollmentEditorOpen = true
                                    }) { Text("+ Gán học viên vào khóa") }
                                    if (data.students.isEmpty()) Text("Hãy thêm học viên trước khi ghi danh.")
                                }
                            }
                            val enrolledIds = data.enrollments.filter { it.courseId == courseId }.map { it.studentId }.toSet()
                            val students = data.students.filter { it.id in enrolledIds }
                            item { Text("Danh sách ghi danh (${students.size})", fontWeight = FontWeight.SemiBold) }
                            if (students.isEmpty()) item { EmptyState("Khóa học chưa có học viên", "Chọn Gán học viên vào khóa để thêm học viên.") }
                            items(students, key = { it.id }) { student ->
                                StudentCard(student) {
                                    TextButton(enabled = !busy, onClick = {
                                        actionError = null
                                        deletion = DeleteRequest("Hủy ghi danh?", "Hủy ghi danh ${student.name} khỏi ${selectedCourse.name}? Hồ sơ học viên vẫn được giữ.") {
                                            database.unenroll(student.id, selectedCourse.id)
                                        }
                                    }) { Text("Hủy ghi danh", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                    Screen.REPORTS -> {
                        item {
                            Panel {
                                Text("Báo cáo học viên", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                FilterChip(selected = !pythonReport, onClick = { pythonReport = false }, label = { Text("Theo khóa học") })
                                FilterChip(selected = pythonReport, onClick = { pythonReport = true }, label = { Text("10–12 tuổi · Python cơ bản") })
                                if (!pythonReport) CourseChoice(data.courses, courseId, !busy) { selectedCourseId = it }
                                else Text("Học viên từ 10 đến 12 tuổi (bao gồm cả hai mốc), đã ghi danh ít nhất một khóa Python cấp độ Cơ bản.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item { ReportResults(database, data, pythonReport, courseId) }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // Wait for initial data after an Activity recreation before restoring an edit form.
    if (!loading && loadError == null) {
        if (studentEditorOpen) StudentEditor(data.students.firstOrNull { it.id == editingStudentId }, busy, actionError,
            onDismiss = { studentEditorOpen = false; actionError = null }) { code, name, age, level ->
            perform("Đã lưu học viên", { database.saveStudent(editingStudentId, code, name, age, level) }) { studentEditorOpen = false }
        }
        if (courseEditorOpen) CourseEditor(data.courses.firstOrNull { it.id == editingCourseId }, busy, actionError,
            onDismiss = { courseEditorOpen = false; actionError = null }) { code, name, language, level ->
            perform("Đã lưu khóa học", { database.saveCourse(editingCourseId, code, name, language, level) }) { courseEditorOpen = false }
        }
        if (enrollmentEditorOpen && selectedCourse != null) {
            val enrolled = data.enrollments.filter { it.courseId == selectedCourse.id }.map { it.studentId }.toSet()
            EnrollmentEditor(selectedCourse, data.students.filter { it.id !in enrolled }, busy, actionError,
                onDismiss = { enrollmentEditorOpen = false; actionError = null }) { studentId ->
                perform("Đã ghi danh học viên", { database.enroll(studentId, selectedCourse.id) }) { enrollmentEditorOpen = false }
            }
        }
    }
    deletion?.let { request ->
        AlertDialog(onDismissRequest = { if (!busy) { deletion = null; actionError = null } },
            title = { Text(request.title) }, text = { Column { Text(request.message); ErrorText(actionError) } },
            confirmButton = { Button(enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = { perform("Đã hoàn thành", request.action) { deletion = null } }) { Text("Xác nhận") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deletion = null; actionError = null }) { Text("Hủy") } })
    }
}

@Composable
private fun Summary(data: SchoolData) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)).padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(data.students.size to "Học viên", data.courses.size to "Khóa học", data.enrollments.size to "Ghi danh").forEach { (count, label) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun SearchField(value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(placeholder) }, shape = RoundedCornerShape(14.dp))
}

@Composable
private fun CourseChoice(courses: List<Course>, selected: Long?, enabled: Boolean, onSelect: (Long) -> Unit) {
    ChoiceField("Khóa học", courses.map { it.id.toString() to "${it.code} · ${it.name}" }, selected?.toString(), enabled) {
        onSelect(it.toLong())
    }
}

@Composable
private fun ReportResults(database: SchoolDatabase, data: SchoolData, python: Boolean, courseId: Long?) {
    var retry by remember { mutableIntStateOf(0) }
    var students by remember(data, python, courseId, retry) { mutableStateOf<List<Student>>(emptyList()) }
    var loading by remember(data, python, courseId, retry) { mutableStateOf(true) }
    var error by remember(data, python, courseId, retry) { mutableStateOf<String?>(null) }
    LaunchedEffect(data, python, courseId, retry) {
        try {
            students = withContext(Dispatchers.IO) {
                if (python) database.pythonBasicStudents()
                else if (courseId != null) database.studentsInCourse(courseId) else emptyList()
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = "Không thể thực hiện truy vấn. Vui lòng thử lại." }
        finally { loading = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> Panel { ErrorText(error); TextButton(onClick = { retry++ }) { Text("Thử lại truy vấn") } }
            !python && courseId == null -> EmptyState("Chưa có khóa học", "Thêm khóa học để xem danh sách học viên.")
            else -> {
                Text("Kết quả: ${students.size} học viên", fontWeight = FontWeight.SemiBold)
                if (students.isEmpty()) EmptyState("Chưa có kết quả phù hợp", "Kiểm tra thông tin học viên, cấp độ khóa học và danh sách ghi danh.")
                students.forEach { StudentCard(it) }
            }
        }
    }
}
