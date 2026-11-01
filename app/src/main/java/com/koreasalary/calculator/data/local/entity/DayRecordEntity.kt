package com.koreasalary.calculator.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Запись рабочего дня в табеле (DTR Calendar)
 */
@Entity(tableName = "day_records")
data class DayRecordEntity(
    @PrimaryKey
    val date: String, // Формат: "YYYY-MM-DD"
    val yearMonth: String, // Формат: "YYYY-MM"
    @ColumnInfo(defaultValue = "1")
    val workHoursVersion: Int = 2, // 1: legacy special-day conversion; 2: explicit categories
    val regularHours: Double = 0.0, // Базовые часы (до 8ч, 1.0x)
    val scheduledBaseHours: Double = 0.0, // Legacy schedule metadata; not a paid-day allowance
    val overtimeHours: Double = 0.0, // Combined 1.5x work; legacy holidayHours remains separate
    val nightHours: Double = 0.0, // Ночные часы (22:00-06:00, +0.5x)
    val holidayHours: Double = 0.0, // Праздничные/выходные до 8ч (1.5x)
    val holidayOvertimeHours: Double = 0.0, // Праздничные свыше 8ч (2.0x Чоп)
    val isPaidHoliday: Boolean = false, // Paid holiday: separate fixed 8-hour allowance
    val isSundayPay: Boolean = false, // Weekly paid rest: separate fixed 8-hour allowance
    val templateId: Long? = null,
    val note: String = ""
)
