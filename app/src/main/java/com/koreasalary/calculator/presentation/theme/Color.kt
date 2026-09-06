package com.koreasalary.calculator.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Цветовые роли приложения. Значения синхронизированы с двумя HTML-макетами
 * калькулятора и не зависят от системной динамической палитры Android.
 */
@Immutable
data class AppPalette(
    val screenBackground: Color,
    val card: Color,
    val cardBorder: Color,
    val accent: Color,
    val heroInner: Color,
    val onAccent: Color,
    val heroSecondaryText: Color,
    val structureText: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val danger: Color,
    val heroDanger: Color,
    val success: Color,
    val warning: Color,
    val purple: Color,
    val gray: Color,
    val activeIconBackground: Color,
    val onActiveIconBackground: Color,
    val onDanger: Color
)

val LightAppPalette = AppPalette(
    screenBackground = Color(0xFFF5F7F9),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFDFE1E5),
    accent = Color(0xFF476A9C),
    heroInner = Color(0xFF36537E),
    onAccent = Color(0xFFFFFFFF),
    heroSecondaryText = Color(0xFFFFFFFF),
    structureText = Color(0xFFFFFFFF),
    primaryText = Color(0xFF1C1F24),
    secondaryText = Color(0xFF6E7278),
    tertiaryText = Color(0xFF777A80),
    danger = Color(0xFFA96464),
    heroDanger = Color(0xFFA96464),
    success = Color(0xFF5C875D),
    warning = Color(0xFFBD835B),
    purple = Color(0xFF816FA3),
    gray = Color(0xFF6E7278),
    activeIconBackground = Color(0xFFDCE5F2),
    onActiveIconBackground = Color(0xFF36537E),
    onDanger = Color(0xFFFFFFFF)
)

val DarkAppPalette = AppPalette(
    screenBackground = Color(0xFF0F0B03),
    card = Color(0xFF181309),
    cardBorder = Color(0xFF2A261B),
    accent = Color(0xFFDED7C5),
    heroInner = Color(0xFFC6BDA8),
    onAccent = Color(0xFF231F14),
    heroSecondaryText = Color(0xFF534D3E),
    structureText = Color(0xFF322D22),
    primaryText = Color(0xFFEEEEF0),
    secondaryText = Color(0xFF928F88),
    tertiaryText = Color(0xFF7D7A74),
    danger = Color(0xFFCA8281),
    heroDanger = Color(0xFF972430),
    success = Color(0xFF79A67A),
    warning = Color(0xFFD1966D),
    purple = Color(0xFFA08DC3),
    gray = Color(0xFF89867F),
    activeIconBackground = Color(0xFFDED7C5),
    onActiveIconBackground = Color(0xFF231F14),
    onDanger = Color(0xFF231F14)
)

val LocalAppPalette = staticCompositionLocalOf { LightAppPalette }
