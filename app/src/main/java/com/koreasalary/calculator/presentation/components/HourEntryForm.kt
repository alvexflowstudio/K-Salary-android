package com.koreasalary.calculator.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogWindowProvider
import android.view.WindowManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koreasalary.calculator.data.model.WorkHours
import com.koreasalary.calculator.domain.i18n.AppStrings

@Stable
internal class HourEntryState(val initial: WorkHours) {
    var base by mutableStateOf(formatEntryHours(initial.regular))
    var combined by mutableStateOf(formatEntryHours(initial.combinedTimeAndHalf))
    var night by mutableStateOf(formatEntryHours(initial.night))
    var doubleTime by mutableStateOf(formatEntryHours(initial.holidayOvertime))

    val baseInvalid: Boolean get() = !validEntryHours(base) || entryHours(base) > 8.0
    val additionalInvalid: Boolean get() = listOf(combined, night, doubleTime).any { !validEntryHours(it) }
    val valid: Boolean get() = !baseInvalid && !additionalInvalid

    fun hours(): WorkHours {
        check(valid)
        return initial.withCombinedTimeAndHalf(entryHours(combined)).copy(
            regular = entryHours(base), night = entryHours(night),
            holidayOvertime = entryHours(doubleTime)
        )
    }
}

// An empty field means zero while editing, so Next never causes a spurious error.
internal fun entryHours(text: String): Double = if (text.isBlank()) 0.0 else text.replace(',', '.').toDoubleOrNull() ?: Double.NaN
internal fun validEntryHours(text: String): Boolean = entryHours(text).let { it.isFinite() && it >= 0.0 }
private fun formatEntryHours(value: Double): String = java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

@Composable
internal fun rememberHourEntryState(initial: WorkHours): HourEntryState = rememberSaveable(
    initial,
    saver = listSaver(
        save = { listOf(it.base, it.combined, it.night, it.doubleTime) },
        restore = { HourEntryState(initial).apply { base = it[0]; combined = it[1]; night = it[2]; doubleTime = it[3] } }
    )
) { HourEntryState(initial) }

/** Shared by calendar and templates; flags never rewrite these fields. */
@Composable
internal fun HourEntryForm(state: HourEntryState, strings: AppStrings, isSunday: Boolean = false) {
    var baseFocused by remember { mutableStateOf(false) }
    AppInputField(
        value = state.base, onValueChange = { state.base = it },
        label = strings.ui.dayBaseHoursLabel, compactLabel = true,
        isError = state.baseInvalid, imeAction = ImeAction.Next,
        onFocusChange = { baseFocused = it }, inputModifier = Modifier.testTag("hours-base")
    )
    if (isSunday && baseFocused) {
        Text(strings.ui.sundayBaseHoursWarning, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("sunday-warning"))
    }
    if (state.baseInvalid) {
        Text(strings.ui.baseHoursValidation, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error)
    }
    AppInputField(
        value = state.combined, onValueChange = { state.combined = it },
        label = strings.ui.dayOvertimeHoursLabel, compactLabel = true,
        isError = !validEntryHours(state.combined), imeAction = ImeAction.Next,
        inputModifier = Modifier.testTag("hours-combined")
    )
    AppInputField(
        value = state.night, onValueChange = { state.night = it },
        label = strings.ui.dayNightHoursLabel, compactLabel = true,
        isError = !validEntryHours(state.night), imeAction = ImeAction.Next,
        inputModifier = Modifier.testTag("hours-night")
    )
    AppInputField(
        value = state.doubleTime, onValueChange = { state.doubleTime = it },
        label = strings.ui.dayHolidayOvertimeHoursLabel, compactLabel = true,
        isError = !validEntryHours(state.doubleTime), imeAction = ImeAction.Next,
        inputModifier = Modifier.testTag("hours-double")
    )
    if (state.additionalInvalid) {
        Text(strings.ui.hoursValidation, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error)
    }
}

/** Insets bound the scroll viewport, including actions, above the visible IME. */
@Composable
internal fun HourEditorDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    // Read from the activity's inset owner: Compose 1.7 dialogs can receive zero
    // IME insets even while their host activity reports the visible keyboard.
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val systemTop = WindowInsets.systemBars.getTop(density)
    val systemBottom = WindowInsets.systemBars.getBottom(density)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val availableHeight = (screenHeight - with(density) {
        (systemTop + maxOf(imeBottom, systemBottom)).toDp()
    }).coerceAtLeast(1.dp)
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true, decorFitsSystemWindows = true)) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        DisposableEffect(window) {
            val previousMode = window.attributes.softInputMode
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            onDispose { window.setSoftInputMode(previousMode) }
        }
        Box(Modifier.heightIn(max = availableHeight).fillMaxWidth().padding(12.dp),
            contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp).testTag("hour-editor-scroll"),
                    verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            }
        }
    }
}
