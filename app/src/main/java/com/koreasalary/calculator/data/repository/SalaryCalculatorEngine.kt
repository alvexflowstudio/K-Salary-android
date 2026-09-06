package com.koreasalary.calculator.data.repository

import com.koreasalary.calculator.data.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.max

/**
 * Расчётное ядро приложения.
 *
 * Основной сценарий приложения рассчитан на иностранного работника с удержанием
 * 3,3%. Социальные взносы и подоходный налог остаются отдельными опциями, потому
 * что они применяются только при подтверждённом статусе и основании начисления.
 */
object SalaryCalculatorEngine {

    private const val NPS_MIN_STANDARD_INCOME = 410_000L
    private const val NPS_MAX_STANDARD_INCOME = 6_590_000L
    private const val BASIC_PERSONAL_DEDUCTION = 1_500_000L

    private val WORKER_NPS_RATE = BigDecimal("0.0475")
    private val WORKER_HEALTH_RATE = BigDecimal("0.03595")
    private val EMPLOYMENT_INSURANCE_RATE = BigDecimal("0.009")
    private val BUSINESS_INCOME_RATE = BigDecimal("0.033")
    private val LOCAL_TAX_RATE = BigDecimal("0.10")
    private val LONG_TERM_CARE_RATE = BigDecimal("0.009448")
        .divide(BigDecimal("0.0719"), 12, RoundingMode.HALF_UP)

    /**
     * Основной расчёт заработка.
     *
     * Суммы надбавок считаются начислением и входят в базу удержаний. Любая
     * договорная доплата добавляется пользователем как обычная надбавка с
     * названием из расчётного листка.
     */
    fun calculateSalary(
        input: SalaryInputState,
        deductions: DeductionSettings
    ): SalaryResult {
        val hourlyRate = nonNegativeFinite(input.hourlyRate)
        val regularHours = nonNegativeFinite(input.regularHours)
        val sundayPayHours = nonNegativeFinite(input.sundayPayHours)
        val paidHolidayHours = nonNegativeFinite(input.paidHolidayHours)
        val overtimeHours = nonNegativeFinite(input.overtimeHours)
        val nightHours = nonNegativeFinite(input.nightHours)
        val holidayHours = nonNegativeFinite(input.holidayHours)
        val holidayOvertimeHours = nonNegativeFinite(input.holidayOvertimeHours)

        // 1. Оплата базовых отработанных часов (1.0x)
        val regularPay = money(regularHours * hourlyRate)

        // 2. Еженедельный оплачиваемый отдых (주휴수당, 1.0x)
        val sundayPay = money(sundayPayHours * hourlyRate)

        // 3. Оплачиваемые праздничные дни (유급휴일, 1.0x)
        val paidHolidayPay = money(paidHolidayHours * hourlyRate)

        // 4. Сверхурочные часы (연장근로): 1.5x.
        // Калькулятор ориентирован на типичный табель крупного предприятия;
        // отдельного режима для микропредприятий в продукте больше нет.
        val overtimePay = money(overtimeHours * hourlyRate * 1.5)

        // 5. Ночные часы (야간근로 22:00-06:00): доплата +0.5x.
        val nightPay = money(nightHours * hourlyRate * 0.5)

        // 6. Праздничная работа: до 8ч 1.5x, свыше 8ч 2.0x.
        val normalHoliday = holidayHours * hourlyRate * 1.5
        val overtimeHoliday = holidayOvertimeHours * hourlyRate * 2.0
        val holidayPay = money(normalHoliday + overtimeHoliday)

        // 7. Доплаты и надбавки: все значения проходят одну проверку.
        val totalAllowancesPay = money(
            nonNegativeFinite(input.allowances.fuelAllowance) +
                nonNegativeFinite(input.allowances.annualLeaveAllowance) +
                input.allowances.customBonuses.orEmpty().sumOf { nonNegativeFinite(it.amount) }
        )

        val grossSalary = saturatedAdd(
            regularPay,
            sundayPay,
            paidHolidayPay,
            overtimePay,
            nightPay,
            holidayPay,
            totalAllowancesPay
        )

        // В приложении нет отдельной льготной базы: пользовательские надбавки
        // считаются начислением, если расчётный листок не подтверждает иное.
        val taxableAmount = grossSalary

        val businessTax33 = if (deductions.isBusinessTax33Enabled) {
            percentage(taxableAmount, BUSINESS_INCOME_RATE)
        } else 0L

        val nationalPension = if (deductions.isNationalPensionEnabled) {
            percentage(nationalPensionBase(taxableAmount), WORKER_NPS_RATE)
        } else 0L

        val healthInsurance = if (deductions.isHealthInsuranceEnabled) {
            percentage(taxableAmount, WORKER_HEALTH_RATE)
        } else 0L

        val longTermCare = if (deductions.isLongTermCareEnabled) {
            val healthPremium = if (healthInsurance > 0L) {
                healthInsurance
            } else {
                percentage(taxableAmount, WORKER_HEALTH_RATE)
            }
            percentage(healthPremium, LONG_TERM_CARE_RATE)
        } else 0L

        val employmentInsurance = if (deductions.isEmploymentInsuranceEnabled) {
            percentage(taxableAmount, EMPLOYMENT_INSURANCE_RATE)
        } else 0L

        val (incomeTax, localIncomeTax) = if (deductions.isIncomeTaxEnabled) {
            calculateIncomeTax(taxableAmount)
        } else Pair(0L, 0L)

        val otherDeductions = money(
            deductions.customDeductions.orEmpty().sumOf { nonNegativeFinite(it.amount) }
        )

        val totalDeductions = saturatedAdd(
            businessTax33,
            nationalPension,
            healthInsurance,
            longTermCare,
            employmentInsurance,
            incomeTax,
            localIncomeTax,
            otherDeductions
        )

        return SalaryResult(
            breakdown = SalaryBreakdown(
                regularPay = regularPay,
                sundayPay = sundayPay,
                paidHolidayPay = paidHolidayPay,
                overtimePay = overtimePay,
                nightPay = nightPay,
                holidayPay = holidayPay,
                totalAllowancesPay = totalAllowancesPay,
                grossSalary = grossSalary,
                taxableAmount = taxableAmount
            ),
            deductions = DeductionBreakdown(
                businessTax33 = businessTax33,
                nationalPension = nationalPension,
                healthInsurance = healthInsurance,
                longTermCare = longTermCare,
                employmentInsurance = employmentInsurance,
                incomeTax = incomeTax,
                localIncomeTax = localIncomeTax,
                otherDeductions = otherDeductions,
                totalDeductions = totalDeductions
            ),
            netSalary = max(0L, grossSalary - totalDeductions),
            isMixedMode = deductions.isMixedMode
        )
    }

    /**
     * Предварительная оценка подоходного налога для режима заработной платы.
     *
     * Это годовой прогрессивный расчёт без иждивенцев, специальных вычетов и
     * налоговых кредитов. Он не пытается заменить таблицу ежемесячного удержания
     * работодателя, которая зависит от семейных данных и статуса работника.
     */
    private fun calculateIncomeTax(grossMonthly: Long): Pair<Long, Long> {
        val annualGross = saturatedMultiply(grossMonthly, 12L)
        val earnedIncomeDeduction = calculateEarnedIncomeDeduction(annualGross)
        val annualTaxableIncome = max(
            0L,
            annualGross - earnedIncomeDeduction - BASIC_PERSONAL_DEDUCTION
        )
        val annualNationalTax = progressiveIncomeTax(annualTaxableIncome)
        val monthlyNationalTax = divideAndRound(annualNationalTax, 12L)
        val localTax = percentage(monthlyNationalTax, LOCAL_TAX_RATE)
        return Pair(monthlyNationalTax, localTax)
    }

    private fun calculateEarnedIncomeDeduction(annualGross: Long): Long {
        // 소득세법 제47조의 근로소득공제에는 연 2,000만원 한도가 있다.
        return when {
            annualGross <= 5_000_000L -> percentage(annualGross, BigDecimal("0.70"))
            annualGross <= 15_000_000L -> 3_500_000L + percentage(annualGross - 5_000_000L, BigDecimal("0.40"))
            annualGross <= 45_000_000L -> 7_500_000L + percentage(annualGross - 15_000_000L, BigDecimal("0.15"))
            annualGross <= 100_000_000L -> 12_000_000L + percentage(annualGross - 45_000_000L, BigDecimal("0.05"))
            else -> 14_750_000L + percentage(annualGross - 100_000_000L, BigDecimal("0.02"))
        }.coerceAtMost(20_000_000L)
    }

    /** Прогрессивная шкала подоходного налога, действующая с 2023 года. */
    private fun progressiveIncomeTax(annualTaxableIncome: Long): Long {
        val tax = when {
            annualTaxableIncome <= 14_000_000L -> percentage(annualTaxableIncome, BigDecimal("0.06"))
            annualTaxableIncome <= 50_000_000L -> percentage(annualTaxableIncome, BigDecimal("0.15")) - 1_260_000L
            annualTaxableIncome <= 88_000_000L -> percentage(annualTaxableIncome, BigDecimal("0.24")) - 5_760_000L
            annualTaxableIncome <= 150_000_000L -> percentage(annualTaxableIncome, BigDecimal("0.35")) - 15_440_000L
            annualTaxableIncome <= 300_000_000L -> percentage(annualTaxableIncome, BigDecimal("0.38")) - 19_940_000L
            annualTaxableIncome <= 500_000_000L -> percentage(annualTaxableIncome, BigDecimal("0.40")) - 25_940_000L
            annualTaxableIncome <= 1_000_000_000L -> percentage(annualTaxableIncome, BigDecimal("0.42")) - 35_940_000L
            else -> percentage(annualTaxableIncome, BigDecimal("0.45")) - 65_940_000L
        }
        return tax.coerceAtLeast(0L)
    }

    /**
     * Национальная пенсия: база округляется вниз до 1 000 вон и ограничивается
     * действующим диапазоном стандартного месячного дохода 410 000–6 590 000.
     */
    private fun nationalPensionBase(grossSalary: Long): Long {
        if (grossSalary <= 0L) return 0L
        val roundedDownToThousand = (grossSalary / 1_000L) * 1_000L
        return roundedDownToThousand.coerceIn(
            NPS_MIN_STANDARD_INCOME,
            NPS_MAX_STANDARD_INCOME
        )
    }

    /**
     * Расчёт выходного пособия (퇴직금).
     *
     * Период последних трёх месяцев должен быть задан фактическим числом
     * календарных дней. Нулевой или отрицательный период делает результат
     * недействительным и больше не заменяется искусственными 92 днями.
     */
    fun calculateSeverance(input: SeveranceInputState): SeveranceResult {
        val serviceDays = input.totalDaysWorked.coerceAtLeast(0)
        val weeklyHours = nonNegativeFinite(input.averageWeeklyHours)
        val calculationPeriodDays = input.daysInLast3Months.coerceAtLeast(0)
        val isCalculationPeriodValid = calculationPeriodDays > 0
        val isEligible = serviceDays >= 365 && weeklyHours >= 15.0 && isCalculationPeriodValid

        val month1Salary = nonNegativeFinite(input.month1Salary)
        val month2Salary = nonNegativeFinite(input.month2Salary)
        val month3Salary = nonNegativeFinite(input.month3Salary)
        val annualBonus = nonNegativeFinite(input.annualBonus)
        val unusedAnnualLeavePay = nonNegativeFinite(input.unusedAnnualLeavePay)
        val baseQuarterSalary = month1Salary + month2Salary + month3Salary
        val bonusShare = annualBonus * 3.0 / 12.0
        val leaveShare = unusedAnnualLeavePay * 3.0 / 12.0
        val total3MonthsWage = baseQuarterSalary + bonusShare + leaveShare

        val averageDailyWage = if (isCalculationPeriodValid) {
            total3MonthsWage / calculationPeriodDays.toDouble()
        } else {
            0.0
        }
        val ordinaryDailyWage = nonNegativeFinite(input.ordinaryDailyWage)
        val appliedDailyWage = if (isCalculationPeriodValid) {
            max(averageDailyWage, ordinaryDailyWage)
        } else {
            0.0
        }

        val grossSeverancePay = if (isEligible) {
            money(appliedDailyWage * 30.0 * (serviceDays.toDouble() / 365.0))
        } else 0L

        val estimatedTax = calculateRetirementIncomeTax(grossSeverancePay, serviceDays)
        val netSeverancePay = max(0L, grossSeverancePay - estimatedTax)

        val departureInsurance = money(input.departureInsurancePaid)
        val differenceToPay = max(0L, grossSeverancePay - departureInsurance)

        return SeveranceResult(
            isEligible = isEligible,
            isCalculationPeriodValid = isCalculationPeriodValid,
            total3MonthsSalary = total3MonthsWage,
            bonusQuarterShare = bonusShare + leaveShare,
            averageDailyWage = averageDailyWage,
            appliedDailyWage = appliedDailyWage,
            grossSeverancePay = grossSeverancePay,
            estimatedRetirementTax = estimatedTax,
            netSeverancePay = netSeverancePay,
            departureInsuranceDeduction = departureInsurance,
            employerDifferenceToPay = differenceToPay
        )
    }

    /**
     * Оценка 퇴직소득세 по официальной последовательности:
     * вычет за стаж, пересчёт годового дохода, вычет из пересчитанной зарплаты,
     * прогрессивная шкала и местный налог 10%.
     */
    private fun calculateRetirementIncomeTax(grossSeverancePay: Long, serviceDays: Int): Long {
        if (grossSeverancePay <= 0L || serviceDays <= 0) return 0L

        // Для расчёта вычетов неполный год стажа округляется вверх.
        val serviceYears = max(1, ceil(serviceDays / 365.0).toInt())
        val serviceDeduction = calculateServiceDeduction(serviceYears)
        val retirementIncomeAfterDeduction = max(0L, grossSeverancePay - serviceDeduction)
        val convertedSalary = divideAndRound(
            saturatedMultiply(retirementIncomeAfterDeduction, 12L),
            serviceYears.toLong()
        )
        val convertedSalaryDeduction = calculateConvertedSalaryDeduction(convertedSalary)
        val convertedTaxBase = max(0L, convertedSalary - convertedSalaryDeduction)
        val convertedTax = progressiveIncomeTax(convertedTaxBase)
        val nationalTax = divideAndRound(
            saturatedMultiply(convertedTax, serviceYears.toLong()),
            12L
        )
        val localTax = percentage(nationalTax, LOCAL_TAX_RATE)
        return saturatedAdd(nationalTax, localTax)
    }

    private fun calculateServiceDeduction(serviceYears: Int): Long {
        return when {
            serviceYears <= 5 -> serviceYears * 1_000_000L
            serviceYears <= 10 -> 5_000_000L + (serviceYears - 5) * 2_000_000L
            serviceYears <= 20 -> 15_000_000L + (serviceYears - 10) * 2_500_000L
            else -> 40_000_000L + (serviceYears - 20) * 3_000_000L
        }
    }

    private fun calculateConvertedSalaryDeduction(convertedSalary: Long): Long {
        return when {
            convertedSalary <= 8_000_000L -> convertedSalary
            convertedSalary <= 70_000_000L -> 8_000_000L + percentage(convertedSalary - 8_000_000L, BigDecimal("0.60"))
            convertedSalary <= 100_000_000L -> 45_200_000L + percentage(convertedSalary - 70_000_000L, BigDecimal("0.55"))
            convertedSalary <= 300_000_000L -> 61_700_000L + percentage(convertedSalary - 100_000_000L, BigDecimal("0.45"))
            convertedSalary <= 500_000_000L -> 151_700_000L + percentage(convertedSalary - 300_000_000L, BigDecimal("0.35"))
            convertedSalary <= 1_000_000_000L -> 221_700_000L + percentage(convertedSalary - 500_000_000L, BigDecimal("0.25"))
            else -> 346_700_000L + percentage(convertedSalary - 1_000_000_000L, BigDecimal("0.05"))
        }
    }

    private fun percentage(amount: Long, rate: BigDecimal): Long {
        if (amount <= 0L) return 0L
        return BigDecimal.valueOf(amount)
            .multiply(rate)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun divideAndRound(numerator: Long, denominator: Long): Long {
        if (numerator <= 0L || denominator <= 0L) return 0L
        return BigDecimal.valueOf(numerator)
            .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun money(value: Double): Long {
        return when {
            value.isNaN() || value <= 0.0 -> 0L
            value.isInfinite() || value >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
            else -> BigDecimal.valueOf(value)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }
    }

    private fun nonNegativeFinite(value: Double): Double {
        return if (value.isFinite()) value.coerceAtLeast(0.0) else 0.0
    }

    private fun saturatedAdd(vararg values: Long): Long {
        var result = 0L
        values.forEach { value ->
            if (value <= 0L) return@forEach
            result = if (Long.MAX_VALUE - result < value) {
                Long.MAX_VALUE
            } else {
                result + value
            }
        }
        return result
    }

    private fun saturatedMultiply(value: Long, multiplier: Long): Long {
        if (value <= 0L || multiplier <= 0L) return 0L
        return if (value > Long.MAX_VALUE / multiplier) {
            Long.MAX_VALUE
        } else {
            value * multiplier
        }
    }
}
