package com.koreasalary.calculator.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.koreasalary.calculator.data.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = DarkAppPalette.accent,
    onPrimary = DarkAppPalette.onAccent,
    primaryContainer = DarkAppPalette.activeIconBackground,
    onPrimaryContainer = DarkAppPalette.onActiveIconBackground,
    secondary = DarkAppPalette.success,
    onSecondary = DarkAppPalette.onAccent,
    secondaryContainer = DarkAppPalette.warning,
    onSecondaryContainer = DarkAppPalette.onAccent,
    tertiary = DarkAppPalette.purple,
    onTertiary = DarkAppPalette.onAccent,
    tertiaryContainer = DarkAppPalette.gray,
    onTertiaryContainer = DarkAppPalette.onAccent,
    error = DarkAppPalette.danger,
    onError = DarkAppPalette.onDanger,
    errorContainer = DarkAppPalette.heroDanger,
    onErrorContainer = DarkAppPalette.onAccent,
    background = DarkAppPalette.screenBackground,
    onBackground = DarkAppPalette.primaryText,
    surface = DarkAppPalette.card,
    onSurface = DarkAppPalette.primaryText,
    surfaceVariant = DarkAppPalette.cardBorder,
    onSurfaceVariant = DarkAppPalette.secondaryText,
    outline = DarkAppPalette.cardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = LightAppPalette.accent,
    onPrimary = LightAppPalette.onAccent,
    primaryContainer = LightAppPalette.activeIconBackground,
    onPrimaryContainer = LightAppPalette.onActiveIconBackground,
    secondary = LightAppPalette.success,
    onSecondary = LightAppPalette.onAccent,
    secondaryContainer = LightAppPalette.warning,
    onSecondaryContainer = LightAppPalette.primaryText,
    tertiary = LightAppPalette.purple,
    onTertiary = LightAppPalette.onAccent,
    tertiaryContainer = LightAppPalette.gray,
    onTertiaryContainer = LightAppPalette.onAccent,
    error = LightAppPalette.danger,
    onError = LightAppPalette.onDanger,
    errorContainer = LightAppPalette.heroDanger,
    onErrorContainer = LightAppPalette.onAccent,
    background = LightAppPalette.screenBackground,
    onBackground = LightAppPalette.primaryText,
    surface = LightAppPalette.card,
    onSurface = LightAppPalette.primaryText,
    surfaceVariant = LightAppPalette.cardBorder,
    onSurfaceVariant = LightAppPalette.secondaryText,
    outline = LightAppPalette.cardBorder
)

@Composable
fun KoreaSalaryTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val appPalette = if (darkTheme) DarkAppPalette else LightAppPalette

    CompositionLocalProvider(LocalAppPalette provides appPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
