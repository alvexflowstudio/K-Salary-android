package com.koreasalary.calculator.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.koreasalary.calculator.domain.i18n.AppStrings

sealed class Screen(
    val route: String,
    val icon: ImageVector,
    val getTitle: (AppStrings) -> String
) {
    object Calculator : Screen(
        route = "calculator",
        icon = Icons.Default.Calculate,
        getTitle = { it.tabCalculator }
    )

    object Deductions : Screen(
        route = "deductions",
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        getTitle = { it.tabDeductions }
    )

    object Info : Screen(
        route = "info",
        icon = Icons.Default.Info,
        getTitle = { it.tabInfo }
    )

    object Settings : Screen(
        route = "settings",
        icon = Icons.Default.Settings,
        getTitle = { it.tabSettings }
    )

    companion object {
        val items = listOf(Calculator, Deductions, Info, Settings)
    }
}
