package com.koreasalary.calculator.domain.i18n

import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Localize presentation only; the payroll calendar retains Gregorian/ISO dates. */
fun formatDayHeading(date: LocalDate, strings: AppStrings): String = date.format(
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.forLanguageTag(strings.localeTag))
        .withChronology(IsoChronology.INSTANCE)
)

fun formatMonthHeading(month: YearMonth, strings: AppStrings): String =
    if (strings.localeTag == "ko") month.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN))
    else "${strings.ui.monthNames[month.monthValue - 1]} ${month.year}"
