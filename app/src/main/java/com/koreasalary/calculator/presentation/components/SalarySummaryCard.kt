package com.koreasalary.calculator.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koreasalary.calculator.data.model.SalaryResult
import com.koreasalary.calculator.domain.i18n.AppStrings
import com.koreasalary.calculator.presentation.theme.LocalAppPalette

@Composable
fun SalarySummaryCard(
    result: SalaryResult,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val palette = LocalAppPalette.current

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.accent)
                .padding(18.dp)
        ) {
            // 1. Крупная итоговая сумма "К выплате (실수령액)" сверху
            Text(
                text = strings.netSalaryLabel,
                style = MaterialTheme.typography.titleMedium,
                color = palette.onAccent.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${CurrencyFormatUtil.formatWon(result.netSalary)} ${strings.currencyWon}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.onAccent,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Две равные независимые колонки (weight 1f) для "Всего начислено" и "Всего вычетов"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.heroInner)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая колонка: Всего начислено
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = strings.grossSalaryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.heroSecondaryText.copy(alpha = 0.9f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${CurrencyFormatUtil.formatWon(result.breakdown.grossSalary)} ${strings.currencyWon}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.onAccent,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Вертикальный тонкий разделитель
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(palette.onAccent.copy(alpha = 0.2f))
                )

                // Правая колонка: Всего вычетов
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = strings.totalDeductionsLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.heroSecondaryText.copy(alpha = 0.9f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "-${CurrencyFormatUtil.formatWon(result.deductions.totalDeductions)} ${strings.currencyWon}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.heroDanger,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Пропорциональный индикатор соотношения Net Pay и Вычетов
            if (result.breakdown.grossSalary > 0L) {
                Spacer(modifier = Modifier.height(12.dp))
                val netRatio = (result.netSalary.toFloat() / result.breakdown.grossSalary.toFloat()).coerceIn(0f, 1f)
                val dedRatio = (1f - netRatio).coerceIn(0f, 1f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(palette.onAccent.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(if (netRatio > 0f) netRatio else 0.001f)
                            .fillMaxHeight()
                            .background(palette.success)
                    )
                    if (dedRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(dedRatio)
                                .fillMaxHeight()
                                .background(palette.heroDanger)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Раскрывающийся блок деталей начислений
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.breakdownTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.structureText
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = palette.onAccent
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    HorizontalDivider(color = palette.onAccent.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    SummaryDetailRow(
                        title = strings.ui.detailRegularPayTitle,
                        value = "${CurrencyFormatUtil.formatWon(result.breakdown.regularPay)} ${strings.currencyWon}"
                    )
                    if (result.breakdown.sundayPay > 0L) {
                        SummaryDetailRow(
                            title = strings.ui.detailSundayPayTitle,
                            value = "+${CurrencyFormatUtil.formatWon(result.breakdown.sundayPay)} ${strings.currencyWon}"
                        )
                    }
                    if (result.breakdown.paidHolidayPay > 0L) {
                        SummaryDetailRow(
                            title = strings.ui.detailPaidHolidayPayTitle,
                            value = "+${CurrencyFormatUtil.formatWon(result.breakdown.paidHolidayPay)} ${strings.currencyWon}"
                        )
                    }
                    if (result.breakdown.overtimePay > 0L) {
                        SummaryDetailRow(
                            title = strings.ui.detailOvertimePayTitle,
                            value = "+${CurrencyFormatUtil.formatWon(result.breakdown.overtimePay)} ${strings.currencyWon}"
                        )
                    }
                    if (result.breakdown.nightPay > 0L) {
                        SummaryDetailRow(
                            title = strings.ui.detailNightPayTitle,
                            value = "+${CurrencyFormatUtil.formatWon(result.breakdown.nightPay)} ${strings.currencyWon}"
                        )
                    }
                    if (result.breakdown.holidayPay > 0L) {
                        SummaryDetailRow(
                            title = strings.ui.detailHolidayPayTitle,
                            value = "+${CurrencyFormatUtil.formatWon(result.breakdown.holidayPay)} ${strings.currencyWon}"
                        )
                    }
                    if (result.breakdown.totalAllowancesPay > 0L) {
                        SummaryDetailRow(
                            title = strings.ui.detailAllowancesPayTitle,
                            value = "+${CurrencyFormatUtil.formatWon(result.breakdown.totalAllowancesPay)} ${strings.currencyWon}"
                        )
                    }
                    if (result.deductions.otherDeductions > 0L) {
                        SummaryDetailRow(
                            title = strings.ui.otherDeductionsSectionTitle,
                            value = "-${CurrencyFormatUtil.formatWon(result.deductions.otherDeductions)} ${strings.currencyWon}"
                        )
                    }
                    SummaryDetailRow(
                        title = strings.taxableAmountLabel,
                        value = "${CurrencyFormatUtil.formatWon(result.breakdown.taxableAmount)} ${strings.currencyWon}"
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryDetailRow(
    title: String,
    value: String
) {
    val palette = LocalAppPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = palette.heroSecondaryText.copy(alpha = 0.9f),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.onAccent,
            modifier = Modifier.weight(0.65f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
