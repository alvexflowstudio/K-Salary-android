package com.koreasalary.calculator.data.model

import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.local.entity.ShiftTemplateEntity

/** Mutually exclusive work categories. Night hours are an additional premium only. */
data class WorkHours(
    val regular: Double = 0.0,
    val overtime: Double = 0.0,
    val night: Double = 0.0,
    val holiday: Double = 0.0,
    val holidayOvertime: Double = 0.0
) {
    val combinedTimeAndHalf: Double get() = overtime + holiday

    /** A no-op edit preserves the exact legacy split, including fractional hours. */
    fun withCombinedTimeAndHalf(total: Double): WorkHours {
        require(total.isFinite() && total >= 0.0)
        if (total == combinedTimeAndHalf) return this
        val retainedHoliday = holiday.coerceAtMost(total)
        return copy(overtime = total - retainedHoliday, holiday = retainedHoliday)
    }

    companion object {
        fun from(record: DayRecordEntity): WorkHours = decode(
            record.regularHours, record.overtimeHours, record.nightHours,
            record.holidayHours, record.holidayOvertimeHours,
            record.workHoursVersion, record.isPaidHoliday || record.isSundayPay
        )

        fun from(template: ShiftTemplateEntity): WorkHours = decode(
            template.regularHours, template.overtimeHours, template.nightHours,
            template.holidayHours, template.holidayOvertimeHours,
            template.workHoursVersion, template.isPaidHoliday || template.isSundayPay
        )

        private fun decode(
            regular: Double, overtime: Double, night: Double, holiday: Double,
            holidayOvertime: Double, version: Int, specialDay: Boolean
        ): WorkHours {
            fun safe(value: Double) = if (value.isFinite()) value.coerceAtLeast(0.0) else 0.0
            val base = safe(regular).coerceAtMost(8.0)
            val rawHoliday = safe(holiday)
            val firstHoliday = rawHoliday.coerceAtMost(8.0)
            val extraHoliday = safe(holidayOvertime) + (rawHoliday - firstHoliday)
            // Reproduce the previous ViewModel interpretation only for legacy records.
            // New checkbox/date changes never move actual work between categories.
            val moved = if (version < 2 && specialDay) base else 0.0
            val movedFirst = moved.coerceAtMost(8.0 - firstHoliday)
            return WorkHours(
                regular = base - moved,
                overtime = safe(overtime),
                night = safe(night),
                holiday = firstHoliday + movedFirst,
                holidayOvertime = extraHoliday + (moved - movedFirst)
            )
        }
    }
}

/** Apply date-level rules before aggregation, where overlapping flags are still known. */
fun SalaryInputState.withCalendarRecords(records: Collection<DayRecordEntity>): SalaryInputState {
    val hours = records.map(WorkHours::from)
    return copy(
        regularHours = hours.sumOf { it.regular },
        overtimeHours = hours.sumOf { it.overtime },
        nightHours = hours.sumOf { it.night },
        holidayHours = hours.sumOf { it.holiday },
        holidayOvertimeHours = hours.sumOf { it.holidayOvertime },
        // Public holiday takes reporting precedence; retain both saved flags.
        paidHolidayHours = 8.0 * records.count { it.isPaidHoliday },
        sundayPayHours = 8.0 * records.count { it.isSundayPay && !it.isPaidHoliday }
    )
}

fun ShiftTemplateEntity.recordFor(date: java.time.LocalDate): DayRecordEntity {
    val hours = WorkHours.from(this)
    return DayRecordEntity(
        date = date.toString(), yearMonth = java.time.YearMonth.from(date).toString(),
        regularHours = hours.regular, scheduledBaseHours = scheduledBaseHours,
        overtimeHours = hours.overtime, nightHours = hours.night,
        holidayHours = hours.holiday, holidayOvertimeHours = hours.holidayOvertime,
        isPaidHoliday = isPaidHoliday, isSundayPay = isSundayPay,
        templateId = id, note = if (isDefault) "" else title
    )
}
