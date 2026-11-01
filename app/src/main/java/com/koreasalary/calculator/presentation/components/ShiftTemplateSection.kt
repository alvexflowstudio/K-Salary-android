package com.koreasalary.calculator.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import com.koreasalary.calculator.data.local.entity.ShiftTemplateEntity
import com.koreasalary.calculator.domain.i18n.AppStrings
import com.koreasalary.calculator.presentation.theme.LightAppPalette
import com.koreasalary.calculator.presentation.theme.LocalAppPalette
import com.koreasalary.calculator.data.model.WorkHours
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ShiftTemplateSection(
    templates: List<ShiftTemplateEntity>,
    activeTemplate: ShiftTemplateEntity?,
    strings: AppStrings,
    onToggleTemplate: (ShiftTemplateEntity) -> Unit,
    onCreateTemplate: (title: String, colorHex: String, reg: Double, ot: Double, night: Double, hol: Double, holOt: Double, scheduledBaseHours: Double, paidHol: Boolean, sundayPay: Boolean) -> Unit,
    onUpdateTemplate: (ShiftTemplateEntity) -> Unit,
    onDeleteTemplate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<ShiftTemplateEntity?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<ShiftTemplateEntity?>(null) }

    // Dialogs
    if (showCreateDialog) {
        ShiftTemplateEditDialog(
            template = null,
            strings = strings,
            onDismiss = { showCreateDialog = false },
            onSave = { title, colorHex, reg, ot, night, hol, holOt, scheduledBaseHours, paidHol, sundayPay ->
                onCreateTemplate(title, colorHex, reg, ot, night, hol, holOt, scheduledBaseHours, paidHol, sundayPay)
                showCreateDialog = false
            }
        )
    }

    editingTemplate?.let { template ->
        ShiftTemplateEditDialog(
            template = template,
            strings = strings,
            onDismiss = { editingTemplate = null },
            onSave = { title, colorHex, reg, ot, night, hol, holOt, scheduledBaseHours, paidHol, sundayPay ->
                val keepBuiltInTemplate = template.isDefault && title == template.displayTitle(strings)
                onUpdateTemplate(
                    template.copy(
                        // Если пользователь только открыл встроенный шаблон и сохранил
                        // его без переименования, он остаётся локализуемым встроенным.
                        title = if (keepBuiltInTemplate) template.title else title,
                        colorHex = colorHex,
                        workHoursVersion = 2,
                        regularHours = reg,
                        scheduledBaseHours = scheduledBaseHours,
                        overtimeHours = ot,
                        nightHours = night,
                        holidayHours = hol,
                        holidayOvertimeHours = holOt,
                        isPaidHoliday = paidHol,
                        isSundayPay = sundayPay,
                        // После переименования встроенный шаблон становится пользовательским.
                        isDefault = keepBuiltInTemplate
                    )
                )
                editingTemplate = null
            },
            onDelete = {
                templateToDelete = template
                editingTemplate = null
            }
        )
    }

    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text(strings.ui.deleteTemplateTitle) },
            text = { Text(strings.ui.deleteTemplateMessage.format(template.displayTitle(strings))) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTemplate(template.id)
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.ui.deleteAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text(strings.ui.cancelAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        )
    }

    if (showHelpDialog) {
        ShiftRulesHelpDialog(strings = strings, onDismiss = { showHelpDialog = false })
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title + Help Red (i) Button
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = strings.ui.shiftTemplatesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Красная кнопка (i) с инфо-диалогом правил работы
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = strings.ui.shiftRulesDescription,
                        tint = palette.danger,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (activeTemplate != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.ui.selectedTemplateBannerFormat.format(activeTemplate.displayTitle(strings)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // FlowRow контейнер (динамический сдвиг сетки календаря вниз)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка [+] слева для добавления нового шаблона
                FilledTonalIconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = palette.activeIconBackground,
                        contentColor = palette.onActiveIconBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.ui.addTemplateDescription,
                        tint = palette.onActiveIconBackground
                    )
                }

                // Список шаблонов
                templates.forEach { template ->
                    val isActive = activeTemplate?.id == template.id
                    val chipColor = resolveTemplateColor(template.colorHex)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .combinedClickable(
                                onClick = { onToggleTemplate(template) },
                                onLongClick = { editingTemplate = template }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(chipColor)
                            )
                            Text(
                                text = template.displayTitle(strings),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Text(
                text = strings.ui.templateHint,
                style = MaterialTheme.typography.bodySmall,
                color = palette.tertiaryText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ShiftTemplateEditDialog(
    template: ShiftTemplateEntity?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (title: String, colorHex: String, reg: Double, ot: Double, night: Double, hol: Double, holOt: Double, scheduledBaseHours: Double, paidHol: Boolean, sundayPay: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val initialTitle = template?.displayTitle(strings) ?: ""
    val titleStateKey = template?.displayTitle(strings) ?: "new-template"
    var title by remember(template?.id, titleStateKey) { mutableStateOf(initialTitle) }
    var colorHex by remember { mutableStateOf(template?.colorHex ?: "#5C875D") }
    val initial = template?.let(WorkHours::from) ?: WorkHours()
    val hours = rememberHourEntryState(initial)
    var isPaidHoliday by rememberSaveable { mutableStateOf(template?.isPaidHoliday ?: false) }
    var isSundayPay by rememberSaveable { mutableStateOf(template?.isSundayPay ?: false) }

    val availableColors = listOf(
        "#5C875D", // Green
        "#BD835B", // Orange
        "#816FA3", // Purple
        "#476A9C", // Accent
        "#A96464", // Red
        "#6E7278"  // Gray
    )

    HourEditorDialog(onDismiss) {
        Text(
            text = if (template == null) strings.ui.newTemplateTitle else strings.ui.editTemplateTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Название шаблона
        AppInputField(
            value = title,
            onValueChange = { title = it },
            label = "${strings.ui.templateNameLabel} (${strings.ui.templateNameHint})",
            keyboardType = KeyboardType.Text
        )

        // Выбор цвета
        Text(text = strings.ui.markerColorLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableColors.forEach { hex ->
                val isSelected = canonicalTemplateColor(colorHex) == hex
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(resolveTemplateColor(hex))
                        .border(
                            width = if (isSelected) 2.5.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { colorHex = hex }
                )
            }
        }

        HourEntryForm(hours, strings)

        // Чекбоксы
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isPaidHoliday,
                onCheckedChange = {
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

        // Кнопки действий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.ui.deleteAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                enabled = title.isNotBlank() && hours.valid,
                onClick = {
                    val value = hours.hours()
                    onSave(
                        title,
                        colorHex,
                        value.regular,
                        value.overtime,
                        value.night,
                        value.holiday,
                        value.holidayOvertime,
                        template?.scheduledBaseHours ?: value.regular,
                        isPaidHoliday,
                        isSundayPay
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

@Composable
fun ShiftRulesHelpDialog(
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.ui.rulesDialogTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LocalAppPalette.current.danger
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = strings.close)
                    }
                }

                HorizontalDivider()

                strings.ui.shiftRules.forEach { rule ->
                    RuleItem(title = rule.title, description = rule.description)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.ui.understoodAction, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun RuleItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun parseColorHex(hex: String): Color {
    return try {
        Color(hex.toColorInt())
    } catch (e: Exception) {
        LightAppPalette.success
    }
}

@Composable
private fun resolveTemplateColor(hex: String): Color {
    val palette = LocalAppPalette.current
    return when (canonicalTemplateColor(hex)) {
        "#5C875D" -> palette.success
        "#BD835B" -> palette.warning
        "#816FA3" -> palette.purple
        "#476A9C" -> palette.accent
        "#A96464" -> palette.danger
        "#6E7278" -> palette.gray
        else -> parseColorHex(hex)
    }
}

private fun canonicalTemplateColor(hex: String): String {
    return when (hex.trim().uppercase()) {
        "#2E7D32", "#5C875D", "#79A67A" -> "#5C875D"
        "#F57C00", "#BD835B", "#D1966D" -> "#BD835B"
        "#5E35B1", "#512DA8", "#816FA3", "#A08DC3" -> "#816FA3"
        "#1976D2", "#476A9C", "#36537E", "#DED7C5" -> "#476A9C"
        "#D32F2F", "#A96464", "#CA8281", "#972430" -> "#A96464"
        "#757575", "#6E7278", "#89867F" -> "#6E7278"
        else -> hex.trim().uppercase()
    }
}

private fun ShiftTemplateEntity.displayTitle(strings: AppStrings): String {
    if (!isDefault) return title
    return if (title == ShiftTemplateEntity.STORED_DEFAULT_NIGHT_TITLE) {
        strings.ui.defaultNightShiftTemplateName
    } else {
        strings.ui.defaultDayShiftTemplateName
    }
}
