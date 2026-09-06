package com.koreasalary.calculator.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.koreasalary.calculator.data.local.dao.DayRecordDao
import com.koreasalary.calculator.data.local.dao.MonthlySnapshotDao
import com.koreasalary.calculator.data.local.dao.ShiftTemplateDao
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.local.entity.MonthlySnapshotEntity
import com.koreasalary.calculator.data.local.entity.ShiftTemplateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ShiftTemplateEntity::class,
        DayRecordEntity::class,
        MonthlySnapshotEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shiftTemplateDao(): ShiftTemplateDao
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun monthlySnapshotDao(): MonthlySnapshotDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE monthly_snapshots ADD COLUMN companySize TEXT NOT NULL DEFAULT 'FIVE_OR_MORE'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE shift_templates SET title = '8ч Ночная', nightHours = 8.0 " +
                        "WHERE isDefault = 1 AND title = '8ч Дневная'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE day_records ADD COLUMN scheduledBaseHours REAL NOT NULL DEFAULT 0.0"
                )
                db.execSQL(
                    "ALTER TABLE shift_templates ADD COLUMN scheduledBaseHours REAL NOT NULL DEFAULT 0.0"
                )
                // Старые записи хранили плановую смену в regularHours. Сохраняем
                // её для особых дней, где regularHours теперь должен быть 0.
                db.execSQL(
                    "UPDATE day_records SET scheduledBaseHours = regularHours " +
                        "WHERE scheduledBaseHours = 0.0 AND regularHours > 0.0"
                )
                db.execSQL(
                    "UPDATE shift_templates SET scheduledBaseHours = regularHours " +
                        "WHERE scheduledBaseHours = 0.0 AND regularHours > 0.0"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Обновлённым пользователям добавляем дневной встроенный шаблон,
                // сохраняя существующий ночной и все пользовательские шаблоны.
                db.execSQL(
                    "INSERT INTO shift_templates " +
                        "(title, colorHex, regularHours, scheduledBaseHours, overtimeHours, nightHours, " +
                        "holidayHours, holidayOvertimeHours, isPaidHoliday, isSundayPay, isDefault, orderIndex) " +
                        "SELECT '8ч Дневная', '#476A9C', 8.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 1, 0 " +
                        "WHERE NOT EXISTS (SELECT 1 FROM shift_templates WHERE isDefault = 1 AND nightHours <= 0.0)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "korea_salary_calculator.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.shiftTemplateDao())
                }
            }
        }

        suspend fun populateInitialData(templateDao: ShiftTemplateDao) {
            // Новый пользователь сразу получает дневной и ночной шаблон по 8 часов.
            templateDao.insertAll(ShiftTemplateEntity.builtInDefaults())
        }
    }
}
