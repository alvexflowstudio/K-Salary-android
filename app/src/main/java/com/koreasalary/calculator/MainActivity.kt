package com.koreasalary.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.koreasalary.calculator.presentation.navigation.AppNavGraph
import com.koreasalary.calculator.presentation.theme.KoreaSalaryTheme
import com.koreasalary.calculator.presentation.viewmodel.SalaryViewModel
import com.koreasalary.calculator.presentation.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val salaryViewModel: SalaryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
            val currentStrings by settingsViewModel.currentStrings.collectAsState()
            val currentTheme by settingsViewModel.currentTheme.collectAsState()

            KoreaSalaryTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        salaryViewModel = salaryViewModel,
                        settingsViewModel = settingsViewModel,
                        currentLanguage = currentLanguage,
                        strings = currentStrings
                    )
                }
            }
        }
    }
}
