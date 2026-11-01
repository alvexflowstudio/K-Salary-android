package com.koreasalary.calculator.data.repository

import com.google.gson.Gson
import com.koreasalary.calculator.data.local.entity.*
import com.koreasalary.calculator.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CalendarPayrollTest {
    private val day = DayRecordEntity("2026-11-01", "2026-11") // Sunday
    private fun input(vararg records: DayRecordEntity) = SalaryInputState(hourlyRate = 10_000.0).withCalendarRecords(records.toList())
    private fun pay(vararg records: DayRecordEntity) = SalaryCalculatorEngine.calculateSalary(input(*records), DeductionSettings(isBusinessTax33Enabled = false)).breakdown

    @Test fun baseHoursPayTheSameOnAllSevenWeekdays() {
        repeat(7) {
            val record = day.copy(date = LocalDate.parse(day.date).plusDays(it.toLong()).toString(), regularHours = 8.0)
            assertEquals(80_000L, pay(record).grossSalary)
            assertEquals(0.0, input(record).holidayHours, 0.0)
        }
    }
    @Test fun rotatingFourOnTwoOffIncludesOrdinarySunday() {
        val records = (0 until 12).map { offset ->
            day.copy(date = LocalDate.parse(day.date).plusDays(offset.toLong()).toString(), regularHours = if (offset % 6 < 4) 8.0 else 0.0)
        }
        assertEquals(640_000L, pay(*records.toTypedArray()).grossSalary)
        assertEquals(0.0, input(*records.toTypedArray()).sundayPayHours, 0.0)
    }
    @Test fun paidSundayWithoutWorkAddsEightHoursRegardlessOfSchedule() {
        for (schedule in listOf(0.0, 4.0, 8.0)) {
            val record = day.copy(scheduledBaseHours = schedule, isSundayPay = true)
            assertEquals(80_000L, pay(record).sundayPay)
            assertEquals(0.0, input(record).regularHours, 0.0)
        }
    }
    @Test fun paidSundayWorkIsSeparateAndKeepsManualCategories() {
        assertEquals(160_000L, pay(day.copy(regularHours = 8.0, isSundayPay = true)).grossSalary)
        assertEquals(200_000L, pay(day.copy(overtimeHours = 8.0, isSundayPay = true)).grossSalary)
    }
    @Test fun paidHolidayWithoutWorkAndWithFirstEightPlusExtraTwo() {
        assertEquals(80_000L, pay(day.copy(isPaidHoliday = true)).grossSalary)
        val record = day.copy(holidayHours = 8.0, holidayOvertimeHours = 2.0, isPaidHoliday = true)
        assertEquals(240_000L, pay(record).grossSalary)
        assertEquals(160_000L, pay(record).holidayPay)
        assertEquals(0.0, input(record).regularHours, 0.0)
    }
    @Test fun mixedManualCategoriesAndNightPremiumAreCountedExactlyOnce() {
        val result = pay(day.copy(regularHours = 8.0, overtimeHours = 2.0, nightHours = 4.0, holidayOvertimeHours = 1.0))
        assertEquals(80_000L, result.regularPay)
        assertEquals(30_000L, result.overtimePay)
        assertEquals(20_000L, result.holidayPay)
        assertEquals(20_000L, result.nightPay)
        assertEquals(150_000L, result.grossSalary)
    }
    @Test fun overlappingFlagsPayOnlyOneAllowanceWithOrWithoutWork() {
        for (work in listOf(0.0, 8.0)) {
            val record = day.copy(overtimeHours = work, isSundayPay = true, isPaidHoliday = true)
            assertEquals(80_000L, pay(record).paidHolidayPay)
            assertEquals(0L, pay(record).sundayPay)
            assertEquals((80_000 + work * 15_000).toLong(), pay(record).grossSalary)
        }
        val rest = day.copy(isSundayPay = true)
        val holiday = day.copy(date = "2026-11-02", isPaidHoliday = true)
        assertEquals(160_000L, pay(rest, holiday).grossSalary)
    }
    @Test fun allowanceDoesNotUseTheMonthlyAverageOrCountAsWork() {
        val result = input(day.copy(regularHours = 4.0), day.copy(date = "2026-11-02", isSundayPay = true))
        assertEquals(4.0, result.regularHours, 0.0)
        assertEquals(8.0, result.sundayPayHours, 0.0)
    }
    @Test fun oldSpecialDayWorkRetainsItsPreviousInterpretation() {
        val old = day.copy(workHoursVersion = 1, regularHours = 8.0, overtimeHours = 2.0,
            holidayHours = 3.0, holidayOvertimeHours = 0.5, nightHours = 4.0, isPaidHoliday = true)
        val hours = WorkHours.from(old)
        assertEquals(WorkHours(0.0, 2.0, 4.0, 8.0, 3.5), hours)
        assertEquals(320_000L, pay(old).grossSalary)
        assertEquals(8.0, old.regularHours, 0.0) // decoding never mutates stored data
    }
    @Test fun oldJsonWithoutVersionAndNewJsonKeepTheirDistinctMeaning() {
        val gson = Gson()
        val legacy = gson.fromJson("""{"date":"2026-11-01","yearMonth":"2026-11","regularHours":8,"isSundayPay":true}""", DayRecordEntity::class.java)
        assertEquals(0, legacy.workHoursVersion)
        assertEquals(200_000L, pay(legacy).grossSalary)
        val explicit = day.copy(regularHours = 8.0, isSundayPay = true)
        assertEquals(explicit, gson.fromJson(gson.toJson(explicit), DayRecordEntity::class.java))
        assertEquals(160_000L, pay(explicit).grossSalary)
    }
    @Test fun templatesDecodeLegacyOnceAndKeepNewCategoriesOnSunday() {
        val old = ShiftTemplateEntity(title = "legacy", colorHex = "#476A9C", workHoursVersion = 1,
            regularHours = 8.0, holidayHours = 2.0, overtimeHours = 0.25, isSundayPay = true)
        val applied = old.recordFor(LocalDate.parse(day.date))
        assertEquals(2, applied.workHoursVersion)
        assertEquals(WorkHours.from(old), WorkHours.from(applied))
        assertEquals(243_750L, pay(applied).grossSalary)
        val normal = ShiftTemplateEntity.builtInDay().recordFor(LocalDate.parse(day.date))
        assertEquals(80_000L, pay(normal).grossSalary)
    }
    @Test fun combinedHoursRetainFractionalSplitAndDoNotReclassifyByFlags() {
        val old = WorkHours(overtime = 0.1, holiday = 0.2)
        assertEquals(old, old.withCombinedTimeAndHalf(old.combinedTimeAndHalf))
        assertEquals(WorkHours(overtime = 1.8, holiday = 0.2), old.withCombinedTimeAndHalf(2.0))
        assertEquals(WorkHours(holiday = 0.1), old.withCombinedTimeAndHalf(0.1))
        assertEquals(WorkHours(overtime = 8.0), WorkHours().withCombinedTimeAndHalf(8.0))
    }
    @Test fun splitNoOpKeepsPayIncludingRounding() {
        val old = day.copy(overtimeHours = 0.1, holidayHours = 0.2, holidayOvertimeHours = 0.3)
        val hours = WorkHours.from(old).withCombinedTimeAndHalf(0.1 + 0.2)
        val saved = old.copy(overtimeHours = hours.overtime, holidayHours = hours.holiday)
        assertEquals(old, saved)
        assertEquals(pay(old), pay(saved))
    }
}
