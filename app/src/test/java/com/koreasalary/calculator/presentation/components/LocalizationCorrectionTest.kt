package com.koreasalary.calculator.presentation.components

import com.koreasalary.calculator.data.model.AppLanguage
import com.koreasalary.calculator.domain.i18n.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class LocalizationCorrectionTest {
    @Test fun instructionsDoNotRestrictRotatingShiftsToWeekdays() {
        assertFalse(LanguageManager.getStringsFor(AppLanguage.RU).ui.shiftRules[1].description.contains("будн"))
        assertFalse(LanguageManager.getStringsFor(AppLanguage.KO).ui.shiftRules[1].description.contains("평일"))
        assertEquals("Rehat", LanguageManager.getStringsFor(AppLanguage.ID).calendarSundayPayBadge)
    }
    @Test fun dateHeadersUseTheSelectedLocaleAndKeepGregorianYears() {
        val date = LocalDate.of(2026, 11, 1)
        val ru = LanguageManager.getStringsFor(AppLanguage.RU)
        val ko = LanguageManager.getStringsFor(AppLanguage.KO)
        assertTrue(formatDayHeading(date, ru).contains("1 ноября"))
        assertEquals("2026년 11월 1일", formatDayHeading(date, ko))
        assertEquals("2026년 11월", formatMonthHeading(YearMonth.from(date), ko))
        assertEquals("Ноябрь 2026", formatMonthHeading(YearMonth.from(date), ru))
        AppLanguage.entries.forEach { language ->
            val strings = LanguageManager.getStringsFor(language)
            assertEquals(language.code, strings.localeTag)
            val formatted = formatDayHeading(date, strings)
            assertTrue("Gregorian year in $language: $formatted", formatted.contains("2026"))
            assertFalse(formatted.contains("2569"))
        }
    }
    @Test fun allLocalesHaveRangeValidationAndCompatibleFormatArguments() {
        val all = AppLanguage.entries.map(LanguageManager::getStringsFor)
        assertEquals(12, all.map { it.ui.baseHoursValidation }.toSet().size)
        all.forEach { strings ->
            for (message in listOf(strings.ui.versionLabel, strings.appVersion)) {
                assertFalse(message.contains("1.0.0"))
                assertTrue(String.format(Locale.ROOT, message, "1.0.4").contains("1.0.4"))
            }
            for (message in listOf(strings.ui.selectedTemplateBannerFormat, strings.ui.deleteTemplateMessage)) {
                assertTrue(String.format(Locale.ROOT, message, "Audit % title").contains("Audit % title"))
            }
            assertFalse(strings.baseHoursHelper.contains("209"))
            assertFalse(strings.baseHoursHelper.contains("35"))
            assertEquals(strings.ui.calendarPaidDayRule, strings.ui.shiftRules[4].description)
            assertEquals(strings.ui.calendarPaidDayRule, strings.ui.shiftRules[5].description)
            assertTrue(strings.ui.dayBaseHoursLabel.contains("1.0×"))
            assertTrue(strings.ui.dayOvertimeHoursLabel.contains("1.5×"))
            assertTrue(strings.ui.dayNightHoursLabel.contains("+0.5×"))
            assertTrue(strings.ui.dayHolidayOvertimeHoursLabel.contains("2.0×"))
            assertTrue(strings.ui.sundayBaseHoursWarning.contains("1.5×"))
            assertTrue(strings.ui.sundayBaseHoursWarning.contains("2.0×"))
        }
    }
}
