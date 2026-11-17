package com.example.btl

import com.example.btl.data.Validation
import org.junit.Assert.*
import org.junit.Test

class ValidationTest {
    @Test fun studentAcceptsTrimmedFieldsAndChildAgeLimits() {
        assertNull(Validation.student(" HV01 ", " An ", "1", "Chưa học"))
        assertNull(Validation.student("HV02", "Bình", "17", "Nâng cao"))
    }

    @Test fun studentRejectsBlankAndOversizedFields() {
        assertNotNull(Validation.student(" ", "An", "10", "Cơ bản"))
        assertNotNull(Validation.student("HV01", " ", "10", "Cơ bản"))
        assertNotNull(Validation.student("x".repeat(31), "An", "10", "Cơ bản"))
        assertNotNull(Validation.student("HV01", "x".repeat(101), "10", "Cơ bản"))
    }

    @Test fun studentRejectsInvalidAgesAndLevels() {
        listOf("", "0", "18", "-1", "10.5", "abc", "999999999999").forEach {
            assertNotNull("Invalid age: $it", Validation.student("HV01", "An", it, "Cơ bản"))
        }
        assertNotNull(Validation.student("HV01", "An", "10", "Tùy ý"))
    }

    @Test fun courseAcceptsBothLanguages() {
        assertNull(Validation.course("KH01", "Lập trình", "Scratch", "Cơ bản"))
        assertNull(Validation.course("KH02", "Lập trình", "Python", "Nâng cao"))
    }

    @Test fun courseRejectsMissingFieldsAndUnsupportedOptions() {
        assertNotNull(Validation.course(" ", "Lập trình", "Python", "Cơ bản"))
        assertNotNull(Validation.course("KH01", " ", "Python", "Cơ bản"))
        assertNotNull(Validation.course("KH01", "Lập trình", "Java", "Cơ bản"))
        assertNotNull(Validation.course("KH01", "Lập trình", "Python", "beginner"))
    }
}
