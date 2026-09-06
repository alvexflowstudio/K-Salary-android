package com.koreasalary.calculator.domain.i18n

import com.koreasalary.calculator.data.model.AppLanguage
import com.koreasalary.calculator.domain.i18n.translations.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.RU) // Default: Russian
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _currentStrings = MutableStateFlow(RuStrings.copy(ui = UiTranslations.forLanguage(AppLanguage.RU)))
    val currentStrings: StateFlow<AppStrings> = _currentStrings.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        _currentStrings.value = getStringsFor(language)
    }

    fun getStringsFor(language: AppLanguage): AppStrings {
        val baseStrings = when (language) {
            AppLanguage.RU -> RuStrings
            AppLanguage.KO -> KoStrings
            AppLanguage.EN -> EnStrings
            AppLanguage.KY -> KyStrings
            AppLanguage.UZ -> UzStrings
            AppLanguage.KK -> KkStrings
            AppLanguage.VI -> ViStrings
            AppLanguage.TH -> ThStrings
            AppLanguage.FIL -> FilStrings
            AppLanguage.ID -> IdStrings
            AppLanguage.MS -> MsStrings
            AppLanguage.MY -> MyStrings
        }
        return baseStrings.copy(ui = UiTranslations.forLanguage(language))
    }
}
