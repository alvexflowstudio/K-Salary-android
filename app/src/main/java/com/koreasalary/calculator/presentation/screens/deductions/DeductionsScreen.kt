package com.koreasalary.calculator.presentation.screens.deductions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.koreasalary.calculator.data.model.PresetType
import com.koreasalary.calculator.data.model.DeductionSettings
import com.koreasalary.calculator.domain.i18n.AppStrings
import com.koreasalary.calculator.presentation.components.AppInputField
import com.koreasalary.calculator.presentation.components.CurrencyFormatUtil
import com.koreasalary.calculator.presentation.components.DeductionRowItem
import com.koreasalary.calculator.presentation.components.MixedModeBanner
import com.koreasalary.calculator.presentation.viewmodel.SalaryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeductionsScreen(
    viewModel: SalaryViewModel,
    strings: AppStrings,
    resetKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val deductionsState by viewModel.deductionsState.collectAsState()
    val salaryResult by viewModel.salaryResult.collectAsState()
    val inputState by viewModel.inputState.collectAsState()
    val selectedPreset = deductionsState.selectedPresetOrNull()
    val scrollState = rememberScrollState()

    LaunchedEffect(resetKey) {
        scrollState.scrollTo(0)
    }

    var fuelText by remember(inputState.allowances.fuelAllowance) {
        mutableStateOf(
            if (inputState.allowances.fuelAllowance % 1.0 == 0.0) {
                inputState.allowances.fuelAllowance.toLong().toString()
            } else {
                inputState.allowances.fuelAllowance.toString()
            }
        )
    }

    var leaveText by remember(inputState.allowances.annualLeaveAllowance) {
        mutableStateOf(
            if (inputState.allowances.annualLeaveAllowance % 1.0 == 0.0) {
                inputState.allowances.annualLeaveAllowance.toLong().toString()
            } else {
                inputState.allowances.annualLeaveAllowance.toString()
            }
        )
    }

    var showAddBonusDialog by remember { mutableStateOf(false) }
    var showAddDeductionDialog by remember { mutableStateOf(false) }

    if (showAddBonusDialog) {
        AddBonusDialog(
            strings = strings,
            onDismiss = { showAddBonusDialog = false },
            onAdd = { title, amount ->
                viewModel.addCustomBonus(title, amount)
                showAddBonusDialog = false
            }
        )
    }

    if (showAddDeductionDialog) {
        AddDeductionDialog(
            strings = strings,
            onDismiss = { showAddDeductionDialog = false },
            onAdd = { title, amount ->
                viewModel.addCustomDeduction(title, amount)
                showAddDeductionDialog = false
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(strings.deductionsTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.deductionsSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Быстрые пресеты оформления
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = strings.presetsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        maxItemsInEachRow = 2
                    ) {
                        SuggestionChip(
                            onClick = { viewModel.applyPreset(PresetType.BUSINESS_3_3) },
                            label = { Text(strings.preset33, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                            colors = presetChipColors(selectedPreset == PresetType.BUSINESS_3_3),
                            modifier = Modifier.weight(1f).heightIn(min = 32.dp)
                        )
                        SuggestionChip(
                            onClick = { viewModel.applyPreset(PresetType.FOUR_INSURANCES) },
                            label = { Text(strings.preset4Insurances, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                            colors = presetChipColors(selectedPreset == PresetType.FOUR_INSURANCES),
                            modifier = Modifier.weight(1f).heightIn(min = 32.dp)
                        )
                        SuggestionChip(
                            onClick = { viewModel.applyPreset(PresetType.FULL_EMPLOYEE) },
                            label = { Text(strings.presetFullEmployee, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                            colors = presetChipColors(selectedPreset == PresetType.FULL_EMPLOYEE),
                            modifier = Modifier.weight(1f).heightIn(min = 32.dp)
                        )
                        SuggestionChip(
                            onClick = { viewModel.applyPreset(PresetType.NO_DEDUCTIONS) },
                            label = { Text(strings.presetNoDeductions, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                            colors = presetChipColors(selectedPreset == PresetType.NO_DEDUCTIONS),
                            modifier = Modifier.weight(1f).heightIn(min = 32.dp)
                        )
                    }
                }
            }

            // Баннер смешанного режима
            if (deductionsState.isMixedMode) {
                MixedModeBanner(strings = strings)
            }

            // 6 независимых тумблеров налогов и страховок
            DeductionRowItem(
                title = strings.tax33Label,
                description = strings.tax33Desc,
                isChecked = deductionsState.isBusinessTax33Enabled,
                calculatedAmount = salaryResult.deductions.businessTax33,
                onCheckedChange = { viewModel.toggleBusinessTax33(it) }
            )

            DeductionRowItem(
                title = strings.nationalPensionLabel,
                description = strings.nationalPensionDesc,
                isChecked = deductionsState.isNationalPensionEnabled,
                calculatedAmount = salaryResult.deductions.nationalPension,
                onCheckedChange = { viewModel.toggleNationalPension(it) }
            )

            DeductionRowItem(
                title = strings.healthInsuranceLabel,
                description = strings.healthInsuranceDesc,
                isChecked = deductionsState.isHealthInsuranceEnabled,
                calculatedAmount = salaryResult.deductions.healthInsurance,
                onCheckedChange = { viewModel.toggleHealthInsurance(it) }
            )

            DeductionRowItem(
                title = strings.longTermCareLabel,
                description = strings.longTermCareDesc,
                isChecked = deductionsState.isLongTermCareEnabled,
                calculatedAmount = salaryResult.deductions.longTermCare,
                onCheckedChange = { viewModel.toggleLongTermCare(it) }
            )

            DeductionRowItem(
                title = strings.employmentInsuranceLabel,
                description = strings.employmentInsuranceDesc,
                isChecked = deductionsState.isEmploymentInsuranceEnabled,
                calculatedAmount = salaryResult.deductions.employmentInsurance,
                onCheckedChange = { viewModel.toggleEmploymentInsurance(it) }
            )

            DeductionRowItem(
                title = strings.incomeTaxLabel,
                description = strings.incomeTaxDesc,
                isChecked = deductionsState.isIncomeTaxEnabled,
                calculatedAmount = salaryResult.deductions.incomeTax + salaryResult.deductions.localIncomeTax,
                onCheckedChange = { viewModel.toggleIncomeTax(it) }
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.ui.otherDeductionsSectionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.ui.otherDeductionsDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showAddDeductionDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.ui.addDeductionDescription,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    deductionsState.customDeductions.orEmpty().forEach { deduction ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deduction.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "-${CurrencyFormatUtil.formatWon(deduction.amount)} ${strings.currencyWon}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(onClick = { viewModel.removeCustomDeduction(deduction.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = strings.ui.deleteDeductionDescription,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showAddDeductionDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.ui.addDeductionButton, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }

            // Доплаты и надбавки пользователь добавляет здесь с собственным
            // названием и суммой из расчётного листка.
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.ui.allowancesSectionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showAddBonusDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.ui.addBonusDescription,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 1. Топливо / Транспорт (유류비 / 교통비)
                    AppInputField(
                        value = fuelText,
                        onValueChange = {
                            fuelText = it
                            it.toDoubleOrNull()?.let(viewModel::updateFuelAllowance)
                        },
                        label = strings.ui.fuelAllowanceLabel,
                        suffixText = strings.currencyWon
                    )

                    // 2. Отпускные / надбавки (연차수당 / 월차수당)
                    AppInputField(
                        value = leaveText,
                        onValueChange = {
                            leaveText = it
                            it.toDoubleOrNull()?.let(viewModel::updateAnnualLeaveAllowance)
                        },
                        label = strings.ui.annualLeaveAllowanceLabel,
                        suffixText = strings.currencyWon
                    )

                    // Кастомные бонусы
                    inputState.allowances.customBonuses.orEmpty().forEach { bonus ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = bonus.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = "+${CurrencyFormatUtil.formatWon(bonus.amount)} ${strings.currencyWon}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.removeCustomBonus(bonus.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = strings.ui.deleteBonusDescription,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showAddBonusDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.ui.addBonusButton, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }

        }
    }
}

@Composable
private fun AddDeductionDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onAdd: (title: String, amount: Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("0") }
    val amount = amountText.toDoubleOrNull()
    val isAmountValid = amount != null && amount.isFinite() && amount >= 0.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.ui.addDeductionDialogTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                AppInputField(
                    value = title,
                    onValueChange = { title = it },
                    label = "${strings.ui.deductionNameLabel} (${strings.ui.deductionNameHint})",
                    keyboardType = KeyboardType.Text
                )

                AppInputField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = strings.ui.deductionAmountLabel,
                    suffixText = strings.currencyWon,
                    isError = amountText.isNotBlank() && !isAmountValid
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(strings.ui.cancelAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Button(
                        enabled = title.isNotBlank() && isAmountValid,
                        onClick = { onAdd(title, amount ?: 0.0) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(strings.ui.addAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBonusDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onAdd: (title: String, amount: Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("0") }
    val amount = amountText.toDoubleOrNull()
    val isAmountValid = amount != null && amount.isFinite() && amount >= 0.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.ui.addBonusDialogTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                AppInputField(
                    value = title,
                    onValueChange = { title = it },
                    label = "${strings.ui.bonusNameLabel} (${strings.ui.bonusNameHint})",
                    keyboardType = KeyboardType.Text
                )

                AppInputField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = strings.ui.bonusAmountLabel,
                    suffixText = strings.currencyWon,
                    isError = amountText.isNotBlank() && !isAmountValid
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(strings.ui.cancelAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Button(
                        enabled = title.isNotBlank() && isAmountValid,
                        onClick = {
                            onAdd(title, amount ?: 0.0)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(strings.ui.addAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun presetChipColors(isSelected: Boolean) =
    SuggestionChipDefaults.suggestionChipColors(
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        labelColor = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )

private fun DeductionSettings.selectedPresetOrNull(): PresetType? = when {
    isBusinessTax33Enabled &&
        !isNationalPensionEnabled &&
        !isHealthInsuranceEnabled &&
        !isLongTermCareEnabled &&
        !isEmploymentInsuranceEnabled &&
        !isIncomeTaxEnabled -> PresetType.BUSINESS_3_3

    !isBusinessTax33Enabled &&
        isNationalPensionEnabled &&
        isHealthInsuranceEnabled &&
        isLongTermCareEnabled &&
        isEmploymentInsuranceEnabled &&
        !isIncomeTaxEnabled -> PresetType.FOUR_INSURANCES

    !isBusinessTax33Enabled &&
        isNationalPensionEnabled &&
        isHealthInsuranceEnabled &&
        isLongTermCareEnabled &&
        isEmploymentInsuranceEnabled &&
        isIncomeTaxEnabled -> PresetType.FULL_EMPLOYEE

    !isBusinessTax33Enabled &&
        !isNationalPensionEnabled &&
        !isHealthInsuranceEnabled &&
        !isLongTermCareEnabled &&
        !isEmploymentInsuranceEnabled &&
        !isIncomeTaxEnabled -> PresetType.NO_DEDUCTIONS

    else -> null
}
