package com.koreasalary.calculator.presentation.screens.calculator

import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.model.AppLanguage
import com.koreasalary.calculator.domain.i18n.LanguageManager
import com.koreasalary.calculator.presentation.theme.DarkAppPalette
import com.koreasalary.calculator.presentation.theme.LightAppPalette
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

    // Premium work categories determine the worked-hours badge color; paid-day flags
    // keep their status styling only for days without physical work.
    private fun octoberRecord(dayNumber: Int) = day.copy(date = "2026-10-%02d".format(dayNumber), yearMonth = "2026-10")

    @Test fun ordinaryWeekdayCategoryColors() {
        assertEquals(CalendarWorkKind.Base, calendarWorkKind(DayOfWeek.MONDAY, day.copy(regularHours = 8.0)))
        assertEquals(CalendarWorkKind.Overtime, calendarWorkKind(DayOfWeek.WEDNESDAY, day.copy(overtimeHours = 2.5)))
        assertEquals(CalendarWorkKind.Night, calendarWorkKind(DayOfWeek.MONDAY, day.copy(nightHours = 6.0)))
        assertEquals(CalendarWorkKind.Night, calendarWorkKind(DayOfWeek.SUNDAY, day.copy(nightHours = 6.0)))
        // Base-only work stays green even on a recognized premium day.
        for (weekday in DayOfWeek.entries) {
            assertEquals(weekday.name, CalendarWorkKind.Base, calendarWorkKind(weekday, day.copy(regularHours = 8.0)))
        }
        // Night premium alone never implies holiday work.
        assertEquals(CalendarWorkKind.Night, calendarWorkKind(DayOfWeek.MONDAY, day.copy(nightHours = 8.0, isPaidHoliday = true)))
    }

    @Test fun weekendAndFlaggedPremiumWorkIsRuby() {
        for (weekday in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            assertEquals(CalendarWorkKind.Premium, calendarWorkKind(weekday, day.copy(overtimeHours = 12.0)))
        }
        assertEquals(CalendarWorkKind.Premium, calendarWorkKind(DayOfWeek.SUNDAY, octoberRecord(25).copy(overtimeHours = 12.0, isSundayPay = true)))
        // Holidays only turn premium when actual premium hours are entered.
        assertEquals(CalendarWorkKind.Base, calendarWorkKind(DayOfWeek.WEDNESDAY, day.copy(isPaidHoliday = true, regularHours = 8.0)))
        assertEquals(CalendarWorkKind.Premium, calendarWorkKind(DayOfWeek.WEDNESDAY, day.copy(isPaidHoliday = true, overtimeHours = 4.0)))
        assertEquals(CalendarWorkKind.Premium, calendarWorkKind(DayOfWeek.WEDNESDAY, day.copy(holidayHours = 8.0, holidayOvertimeHours = 2.0)))
        // Paid-rest flag with premium work stays ruby.
        assertEquals(CalendarWorkKind.Premium, calendarWorkKind(DayOfWeek.TUESDAY, day.copy(isSundayPay = true, holidayHours = 8.0)))
    }

    @Test fun premiumWorkTakesPrecedenceOverBaseNightAndFlags() {
        assertEquals(CalendarWorkKind.Premium, calendarWorkKind(DayOfWeek.SUNDAY, day.copy(overtimeHours = 2.0, nightHours = 4.0)))
        assertEquals(CalendarWorkKind.Premium, calendarWorkKind(DayOfWeek.MONDAY, day.copy(isPaidHoliday = true, holidayHours = 8.0, nightHours = 8.0, regularHours = 0.0)))
        assertEquals(CalendarWorkKind.Night, calendarWorkKind(DayOfWeek.FRIDAY, day.copy(regularHours = 8.0, overtimeHours = 2.0, nightHours = 4.0)))
        // Ordinary-weekday overtime stays orange when a paid-rest flag is not selected.
        assertEquals(CalendarWorkKind.Overtime, calendarWorkKind(DayOfWeek.THURSDAY, day.copy(overtimeHours = 12.0)))
    }

    @Test fun legacyAndNoWorkStatusColoring() {
        // Legacy records moved base hours of special days into the holiday category.
        val legacy = day.copy(regularHours = 8.0, isPaidHoliday = true, workHoursVersion = 1)
        assertEquals(CalendarWorkKind.Premium, calendarWorkKind(DayOfWeek.THURSDAY, legacy))
        // No physical work keeps the paid-holiday / paid-rest status styling.
        assertEquals(null, calendarWorkKind(DayOfWeek.MONDAY, day.copy(isPaidHoliday = true)))
        assertEquals(null, calendarWorkKind(DayOfWeek.SUNDAY, day.copy(isSundayPay = true)))
        assertEquals(null, calendarWorkKind(DayOfWeek.MONDAY, null))
        // Badge colors map onto the palette in both themes.
        val monday = DayOfWeek.MONDAY
        assertEquals(LightAppPalette.success, calendarBadgeColor(monday, day.copy(regularHours = 8.0), LightAppPalette))
        assertEquals(DarkAppPalette.success, calendarBadgeColor(monday, day.copy(regularHours = 8.0), DarkAppPalette))
        assertEquals(LightAppPalette.warning, calendarBadgeColor(monday, day.copy(overtimeHours = 2.0), LightAppPalette))
        assertEquals(LightAppPalette.danger, calendarBadgeColor(DayOfWeek.SUNDAY, day.copy(overtimeHours = 12.0), LightAppPalette))
        assertEquals(DarkAppPalette.danger, calendarBadgeColor(DayOfWeek.SUNDAY, day.copy(overtimeHours = 12.0), DarkAppPalette))
        assertEquals(LightAppPalette.purple, calendarBadgeColor(monday, day.copy(nightHours = 6.0), LightAppPalette))
        assertEquals(LightAppPalette.success, calendarBadgeColor(monday, day.copy(isPaidHoliday = true), LightAppPalette))
    }

    @Test fun october22to25ExampleFollowsSavedCategories() {
        // Saturday 24: combined 1.5× work is premium and stays ruby.
        assertEquals(CalendarWorkKind.Premium, calendarWorkKind(DayOfWeek.SATURDAY, octoberRecord(24).copy(overtimeHours = 12.0)))
        // Sunday 25 with the weekly-rest flag must not color 12 premium hours green.
        assertEquals(CalendarWorkKind.Premium, calendarWorkKind(DayOfWeek.SUNDAY, octoberRecord(25).copy(overtimeHours = 12.0, isSundayPay = true)))
        // Neighbouring ordinary shifts keep their category colors.
        assertEquals(CalendarWorkKind.Base, calendarWorkKind(DayOfWeek.THURSDAY, octoberRecord(22).copy(regularHours = 8.0, scheduledBaseHours = 8.0)))
        assertEquals(CalendarWorkKind.Overtime, calendarWorkKind(DayOfWeek.FRIDAY, octoberRecord(23).copy(regularHours = 8.0, overtimeHours = 2.5)))
        // Clamped base-only input (12) is not premium: the eight-hour input limit keeps it base work.
        assertEquals(CalendarWorkKind.Base, calendarWorkKind(DayOfWeek.SATURDAY, octoberRecord(24).copy(regularHours = 12.0)))
    }

}
