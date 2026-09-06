package com.koreasalary.calculator.data.local.entity

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
    val regularHours: Double = 0.0, // Базовые часы (до 8ч, 1.0x)
    val scheduledBaseHours: Double = 0.0, // Плановые базовые часы для 유급휴일/주휴일
    val overtimeHours: Double = 0.0, // Сверхурочные часы (свыше 8ч, 1.5x)
    val nightHours: Double = 0.0, // Ночные часы (22:00-06:00, +0.5x)
    val holidayHours: Double = 0.0, // Праздничные/выходные до 8ч (1.5x)
    val holidayOvertimeHours: Double = 0.0, // Праздничные свыше 8ч (2.0x Чоп)
    val isPaidHoliday: Boolean = false, // Оплачиваемый праздник: плановые базовые часы 1.0x
    val isSundayPay: Boolean = false, // Еженедельный оплачиваемый отдых: базовые часы 1.0x
    val templateId: Long? = null,
    val note: String = ""
)
