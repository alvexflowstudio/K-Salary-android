package com.koreasalary.calculator.presentation.components

import com.koreasalary.calculator.data.model.*
import com.koreasalary.calculator.domain.i18n.LanguageManager
import org.junit.Assert.*
import org.junit.Test

class HourEntryStateTest {
    @Test fun baseLimitAndInvalidNumbersAreEnforced() {
        val state = HourEntryState(WorkHours())
        for (invalid in listOf("9", "-1", "NaN", "Infinity", "bad")) {
            state.base = invalid
            assertFalse(invalid, state.valid)
        }
        state.base = "8"
        for (invalid in listOf("-1", "NaN", "Infinity", "bad")) {
            state.combined = invalid
            assertFalse(invalid, state.valid)
        }
    }
    @Test fun emptyAndCommaDecimalInputsWorkWithoutLosingLegacySplit() {
        val state = HourEntryState(WorkHours(overtime = 2.5, holiday = 8.0))
        assertEquals(WorkHours(overtime = 2.5, holiday = 8.0), state.hours())
        state.base = ""
        state.night = "1,5"
        assertTrue(state.valid)
        assertEquals(1.5, state.hours().night, 0.0)
    }
    @Test fun allTwelveLanguagesHaveLocalizedLabelsRulesAndWarnings() {
        val all = AppLanguage.entries.map { LanguageManager.getStringsFor(it).ui }
        assertEquals(12, all.size)
        assertEquals(12, all.map { it.sundayBaseHoursWarning }.toSet().size)
        all.forEach {
            assertTrue(it.dayBaseHoursLabel.contains("1.0×"))
            assertTrue(it.dayOvertimeHoursLabel.contains("1.5×"))
            assertTrue(it.dayNightHoursLabel.contains("+0.5×"))
            assertTrue(it.dayHolidayOvertimeHoursLabel.contains("2.0×"))
            assertTrue(it.sundayBaseHoursWarning.contains("1.5×"))
            assertTrue(it.sundayBaseHoursWarning.contains("2.0×"))
            assertEquals(it.calendarPaidDayRule, it.shiftRules[4].description)
            assertEquals(it.calendarPaidDayRule, it.shiftRules[5].description)
        }
    }
}
