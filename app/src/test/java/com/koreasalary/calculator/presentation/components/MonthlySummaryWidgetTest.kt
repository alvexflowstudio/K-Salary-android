package com.koreasalary.calculator.presentation.components

import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlySummaryWidgetTest {

    @Test
    fun weeklyRestAndPaidHolidayMarkersDoNotCountAsWorkedDays() {
        val records = buildList {
            repeat(4) { day ->
                add(
                    DayRecordEntity(
                        date = "2026-09-${(day + 1).toString().padStart(2, '0')}",
                        yearMonth = "2026-09",
                        regularHours = 8.0
                    )
                )
            }
            repeat(4) { day ->
                add(
                    DayRecordEntity(
                        date = "2026-09-${(day + 6).toString().padStart(2, '0')}",
                        yearMonth = "2026-09",
                        scheduledBaseHours = 8.0,
                        isSundayPay = true
                    )
                )
            }
            add(
                DayRecordEntity(
                    date = "2026-09-21",
                    yearMonth = "2026-09",
                    scheduledBaseHours = 8.0,
                    isPaidHoliday = true
                )
            )
        }

        assertEquals(4, countActualWorkDays(records))
    }

    @Test
    fun actualHolidayHoursStillCountAsAWorkedDay() {
        val records = listOf(
            DayRecordEntity(
                date = "2026-09-06",
                yearMonth = "2026-09",
                holidayHours = 8.0,
                isSundayPay = true
            )
        )

        assertEquals(1, countActualWorkDays(records))
    }
}
