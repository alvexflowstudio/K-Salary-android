package com.koreasalary.calculator.data.model

import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.local.entity.MonthlySnapshotEntity
import com.koreasalary.calculator.data.local.entity.ShiftTemplateEntity

/**
 * Размер предприятия в соответствии с ТК Южной Кореи (근로기준법)
 */
@Deprecated("Оставлено только для чтения старых снимков и резервных копий")
enum class CompanySize {
    FIVE_OR_MORE, // 5인 이상 사업장 (применяются все надбавки: 1.5x 연장, +0.5x 야간, 1.5x/2.0x 휴일)
    UNDER_5       // 5인 미만 사업장 (надбавки 1.5x не обязательны по закону, ночные не доплачиваются)
}

/**
 * Элемент дополнительной надбавки или бонуса
 */
data class AllowanceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double = 0.0
)

/**
 * Состояние доплат и надбавок (지급 항목)
 */
data class AllowancesState(
    val fuelAllowance: Double = 0.0, // 유류비 / 교통비
    val annualLeaveAllowance: Double = 0.0, // 연차수당 / 월차수당
    val customBonuses: List<AllowanceItem> = emptyList() // Кастомные надбавки/бонусы
) {
    val totalAllowances: Double
        get() = fuelAllowance + annualLeaveAllowance + customBonuses.sumOf { it.amount }
}

/**
 * Произвольное удержание из расчётного листка.
 * Название и сумму пользователь вводит вручную, потому что состав таких
 * удержаний зависит от договора и конкретного работодателя.
 */
data class CustomDeductionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double = 0.0
)

/**
 * Входные параметры для расчета зарплаты
 */
data class SalaryInputState(
    val hourlyRate: Double = 0.0, // Новому пользователю нужно ввести свою ставку вручную
    val regularHours: Double = 0.0,    // Отработанные базовые часы (до 8ч в день, 1.0x)
    val overtimeHours: Double = 0.0,   // Сверхурочные часы (будни свыше 8ч, 1.5x)
    val nightHours: Double = 0.0,      // Ночные часы (22:00 - 06:00, доплата +0.5x)
    val holidayHours: Double = 0.0,    // Праздничные/выходные до 8ч (1.5x)
    val holidayOvertimeHours: Double = 0.0, // Праздничные свыше 8ч (Чоп, 2.0x)
    val paidHolidayHours: Double = 0.0, // Оплачиваемые праздничные дни (유급휴일, 1.0x базовой)
    val sundayPayHours: Double = 0.0,   // Еженедельные оплачиваемые выходные (주휴수당, 1.0x базовой)
    val allowances: AllowancesState = AllowancesState()
)

/**
 * Настройки налогов и страховых вычетов с независимыми тумблерами
 */
data class DeductionSettings(
    val isBusinessTax33Enabled: Boolean = true,       // 3.3% налог (사업소득세) - ПО УМОЛЧАНИЮ ВКЛЮЧЕН
    val isNationalPensionEnabled: Boolean = false,     // 4.75% доля работника (국민연금) - ПО УМОЛЧАНИЮ ВЫКЛ
    val isHealthInsuranceEnabled: Boolean = false,     // 3.595% доля работника (건강보험) - ПО УМОЛЧАНИЮ ВЫКЛ
    val isLongTermCareEnabled: Boolean = false,        // 13.1405% от взноса за медстрахование - ПО УМОЛЧАНИЮ ВЫКЛ
    val isEmploymentInsuranceEnabled: Boolean = false, // 0.9% Страхование от безработицы (고용보험) - ПО УМОЛЧАНИЮ ВЫКЛ
    val isIncomeTaxEnabled: Boolean = false,           // Оценка подоходного налога (소득세 / 지방소득세) - ПО УМОЛЧАНИЮ ВЫКЛ
    val customDeductions: List<CustomDeductionItem> = emptyList()
) {
    /**
     * Проверка на смешанный режим (3.3% + страховки или подоходный налог)
     */
    val isMixedMode: Boolean
        get() = isBusinessTax33Enabled && (
                isNationalPensionEnabled ||
                isHealthInsuranceEnabled ||
                isLongTermCareEnabled ||
                isEmploymentInsuranceEnabled ||
                isIncomeTaxEnabled
        )
}

/**
 * Детализация начислений (Gross)
 */
data class SalaryBreakdown(
    val regularPay: Long = 0L,        // Оплата базовых часов (1.0x)
    val sundayPay: Long = 0L,         // Оплата 주휴수당 (1.0x)
    val paidHolidayPay: Long = 0L,    // Оплата 유급휴일 (1.0x)
    val overtimePay: Long = 0L,       // Оплата сверхурочных (1.5x)
    val nightPay: Long = 0L,          // Доплата за ночные (+0.5x)
    val holidayPay: Long = 0L,        // Оплата праздничных/выходных (1.5x / 2.0x)
    val totalAllowancesPay: Long = 0L,// Доплаты и надбавки (지급 항목)
    val grossSalary: Long = 0L,       // Всего начислено (총지급액)
    val taxableAmount: Long = 0L      // Налогооблагаемая база (과세대상액)
)

/**
 * Детализация удержаний (Deductions)
 */
data class DeductionBreakdown(
    val businessTax33: Long = 0L,
    val nationalPension: Long = 0L,
    val healthInsurance: Long = 0L,
    val longTermCare: Long = 0L,
    val employmentInsurance: Long = 0L,
    val incomeTax: Long = 0L,
    val localIncomeTax: Long = 0L,
    val otherDeductions: Long = 0L,
    val totalDeductions: Long = 0L
)

/**
 * Полный результат расчета зарплаты
 */
data class SalaryResult(
    val breakdown: SalaryBreakdown = SalaryBreakdown(),
    val deductions: DeductionBreakdown = DeductionBreakdown(),
    val netSalary: Long = 0L,         // К выплате (실수령액)
    val isMixedMode: Boolean = false
)

/**
 * Структура резервной копии данных для Экспорта/Импорта JSON
 */
data class BackupPayload(
    val version: Int = 3,
    val timestamp: Long = System.currentTimeMillis(),
    val templates: List<ShiftTemplateEntity> = emptyList(),
    val dayRecords: List<DayRecordEntity> = emptyList(),
    val snapshots: List<MonthlySnapshotEntity> = emptyList()
)
