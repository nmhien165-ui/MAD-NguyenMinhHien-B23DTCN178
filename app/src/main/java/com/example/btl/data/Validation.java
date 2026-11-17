package com.example.btl.data;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class Validation {
    public static final String[] LANGUAGES = {"Scratch", "Python"};
    public static final String[] LEVELS = {"Cơ bản", "Trung cấp", "Nâng cao"};
    public static final String[] SCRATCH_LEVELS = {"Chưa học", "Cơ bản", "Trung cấp", "Nâng cao"};
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Pattern LETTER_OR_DIGIT = Pattern.compile("[A-Za-z0-9]");
    private static final Pattern STUDENT_NAME = Pattern.compile("[\\p{L}\\p{M} .’'-]+");
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");
    private Validation() {}

    public static String trim(String input) {
        int start = 0, end = input.length();
        while (start < end && isSpace(input.charAt(start))) start++;
        while (end > start && isSpace(input.charAt(end - 1))) end--;
        return input.substring(start, end);
    }

    private static boolean isSpace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }

    public static String code(String input, String entity) {
        String value = trim(input);
        if (value.isEmpty()) return "Vui lòng nhập mã " + entity + ".";
        if (value.length() > 30) return "Mã " + entity + " tối đa 30 ký tự.";
        if (!CODE.matcher(value).matches() || !LETTER_OR_DIGIT.matcher(value).find()) {
            return "Mã chỉ dùng chữ không dấu, số, dấu gạch nối hoặc gạch dưới.";
        }
        return null;
    }

    public static String name(String input, boolean student) {
        String value = trim(input);
        String entity = student ? "học viên" : "khóa học";
        if (value.isEmpty()) return "Vui lòng nhập tên " + entity + ".";
        if (value.length() > 100) return "Tên " + entity + " tối đa 100 ký tự.";
        if (value.codePoints().noneMatch(Character::isLetter)) return "Tên " + entity + " phải có chữ cái.";
        if (student && !STUDENT_NAME.matcher(value).matches()) {
            return "Tên học viên không được chứa số hoặc ký tự đặc biệt.";
        }
        if (input.codePoints().anyMatch(Character::isISOControl)) return "Tên " + entity + " phải nằm trên một dòng.";
        return null;
    }

    public static String age(String input) {
        String value = trim(input);
        if (DIGITS.matcher(value).matches()) {
            try {
                int age = Integer.parseInt(value);
                if (age >= 1 && age <= 17) return null;
            } catch (NumberFormatException ignored) {
                // The input can contain more digits than an int can represent.
            }
        }
        return "Tuổi học viên phải là số nguyên từ 1 đến 17.";
    }

    public static void student(String code, String name, String age, String level) {
        require(code(code, "học viên"));
        require(name(name, true));
        require(age(age));
        if (!Arrays.asList(SCRATCH_LEVELS).contains(level)) throw new IllegalArgumentException("Trình độ Scratch không hợp lệ.");
    }

    public static void course(String code, String name, String language, String level) {
        require(code(code, "khóa học"));
        require(name(name, false));
        if (!Arrays.asList(LANGUAGES).contains(language) || !Arrays.asList(LEVELS).contains(level)) {
            throw new IllegalArgumentException("Ngôn ngữ hoặc cấp độ không hợp lệ.");
        }
    }

    private static void require(String error) {
        if (error != null) throw new IllegalArgumentException(error);
    }
}
