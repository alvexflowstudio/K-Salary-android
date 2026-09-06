package com.koreasalary.calculator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.koreasalary.calculator.data.model.SeveranceInputState
import com.koreasalary.calculator.data.model.SeveranceResult
import com.koreasalary.calculator.data.repository.SalaryCalculatorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SeveranceViewModel : ViewModel() {

    private val _inputState = MutableStateFlow(SeveranceInputState())
    val inputState: StateFlow<SeveranceInputState> = _inputState.asStateFlow()

    private val _result = MutableStateFlow(SalaryCalculatorEngine.calculateSeverance(SeveranceInputState()))
    val result: StateFlow<SeveranceResult> = _result.asStateFlow()

    private fun recalculate() {
        _result.value = SalaryCalculatorEngine.calculateSeverance(_inputState.value)
    }

    fun updateMonth1Salary(salary: Double) {
        _inputState.update { it.copy(month1Salary = normalizedAmount(salary)) }
        recalculate()
    }

    fun updateMonth2Salary(salary: Double) {
        _inputState.update { it.copy(month2Salary = normalizedAmount(salary)) }
        recalculate()
    }

    fun updateMonth3Salary(salary: Double) {
        _inputState.update { it.copy(month3Salary = normalizedAmount(salary)) }
        recalculate()
    }

    fun updateAnnualBonus(bonus: Double) {
        _inputState.update { it.copy(annualBonus = normalizedAmount(bonus)) }
        recalculate()
    }

    fun updateUnusedLeave(leavePay: Double) {
        _inputState.update { it.copy(unusedAnnualLeavePay = normalizedAmount(leavePay)) }
        recalculate()
    }

    fun updateTotalDaysWorked(days: Int) {
        _inputState.update { it.copy(totalDaysWorked = days.coerceAtLeast(0)) }
        recalculate()
    }

    fun updateDaysInLast3Months(days: Int) {
        _inputState.update { it.copy(daysInLast3Months = days.coerceAtLeast(0)) }
        recalculate()
    }

    fun updateOrdinaryDailyWage(wage: Double) {
        _inputState.update { it.copy(ordinaryDailyWage = normalizedAmount(wage)) }
        recalculate()
    }

    fun updateDepartureInsurance(paid: Double) {
        _inputState.update { it.copy(departureInsurancePaid = normalizedAmount(paid)) }
        recalculate()
    }

    fun reset() {
        _inputState.value = SeveranceInputState()
        recalculate()
    }

    private fun normalizedAmount(value: Double): Double {
        return if (value.isFinite()) value.coerceAtLeast(0.0) else 0.0
    }
}
