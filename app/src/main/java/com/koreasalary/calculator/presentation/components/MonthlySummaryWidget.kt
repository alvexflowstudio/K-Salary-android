package com.koreasalary.calculator.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.model.SalaryInputState
import com.koreasalary.calculator.domain.i18n.AppStrings
import com.koreasalary.calculator.presentation.theme.LocalAppPalette
import java.util.Locale

@Composable
fun MonthlySummaryWidget(
    inputState: SalaryInputState,
    records: Map<Int, DayRecordEntity>,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    // Отметки оплачиваемого праздника и еженедельного отдыха начисляют
    // отдельные часы, но сами по себе не означают фактическую работу в этот день.
    val totalWorkDays = countActualWorkDays(records.values)
    val totalSundaysPaid = records.values.count { it.isSundayPay }
    val totalPaidHolidays = records.values.count { it.isPaidHoliday }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = strings.ui.monthlySummaryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            StatRow(
                title = strings.ui.workedDaysSummaryTitle,
                value = "$totalWorkDays ${strings.daysUnit}",
                highlightColor = MaterialTheme.colorScheme.primary
            )

            StatRow(
                title = strings.ui.baseHoursSummaryTitle,
                value = "${formatHours(inputState.regularHours)} ${strings.hoursUnit}"
            )

            if (inputState.sundayPayHours > 0.0) {
                StatRow(
                    title = "${strings.ui.weeklyHolidaySummaryTitle} ($totalSundaysPaid ${strings.daysUnit}):",
                    value = "+${formatHours(inputState.sundayPayHours)} ${strings.hoursUnit}",
                    highlightColor = palette.success
                )
            }

            if (inputState.paidHolidayHours > 0.0) {
                StatRow(
                    title = "${strings.ui.paidHolidaySummaryTitle} ($totalPaidHolidays ${strings.daysUnit}):",
                    value = "+${formatHours(inputState.paidHolidayHours)} ${strings.hoursUnit}",
                    highlightColor = palette.success
                )
            }

            if (inputState.overtimeHours > 0.0) {
                StatRow(
                    title = strings.ui.overtimeSummaryTitle,
                    value = "+${formatHours(inputState.overtimeHours)} ${strings.hoursUnit}",
                    highlightColor = palette.warning
                )
            }

            if (inputState.nightHours > 0.0) {
                StatRow(
                    title = strings.ui.nightSummaryTitle,
                    value = "+${formatHours(inputState.nightHours)} ${strings.hoursUnit}",
                    highlightColor = palette.purple
                )
            }

            if (inputState.holidayHours > 0.0 || inputState.holidayOvertimeHours > 0.0) {
                val totalHol = formatHours(inputState.holidayHours + inputState.holidayOvertimeHours)
                StatRow(
                    title = strings.ui.holidaySummaryTitle,
                    value = "+$totalHol ${strings.hoursUnit}",
                    highlightColor = palette.danger
                )
            }

            if (inputState.allowances.totalAllowances > 0.0) {
                StatRow(
                    title = strings.ui.allowancesSummaryTitle,
                    value = "+${CurrencyFormatUtil.formatWon(inputState.allowances.totalAllowances)} ${strings.currencyWon}",
                    highlightColor = palette.gray
                )
            }
        }
    }
}

internal fun countActualWorkDays(records: Collection<DayRecordEntity>): Int = records.count {
    it.regularHours.isFinite() && it.regularHours > 0 ||
            it.holidayHours.isFinite() && it.holidayHours > 0 ||
            it.holidayOvertimeHours.isFinite() && it.holidayOvertimeHours > 0 ||
            it.overtimeHours.isFinite() && it.overtimeHours > 0 ||
            it.nightHours.isFinite() && it.nightHours > 0
}

private fun formatHours(value: Double): String {
    if (!value.isFinite() || value <= 0.0) return "0"
    val rounded = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    return rounded
}

@Composable
private fun StatRow(
    title: String,
    value: String,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.55f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
