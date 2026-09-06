package com.koreasalary.calculator.data.repository

import com.koreasalary.calculator.data.model.*
import org.junit.Assert.*
import org.junit.Test

class SalaryCalculatorEngineTest {

    @Test
    fun newUserDefaultsUseManualHourlyRateAndOnlyBusinessTax() {
        assertEquals(0.0, SalaryInputState().hourlyRate, 0.0)
        val deductions = DeductionSettings()
        assertTrue(deductions.isBusinessTax33Enabled)
        assertFalse(deductions.isNationalPensionEnabled)
        assertFalse(deductions.isHealthInsuranceEnabled)
        assertFalse(deductions.isLongTermCareEnabled)
        assertFalse(deductions.isEmploymentInsuranceEnabled)
        assertFalse(deductions.isIncomeTaxEnabled)
    }

    @Test
    fun testDefaultStateSalaryCalculation() {
        val input = SalaryInputState(
            hourlyRate = 10320.0,
            regularHours = 174.0, // 174 базовых часов
            overtimeHours = 0.0,
            nightHours = 0.0,
            holidayHours = 0.0,
            holidayOvertimeHours = 0.0,
            paidHolidayHours = 0.0,
            sundayPayHours = 35.0 // 35ч 주휴수당 = 209ч суммарно
        )
        val deductions = DeductionSettings(
            isBusinessTax33Enabled = true,
            isNationalPensionEnabled = false,
            isHealthInsuranceEnabled = false,
            isLongTermCareEnabled = false,
            isEmploymentInsuranceEnabled = false,
            isIncomeTaxEnabled = false
        )

        val result = SalaryCalculatorEngine.calculateSalary(input, deductions)

        // (174 + 35) * 10,320 = 209 * 10,320 = 2,156,880 KRW
        assertEquals(1795680L, result.breakdown.regularPay)
        assertEquals(361200L, result.breakdown.sundayPay)
        assertEquals(2156880L, result.breakdown.grossSalary)
        assertEquals(2156880L, result.breakdown.taxableAmount)

        // 3.3% tax = 2,156,880 * 0.033 = 71,177.04 -> 71,177 KRW
        assertEquals(71177L, result.deductions.businessTax33)
        assertEquals(0L, result.deductions.nationalPension)
        assertEquals(71177L, result.deductions.totalDeductions)

        // Net salary = 2,156,880 - 71,177 = 2,085,703 KRW
        assertEquals(2085703L, result.netSalary)
        assertFalse(result.isMixedMode)
    }

    @Test
    fun testOvertimeNightHolidayAndAllowances() {
        val input = SalaryInputState(
            hourlyRate = 10000.0,
            regularHours = 160.0,
            overtimeHours = 10.0, // 10 * 10,000 * 1.5 = 150,000
            nightHours = 10.0,    // 10 * 10,000 * 0.5 = 50,000
            holidayHours = 8.0,   // 8 * 10,000 * 1.5 = 120,000
            holidayOvertimeHours = 2.0, // 2 * 10,000 * 2.0 = 40,000
            paidHolidayHours = 8.0, // 8 * 10,000 * 1.0 = 80,000
            sundayPayHours = 32.0,  // 32 * 10,000 * 1.0 = 320,000
            allowances = AllowancesState(
                fuelAllowance = 100000.0,
                annualLeaveAllowance = 50000.0,
                customBonuses = listOf(AllowanceItem(title = "Бонус", amount = 50000.0))
            )
        )
        val deductions = DeductionSettings(
            isBusinessTax33Enabled = false
        )

        val result = SalaryCalculatorEngine.calculateSalary(input, deductions)

        // Regular: 160 * 10,000 = 1,600,000
        assertEquals(1600000L, result.breakdown.regularPay)
        // Sunday pay: 320,000
        assertEquals(320000L, result.breakdown.sundayPay)
        // Paid holiday: 80,000
        assertEquals(80000L, result.breakdown.paidHolidayPay)
        // Overtime: 150,000
        assertEquals(150000L, result.breakdown.overtimePay)
        // Night: 50,000
        assertEquals(50000L, result.breakdown.nightPay)
        // Holiday: 120,000 + 40,000 = 160,000
        assertEquals(160000L, result.breakdown.holidayPay)
        // Allowances: 100k + 50k + 50k = 200,000
        assertEquals(200000L, result.breakdown.totalAllowancesPay)

        // Gross: 1,600,000 + 320,000 + 80,000 + 150,000 + 50,000 + 160,000 + 200,000 = 2,560,000
        assertEquals(2560000L, result.breakdown.grossSalary)
    }

    @Test
    fun testCustomAllowanceIsIncludedWithoutArtificialCap() {
        val input = SalaryInputState(
            hourlyRate = 10000.0,
            regularHours = 1.0,
            allowances = AllowancesState(
                customBonuses = listOf(
                    AllowanceItem(title = "Доплата по договору", amount = 300000.0)
                )
            )
        )

        val result = SalaryCalculatorEngine.calculateSalary(
            input = input,
            deductions = DeductionSettings(isBusinessTax33Enabled = false)
        )

        assertEquals(300000L, result.breakdown.totalAllowancesPay)
        assertEquals(310000L, result.breakdown.grossSalary)
        assertEquals(310000L, result.breakdown.taxableAmount)
    }

    @Test
    fun testForeignWorkerMultipliersAreUsedWithoutCompanySizeOption() {
        val input = SalaryInputState(
            hourlyRate = 10000.0,
            regularHours = 160.0,
            overtimeHours = 10.0,
            nightHours = 10.0,
            holidayHours = 8.0,
            holidayOvertimeHours = 2.0
        )
        val deductions = DeductionSettings(isBusinessTax33Enabled = false)

        val result = SalaryCalculatorEngine.calculateSalary(input, deductions)

        // The product always uses the large-workplace default factors for its
        // foreign-worker use case; there is no company-size switch in the input.
        assertEquals(150000L, result.breakdown.overtimePay)
        assertEquals(50000L, result.breakdown.nightPay)
        assertEquals(160000L, result.breakdown.holidayPay)
    }

    @Test
    fun testMixedModeDetectionAndDeductions() {
        val deductions = DeductionSettings(
            isBusinessTax33Enabled = true,
            isNationalPensionEnabled = true,
            isHealthInsuranceEnabled = true,
            isLongTermCareEnabled = true,
            isEmploymentInsuranceEnabled = true
        )
        assertTrue(deductions.isMixedMode)
    }

    @Test
    fun testCurrentWorkerInsuranceRatesAndNpsLimits() {
        val oneMillionInput = SalaryInputState(
            hourlyRate = 1_000_000.0,
            regularHours = 1.0
        )
        val deductions = DeductionSettings(
            isBusinessTax33Enabled = false,
            isNationalPensionEnabled = true,
            isHealthInsuranceEnabled = true,
            isLongTermCareEnabled = true,
            isEmploymentInsuranceEnabled = true
        )

        val result = SalaryCalculatorEngine.calculateSalary(oneMillionInput, deductions)

        assertEquals(47_500L, result.deductions.nationalPension)
        assertEquals(35_950L, result.deductions.healthInsurance)
        assertEquals(4_724L, result.deductions.longTermCare)
        assertEquals(9_000L, result.deductions.employmentInsurance)

        val lowIncomeResult = SalaryCalculatorEngine.calculateSalary(
            oneMillionInput.copy(hourlyRate = 100_000.0),
            deductions.copy(isHealthInsuranceEnabled = false, isLongTermCareEnabled = false, isEmploymentInsuranceEnabled = false)
        )
        // NPS standard monthly income cannot fall below 410,000 won when the
        // optional workplace pension calculation is enabled.
        assertEquals(19_475L, lowIncomeResult.deductions.nationalPension)
    }

    @Test
    fun testIncomeTaxEstimateUsesAnnualProgressiveScaleAndLocalTax() {
        val result = SalaryCalculatorEngine.calculateSalary(
            input = SalaryInputState(hourlyRate = 3_000_000.0, regularHours = 1.0),
            deductions = DeductionSettings(
                isBusinessTax33Enabled = false,
                isIncomeTaxEnabled = true
            )
        )

        // Annual gross 36,000,000; taxable estimate after earned-income and
        // basic deductions is 23,850,000. National tax is 193,125/month.
        assertEquals(193_125L, result.deductions.incomeTax)
        assertEquals(19_313L, result.deductions.localIncomeTax)
        assertEquals(212_438L, result.deductions.totalDeductions)
    }

    @Test
    fun testIncomeTaxCapsEarnedIncomeDeductionAtTwentyMillionWon() {
        val result = SalaryCalculatorEngine.calculateSalary(
            input = SalaryInputState(hourlyRate = 40_000_000.0, regularHours = 1.0),
            deductions = DeductionSettings(
                isBusinessTax33Enabled = false,
                isIncomeTaxEnabled = true
            )
        )

        // Annual gross is 480,000,000 won. The uncapped formula would produce
        // a 22,350,000 won earned-income deduction, but the Income Tax Act
        // limits it to 20,000,000 won.
        assertEquals(13_121_667L, result.deductions.incomeTax)
        assertEquals(1_312_167L, result.deductions.localIncomeTax)
    }

    @Test
    fun testCustomDeductionsAreIncludedInTotalAndNetSalary() {
        val result = SalaryCalculatorEngine.calculateSalary(
            input = SalaryInputState(hourlyRate = 10_000.0, regularHours = 8.0),
            deductions = DeductionSettings(
                isBusinessTax33Enabled = false,
                customDeductions = listOf(
                    CustomDeductionItem(title = "Жильё", amount = 12_345.0),
                    CustomDeductionItem(title = "Штраф", amount = 655.0)
                )
            )
        )

        assertEquals(80_000L, result.breakdown.grossSalary)
        assertEquals(13_000L, result.deductions.otherDeductions)
        assertEquals(13_000L, result.deductions.totalDeductions)
        assertEquals(67_000L, result.netSalary)
    }

    @Test
    fun testSeveranceCalculationAndEligibility() {
        val eligibleInput = SeveranceInputState(
            month1Salary = 3000000.0,
            month2Salary = 3000000.0,
            month3Salary = 3000000.0,
            annualBonus = 0.0,
            unusedAnnualLeavePay = 0.0,
            totalDaysWorked = 730, // 2 years
            daysInLast3Months = 90,
            departureInsurancePaid = 4000000.0
        )

        val result = SalaryCalculatorEngine.calculateSeverance(eligibleInput)

        assertTrue(result.isEligible)
        // Average daily: 9,000,000 / 90 = 100,000 KRW/day
        assertEquals(100000.0, result.averageDailyWage, 0.001)
        // Severance: 100,000 * 30 * (730 / 365) = 6,000,000 KRW
        assertEquals(6000000L, result.grossSeverancePay)
        // Employer difference: 6,000,000 - 4,000,000 = 2,000,000 KRW
        assertEquals(2000000L, result.employerDifferenceToPay)
        // Official retirement-income tax sequence, including 10% local tax.
        assertEquals(70400L, result.estimatedRetirementTax)
    }

    @Test
    fun testSeveranceRequiresOneYearAndFifteenWeeklyHours() {
        val baseline = SeveranceInputState(
            totalDaysWorked = 365,
            averageWeeklyHours = 40.0
        )

        assertFalse(
            SalaryCalculatorEngine.calculateSeverance(
                baseline.copy(totalDaysWorked = 364)
            ).isEligible
        )
        assertFalse(
            SalaryCalculatorEngine.calculateSeverance(
                baseline.copy(averageWeeklyHours = 14.99)
            ).isEligible
        )
        assertTrue(
            SalaryCalculatorEngine.calculateSeverance(
                baseline.copy(averageWeeklyHours = 15.0)
            ).isEligible
        )
    }

    @Test
    fun severanceAmountDoesNotChangeForQualifyingWeeklyHours() {
        val baseline = SeveranceInputState(
            month1Salary = 3_000_000.0,
            month2Salary = 3_000_000.0,
            month3Salary = 3_000_000.0,
            totalDaysWorked = 730,
            daysInLast3Months = 90
        )

        val results = listOf(30.0, 50.0, 60.0).map { weeklyHours ->
            SalaryCalculatorEngine.calculateSeverance(
                baseline.copy(averageWeeklyHours = weeklyHours)
            )
        }

        assertTrue(results.all { it.isEligible })
        assertEquals(1, results.map { it.grossSeverancePay }.toSet().size)
    }

    @Test
    fun testSeveranceUsesHigherOrdinaryDailyWage() {
        val input = SeveranceInputState(
            month1Salary = 3000000.0,
            month2Salary = 3000000.0,
            month3Salary = 3000000.0,
            totalDaysWorked = 730,
            daysInLast3Months = 90,
            ordinaryDailyWage = 120000.0
        )

        val result = SalaryCalculatorEngine.calculateSeverance(input)

        assertEquals(100000.0, result.averageDailyWage, 0.001)
        assertEquals(120000.0, result.appliedDailyWage, 0.001)
        assertEquals(7200000L, result.grossSeverancePay)
    }

    @Test
    fun testSeveranceRejectsInvalidCalculationPeriod() {
        val input = SeveranceInputState(
            month1Salary = 3000000.0,
            month2Salary = 3000000.0,
            month3Salary = 3000000.0,
            totalDaysWorked = 365,
            daysInLast3Months = 0
        )

        val result = SalaryCalculatorEngine.calculateSeverance(input)

        assertFalse(result.isCalculationPeriodValid)
        assertFalse(result.isEligible)
        assertEquals(0.0, result.averageDailyWage, 0.001)
        assertEquals(0.0, result.appliedDailyWage, 0.001)
        assertEquals(0L, result.grossSeverancePay)
    }

    @Test
    fun testSeveranceSanitizesInvalidNumericInput() {
        val input = SeveranceInputState(
            month1Salary = -100.0,
            month2Salary = Double.NaN,
            month3Salary = Double.POSITIVE_INFINITY,
            annualBonus = -500.0,
            unusedAnnualLeavePay = Double.NaN,
            totalDaysWorked = -1,
            daysInLast3Months = 0,
            averageWeeklyHours = Double.NaN,
            ordinaryDailyWage = Double.POSITIVE_INFINITY,
            departureInsurancePaid = -100.0
        )

        val result = SalaryCalculatorEngine.calculateSeverance(input)

        assertFalse(result.isEligible)
        assertEquals(0.0, result.averageDailyWage, 0.001)
        assertEquals(0.0, result.appliedDailyWage, 0.001)
        assertEquals(0L, result.grossSeverancePay)
        assertEquals(0L, result.departureInsuranceDeduction)
    }
}
