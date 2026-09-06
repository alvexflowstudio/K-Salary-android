package com.koreasalary.calculator.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.koreasalary.calculator.data.model.AppLanguage
import com.koreasalary.calculator.data.model.AppTheme
import com.koreasalary.calculator.domain.i18n.AppStrings
import com.koreasalary.calculator.domain.i18n.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val currentLanguage: StateFlow<AppLanguage> = LanguageManager.currentLanguage
    val currentStrings: StateFlow<AppStrings> = LanguageManager.currentStrings

    private val _currentTheme = MutableStateFlow(loadTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    init {
        loadLanguage()?.let(LanguageManager::setLanguage)
    }

    fun setLanguage(language: AppLanguage) {
        LanguageManager.setLanguage(language)
        preferences.edit {
            putString(KEY_LANGUAGE, language.code)
        }
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        preferences.edit {
            putString(KEY_THEME, theme.name)
        }
    }

    fun resetSettings() {
        preferences.edit {
            clear()
        }
        _currentTheme.value = AppTheme.SYSTEM
        LanguageManager.setLanguage(AppLanguage.RU)
    }

    private fun loadLanguage(): AppLanguage? {
        val code = preferences.getString(KEY_LANGUAGE, null) ?: return null
        return AppLanguage.values().firstOrNull { it.code == code }
    }

    private fun loadTheme(): AppTheme {
        val savedTheme = preferences.getString(KEY_THEME, null) ?: return AppTheme.SYSTEM
        return AppTheme.values().firstOrNull { it.name == savedTheme } ?: AppTheme.SYSTEM
    }

    private companion object {
        const val PREFERENCES_NAME = "salary_calculator_settings"
        const val KEY_LANGUAGE = "language_code"
        const val KEY_THEME = "theme"
    }
}
