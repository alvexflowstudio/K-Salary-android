package com.koreasalary.calculator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.koreasalary.calculator.data.model.CompanySize

/**
 * Независимые настройки ставки, вычетов и доплат для каждого месяца (Monthly Snapshot).
 * Ключ yearMonth отделяет записи месяцев друг от друга.
 */
@Entity(tableName = "monthly_snapshots")
data class MonthlySnapshotEntity(
    @PrimaryKey
    val yearMonth: String, // Формат: "YYYY-MM"
    val hourlyRate: Double = 0.0,
    val companySize: CompanySize = CompanySize.FIVE_OR_MORE,
    val deductionsJson: String = "", // Сериализованные настройки налогов
    val allowancesJson: String = "", // Сериализованные доплаты и надбавки
    // Оставлено для чтения старых резервных копий и схемы Room; ручная заморозка удалена из UI.
    val isFrozen: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
