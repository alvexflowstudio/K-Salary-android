package com.koreasalary.calculator.data.model

/**
 * Входные данные для расчета выходного пособия (퇴직금)
 */
data class SeveranceInputState(
    val month1Salary: Double = 3000000.0, // Зарплата за последний 1-й месяц (최근 1개월 급여)
    val month2Salary: Double = 3000000.0, // Зарплата за последний 2-й месяц (최근 2개월 급여)
    val month3Salary: Double = 3000000.0, // Зарплата за последний 3-й месяц (최근 3개월 급여)
    val annualBonus: Double = 0.0,        // Годовые бонусы/премии (연간 상여금 총액의 3/12 반영)
    val unusedAnnualLeavePay: Double = 0.0, // Компенсация за неиспользованный отпуск (연차수당 3/12 반영)
    val totalDaysWorked: Int = 730,       // Общее количество дней непрерывной работы (например, 2 года = 730 дней)
    val daysInLast3Months: Int = 92,      // Календарных дней в расчетном периоде
    // Сохраняем показатель для совместимости с проверками и старым кодом.
    // Пользовательский ввод убран из окна: условие 15 часов проверяется по документам.
    val averageWeeklyHours: Double = 40.0,
    val ordinaryDailyWage: Double = 0.0, // Обычная дневная зарплата, если она выше средней
    val departureInsurancePaid: Double = 0.0 // Выплата страхования выезда 출국만기보험 (для виз E-9 / H-2)
)

/**
 * Результат расчета выходного пособия (퇴직금)
 */
data class SeveranceResult(
    val isEligible: Boolean = false,          // 1 год непрерывной работы и средняя занятость от 15 часов в неделю
    val isCalculationPeriodValid: Boolean = true, // Введено положительное число календарных дней
    val total3MonthsSalary: Double = 0.0,     // Суммарный заработок за 3 месяца
    val bonusQuarterShare: Double = 0.0,      // Доля бонусов за 3 месяца
    val averageDailyWage: Double = 0.0,       // Среднедневной заработок (1일 평균임금)
    val appliedDailyWage: Double = 0.0,        // Дневная сумма после сравнения с обычной зарплатой
    val grossSeverancePay: Long = 0L,         // Начисленное выходное пособие (법정 퇴직금)
    val estimatedRetirementTax: Long = 0L,    // Примерный налог на выходное пособие (퇴직소득세)
    val netSeverancePay: Long = 0L,           // Выходное пособие на руки (세후 실수령액)
    val departureInsuranceDeduction: Long = 0L, // Сумма 출국만기보험
    val employerDifferenceToPay: Long = 0L    // Доплата работодателя (퇴직금 차액)
)
