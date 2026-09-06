package com.koreasalary.calculator.presentation.screens.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.local.entity.ShiftTemplateEntity
import com.koreasalary.calculator.data.model.DeductionSettings
import com.koreasalary.calculator.data.model.SalaryInputState
import com.koreasalary.calculator.data.model.SalaryResult
import com.koreasalary.calculator.domain.i18n.AppStrings
import com.koreasalary.calculator.presentation.components.*
import com.koreasalary.calculator.presentation.theme.LocalAppPalette
import com.koreasalary.calculator.presentation.viewmodel.SalaryViewModel
import java.time.DayOfWeek
import java.time.YearMonth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: SalaryViewModel,
    strings: AppStrings,
    modifier: Modifier = Modifier,
    resetKey: Int = 0
) {
    val inputState: SalaryInputState by viewModel.inputState.collectAsState()
    val deductionsState: DeductionSettings by viewModel.deductionsState.collectAsState()
    val salaryResult: SalaryResult by viewModel.salaryResult.collectAsState()
    val currentYearMonth: YearMonth by viewModel.currentYearMonth.collectAsState()
    val shiftTemplates: List<ShiftTemplateEntity> by viewModel.shiftTemplates.collectAsState()
    val activeTemplate: ShiftTemplateEntity? by viewModel.activeTemplate.collectAsState()
    val monthDayRecords: Map<Int, DayRecordEntity> by viewModel.monthDayRecords.collectAsState()
    val palette = LocalAppPalette.current
    val scrollState = rememberScrollState()

    LaunchedEffect(resetKey) {
        scrollState.scrollTo(0)
    }

    var hourlyRateText by remember(inputState.hourlyRate) {
        mutableStateOf(
            if (inputState.hourlyRate % 1.0 == 0.0) inputState.hourlyRate.toLong().toString()
            else inputState.hourlyRate.toString()
        )
    }

    var dayToEdit by remember { mutableStateOf<Int?>(null) }
    var showHourlyRateHelp by remember { mutableStateOf(false) }

    if (showHourlyRateHelp) {
        HourlyRateHelpDialog(
            strings = strings,
            onDismiss = { showHourlyRateHelp = false }
        )
    }

    // Direct Date Edit Dialog
    dayToEdit?.let { day: Int ->
        val record: DayRecordEntity? = monthDayRecords[day]
        val recordTemplate = record?.templateId?.let { templateId ->
            shiftTemplates.firstOrNull { it.id == templateId }
        }
        val dialogRecord = if (
            recordTemplate?.isDefault == true &&
            record.note in setOf(
                ShiftTemplateEntity.STORED_DEFAULT_TITLE,
                ShiftTemplateEntity.STORED_LEGACY_DEFAULT_TITLE,
                strings.ui.defaultDayShiftTemplateName,
                strings.ui.defaultNightShiftTemplateName
            )
        ) {
            // Старые версии уже успели записать имя встроенной смены в note.
            // Убираем только это автоматически созданное значение, сохраняя часы.
            record.copy(note = "")
        } else {
            record
        }
        DayEditDialog(
            day = day,
            yearMonth = currentYearMonth,
            existingRecord = dialogRecord,
            strings = strings,
            onDismiss = { dayToEdit = null },
            onSave = { reg: Double, ot: Double, night: Double, hol: Double, holOt: Double, paidHol: Boolean, sundayPay: Boolean, note: String, scheduledBaseHours: Double ->
                viewModel.saveCustomDayRecord(
                    day = day,
                    regularHours = reg,
                    overtimeHours = ot,
                    nightHours = night,
                    holidayHours = hol,
                    holidayOvertimeHours = holOt,
                    isPaidHoliday = paidHol,
                    isSundayPay = sundayPay,
                    note = note,
                    scheduledBaseHours = scheduledBaseHours
                )
                dayToEdit = null
            },
            onClear = {
                viewModel.clearDayRecord(day)
                dayToEdit = null
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.ui.calculatorTopBarTitle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp
                    )
                },
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
            // 1. Верхняя карточка результатов (Result Card)
            SalarySummaryCard(result = salaryResult, strings = strings)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = strings.foreignWorkerNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Баннер смешанного режима (если активен)
            if (deductionsState.isMixedMode) {
                MixedModeBanner(strings = strings)
            }

            // 2. Часовая ставка с отдельной памяткой по корейским терминам.
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
                        Text(
                            text = strings.ui.hourlyRateSectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showHourlyRateHelp = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = strings.ui.hourlyRateHelpTitle,
                                tint = palette.danger,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    AppInputField(
                        value = hourlyRateText,
                        onValueChange = {
                            hourlyRateText = it
                            it.toDoubleOrNull()?.let(viewModel::updateHourlyRate)
                        },
                        label = strings.ui.hourlyRateInputLabel,
                        suffixText = strings.currencyWon
                    )

                }
            }

            // 3. Блок "Шаблоны смен" в контейнере FlowRow (с кнопкой [+] и красной (i))
            ShiftTemplateSection(
                templates = shiftTemplates,
                activeTemplate = activeTemplate,
                strings = strings,
                onToggleTemplate = { template: ShiftTemplateEntity -> viewModel.toggleTemplate(template) },
                onCreateTemplate = { title: String, colorHex: String, reg: Double, ot: Double, night: Double, hol: Double, holOt: Double, scheduledBaseHours: Double, paidHol: Boolean, sundayPay: Boolean ->
                    viewModel.createShiftTemplate(
                        title = title,
                        colorHex = colorHex,
                        regularHours = reg,
                        overtimeHours = ot,
                        nightHours = night,
                        holidayHours = hol,
                        holidayOvertimeHours = holOt,
                        isPaidHoliday = paidHol,
                        isSundayPay = sundayPay,
                        scheduledBaseHours = scheduledBaseHours
                    )
                },
                onUpdateTemplate = { template: ShiftTemplateEntity -> viewModel.updateShiftTemplate(template) },
                onDeleteTemplate = { templateId: Long -> viewModel.deleteShiftTemplate(templateId) }
            )

            // 4. Интерактивный Календарь (DTR Calendar)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header календаря: Переключение месяца и года
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.previousMonth() }) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = strings.ui.previousMonthDescription)
                        }

                        val monthName = strings.ui.monthNames[currentYearMonth.monthValue - 1]
                        Text(
                            text = "$monthName ${currentYearMonth.year}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { viewModel.nextMonth() }) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = strings.ui.nextMonthDescription)
                        }
                    }

                    // Дни недели: Пн, Вт, Ср, Чт, Пт, Сб, Вс
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val daysOfWeek = strings.ui.weekdaysShort
                        daysOfWeek.forEachIndexed { index, dow ->
                            val textColor = when (index) {
                                5 -> palette.accent // Суббота
                                6 -> palette.danger // Воскресенье
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = dow,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }

                    // Сетка дней месяца
                    val daysInMonth = currentYearMonth.lengthOfMonth()
                    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value // 1 (Mon) .. 7 (Sun)
                    val leadingEmptyDays = firstDayOfWeek - 1
                    val totalCells = leadingEmptyDays + daysInMonth
                    val rows = (totalCells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (rowIndex in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (colIndex in 0 until 7) {
                                    val cellIndex = rowIndex * 7 + colIndex
                                    val dayNum = cellIndex - leadingEmptyDays + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val dayRecord: DayRecordEntity? = monthDayRecords[dayNum]
                                        val dayDate = currentYearMonth.atDay(dayNum)

                                        CalendarCell(
                                            dayNumber = dayNum,
                                            dayRecord = dayRecord,
                                            dayOfWeek = dayDate.dayOfWeek,
                                            strings = strings,
                                            onClick = {
                                                val template = activeTemplate
                                                if (template != null) {
                                                    // Повторный клик по дню с тем же шаблоном очищает его.
                                                    viewModel.toggleTemplateOnDay(dayNum, template)
                                                } else {
                                                    // Прямой клик по дате открывает диалог ввода часов
                                                    dayToEdit = dayNum
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Виджет "Итоги по табелю за месяц"
            MonthlySummaryWidget(
                inputState = inputState,
                records = monthDayRecords,
                strings = strings
            )

        }
    }
}

@Composable
private fun CalendarCell(
    dayNumber: Int,
    dayRecord: DayRecordEntity?,
    dayOfWeek: DayOfWeek,
    strings: AppStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val hasShift = dayRecord != null && (
            dayRecord.regularHours > 0 ||
            dayRecord.overtimeHours > 0 ||
            dayRecord.nightHours > 0 ||
            dayRecord.holidayHours > 0 ||
            dayRecord.holidayOvertimeHours > 0 ||
            dayRecord.isPaidHoliday ||
            dayRecord.isSundayPay ||
            dayRecord.note.isNotBlank()
    )

    val badgeLabel = when {
        dayRecord == null -> ""
        dayRecord.isPaidHoliday -> strings.calendarPaidHolidayBadge
        dayRecord.isSundayPay -> strings.calendarSundayPayBadge
        dayRecord.holidayHours > 0 || dayRecord.holidayOvertimeHours > 0 -> strings.calendarHolidayBadge
        dayRecord.regularHours == 8.0 && dayRecord.overtimeHours == 4.0 && dayRecord.nightHours > 0 -> strings.calendarNightShiftBadge
        dayRecord.regularHours == 8.0 && dayRecord.overtimeHours == 4.0 -> strings.calendarDayShiftBadge
        dayRecord.regularHours == 8.0 -> strings.calendarEightHourBadge
        dayRecord.regularHours > 0 -> "${formatCalendarHours(dayRecord.regularHours)}${strings.hoursUnit}"
        dayRecord.note.isNotBlank() -> dayRecord.note.take(4)
        else -> ""
    }
    val badgeColor = when {
        dayRecord?.isPaidHoliday == true || dayRecord?.isSundayPay == true -> palette.success
        (dayRecord?.holidayHours ?: 0.0) > 0 || (dayRecord?.holidayOvertimeHours ?: 0.0) > 0 -> palette.danger
        (dayRecord?.nightHours ?: 0.0) > 0 -> palette.purple
        (dayRecord?.overtimeHours ?: 0.0) > 0 -> palette.warning
        else -> palette.success
    }

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (hasShift) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (hasShift) 1.5.dp else 0.5.dp,
                color = if (hasShift) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(vertical = 2.dp)
        ) {
            Text(
                text = dayNumber.toString(),
                fontSize = 12.sp,
                fontWeight = if (hasShift) FontWeight.ExtraBold else FontWeight.Medium,
                color = when (dayOfWeek) {
                    DayOfWeek.SATURDAY -> palette.accent
                    DayOfWeek.SUNDAY -> palette.danger
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (badgeLabel.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun formatCalendarHours(value: Double): String {
    if (!value.isFinite() || value <= 0.0) return "0"
    return String.format(Locale.US, "%.2f", value)
        .trimEnd('0')
        .trimEnd('.')
}
