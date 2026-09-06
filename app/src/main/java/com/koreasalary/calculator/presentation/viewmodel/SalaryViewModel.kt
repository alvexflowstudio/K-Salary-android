package com.koreasalary.calculator.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koreasalary.calculator.data.local.AppDatabase
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.local.entity.ShiftTemplateEntity
import com.koreasalary.calculator.data.model.*
import com.koreasalary.calculator.data.repository.SalaryCalculatorEngine
import com.koreasalary.calculator.data.repository.SalaryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SalaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SalaryRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = SalaryRepository(db)
    }

    // Текущий выбранный год и месяц календаря. Ставка по умолчанию и календарь
    // должны открываться в текущем месяце, а не в месяце старой разработки.
    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth.asStateFlow()

    // Шаблоны смен из Room Database
    val shiftTemplates: StateFlow<List<ShiftTemplateEntity>> = repository.getAllShiftTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Активный шаблон: если он выбран, клик по дате сразу применяет его.
    private val _activeTemplate = MutableStateFlow<ShiftTemplateEntity?>(null)
    val activeTemplate: StateFlow<ShiftTemplateEntity?> = _activeTemplate.asStateFlow()

    // Записи дней текущего месяца из Room Database
    val monthDayRecords: StateFlow<Map<Int, DayRecordEntity>> = _currentYearMonth
        .flatMapLatest { ym ->
            val ymString = String.format(Locale.ROOT, "%04d-%02d", ym.year, ym.monthValue)
            repository.getDayRecordsForMonth(ymString)
        }
        .map { records ->
            records.associateBy { record ->
                val parts = record.date.split("-")
                parts.lastOrNull()?.toIntOrNull() ?: 1
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Входные данные для расчета зарплаты
    private val _inputState = MutableStateFlow(SalaryInputState())
    val inputState: StateFlow<SalaryInputState> = _inputState.asStateFlow()

    // Настройки вычетов: 3,3% включён по умолчанию, остальные режимы выбираются отдельно.
    private val _deductionsState = MutableStateFlow(DeductionSettings())
    val deductionsState: StateFlow<DeductionSettings> = _deductionsState.asStateFlow()

    // Результат расчета зарплаты
    val salaryResult: StateFlow<SalaryResult> = combine(_inputState, _deductionsState) { input, deductions ->
        SalaryCalculatorEngine.calculateSalary(input, deductions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalaryCalculatorEngine.calculateSalary(SalaryInputState(), DeductionSettings())
    )

    init {
        // Подписка на обновление записей дней и пересчет итогов
        viewModelScope.launch {
            combine(monthDayRecords, _inputState) { records, input ->
                Pair(records, input.allowances)
            }.collect { (recordsMap, allowances) ->
                val records = recordsMap.values
                val normalizedRecords = records.map { record ->
                    normalizeWorkHours(
                        regularHours = record.regularHours,
                        overtimeHours = record.overtimeHours,
                        nightHours = record.nightHours,
                        holidayHours = record.holidayHours,
                        holidayOvertimeHours = record.holidayOvertimeHours,
                        isSpecialDay = record.isPaidHoliday || record.isSundayPay
                    )
                }
                val regularHours = normalizedRecords.sumOf { it.regularHours }
                val overtimeHours = normalizedRecords.sumOf { it.overtimeHours }
                val nightHours = normalizedRecords.sumOf { it.nightHours }
                val holidayHours = normalizedRecords.sumOf { it.holidayHours }
                val holidayOvertimeHours = normalizedRecords.sumOf { it.holidayOvertimeHours }

                // Для оплачиваемого праздника и еженедельного отдыха берём
                // обычную базовую смену месяца. Если в месяце ещё нет смен,
                // используем стандартные 8 часов, которые видит пользователь.
                val averageScheduledDayHours = records
                    .filter { record -> !record.isPaidHoliday && !record.isSundayPay }
                    .map { scheduledBaseHours(it) }
                    .filter { it > 0.0 }
                    .let { hours -> if (hours.isEmpty()) 8.0 else hours.average() }
                val paidHolidayHours = records
                    .filter { it.isPaidHoliday }
                    .sumOf {
                        scheduledBaseHours(it)
                            .takeIf { hours -> hours > 0.0 } ?: averageScheduledDayHours
                    }
                val sundayPayHours = records
                    .filter { it.isSundayPay }
                    .sumOf {
                        scheduledBaseHours(it)
                            .takeIf { hours -> hours > 0.0 } ?: averageScheduledDayHours
                    }

                _inputState.update {
                    it.copy(
                        regularHours = regularHours,
                        overtimeHours = overtimeHours,
                        nightHours = nightHours,
                        holidayHours = holidayHours,
                        holidayOvertimeHours = holidayOvertimeHours,
                        paidHolidayHours = paidHolidayHours,
                        sundayPayHours = sundayPayHours,
                        allowances = allowances
                    )
                }
            }
        }

        // Загрузка снимка настроек при смене месяца
        viewModelScope.launch {
            _currentYearMonth.collect {
                reloadCurrentMonthState()
            }
        }
    }

    // --- Навигация календаря ---
    fun previousMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(1)
    }

    fun setYearMonth(yearMonth: YearMonth) {
        _currentYearMonth.value = yearMonth
    }

    // --- Управление шаблонами ---
    fun toggleTemplate(template: ShiftTemplateEntity) {
        if (_activeTemplate.value?.id == template.id) {
            _activeTemplate.value = null
        } else {
            _activeTemplate.value = template
        }
    }

    fun clearActiveTemplate() {
        _activeTemplate.value = null
    }

    fun applyTemplateToDay(day: Int, template: ShiftTemplateEntity) {
        applyTemplateToDays(setOf(day), template)
    }

    fun toggleTemplateOnDay(day: Int, template: ShiftTemplateEntity) {
        val ym = _currentYearMonth.value
        val dateStr = String.format(Locale.ROOT, "%04d-%02d-%02d", ym.year, ym.monthValue, day)
        viewModelScope.launch {
            val existingRecord = repository.getDayRecord(dateStr)
            if (existingRecord?.templateId == template.id) {
                repository.deleteDayRecord(dateStr)
            } else {
                repository.saveDayRecord(buildTemplateRecord(ym, day, template))
            }
            saveCurrentMonthSnapshot()
        }
    }

    fun applyTemplateToDays(days: Set<Int>, template: ShiftTemplateEntity) {
        val ym = _currentYearMonth.value
        val records = days.map { day -> buildTemplateRecord(ym, day, template) }
        viewModelScope.launch {
            repository.saveDayRecords(records)
            saveCurrentMonthSnapshot()
        }
    }

    private fun buildTemplateRecord(
        ym: YearMonth,
        day: Int,
        template: ShiftTemplateEntity
    ): DayRecordEntity {
        val ymString = String.format(Locale.ROOT, "%04d-%02d", ym.year, ym.monthValue)
        val dateStr = String.format(Locale.ROOT, "%04d-%02d-%02d", ym.year, ym.monthValue, day)
        val hours = normalizeWorkHours(
            regularHours = template.regularHours,
            overtimeHours = template.overtimeHours,
            nightHours = template.nightHours,
            holidayHours = template.holidayHours,
            holidayOvertimeHours = template.holidayOvertimeHours,
            isSpecialDay = template.isPaidHoliday || template.isSundayPay
        )
        val scheduledBaseHours = normalizedHours(
            template.scheduledBaseHours.takeIf { it > 0.0 } ?: template.regularHours,
            8.0
        )
        return DayRecordEntity(
            date = dateStr,
            yearMonth = ymString,
            regularHours = hours.regularHours,
            scheduledBaseHours = scheduledBaseHours,
            overtimeHours = hours.overtimeHours,
            nightHours = hours.nightHours,
            holidayHours = hours.holidayHours,
            holidayOvertimeHours = hours.holidayOvertimeHours,
            isPaidHoliday = template.isPaidHoliday,
            isSundayPay = template.isSundayPay,
            templateId = template.id,
            // Встроенное имя локализуется на экране через templateId и не должно
            // попадать в пользовательскую заметку в виде русского текста.
            note = if (template.isDefault) "" else template.title
        )
    }

    // --- Ручной ввод дня ---
    fun saveCustomDayRecord(
        day: Int,
        regularHours: Double,
        overtimeHours: Double,
        nightHours: Double,
        holidayHours: Double,
        holidayOvertimeHours: Double,
        isPaidHoliday: Boolean,
        isSundayPay: Boolean,
        note: String,
        scheduledBaseHours: Double = regularHours
    ) {
        val ym = _currentYearMonth.value
        val ymString = String.format(Locale.ROOT, "%04d-%02d", ym.year, ym.monthValue)
        val dateStr = String.format(Locale.ROOT, "%04d-%02d-%02d", ym.year, ym.monthValue, day)

        val hours = normalizeWorkHours(
            regularHours = regularHours,
            overtimeHours = overtimeHours,
            nightHours = nightHours,
            holidayHours = holidayHours,
            holidayOvertimeHours = holidayOvertimeHours,
            isSpecialDay = isPaidHoliday || isSundayPay
        )
        val record = DayRecordEntity(
            date = dateStr,
            yearMonth = ymString,
            regularHours = hours.regularHours,
            scheduledBaseHours = normalizedHours(scheduledBaseHours, 8.0),
            overtimeHours = hours.overtimeHours,
            nightHours = hours.nightHours,
            holidayHours = hours.holidayHours,
            holidayOvertimeHours = hours.holidayOvertimeHours,
            isPaidHoliday = isPaidHoliday,
            isSundayPay = isSundayPay,
            templateId = null,
            note = note.trim()
        )
        viewModelScope.launch {
            repository.saveDayRecord(record)
            saveCurrentMonthSnapshot()
        }
    }

    fun clearDayRecord(day: Int) {
        val ym = _currentYearMonth.value
        val dateStr = String.format(Locale.ROOT, "%04d-%02d-%02d", ym.year, ym.monthValue, day)
        viewModelScope.launch {
            repository.deleteDayRecord(dateStr)
            saveCurrentMonthSnapshot()
        }
    }

    // --- CRUD шаблонов ---
    fun createShiftTemplate(
        title: String,
        colorHex: String,
        regularHours: Double,
        overtimeHours: Double,
        nightHours: Double,
        holidayHours: Double,
        holidayOvertimeHours: Double,
        isPaidHoliday: Boolean,
        isSundayPay: Boolean,
        scheduledBaseHours: Double = regularHours
    ) {
        val hours = normalizeWorkHours(
            regularHours = regularHours,
            overtimeHours = overtimeHours,
            nightHours = nightHours,
            holidayHours = holidayHours,
            holidayOvertimeHours = holidayOvertimeHours,
            isSpecialDay = isPaidHoliday || isSundayPay
        )
        val template = ShiftTemplateEntity(
            title = title.trim(),
            colorHex = colorHex,
            regularHours = hours.regularHours,
            scheduledBaseHours = normalizedHours(scheduledBaseHours, 8.0),
            overtimeHours = hours.overtimeHours,
            nightHours = hours.nightHours,
            holidayHours = hours.holidayHours,
            holidayOvertimeHours = hours.holidayOvertimeHours,
            isPaidHoliday = isPaidHoliday,
            isSundayPay = isSundayPay,
            isDefault = false
        )
        viewModelScope.launch {
            repository.saveShiftTemplate(template)
        }
    }

    fun updateShiftTemplate(template: ShiftTemplateEntity) {
        viewModelScope.launch {
            val hours = normalizeWorkHours(
                regularHours = template.regularHours,
                overtimeHours = template.overtimeHours,
                nightHours = template.nightHours,
                holidayHours = template.holidayHours,
                holidayOvertimeHours = template.holidayOvertimeHours,
                isSpecialDay = template.isPaidHoliday || template.isSundayPay
            )
            repository.saveShiftTemplate(
                template.copy(
                    regularHours = hours.regularHours,
                    scheduledBaseHours = normalizedHours(
                        template.scheduledBaseHours.takeIf { it > 0.0 } ?: template.regularHours,
                        8.0
                    ),
                    overtimeHours = hours.overtimeHours,
                    nightHours = hours.nightHours,
                    holidayHours = hours.holidayHours,
                    holidayOvertimeHours = hours.holidayOvertimeHours
                )
            )
        }
    }

    fun deleteShiftTemplate(templateId: Long) {
        viewModelScope.launch {
            if (_activeTemplate.value?.id == templateId) {
                _activeTemplate.value = null
            }
            repository.deleteShiftTemplate(templateId)
        }
    }

    // --- Ставка и надбавки ---
    fun updateHourlyRate(rate: Double) {
        _inputState.update { it.copy(hourlyRate = normalizedHours(rate)) }
        saveCurrentMonthSnapshot()
    }

    fun updateFuelAllowance(amount: Double) {
        _inputState.update {
            it.copy(allowances = it.allowances.copy(fuelAllowance = normalizedHours(amount)))
        }
        saveCurrentMonthSnapshot()
    }

    fun updateAnnualLeaveAllowance(amount: Double) {
        _inputState.update {
            it.copy(allowances = it.allowances.copy(annualLeaveAllowance = normalizedHours(amount)))
        }
        saveCurrentMonthSnapshot()
    }

    fun addCustomBonus(title: String, amount: Double) {
        if (title.isBlank()) return
        val item = AllowanceItem(title = title.trim(), amount = normalizedHours(amount))
        _inputState.update {
            it.copy(allowances = it.allowances.copy(customBonuses = it.allowances.customBonuses.orEmpty() + item))
        }
        saveCurrentMonthSnapshot()
    }

    fun removeCustomBonus(id: String) {
        _inputState.update {
            it.copy(allowances = it.allowances.copy(customBonuses = it.allowances.customBonuses.orEmpty().filterNot { item -> item.id == id }))
        }
        saveCurrentMonthSnapshot()
    }

    fun addCustomDeduction(title: String, amount: Double) {
        if (title.isBlank()) return
        val item = CustomDeductionItem(title = title.trim(), amount = normalizedHours(amount))
        _deductionsState.update {
            it.copy(customDeductions = it.customDeductions.orEmpty() + item)
        }
        saveCurrentMonthSnapshot()
    }

    fun removeCustomDeduction(id: String) {
        _deductionsState.update {
            it.copy(customDeductions = it.customDeductions.orEmpty().filterNot { item -> item.id == id })
        }
        saveCurrentMonthSnapshot()
    }

    // --- Настройки вычетов ---
    fun toggleBusinessTax33(enabled: Boolean) {
        _deductionsState.update { it.copy(isBusinessTax33Enabled = enabled) }
        saveCurrentMonthSnapshot()
    }

    fun toggleNationalPension(enabled: Boolean) {
        _deductionsState.update { it.copy(isNationalPensionEnabled = enabled) }
        saveCurrentMonthSnapshot()
    }

    fun toggleHealthInsurance(enabled: Boolean) {
        _deductionsState.update { it.copy(isHealthInsuranceEnabled = enabled) }
        saveCurrentMonthSnapshot()
    }

    fun toggleLongTermCare(enabled: Boolean) {
        _deductionsState.update { it.copy(isLongTermCareEnabled = enabled) }
        saveCurrentMonthSnapshot()
    }

    fun toggleEmploymentInsurance(enabled: Boolean) {
        _deductionsState.update { it.copy(isEmploymentInsuranceEnabled = enabled) }
        saveCurrentMonthSnapshot()
    }

    fun toggleIncomeTax(enabled: Boolean) {
        _deductionsState.update { it.copy(isIncomeTaxEnabled = enabled) }
        saveCurrentMonthSnapshot()
    }

    fun applyPreset(preset: PresetType) {
        when (preset) {
            PresetType.BUSINESS_3_3 -> {
                _deductionsState.update {
                    it.copy(
                        isBusinessTax33Enabled = true,
                        isNationalPensionEnabled = false,
                        isHealthInsuranceEnabled = false,
                        isLongTermCareEnabled = false,
                        isEmploymentInsuranceEnabled = false,
                        isIncomeTaxEnabled = false
                    )
                }
            }
            PresetType.FOUR_INSURANCES -> {
                _deductionsState.update {
                    it.copy(
                        isBusinessTax33Enabled = false,
                        isNationalPensionEnabled = true,
                        isHealthInsuranceEnabled = true,
                        isLongTermCareEnabled = true,
                        isEmploymentInsuranceEnabled = true,
                        isIncomeTaxEnabled = false
                    )
                }
            }
            PresetType.FULL_EMPLOYEE -> {
                _deductionsState.update {
                    it.copy(
                        isBusinessTax33Enabled = false,
                        isNationalPensionEnabled = true,
                        isHealthInsuranceEnabled = true,
                        isLongTermCareEnabled = true,
                        isEmploymentInsuranceEnabled = true,
                        isIncomeTaxEnabled = true
                    )
                }
            }
            PresetType.NO_DEDUCTIONS -> {
                _deductionsState.update {
                    it.copy(
                        isBusinessTax33Enabled = false,
                        isNationalPensionEnabled = false,
                        isHealthInsuranceEnabled = false,
                        isLongTermCareEnabled = false,
                        isEmploymentInsuranceEnabled = false,
                        isIncomeTaxEnabled = false
                    )
                }
            }
        }
        saveCurrentMonthSnapshot()
    }

    private fun normalizedHours(value: Double, maximum: Double? = null): Double {
        if (!value.isFinite()) return 0.0
        val nonNegative = value.coerceAtLeast(0.0)
        return maximum?.let { nonNegative.coerceAtMost(it) } ?: nonNegative
    }

    private fun scheduledBaseHours(record: DayRecordEntity): Double {
        return normalizedHours(
            record.scheduledBaseHours.takeIf { it > 0.0 } ?: record.regularHours,
            8.0
        )
    }

    private data class NormalizedWorkHours(
        val regularHours: Double,
        val overtimeHours: Double,
        val nightHours: Double,
        val holidayHours: Double,
        val holidayOvertimeHours: Double
    )

    /**
     * Праздничная или еженедельная работа не может одновременно оставаться
     * базовыми часами. При включённой отметке переносим введённые базовые часы
     * в праздничные поля, чтобы одна и та же работа не считалась дважды.
     */
    private fun normalizeWorkHours(
        regularHours: Double,
        overtimeHours: Double,
        nightHours: Double,
        holidayHours: Double,
        holidayOvertimeHours: Double,
        isSpecialDay: Boolean
    ): NormalizedWorkHours {
        val regular = normalizedHours(regularHours, 8.0)
        val rawHoliday = normalizedHours(holidayHours)
        val holiday = rawHoliday.coerceAtMost(8.0)
        val holidayOvertime = normalizedHours(holidayOvertimeHours) + max(0.0, rawHoliday - holiday)
        if (!isSpecialDay) {
            return NormalizedWorkHours(
                regularHours = regular,
                overtimeHours = normalizedHours(overtimeHours),
                nightHours = normalizedHours(nightHours),
                holidayHours = holiday,
                holidayOvertimeHours = holidayOvertime
            )
        }

        val holidayCapacity = (8.0 - holiday).coerceAtLeast(0.0)
        val movedToHoliday = min(regular, holidayCapacity)
        val movedToHolidayOvertime = max(0.0, regular - movedToHoliday)
        return NormalizedWorkHours(
            regularHours = 0.0,
            overtimeHours = normalizedHours(overtimeHours),
            nightHours = normalizedHours(nightHours),
            holidayHours = holiday + movedToHoliday,
            holidayOvertimeHours = holidayOvertime + movedToHolidayOvertime
        )
    }

    private fun saveCurrentMonthSnapshot() {
        val ym = _currentYearMonth.value
        val ymString = String.format(Locale.ROOT, "%04d-%02d", ym.year, ym.monthValue)
        viewModelScope.launch {
            repository.saveMonthlySnapshot(
                yearMonth = ymString,
                hourlyRate = _inputState.value.hourlyRate,
                deductions = _deductionsState.value,
                allowances = _inputState.value.allowances
            )
        }
    }

    // --- Экспорт / Импорт / Сброс данных ---
    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataToJson()
            onResult(json)
        }
    }

    fun importBackupJson(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importDataFromJson(json)
            if (success) {
                // Импорт может содержать снимок текущего месяца. Обновляем его
                // сразу, чтобы новый экран не ждал переключения месяца.
                reloadCurrentMonthState()
                _activeTemplate.value = null
            }
            onResult(success)
        }
    }

    fun resetAllApplicationData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.resetAllData()
            _currentYearMonth.value = YearMonth.now()
            _activeTemplate.value = null
            _inputState.value = SalaryInputState()
            _deductionsState.value = DeductionSettings()
            onDone()
        }
    }

    private suspend fun reloadCurrentMonthState() {
        val ym = _currentYearMonth.value
        val ymString = String.format(Locale.ROOT, "%04d-%02d", ym.year, ym.monthValue)
        val snapshot = repository.getMonthlySnapshot(ymString).firstOrNull()
        if (snapshot == null) {
            _inputState.value = SalaryInputState()
            _deductionsState.value = DeductionSettings()
        } else {
            _inputState.update {
                it.copy(
                    hourlyRate = normalizedHours(snapshot.hourlyRate),
                    allowances = repository.parseAllowancesJson(snapshot.allowancesJson)
                )
            }
            _deductionsState.value = repository.parseDeductionsJson(snapshot.deductionsJson)
        }
    }
}
