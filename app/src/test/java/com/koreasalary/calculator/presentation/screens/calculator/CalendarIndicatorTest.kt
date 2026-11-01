package com.koreasalary.calculator.presentation.screens.calculator

import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.model.AppLanguage
import com.koreasalary.calculator.domain.i18n.LanguageManager
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek

class CalendarIndicatorTest {
    private val strings = LanguageManager.getStringsFor(AppLanguage.RU)
    private val day = DayRecordEntity("2026-11-01", "2026-11")
    @Test fun workedSundayShowsPhysicalHours() {
        assertEquals("8 ч", calendarBadgeLabel(day.copy(regularHours = 8.0, isSundayPay = true), strings))
    }
    @Test fun workedHolidayShowsPhysicalHours() {
        assertEquals("10 ч", calendarBadgeLabel(day.copy(holidayHours = 8.0, holidayOvertimeHours = 2.0, nightHours = 8.0, isPaidHoliday = true), strings))
    }

    @Test fun bordersUseWeekdayOrManualHolidayOnly() {
        assertTrue(calendarHasRedDateBorder(DayOfWeek.SUNDAY, null))
        assertTrue(calendarHasRedDateBorder(DayOfWeek.SUNDAY, day.copy(regularHours = 8.0)))
        assertTrue(calendarHasRedDateBorder(DayOfWeek.MONDAY, day.copy(isPaidHoliday = true)))
        assertTrue(calendarHasRedDateBorder(DayOfWeek.SATURDAY, day.copy(isPaidHoliday = true, overtimeHours = 8.0)))
        for (weekday in DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }) {
            assertFalse(calendarHasRedDateBorder(weekday, null))
            assertFalse(calendarHasRedDateBorder(weekday, day.copy(holidayHours = 8.0, isSundayPay = true)))
        }
    }
    @Test fun nonworkingPaidDaysKeepLocalizedLabels() {
        assertEquals(strings.calendarPaidHolidayBadge, calendarBadgeLabel(day.copy(isPaidHoliday = true, isSundayPay = true, nightHours = 8.0), strings))
        assertEquals(strings.calendarSundayPayBadge, calendarBadgeLabel(day.copy(isSundayPay = true), strings))
        assertEquals("", calendarBadgeLabel(null, strings))
    }
    @Test fun mixedCategoriesExcludeNightAndAllowancesAndKeepLegacyWork() {
        val record = day.copy(regularHours = 8.0, overtimeHours = 2.0, holidayHours = 3.0, holidayOvertimeHours = 1.0, nightHours = 8.0, isPaidHoliday = true, isSundayPay = true)
        assertEquals(14.0, calendarWorkedHours(record), 0.0)
        assertEquals(14.0, calendarWorkedHours(record.copy(workHoursVersion = 1)), 0.0)
        assertEquals("14 ч", calendarBadgeLabel(record, strings))
    }
    @Test fun ordinarySaturdayAndRotatingSundayShowWorkWithoutHolidayInference() {
        for (weekday in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY)) {
            assertEquals("8 ч", calendarBadgeLabel(day.copy(regularHours = 8.0), strings))
            assertEquals("2.5 ч", calendarBadgeLabel(day.copy(overtimeHours = 2.5), strings))
        }
        assertEquals("", calendarBadgeLabel(day.copy(nightHours = 8.0), strings))
    }
    @Test fun untouchedSundayDoesNotGenerateRestStatus() {
        assertEquals("", calendarBadgeLabel(null, strings))
        assertEquals("", calendarBadgeLabel(day, strings))
    }

}
