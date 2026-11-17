package com.example.btl;

import com.example.btl.data.Validation;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationTest {
    @Test public void ageBoundariesAndWhitespace() {
        for (String value : new String[]{"1", "10", " 12 ", "17"}) assertNull(value, Validation.age(value));
        for (String value : new String[]{"0", "18", "-1", "abc", "1.5", "", "10000000000000"}) {
            assertNotNull(value, Validation.age(value));
        }
    }

    @Test public void validCodesAndForbiddenCharacters() {
        assertNull(Validation.code(" HV-01_2 ", "học viên"));
        assertNull(Validation.code("1", "học viên"));
        for (String value : new String[]{"", "HỌC", "HV 01", "HV/01", "__", "x".repeat(31)}) {
            assertNotNull(value, Validation.code(value, "học viên"));
        }
    }

    @Test public void studentNamesMustBeText() {
        assertNull(Validation.name(" Nguyễn Ánh ", true));
        assertNotNull(Validation.name("12345", true));
        assertNotNull(Validation.name("Nguyễn 1", true));
        assertNotNull(Validation.name("N# A", true));
        assertNotNull(Validation.name("A".repeat(101), true));
    }

    @Test public void courseNamesCanIncludeNumbersButNeedLetters() {
        assertNull(Validation.name("Python 101", false));
        assertNotNull(Validation.name("101", false));
        assertNotNull(Validation.name("Python\n101", false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void studentRejectsUnknownLevel() {
        Validation.student("HV01", "Nguyễn An", "11", "Tùy ý");
    }

    @Test(expected = IllegalArgumentException.class)
    public void courseRejectsUnknownLanguage() {
        Validation.course("KH01", "Lập trình", "Java", "Cơ bản");
    }
}
