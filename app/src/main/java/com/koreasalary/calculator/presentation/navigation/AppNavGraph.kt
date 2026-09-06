package com.koreasalary.calculator.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.koreasalary.calculator.data.model.AppLanguage
import com.koreasalary.calculator.domain.i18n.AppStrings
import com.koreasalary.calculator.presentation.screens.calculator.CalculatorScreen
import com.koreasalary.calculator.presentation.screens.deductions.DeductionsScreen
import com.koreasalary.calculator.presentation.screens.info.InfoScreen
import com.koreasalary.calculator.presentation.screens.settings.SettingsScreen
import com.koreasalary.calculator.presentation.viewmodel.SalaryViewModel
import com.koreasalary.calculator.presentation.viewmodel.SettingsViewModel

@Composable
fun AppNavGraph(
    salaryViewModel: SalaryViewModel,
    settingsViewModel: SettingsViewModel,
    currentLanguage: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var tabEntryKey by remember { mutableIntStateOf(0) }

    // Калькулятор является домашним экраном: первое нажатие "Назад" с другой
    // вкладки возвращает сюда, второе уже обрабатывает Activity и закрывает приложение.
    BackHandler(enabled = currentRoute != null && currentRoute != Screen.Calculator.route) {
        tabEntryKey += 1
        navController.navigate(Screen.Calculator.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    Scaffold(
        // Вкладки сами содержат Scaffold с TopAppBar. Системные inset-поля
        // должны применяться только внутри вкладки, иначе верх и низ считаются дважды.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Screen.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                tabEntryKey += 1
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        // Новый вход в раздел должен начинаться сверху.
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.getTitle(strings)
                            )
                        },
                        label = {
                            Text(
                                text = screen.getTitle(strings),
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        alwaysShowLabel = true
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Calculator.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Calculator.route) {
                CalculatorScreen(
                    viewModel = salaryViewModel,
                    strings = strings,
                    resetKey = tabEntryKey
                )
            }
            composable(Screen.Deductions.route) {
                DeductionsScreen(
                    viewModel = salaryViewModel,
                    strings = strings,
                    resetKey = tabEntryKey
                )
            }
            composable(Screen.Info.route) {
                InfoScreen(
                    currentLanguage = currentLanguage,
                    strings = strings,
                    resetKey = tabEntryKey
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    salaryViewModel = salaryViewModel,
                    strings = strings,
                    resetKey = tabEntryKey
                )
            }
        }
    }
}
