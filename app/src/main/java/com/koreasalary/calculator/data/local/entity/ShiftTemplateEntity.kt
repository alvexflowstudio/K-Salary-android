package com.koreasalary.calculator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Шаблон смены для быстрого проставления в календаре
 */
@Entity(tableName = "shift_templates")
data class ShiftTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val colorHex: String,
    val regularHours: Double = 0.0, // Базовые часы (макс 8)
    val scheduledBaseHours: Double = 0.0, // Плановая смена для 유급휴일/주휴일
    val overtimeHours: Double = 0.0, // Сверхурочные (연장근로 1.5x)
    val nightHours: Double = 0.0, // Ночные часы (야간근로 +0.5x)
    val holidayHours: Double = 0.0, // Праздничные до 8ч (휴일근로 1.5x)
    val holidayOvertimeHours: Double = 0.0, // Праздничные свыше 8ч (휴일연장 2.0x)
    val isPaidHoliday: Boolean = false, // Оплачиваемый праздничный день
    val isSundayPay: Boolean = false, // Еженедельный оплачиваемый отдых
    val isDefault: Boolean = false,
    val orderIndex: Int = 0
) {
    companion object {
        // Имена встроенных смен хранятся как стабильные технические значения;
        // на экране они всегда берутся из выбранного языка.
        const val STORED_DEFAULT_DAY_TITLE = "8ч Дневная"
        const val STORED_DEFAULT_NIGHT_TITLE = "8ч Ночная"
        // Алиас для старых мест кода и резервных копий, где default означал ночь.
        const val STORED_DEFAULT_TITLE = STORED_DEFAULT_NIGHT_TITLE
        const val STORED_LEGACY_DEFAULT_TITLE = STORED_DEFAULT_DAY_TITLE

        fun builtInDay(): ShiftTemplateEntity {
            return ShiftTemplateEntity(
                title = STORED_DEFAULT_DAY_TITLE,
                colorHex = "#476A9C",
                regularHours = 8.0,
                scheduledBaseHours = 8.0,
                isDefault = true,
                orderIndex = 0
            )
        }

        fun builtInNight(): ShiftTemplateEntity {
            return ShiftTemplateEntity(
                title = STORED_DEFAULT_NIGHT_TITLE,
                colorHex = "#5C875D",
                regularHours = 8.0,
                scheduledBaseHours = 8.0,
                nightHours = 8.0,
                isDefault = true,
                orderIndex = 1
            )
        }

        fun builtInDefaults(): List<ShiftTemplateEntity> = listOf(builtInDay(), builtInNight())

        /** Совместимость со старыми вызовами: ранее встроенным считался ночной шаблон. */
        fun builtInDefault(): ShiftTemplateEntity = builtInNight()
    }
}
