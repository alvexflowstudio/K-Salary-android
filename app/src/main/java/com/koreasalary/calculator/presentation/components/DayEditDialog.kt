package com.koreasalary.calculator.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.domain.i18n.AppStrings
import com.koreasalary.calculator.domain.i18n.formatDayHeading
import java.time.YearMonth
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import com.koreasalary.calculator.data.model.WorkHours
import java.time.DayOfWeek

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
    val initial = existingRecord?.let(WorkHours::from) ?: WorkHours()
    val hours = rememberHourEntryState(initial)
    var isPaidHoliday by rememberSaveable { mutableStateOf(existingRecord?.isPaidHoliday ?: false) }
    var isSundayPay by rememberSaveable { mutableStateOf(existingRecord?.isSundayPay ?: false) }
    var note by rememberSaveable { mutableStateOf(existingRecord?.note ?: "") }

    HourEditorDialog(onDismiss) {
        Text(formatDayHeading(yearMonth.atDay(day), strings),
            style = MaterialTheme.typography.titleMedium)
        HourEntryForm(hours, strings, yearMonth.atDay(day).dayOfWeek == DayOfWeek.SUNDAY)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(isPaidHoliday, { isPaidHoliday = it }, modifier = Modifier.testTag("paid-holiday"))
            Text(strings.ui.paidHolidayCheckboxLabel, style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(isSundayPay, { isSundayPay = it }, modifier = Modifier.testTag("weekly-rest"))
            Text(strings.ui.weeklyHolidayCheckboxLabel, style = MaterialTheme.typography.bodySmall)
        }
        AppInputField(value = note, onValueChange = { note = it }, label = strings.ui.noteLabel,
            keyboardType = KeyboardType.Text, inputModifier = Modifier.testTag("day-note"))
        // Save never wraps: it always gets enough room for one whole word, even at
        // 320 dp with the keyboard open. "Очистить день" may use two lines, and both
        // of its lines stay centered inside its action area.
        val hasClear = existingRecord != null
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (hasClear) {
                TextButton(onClear, Modifier.weight(1.1f).testTag("clear-day"),
                    contentPadding = PaddingValues(4.dp)) {
                    FittingText(strings.ui.clearDayAction, modifier = Modifier.fillMaxWidth(),
                        maxLines = 2, textAlign = TextAlign.Center)
                }
            }
            TextButton(onDismiss, Modifier.weight(if (hasClear) 0.9f else 1f).testTag("cancel-day"),
                contentPadding = PaddingValues(4.dp)) {
                FittingText(strings.ui.cancelAction, modifier = Modifier.fillMaxWidth(),
                    maxLines = 2, textAlign = TextAlign.Center)
            }
            Button(onClick = {
                val value = hours.hours()
                onSave(value.regular, value.overtime, value.night, value.holiday, value.holidayOvertime,
                    isPaidHoliday, isSundayPay, note, existingRecord?.scheduledBaseHours ?: value.regular)
            }, enabled = hours.valid,
                modifier = Modifier.weight(if (hasClear) 1.2f else 1f).testTag("save-day"),
                contentPadding = PaddingValues(4.dp)) {
                FittingText(strings.ui.saveAction, modifier = Modifier.fillMaxWidth(),
                    maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}
