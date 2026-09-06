package com.koreasalary.calculator.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.koreasalary.calculator.data.local.AppDatabase
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.local.entity.MonthlySnapshotEntity
import com.koreasalary.calculator.data.local.entity.ShiftTemplateEntity
import com.koreasalary.calculator.data.model.AllowancesState
import com.koreasalary.calculator.data.model.BackupPayload
import com.koreasalary.calculator.data.model.CompanySize
import com.koreasalary.calculator.data.model.DeductionSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SalaryRepository(
    private val database: AppDatabase
) {
    companion object {
        private const val CURRENT_BACKUP_VERSION = 3
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // 1. Shift Templates
    fun getAllShiftTemplates(): Flow<List<ShiftTemplateEntity>> {
        return database.shiftTemplateDao().getAllTemplates()
    }

    suspend fun saveShiftTemplate(template: ShiftTemplateEntity): Long = withContext(Dispatchers.IO) {
        if (template.id == 0L) {
            database.shiftTemplateDao().insert(template)
        } else {
            database.shiftTemplateDao().update(template)
            template.id
        }
    }

    suspend fun deleteShiftTemplate(id: Long) = withContext(Dispatchers.IO) {
        database.shiftTemplateDao().deleteById(id)
    }

    // 2. Day Records (DTR Calendar)
    fun getDayRecordsForMonth(yearMonth: String): Flow<List<DayRecordEntity>> {
        return database.dayRecordDao().getDaysByMonth(yearMonth)
    }

    suspend fun saveDayRecord(record: DayRecordEntity) = withContext(Dispatchers.IO) {
        database.dayRecordDao().insertOrUpdate(record)
    }

    suspend fun saveDayRecords(records: List<DayRecordEntity>) = withContext(Dispatchers.IO) {
        database.dayRecordDao().insertOrUpdateAll(records)
    }

    suspend fun getDayRecord(date: String): DayRecordEntity? = withContext(Dispatchers.IO) {
        database.dayRecordDao().getDayByDate(date)
    }

    suspend fun deleteDayRecord(date: String) = withContext(Dispatchers.IO) {
        database.dayRecordDao().deleteByDate(date)
    }

    // 3. Monthly Snapshots (one independent settings record per YYYY-MM)
    fun getMonthlySnapshot(yearMonth: String): Flow<MonthlySnapshotEntity?> {
        return database.monthlySnapshotDao().getSnapshot(yearMonth)
    }

    suspend fun saveMonthlySnapshot(
        yearMonth: String,
        hourlyRate: Double,
        deductions: DeductionSettings,
        allowances: AllowancesState
    ) = withContext(Dispatchers.IO) {
        val snapshot = MonthlySnapshotEntity(
            yearMonth = yearMonth,
            hourlyRate = hourlyRate,
            deductionsJson = gson.toJson(deductions),
            allowancesJson = gson.toJson(allowances),
            updatedAt = System.currentTimeMillis()
        )
        database.monthlySnapshotDao().insertOrUpdate(snapshot)
    }

    fun parseDeductionsJson(json: String?): DeductionSettings {
        if (json.isNullOrBlank()) return DeductionSettings()
        return try {
            val parsed = gson.fromJson(json, DeductionSettings::class.java)
            (parsed ?: DeductionSettings()).copy(customDeductions = parsed?.customDeductions.orEmpty())
        } catch (e: Exception) {
            DeductionSettings()
        }
    }

    fun parseAllowancesJson(json: String?): AllowancesState {
        if (json.isNullOrBlank()) return AllowancesState()
        return try {
            val parsed = gson.fromJson(json, AllowancesState::class.java)
            (parsed ?: AllowancesState()).copy(customBonuses = parsed?.customBonuses.orEmpty())
        } catch (e: Exception) {
            AllowancesState()
        }
    }

    // 4. Backup & Restore (JSON Export / Import)
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val templates = database.shiftTemplateDao().getAllTemplatesSync()
        val dayRecords = database.dayRecordDao().getAllRecords()
        val snapshots = database.monthlySnapshotDao().getAllSnapshots()

        val payload = BackupPayload(
            version = CURRENT_BACKUP_VERSION,
            timestamp = System.currentTimeMillis(),
            templates = templates,
            dayRecords = dayRecords,
            snapshots = snapshots
        )
        gson.toJson(payload)
    }

    suspend fun importDataFromJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalizedJson = normalizeBackupJson(json) ?: return@withContext false
            val payload = gson.fromJson(normalizedJson, BackupPayload::class.java)
                ?: return@withContext false
            if (payload.version > CURRENT_BACKUP_VERSION) return@withContext false

            database.withTransaction {
                // Импорт переносит состояние приложения целиком. Старые записи,
                // которых нет в файле, не должны оставаться на новом устройстве.
                database.dayRecordDao().clearAll()
                database.monthlySnapshotDao().clearAll()
                database.shiftTemplateDao().clearAll()

                val templates = payload.templates.ifEmpty { ShiftTemplateEntity.builtInDefaults() }
                database.shiftTemplateDao().insertAll(templates)
                if (payload.dayRecords.isNotEmpty()) {
                    database.dayRecordDao().insertOrUpdateAll(payload.dayRecords)
                }
                if (payload.snapshots.isNotEmpty()) {
                    payload.snapshots.forEach { snapshot ->
                        database.monthlySnapshotDao().insertOrUpdate(snapshot)
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Делает импорт обратно совместимым с резервными копиями версии 1.
     * Gson не вызывает Kotlin-конструктор с default-значениями, поэтому новое
     * поле companySize нужно добавить до десериализации старого JSON.
     */
    private fun normalizeBackupJson(json: String): JsonObject? {
        val root = JsonParser.parseString(json).takeIf { it.isJsonObject }?.asJsonObject
            ?: return null

        ensureArray(root, "templates")
        ensureArray(root, "dayRecords")
        ensureArray(root, "snapshots")
        val snapshots = root.getAsJsonArray("snapshots")
        snapshots?.forEach { element ->
            if (!element.isJsonObject) return@forEach
            val snapshot = element.asJsonObject
            val companySize = snapshot.get("companySize")
            val companySizeName = companySize
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString
            if (companySizeName != CompanySize.UNDER_5.name &&
                companySizeName != CompanySize.FIVE_OR_MORE.name
            ) {
                snapshot.addProperty("companySize", CompanySize.FIVE_OR_MORE.name)
            }
        }

        val version = root.get("version")
            ?.takeIf { it.isJsonPrimitive && !it.asJsonPrimitive.isBoolean }
            ?.asInt
        if (version == null || version < 1) {
            root.addProperty("version", 1)
        }

        return root
    }

    private fun ensureArray(root: JsonObject, name: String) {
        val value = root.get(name)
        if (value == null || value.isJsonNull || !value.isJsonArray) {
            root.add(name, com.google.gson.JsonArray())
        }
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        database.dayRecordDao().clearAll()
        database.monthlySnapshotDao().clearAll()
        database.shiftTemplateDao().clearAll()
        database.shiftTemplateDao().insertAll(ShiftTemplateEntity.builtInDefaults())
    }
}
