package com.koreasalary.calculator.data.local.dao

import androidx.room.*
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayRecordDao {
    @Query("SELECT * FROM day_records WHERE yearMonth = :yearMonth ORDER BY date ASC")
    fun getDaysByMonth(yearMonth: String): Flow<List<DayRecordEntity>>

    @Query("SELECT * FROM day_records WHERE date = :date LIMIT 1")
    suspend fun getDayByDate(date: String): DayRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(day: DayRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(days: List<DayRecordEntity>)

    @Query("DELETE FROM day_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("SELECT * FROM day_records")
    suspend fun getAllRecords(): List<DayRecordEntity>

    @Query("DELETE FROM day_records")
    suspend fun clearAll()
}
