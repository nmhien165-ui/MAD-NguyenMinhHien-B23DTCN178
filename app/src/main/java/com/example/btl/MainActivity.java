package com.example.btl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.btl.data.Models.Course;
import com.example.btl.data.Models.Enrollment;
import com.example.btl.data.Models.SchoolData;
import com.example.btl.data.Models.Student;
import com.example.btl.data.SchoolDatabase;
import com.example.btl.data.Validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class MainActivity extends Activity {
    private static final int PRIMARY = Color.rgb(83, 82, 184);
    private static final int TEXT = Color.rgb(29, 33, 56);
    private static final int MUTED = Color.rgb(86, 91, 116);
    private static final int SURFACE = Color.WHITE;
    private static final int SOFT = Color.rgb(234, 232, 255);
    private static final int BORDER = Color.rgb(221, 222, 236);
    private static final int ERROR = Color.rgb(174, 28, 28);
    private static final int MATCH = LinearLayout.LayoutParams.MATCH_PARENT;
    private static final int WRAP = LinearLayout.LayoutParams.WRAP_CONTENT;
    private static final String[] TABS = {"Học viên", "Khóa học", "Ghi danh", "Báo cáo"};
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    static SchoolDatabase testDatabase;
    private SchoolDatabase db;
    private SchoolData data;
    private LinearLayout page, tabs;
    private ScrollView scroll;
    private int tab, reportRequest;
    private long courseId = -1;
    private boolean python, busy;
    private String query = "";
    private Bundle draft;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        page = findViewById(R.id.page);
        tabs = findViewById(R.id.tabs);
        scroll = findViewById(R.id.page_scroll);
        View root = findViewById(R.id.root);
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom));
                return insets;
            });
        }
        db = testDatabase == null ? SchoolDatabase.getInstance(this) : testDatabase;
        if (state != null) {
            tab = state.getInt("tab");
            courseId = state.getLong("course", -1);
            python = state.getBoolean("python");
            query = state.getString("query", "");
            draft = state.getBundle("draft");
        }
        draw();
        reload();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
        out.putLong("course", courseId);
        out.putBoolean("python", python);
        out.putString("query", query);
        if (!busy) out.putBundle("draft", draft);
    }

    private void reload() {
        IO.execute(() -> {
            try {
                SchoolData snapshot = db.snapshot();
                onScreen(() -> {
                    data = snapshot;
                    draw();
                    if (draft != null) edit(draft.getBoolean("student"), draft.getLong("id", -1), draft);
                });
            } catch (RuntimeException ex) {
                onScreen(() -> {
                    page.removeAllViews();
                    add(page, label("Không đọc được dữ liệu.", 18, ERROR, true), 6);
                    add(page, action("Thử lại", true, this::reload), 10);
                });
            }
        });
    }

    private void onScreen(Runnable callback) {
        runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) callback.run(); });
    }

    private void draw() {
        reportRequest++;
        page.removeAllViews();
        tabs.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            Button nav = action(TABS[i], i == tab, () -> changeTab(index));
            nav.setSelected(i == tab);
            nav.setTextSize(12);
            nav.setPadding(dp(2), 0, dp(2), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
            lp.setMargins(dp(2), 0, dp(2), 0);
            tabs.addView(nav, lp);
        }
        add(page, label("QUẢN LÝ LỚP LẬP TRÌNH", 12, PRIMARY, true), 0);
        add(page, label("Lớp học nhí", 31, TEXT, true), 0);
        if (data == null) {
            add(page, new ProgressBar(this), 16);
            return;
        }
        LinearLayout totals = row();
        totals.setBackground(shape(SOFT, 0));
        totals.setPadding(dp(8), dp(12), dp(8), dp(12));
        metric(totals, data.students.size(), "học viên");
        metric(totals, data.courses.size(), "khóa học");
        metric(totals, data.enrollments.size(), "ghi danh");
        add(page, totals, 12);
        LinearLayout toolbar = row();
        TextView heading = label(TABS[tab], 22, TEXT, true);
        toolbar.addView(heading, new LinearLayout.LayoutParams(0, WRAP, 1));
        if (tab < 2) toolbar.addView(action("+ Thêm mới", true, () -> edit(tab == 0, -1, null)));
        add(page, toolbar, 16);
        if (tab < 2) listScreen();
        else if (tab == 2) enrollmentScreen();
        else reports();
    }

    private void changeTab(int index) {
        hideKeyboard();
        tab = index;
        query = "";
        draw();
        scroll.scrollTo(0, 0);
    }

    private void listScreen() {
        boolean students = tab == 0;
        EditText search = input(students ? "Tìm tên hoặc mã học viên" : "Tìm tên hoặc mã khóa học", query, false);
        add(page, search, 6);
        LinearLayout list = column();
        add(page, list, 4);
        renderList(list, students);
        watch(search, value -> {
            query = value;
            renderList(list, students);
        });
    }

    private void renderList(LinearLayout list, boolean students) {
        list.removeAllViews();
        if (students) {
            for (Student student : data.students) if (matches(student.name, student.code, query)) {
                LinearLayout card = studentCard(list, student);
                List<String> names = new ArrayList<>();
                for (Enrollment e : data.enrollments) if (e.studentId == student.id) {
                    Course course = course(e.courseId);
                    if (course != null) names.add(course.name);
                }
                add(card, label(names.isEmpty() ? "Chưa ghi danh khóa học" : "Khóa học: " + String.join(", ", names),
                        13, MUTED, false), 5);
                LinearLayout buttons = actions(card);
                small(buttons, "Sửa", () -> edit(true, student.id, null));
                small(buttons, "Xóa", () -> confirm("Xóa học viên?",
                        "Xóa " + student.name + " và các lượt ghi danh của học viên này?",
                        () -> db.deleteStudent(student.id)));
            }
            if (list.getChildCount() == 0) empty(list, data.students.isEmpty() ? "Chưa có học viên" : "Không tìm thấy học viên",
                    data.students.isEmpty() ? "Bấm Thêm mới để tạo hồ sơ đầu tiên." : "Thử tìm bằng tên hoặc mã khác.");
        } else {
            for (Course course : data.courses) if (matches(course.name, course.code, query)) {
                LinearLayout card = card(list);
                add(card, label(course.language + " · " + course.level, 13, PRIMARY, true), 0);
                add(card, label(course.name, 17, TEXT, true), 5);
                int count = 0;
                for (Enrollment e : data.enrollments) if (e.courseId == course.id) count++;
                add(card, label(course.code + "  ·  " + count + " học viên", 13, MUTED, false), 4);
                LinearLayout buttons = actions(card);
                small(buttons, "Ghi danh", () -> { courseId = course.id; changeTab(2); });
                small(buttons, "Sửa", () -> edit(false, course.id, null));
                small(buttons, "Xóa", () -> confirm("Xóa khóa học?",
                        "Xóa " + course.name + " và các lượt ghi danh? Hồ sơ học viên vẫn được giữ.",
                        () -> db.deleteCourse(course.id)));
            }
            if (list.getChildCount() == 0) empty(list, data.courses.isEmpty() ? "Chưa có khóa học" : "Không tìm thấy khóa học",
                    data.courses.isEmpty() ? "Bấm Thêm mới để tạo khóa Scratch hoặc Python." : "Thử tìm bằng tên hoặc mã khác.");
        }
    }

    private void enrollmentScreen() {
        Course course = currentCourse();
        if (course == null) {
            empty(page, "Cần có khóa học", "Tạo khóa trước khi ghi danh.");
            return;
        }
        LinearLayout panel = card(page);
        coursePicker(panel, course);
        add(panel, label(course.language + " · " + course.level, 14, MUTED, false), 6);
        List<Student> candidates = new ArrayList<>();
        List<Student> enrolled = new ArrayList<>();
        for (Student student : data.students) {
            if (isEnrolled(student.id, course.id)) enrolled.add(student);
            else candidates.add(student);
        }
        Button addButton = action("+ Gán học viên vào khóa", true, () -> enrollmentDialog(course, candidates));
        addButton.setEnabled(!candidates.isEmpty());
        add(panel, addButton, 8);
        if (candidates.isEmpty()) add(panel, label(data.students.isEmpty()
                ? "Hãy thêm học viên trước khi ghi danh." : "Tất cả học viên đã ghi danh vào khóa này.", 13, MUTED, false), 8);
        add(page, label("Danh sách ghi danh (" + enrolled.size() + ")", 16, TEXT, true), 14);
        for (Student student : enrolled) {
            LinearLayout card = studentCard(page, student);
            small(actions(card), "Hủy ghi danh", () -> confirm("Hủy ghi danh?",
                    "Gỡ " + student.name + " khỏi " + course.name + "?", () -> db.unenroll(student.id, course.id)));
        }
        if (enrolled.isEmpty()) empty(page, "Khóa học chưa có học viên", "Chọn Gán học viên vào khóa để bắt đầu.");
    }

    private void reports() {
        LinearLayout panel = card(page);
        add(panel, action("Theo khóa học", !python, () -> { python = false; draw(); }), 0);
        add(panel, action("10–12 tuổi · Python cơ bản", python, () -> { python = true; draw(); }), 6);
        Course selected = currentCourse();
        if (!python && selected == null) {
            empty(page, "Chưa có khóa học", "Thêm khóa học để xem báo cáo.");
            return;
        }
        if (!python) coursePicker(panel, selected);
        else add(panel, label("Học viên từ 10 đến 12 tuổi đã ghi danh khóa Python cấp Cơ bản.", 14, MUTED, false), 8);
        LinearLayout results = column();
        add(page, results, 12);
        add(results, new ProgressBar(this), 2);
        boolean usePython = python;
        int request = ++reportRequest;
        IO.execute(() -> {
            try {
                List<Student> students = usePython ? db.pythonBasicStudents() : db.studentsInCourse(selected.id);
                onScreen(() -> {
                    if (request != reportRequest) return;
                    results.removeAllViews();
                    add(results, label("Kết quả: " + students.size() + " học viên", 16, TEXT, true), 0);
                    for (Student student : students) studentCard(results, student);
                    if (students.isEmpty()) empty(results, "Không có học viên phù hợp", "Kiểm tra bộ lọc hoặc danh sách ghi danh.");
                });
            } catch (RuntimeException ex) {
                onScreen(() -> {
                    if (request != reportRequest) return;
                    results.removeAllViews();
                    empty(results, "Không tải được báo cáo", "Hãy thử lại.");
                    add(results, action("Thử lại", true, this::draw), 8);
                });
            }
        });
    }

    private void coursePicker(LinearLayout parent, Course current) {
        String[] names = new String[data.courses.size()];
        int position = 0;
        for (int i = 0; i < names.length; i++) {
            Course course = data.courses.get(i);
            names[i] = course.code + " · " + course.name;
            if (course.id == current.id) position = i;
        }
        add(parent, label("Khóa học", 13, TEXT, true), 6);
        Spinner picker = select(parent, names, names[position], "Khóa học");
        picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> views, View v, int index, long ignored) {
                long next = data.courses.get(index).id;
                if (next != courseId) { courseId = next; draw(); }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void edit(boolean student, long id, Bundle saved) {
        if (busy) return;
        hideKeyboard();
        Student s = student ? student(id) : null;
        Course c = student ? null : course(id);
        if (id != -1 && s == null && c == null) { draft = null; return; }
        String entity = student ? "học viên" : "khóa học";
        Bundle record = saved == null ? new Bundle() : saved;
        record.putBoolean("student", student);
        record.putLong("id", id);
        draft = record;
        LinearLayout content = form();
        EditText code = input("Mã " + entity, record.getString("code", s != null ? s.code : c != null ? c.code : ""), false);
        EditText name = input("Tên " + entity, record.getString("name", s != null ? s.name : c != null ? c.name : ""), false);
        EditText age = student ? input("Tuổi (1–17)", record.getString("age", s != null ? Integer.toString(s.age) : ""), true) : null;
        TextView error = label("", 13, ERROR, true);
        error.setVisibility(View.GONE);
        add(content, error, 0);
        TextView codeError = field(content, code);
        TextView nameError = field(content, name);
        TextView ageError = age == null ? null : field(content, age);
        if (student) add(content, label("Trình độ Scratch", 13, TEXT, true), 8);
        else add(content, label("Ngôn ngữ", 13, TEXT, true), 8);
        Spinner language = student ? null : select(content, Validation.LANGUAGES,
                record.getString("language", c == null ? "Scratch" : c.language), "Ngôn ngữ");
        if (!student) add(content, label("Cấp độ", 13, TEXT, true), 8);
        Spinner level = select(content, student ? Validation.SCRATCH_LEVELS : Validation.LEVELS,
                record.getString("level", s != null ? s.scratchLevel : c != null ? c.level : student ? "Chưa học" : "Cơ bản"), "Cấp độ");
        watch(code, value -> {
            record.putString("code", value);
            showError(error, null);
            if (record.getBoolean("submitted")) showError(codeError, Validation.code(value, entity));
        });
        watch(name, value -> {
            record.putString("name", value);
            if (record.getBoolean("submitted")) showError(nameError, Validation.name(value, student));
        });
        if (age != null) watch(age, value -> {
            record.putString("age", value);
            if (record.getBoolean("submitted")) showError(ageError, Validation.age(value));
        });
        rememberSelection(level, record, "level");
        if (language != null) rememberSelection(language, record, "language");
        AlertDialog dialog = dialog((id == -1 ? "Thêm " : "Sửa ") + entity, content, "Lưu " + entity);
        dialog.setOnDismissListener(d -> { if (draft == record) draft = null; });
        Runnable validate = () -> {
            showError(codeError, Validation.code(value(code), entity));
            showError(nameError, Validation.name(value(name), student));
            if (age != null) showError(ageError, Validation.age(value(age)));
        };
        if (record.getBoolean("submitted")) validate.run();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            record.putBoolean("submitted", true);
            hideKeyboard(dialog.getCurrentFocus());
            validate.run();
            if (codeError.getVisibility() == View.VISIBLE || nameError.getVisibility() == View.VISIBLE
                    || (ageError != null && ageError.getVisibility() == View.VISIBLE)) return;
            Long key = id == -1 ? null : id;
            String codeValue = value(code), nameValue = value(name), chosen = level.getSelectedItem().toString();
            if (student) {
                String ageValue = value(age);
                write(dialog, error, "Mã học viên đã tồn tại. Hãy nhập mã khác.", "Đã lưu học viên",
                        () -> db.saveStudent(key, codeValue, nameValue, ageValue, chosen));
            } else {
                String lang = language.getSelectedItem().toString();
                write(dialog, error, "Mã khóa học đã tồn tại. Hãy nhập mã khác.", "Đã lưu khóa học",
                        () -> db.saveCourse(key, codeValue, nameValue, lang, chosen));
            }
        });
    }

    private void enrollmentDialog(Course course, List<Student> candidates) {
        LinearLayout body = form();
        add(body, label(course.code + " · " + course.name, 15, TEXT, true), 0);
        EditText search = input("Tìm tên hoặc mã học viên", "", false);
        add(body, search, 8);
        TextView error = label("", 13, ERROR, false);
        error.setVisibility(View.GONE);
        add(body, error, 0);
        Spinner picker = select(body, new String[]{"Chọn học viên"}, "Chọn học viên", "Chọn học viên");
        List<Student> shown = new ArrayList<>();
        AlertDialog dialog = dialog("Gán học viên vào khóa", body, "Gán vào khóa");
        Consumer<String> filter = text -> {
            List<String> names = new ArrayList<>();
            names.add("Chọn học viên");
            shown.clear();
            for (Student student : candidates) if (matches(student.name, student.code, text)) {
                shown.add(student);
                names.add(student.code + " · " + student.name);
            }
            picker.setAdapter(array(names.toArray(new String[0])));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            showError(error, shown.isEmpty() ? "Không tìm thấy học viên phù hợp." : null);
        };
        filter.accept("");
        picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> views, View v, int index, long id) {
                if (!busy) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(index > 0 && index <= shown.size());
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> views) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        watch(search, filter);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int selection = picker.getSelectedItemPosition() - 1;
            if (selection >= 0 && selection < shown.size()) {
                long studentId = shown.get(selection).id;
                write(dialog, error, "Học viên đã ghi danh hoặc dữ liệu đã thay đổi.", "Đã ghi danh học viên",
                        () -> db.enroll(studentId, course.id));
            }
        });
    }

    private void confirm(String title, String message, Runnable change) {
        if (busy) return;
        LinearLayout body = form();
        add(body, label(message, 15, TEXT, false), 0);
        TextView error = label("", 13, ERROR, false);
        error.setVisibility(View.GONE);
        add(body, error, 8);
        AlertDialog dialog = dialog(title, body, "Xác nhận");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
                write(dialog, error, "Không thể thực hiện thao tác.", "Đã hoàn thành", change));
    }

    private void write(AlertDialog dialog, TextView error, String conflict, String success, Runnable mutation) {
        if (busy) return;
        busy = true;
        dialog.setCancelable(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
        showError(error, null);
        IO.execute(() -> {
            try {
                mutation.run();
            } catch (RuntimeException ex) {
                onScreen(() -> {
                    busy = false;
                    dialog.setCancelable(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                    showError(error, ex instanceof SQLiteConstraintException ? conflict
                            : "Thao tác chưa hoàn thành. Vui lòng thử lại.");
                });
                return;
            }
            onScreen(() -> {
                busy = false;
                dialog.dismiss();
                Toast.makeText(this, success, Toast.LENGTH_SHORT).show();
                reload();
            });
        });
    }

    private AlertDialog dialog(String title, LinearLayout content, String positive) {
        ScrollView wrapper = new ScrollView(this);
        wrapper.setFillViewport(false);
        wrapper.addView(content);
        AlertDialog d = new AlertDialog.Builder(this).setTitle(title).setView(wrapper)
                .setNegativeButton("Hủy", null).setPositiveButton(positive, null).create();
        d.setCanceledOnTouchOutside(false);
        d.show();
        if (d.getWindow() != null) d.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        return d;
    }

    private LinearLayout form() {
        LinearLayout body = column();
        body.setPadding(dp(22), dp(10), dp(22), dp(8));
        body.setFocusableInTouchMode(true);
        body.requestFocus();
        return body;
    }

    private LinearLayout studentCard(LinearLayout list, Student student) {
        LinearLayout card = card(list);
        add(card, label(student.name, 17, TEXT, true), 0);
        add(card, label(student.code + "  ·  " + student.age + " tuổi", 13, MUTED, false), 4);
        add(card, label("Scratch: " + student.scratchLevel, 14, TEXT, false), 6);
        return card;
    }

    private void empty(LinearLayout parent, String title, String desc) {
        LinearLayout card = card(parent);
        add(card, label(title, 16, TEXT, true), 0);
        add(card, label(desc, 14, MUTED, false), 5);
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = column();
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(shape(SURFACE, BORDER));
        add(parent, card, 10);
        return card;
    }

    private void metric(LinearLayout parent, int count, String name) {
        LinearLayout part = column();
        TextView number = label(Integer.toString(count), 25, PRIMARY, true);
        number.setGravity(Gravity.CENTER);
        add(part, number, 0);
        TextView title = label(name, 12, TEXT, false);
        title.setGravity(Gravity.CENTER);
        add(part, title, 2);
        parent.addView(part, new LinearLayout.LayoutParams(0, WRAP, 1));
    }

    private LinearLayout actions(LinearLayout parent) {
        LinearLayout row = row();
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        add(parent, row, 8);
        return row;
    }

    private void small(LinearLayout row, String text, Runnable click) {
        Button b = action(text, false, click);
        if (text.equals("Xóa") || text.equals("Hủy ghi danh")) b.setTextColor(ERROR);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, dp(48));
        lp.setMarginStart(dp(6));
        row.addView(b, lp);
    }

    private Button action(String title, boolean primary, Runnable click) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinimumHeight(dp(48));
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setTextColor(primary ? Color.WHITE : PRIMARY);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x337573D0),
                shape(primary ? PRIMARY : SOFT, 0), null));
        b.setStateListAnimator(null);
        b.setOnClickListener(v -> click.run());
        return b;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private EditText input(String hint, String text, boolean number) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setInputType(number ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        field.setTextSize(16);
        field.setHint(hint);
        field.setContentDescription(hint);
        field.setText(text);
        field.setTextColor(TEXT);
        field.setHintTextColor(MUTED);
        field.setMinHeight(dp(52));
        field.setPadding(dp(12), dp(9), dp(12), dp(9));
        field.setBackground(shape(SURFACE, BORDER));
        return field;
    }

    private TextView field(LinearLayout parent, EditText editor) {
        TextView caption = label(editor.getHint().toString(), 13, TEXT, true);
        add(parent, caption, 8);
        add(parent, editor, 2);
        TextView error = label("", 13, ERROR, false);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        error.setVisibility(View.GONE);
        add(parent, error, 2);
        editor.setTag(error);
        editor.setOnFocusChangeListener((view, focused) -> editor.setBackground(shape(SURFACE,
                error.getVisibility() == View.VISIBLE ? ERROR : focused ? PRIMARY : BORDER)));
        return error;
    }

    private static void showError(TextView target, String message) {
        target.setText(message == null ? "" : message);
        target.setVisibility(message == null ? View.GONE : View.VISIBLE);
        if (target.getParent() instanceof LinearLayout) {
            LinearLayout parent = (LinearLayout) target.getParent();
            int index = parent.indexOfChild(target);
            if (index > 0 && parent.getChildAt(index - 1) instanceof EditText) {
                EditText editor = (EditText) parent.getChildAt(index - 1);
                if (editor.getTag() == target) {
                    MainActivity activity = (MainActivity) target.getContext();
                    editor.setBackground(activity.shape(SURFACE, message != null ? ERROR : editor.isFocused() ? PRIMARY : BORDER));
                }
            }
        }
    }

    private Spinner select(LinearLayout parent, String[] options, String selected, String description) {
        Spinner spinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        spinner.setContentDescription(description);
        spinner.setAdapter(array(options));
        spinner.setSelection(Math.max(0, Arrays.asList(options).indexOf(selected)));
        spinner.setMinimumHeight(dp(48));
        add(parent, spinner, 2);
        return spinner;
    }

    private ArrayAdapter<String> array(String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private static void rememberSelection(Spinner spinner, Bundle saved, String key) {
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int i, long id) {
                saved.putString(key, p.getItemAtPosition(i).toString());
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private static void watch(EditText input, Consumer<String> update) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { update.accept(s.toString()); }
            @Override public void afterTextChanged(Editable editable) {}
        });
    }

    private static String value(EditText e) { return e.getText().toString(); }
    private boolean matches(String name, String code, String input) {
        String term = Validation.trim(input).toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(term) || code.toLowerCase(Locale.ROOT).contains(term);
    }

    private boolean isEnrolled(long student, long course) {
        for (Enrollment e : data.enrollments) if (e.studentId == student && e.courseId == course) return true;
        return false;
    }

    private Student student(long id) {
        for (Student student : data.students) if (student.id == id) return student;
        return null;
    }

    private Course course(long id) {
        for (Course course : data.courses) if (course.id == id) return course;
        return null;
    }

    private Course currentCourse() {
        Course current = course(courseId);
        if (current == null && !data.courses.isEmpty()) current = data.courses.get(0);
        courseId = current == null ? -1 : current.id;
        return current;
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable draw = new GradientDrawable();
        draw.setColor(color);
        draw.setCornerRadius(dp(13));
        if (stroke != 0) draw.setStroke(dp(1), stroke);
        return draw;
    }

    private LinearLayout row() {
        LinearLayout linear = new LinearLayout(this);
        linear.setGravity(Gravity.CENTER_VERTICAL);
        return linear;
    }

    private LinearLayout column() {
        LinearLayout linear = new LinearLayout(this);
        linear.setOrientation(LinearLayout.VERTICAL);
        return linear;
    }

    private void add(LinearLayout layout, View view, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH, WRAP);
        params.topMargin = dp(top);
        layout.addView(view, params);
    }

    private void hideKeyboard() { hideKeyboard(getCurrentFocus()); }

    private void hideKeyboard(View focus) {
        if (focus == null) return;
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        focus.clearFocus();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
