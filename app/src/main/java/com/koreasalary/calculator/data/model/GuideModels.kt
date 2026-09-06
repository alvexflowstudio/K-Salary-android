package com.koreasalary.calculator.data.model

/**
 * Модель отдельного пункта в справочнике работника
 */
data class GuideTopic(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val keyPoints: List<String> = emptyList(),
    val iconName: String = "info"
)

/**
 * 5 основных разделов справочника
 */
data class GuideSection(
    val id: String,
    val title: String,
    val koreanTitle: String,
    val description: String,
    val topics: List<GuideTopic>
)
