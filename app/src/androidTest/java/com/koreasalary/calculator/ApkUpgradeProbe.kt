package com.koreasalary.calculator

import androidx.test.platform.app.InstrumentationRegistry
import com.koreasalary.calculator.data.local.AppDatabase
import com.koreasalary.calculator.data.local.entity.*
import com.koreasalary.calculator.data.model.*
import com.koreasalary.calculator.data.repository.SalaryCalculatorEngine
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit emulator-only two-process fixture: seed under v1, verify after adb install -r v2. */
class ApkUpgradeProbe {
    @Test fun persistedStateAcrossPackageReplacement() = runBlocking {
        val mode = InstrumentationRegistry.getArguments().getString("upgradeProbe")
        assumeTrue("Run separately before/after package replacement", mode == "seed" || mode == "verify")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk_gphone"))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val db = AppDatabase.getDatabase(context, scope)
        val prefs = context.getSharedPreferences("salary_calculator_settings", 0)
        val day = DayRecordEntity("2026-10-04", "2026-10", regularHours = 8.0, overtimeHours = 2.5, nightHours = 4.0, isSundayPay = true, templateId = 98765L, note = "upgrade retained shift")
        val template = ShiftTemplateEntity(id = 98765L, title = "upgrade retained template", colorHex = "#476A9C", regularHours = 8.0, overtimeHours = 2.5)
        val snapshot = MonthlySnapshotEntity("2026-10", hourlyRate = 10000.0, deductionsJson = "{\"isBusinessTax33Enabled\":false}", allowancesJson = "{\"fuelAllowance\":1234}", updatedAt = 123456L)
        try {
            if (mode == "seed") {
                db.dayRecordDao().insertOrUpdate(day)
                db.shiftTemplateDao().insert(template)
                db.monthlySnapshotDao().insertOrUpdate(snapshot)
                assertTrue(prefs.edit().putString("language_code", "ko").putString("theme", "DARK").commit())
            }
            assertEquals(day, db.dayRecordDao().getDayByDate(day.date))
            assertEquals(template, db.shiftTemplateDao().getAllTemplatesSync().single { it.id == template.id })
            assertEquals(snapshot, db.monthlySnapshotDao().getSnapshotSync(snapshot.yearMonth))
            assertEquals("ko", prefs.getString("language_code", null))
            assertEquals("DARK", prefs.getString("theme", null))
            val input = SalaryInputState(hourlyRate = 10000.0).withCalendarRecords(listOf(db.dayRecordDao().getDayByDate(day.date)!!))
            assertEquals(217500L, SalaryCalculatorEngine.calculateSalary(input, DeductionSettings(isBusinessTax33Enabled = false)).breakdown.grossSalary)
        } finally { scope.cancel(); db.close() }
    }
}
