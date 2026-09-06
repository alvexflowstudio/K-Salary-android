package com.koreasalary.calculator.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.domain.i18n.AppStrings
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

@Composable
fun DayEditDialog(
    day: Int,
    yearMonth: YearMonth,
    existingRecord: DayRecordEntity?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (reg: Double, ot: Double, night: Double, hol: Double, holOt: Double, paidHol: Boolean, sundayPay: Boolean, note: String, scheduledBaseHours: Double) -> Unit,
    onClear: () -> Unit
) {
    val existingRegularHours = existingRecord?.regularHours?.coerceAtLeast(0.0) ?: 0.0
    val existingScheduledBaseHours = existingRecord?.scheduledBaseHours?.coerceAtLeast(0.0) ?: 0.0
    val initialScheduledBaseHours = if (existingScheduledBaseHours > 0.0) {
        existingScheduledBaseHours
    } else {
        existingRegularHours
    }
    val existingIsSpecialDay = existingRecord?.let { it.isPaidHoliday || it.isSundayPay } == true
    val existingHolidayHours = existingRecord?.holidayHours?.coerceAtLeast(0.0) ?: 0.0
    val existingHolidayOvertimeHours = existingRecord?.holidayOvertimeHours?.coerceAtLeast(0.0) ?: 0.0
    val initialHolidayHours = if (existingIsSpecialDay) {
        min(8.0, existingHolidayHours + existingRegularHours)
    } else {
        existingHolidayHours
    }
    val initialHolidayOvertimeHours = if (existingIsSpecialDay) {
        existingHolidayOvertimeHours + max(0.0, existingHolidayHours + existingRegularHours - 8.0)
    } else {
        existingHolidayOvertimeHours
    }

    var regText by remember {
        mutableStateOf(
            if (existingIsSpecialDay) {
                "0"
            } else {
                existingRecord?.regularHours?.let(::formatHoursValue) ?: "0"
            }
        )
    }
    var otText by remember {
        mutableStateOf(
            existingRecord?.overtimeHours?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "0"
        )
    }
    var nightText by remember {
        mutableStateOf(
            existingRecord?.nightHours?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "0"
        )
    }
    var holText by remember {
        mutableStateOf(
            formatHoursValue(initialHolidayHours)
        )
    }
    var holOtText by remember {
        mutableStateOf(
            formatHoursValue(initialHolidayOvertimeHours)
        )
    }
    var isPaidHoliday by remember { mutableStateOf(existingRecord?.isPaidHoliday ?: false) }
    var isSundayPay by remember { mutableStateOf(existingRecord?.isSundayPay ?: false) }
    var scheduledBaseHours by remember { mutableStateOf(initialScheduledBaseHours) }
    var note by remember { mutableStateOf(existingRecord?.note ?: "") }

    val regVal = regText.toDoubleOrNull() ?: 0.0
    val otVal = otText.toDoubleOrNull() ?: 0.0
    val nightVal = nightText.toDoubleOrNull() ?: 0.0
    val holVal = holText.toDoubleOrNull() ?: 0.0
    val holOtVal = holOtText.toDoubleOrNull() ?: 0.0
    val isSpecialDay = isPaidHoliday || isSundayPay
    val isRegInvalid = !isSpecialDay && (!regText.isValidHoursInput() || regVal > 8.0)
    val hasInvalidAdditionalHours =
        !otText.isValidHoursInput() ||
            !nightText.isValidHoursInput() ||
            !holText.isValidHoursInput() ||
            holVal > 8.0 ||
            !holOtText.isValidHoursInput()
    val isAnyHoursInvalid = isRegInvalid || hasInvalidAdditionalHours

    fun moveRegularHoursToHolidayFields() {
        val regular = regText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        if (regular > 0.0) {
            scheduledBaseHours = regular
        }
        if (regular > 0.0) {
            val holidayRoom = (8.0 - holVal.coerceAtMost(8.0)).coerceAtLeast(0.0)
            val normalHolidayHours = min(regular, holidayRoom)
            holText = formatHoursValue(holVal + normalHolidayHours)
            holOtText = formatHoursValue(holOtVal + (regular - normalHolidayHours))
        }
        regText = "0"
    }

    val monthName = strings.ui.monthNames[yearMonth.monthValue - 1]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header (БЕЗ поля "Название шаблона"!)
                Text(
                    text = "$day $monthName ${yearMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                // Базовые часы с валидацией максимум 8ч
                Column {
                    AppInputField(
                        value = regText,
                        onValueChange = {
                            if (!isSpecialDay) {
                                regText = it
                                it.toDoubleOrNull()?.let { value ->
                                    scheduledBaseHours = value.coerceAtLeast(0.0)
                                }
                            }
                        },
                        label = strings.ui.dayBaseHoursLabel,
                        suffixText = strings.hoursUnit,
                        readOnly = isSpecialDay,
                        isError = isSpecialDay,
                        helperText = if (isSpecialDay) strings.ui.specialDayBaseHoursHelper else null
                    )
                    if (isRegInvalid) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (regVal > 8.0) strings.ui.baseHoursValidation else strings.ui.hoursValidation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                AppInputField(
                    value = otText,
                    onValueChange = { otText = it },
                    label = strings.ui.dayOvertimeHoursLabel,
                    suffixText = strings.hoursUnit,
                    isError = otText.isNotBlank() && !otText.isValidHoursInput()
                )

                AppInputField(
                    value = nightText,
                    onValueChange = { nightText = it },
                    label = strings.ui.dayNightHoursLabel,
                    suffixText = strings.hoursUnit,
                    isError = nightText.isNotBlank() && !nightText.isValidHoursInput()
                )

                AppInputField(
                    value = holText,
                    onValueChange = { holText = it },
                    label = strings.ui.dayHolidayHoursLabel,
                    suffixText = strings.hoursUnit,
                    isError = holText.isNotBlank() && (!holText.isValidHoursInput() || holVal > 8.0)
                )

                AppInputField(
                    value = holOtText,
                    onValueChange = { holOtText = it },
                    label = strings.ui.dayHolidayOvertimeHoursLabel,
                    suffixText = strings.hoursUnit,
                    isError = holOtText.isNotBlank() && !holOtText.isValidHoursInput()
                )

                if (hasInvalidAdditionalHours) {
                    Text(
                        text = strings.ui.hoursValidation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Чекбоксы
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isPaidHoliday,
                        onCheckedChange = {
                            if (it) moveRegularHoursToHolidayFields()
                            isPaidHoliday = it
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.ui.paidHolidayCheckboxLabel,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isSundayPay,
                        onCheckedChange = {
                            if (it) moveRegularHoursToHolidayFields()
                            isSundayPay = it
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.ui.weeklyHolidayCheckboxLabel,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("${strings.ui.noteLabel} (${strings.ui.noteHint})") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existingRecord != null) {
                        TextButton(
                            onClick = onClear,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(strings.ui.clearDayAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(strings.ui.cancelAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Button(
                        enabled = !isAnyHoursInvalid,
                        onClick = {
                            onSave(
                                if (isSpecialDay) 0.0 else regVal,
                                otVal,
                                nightVal,
                                holVal,
                                holOtVal,
                                isPaidHoliday,
                                isSundayPay,
                                note,
                                scheduledBaseHours
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(strings.ui.saveAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

private fun String.isValidHoursInput(): Boolean {
    return toDoubleOrNull()?.let { it.isFinite() && it >= 0.0 } == true
}

private fun formatHoursValue(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
