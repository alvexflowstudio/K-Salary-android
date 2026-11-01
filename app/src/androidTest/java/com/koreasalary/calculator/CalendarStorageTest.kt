package com.koreasalary.calculator

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.koreasalary.calculator.data.local.AppDatabase
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.model.*
import com.koreasalary.calculator.data.repository.SalaryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CalendarStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun versionFiveUpgradePreservesRawRecordsTemplatesAndSnapshotAndRoundTripsBackup() = runBlocking {
        val name = "calendar-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `shift_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `regularHours` REAL NOT NULL, `scheduledBaseHours` REAL NOT NULL, `overtimeHours` REAL NOT NULL, `nightHours` REAL NOT NULL, `holidayHours` REAL NOT NULL, `holidayOvertimeHours` REAL NOT NULL, `isPaidHoliday` INTEGER NOT NULL, `isSundayPay` INTEGER NOT NULL, `isDefault` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `day_records` (`date` TEXT NOT NULL, `yearMonth` TEXT NOT NULL, `regularHours` REAL NOT NULL, `scheduledBaseHours` REAL NOT NULL, `overtimeHours` REAL NOT NULL, `nightHours` REAL NOT NULL, `holidayHours` REAL NOT NULL, `holidayOvertimeHours` REAL NOT NULL, `isPaidHoliday` INTEGER NOT NULL, `isSundayPay` INTEGER NOT NULL, `templateId` INTEGER, `note` TEXT NOT NULL, PRIMARY KEY(`date`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `monthly_snapshots` (`yearMonth` TEXT NOT NULL, `hourlyRate` REAL NOT NULL, `companySize` TEXT NOT NULL, `deductionsJson` TEXT NOT NULL, `allowancesJson` TEXT NOT NULL, `isFrozen` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`yearMonth`))")
                        db.execSQL("INSERT INTO day_records VALUES ('2026-11-01','2026-11',8,4,2.5,4,3,0.5,1,1,7,'legacy note')")
                        db.execSQL("INSERT INTO shift_templates VALUES (7,'legacy','#476A9C',8,4,2.5,4,3,0.5,1,1,0,5)")
                        db.execSQL("INSERT INTO monthly_snapshots VALUES ('2026-11',10000,'FIVE_OR_MORE','{}','{}',0,123)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build())
        helper.writableDatabase.close()
        helper.close()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_5_6).build()
        try {
            val before = database.dayRecordDao().getDayByDate("2026-11-01")!!
            assertEquals(1, before.workHoursVersion)
            assertEquals(8.0, before.regularHours, 0.0)
            assertEquals(2.5, before.overtimeHours, 0.0)
            assertEquals(3.0, before.holidayHours, 0.0)
            assertEquals("legacy note", before.note)
            assertEquals(7L, before.templateId)
            val template = database.shiftTemplateDao().getAllTemplatesSync().single()
            assertEquals(1, template.workHoursVersion)
            assertEquals(WorkHours.from(before), WorkHours.from(template.recordFor(LocalDate.parse(before.date))))
            val snapshot = database.monthlySnapshotDao().getAllSnapshots().single()
            assertEquals(10000.0, snapshot.hourlyRate, 0.0)
            val repository = SalaryRepository(database)
            val json = repository.exportDataToJson()
            assertTrue(json.contains("\"version\": 4"))
            assertTrue(repository.importDataFromJson(json))
            assertEquals(before, database.dayRecordDao().getDayByDate(before.date))
            assertEquals(template, database.shiftTemplateDao().getAllTemplatesSync().single())
            assertEquals(snapshot, database.monthlySnapshotDao().getAllSnapshots().single())
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun oldBackupsAndNewExplicitRecordsRoundTripThroughRealRoom() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = SalaryRepository(database)
            for (version in 1..3) {
                val json = """{"version":$version,"templates":[{"id":7,"title":"old","colorHex":"#476A9C","regularHours":8,"isSundayPay":true}],"dayRecords":[{"date":"2026-11-01","yearMonth":"2026-11","regularHours":8,"overtimeHours":2.5,"holidayHours":3,"isSundayPay":true,"note":"old"}],"snapshots":[]}"""
                assertTrue(repository.importDataFromJson(json))
                val old = database.dayRecordDao().getDayByDate("2026-11-01")!!
                assertEquals(0, old.workHoursVersion)
                assertEquals(WorkHours(0.0, 2.5, 0.0, 8.0, 3.0), WorkHours.from(old))
                val backup = repository.exportDataToJson()
                assertTrue(repository.importDataFromJson(backup))
                assertEquals(old, database.dayRecordDao().getDayByDate(old.date))
            }
            val explicit = DayRecordEntity("2026-11-02", "2026-11", regularHours = 8.0,
                overtimeHours = 2.5, holidayHours = 3.0, isSundayPay = true, isPaidHoliday = true)
            repository.saveDayRecord(explicit)
            val backup = repository.exportDataToJson()
            assertTrue(repository.importDataFromJson(backup))
            assertEquals(explicit, database.dayRecordDao().getDayByDate(explicit.date))
            assertEquals(8.0, WorkHours.from(explicit).regular, 0.0)
        } finally { database.close() }
    }
}
