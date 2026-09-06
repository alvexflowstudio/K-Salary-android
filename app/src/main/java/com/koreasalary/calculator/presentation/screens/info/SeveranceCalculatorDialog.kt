package com.koreasalary.calculator.presentation.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koreasalary.calculator.domain.i18n.AppStrings
import com.koreasalary.calculator.presentation.components.AppInputField
import com.koreasalary.calculator.presentation.components.CurrencyFormatUtil
import com.koreasalary.calculator.presentation.components.QuickActionButton
import com.koreasalary.calculator.presentation.theme.LocalAppPalette
import com.koreasalary.calculator.presentation.viewmodel.SeveranceViewModel

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SeveranceCalculatorDialog(
    viewModel: SeveranceViewModel,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    val inputState by viewModel.inputState.collectAsState()
    val result by viewModel.result.collectAsState()
    val palette = LocalAppPalette.current

    var m1Text by remember(inputState.month1Salary) { mutableStateOf(inputState.month1Salary.toLong().toString()) }
    var m2Text by remember(inputState.month2Salary) { mutableStateOf(inputState.month2Salary.toLong().toString()) }
    var m3Text by remember(inputState.month3Salary) { mutableStateOf(inputState.month3Salary.toLong().toString()) }
    var bonusText by remember(inputState.annualBonus) { mutableStateOf(inputState.annualBonus.toLong().toString()) }
    var leaveText by remember(inputState.unusedAnnualLeavePay) { mutableStateOf(inputState.unusedAnnualLeavePay.toLong().toString()) }
    var daysText by remember(inputState.totalDaysWorked) { mutableStateOf(inputState.totalDaysWorked.toString()) }
    var calendarDaysText by remember(inputState.daysInLast3Months) { mutableStateOf(inputState.daysInLast3Months.toString()) }
    var ordinaryWageText by remember(inputState.ordinaryDailyWage) { mutableStateOf(inputState.ordinaryDailyWage.toLong().toString()) }
    var departureInsText by remember(inputState.departureInsurancePaid) { mutableStateOf(inputState.departureInsurancePaid.toLong().toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.severanceDialogTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text(strings.close, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Result Summary Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.accent)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = strings.severanceResultTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.onAccent.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${CurrencyFormatUtil.formatWon(result.netSeverancePay)} ${strings.currencyWon}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.onAccent
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Eligible badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (result.isEligible) palette.success else palette.heroDanger)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when {
                                        result.isEligible -> strings.severanceEligibleNotice
                                        !result.isCalculationPeriodValid -> strings.ui.severanceCalendarDaysHelper
                                        else -> strings.severanceNotEligibleNotice
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.onAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = palette.onAccent.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            ResultRow(
                                title = strings.severanceAvgDailyWage,
                                value = "${CurrencyFormatUtil.formatWon(result.averageDailyWage)} ${strings.currencyWon} / ${strings.ui.severancePerDaySuffix}"
                            )
                            ResultRow(
                                title = strings.ui.severanceAppliedDailyWageLabel,
                                value = "${CurrencyFormatUtil.formatWon(result.appliedDailyWage)} ${strings.currencyWon} / ${strings.ui.severancePerDaySuffix}"
                            )
                            ResultRow(
                                title = strings.severanceGrossPay,
                                value = "${CurrencyFormatUtil.formatWon(result.grossSeverancePay)} ${strings.currencyWon}"
                            )
                            ResultRow(
                                title = strings.severanceEstimatedTax,
                                value = "-${CurrencyFormatUtil.formatWon(result.estimatedRetirementTax)} ${strings.currencyWon}"
                            )
                            if (result.departureInsuranceDeduction > 0L) {
                                ResultRow(
                                    title = strings.severanceDepartureInsurance,
                                    value = "-${CurrencyFormatUtil.formatWon(result.departureInsuranceDeduction)} ${strings.currencyWon}"
                                )
                                ResultRow(
                                    title = strings.severanceDifferenceLabel,
                                    value = "+${CurrencyFormatUtil.formatWon(result.employerDifferenceToPay)} ${strings.currencyWon}",
                                    isHighlight = true
                                )
                            }
                        }
                    }

                    // 1. Month 1-3 Salaries
                    Text(
                        text = strings.ui.severanceSalarySectionTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    AppInputField(
                        value = m1Text,
                        onValueChange = {
                            m1Text = it
                            it.toDoubleOrNull()?.let(viewModel::updateMonth1Salary)
                        },
                        label = strings.severanceMonth1,
                        suffixText = strings.currencyWon
                    )

                    AppInputField(
                        value = m2Text,
                        onValueChange = {
                            m2Text = it
                            it.toDoubleOrNull()?.let(viewModel::updateMonth2Salary)
                        },
                        label = strings.severanceMonth2,
                        suffixText = strings.currencyWon
                    )

                    AppInputField(
                        value = m3Text,
                        onValueChange = {
                            m3Text = it
                            it.toDoubleOrNull()?.let(viewModel::updateMonth3Salary)
                        },
                        label = strings.severanceMonth3,
                        suffixText = strings.currencyWon
                    )

                    // 2. Days Worked
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppInputField(
                            value = daysText,
                            onValueChange = {
                                daysText = it
                                it.toIntOrNull()?.let(viewModel::updateTotalDaysWorked)
                            },
                            label = strings.severanceDaysWorked,
                            suffixText = strings.daysUnit
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickActionButton(
                                text = strings.ui.severanceOneYearShortcut,
                                onClick = {
                                    daysText = "365"
                                    viewModel.updateTotalDaysWorked(365)
                                },
                                isSelected = inputState.totalDaysWorked == 365
                            )
                            QuickActionButton(
                                text = strings.ui.severanceTwoYearsShortcut,
                                onClick = {
                                    daysText = "730"
                                    viewModel.updateTotalDaysWorked(730)
                                },
                                isSelected = inputState.totalDaysWorked == 730
                            )
                            QuickActionButton(
                                text = strings.ui.severanceThreeYearsShortcut,
                                onClick = {
                                    daysText = "1095"
                                    viewModel.updateTotalDaysWorked(1095)
                                },
                                isSelected = inputState.totalDaysWorked == 1095
                            )
                            QuickActionButton(
                                text = strings.ui.severanceFourYearsTenMonthsShortcut,
                                onClick = {
                                    daysText = "1765"
                                    viewModel.updateTotalDaysWorked(1765)
                                },
                                isSelected = inputState.totalDaysWorked == 1765
                            )
                        }
                    }

                    AppInputField(
                        value = calendarDaysText,
                        onValueChange = {
                            calendarDaysText = it
                            it.toIntOrNull()?.let(viewModel::updateDaysInLast3Months)
                        },
                        label = strings.ui.severanceCalendarDaysLabel,
                        helperText = strings.ui.severanceCalendarDaysHelper,
                        suffixText = strings.daysUnit
                    )

                    AppInputField(
                        value = ordinaryWageText,
                        onValueChange = {
                            ordinaryWageText = it
                            it.toDoubleOrNull()?.let(viewModel::updateOrdinaryDailyWage)
                        },
                        label = strings.ui.severanceOrdinaryDailyWageLabel,
                        helperText = strings.ui.severanceOrdinaryDailyWageHelper,
                        suffixText = strings.currencyWon
                    )

                    // 3. Bonuses & Leave Compensation
                    AppInputField(
                        value = bonusText,
                        onValueChange = {
                            bonusText = it
                            it.toDoubleOrNull()?.let(viewModel::updateAnnualBonus)
                        },
                        label = strings.severanceBonus,
                        suffixText = strings.currencyWon
                    )

                    AppInputField(
                        value = leaveText,
                        onValueChange = {
                            leaveText = it
                            it.toDoubleOrNull()?.let(viewModel::updateUnusedLeave)
                        },
                        label = strings.severanceUnusedLeave,
                        suffixText = strings.currencyWon
                    )

                    // 4. Departure Insurance (출국만기보험) for E-9 / H-2
                    AppInputField(
                        value = departureInsText,
                        onValueChange = {
                            departureInsText = it
                            it.toDoubleOrNull()?.let(viewModel::updateDepartureInsurance)
                        },
                        label = strings.severanceDepartureInsurance,
                        helperText = strings.ui.severanceInsuranceHelper,
                        suffixText = strings.currencyWon
                    )

                    Text(
                        text = strings.ui.severanceScopeNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.ui.severanceCalculationNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryText
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.close, maxLines = 2, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    title: String,
    value: String,
    isHighlight: Boolean = false
) {
    val palette = LocalAppPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = palette.heroSecondaryText.copy(alpha = 0.9f),
            modifier = Modifier
                .weight(0.95f)
                .padding(end = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isHighlight) palette.success else palette.onAccent,
            modifier = Modifier.weight(1.05f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
