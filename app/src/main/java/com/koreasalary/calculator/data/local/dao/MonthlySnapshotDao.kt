package com.koreasalary.calculator.data.local.dao

import androidx.room.*
import com.koreasalary.calculator.data.local.entity.MonthlySnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySnapshotDao {
    @Query("SELECT * FROM monthly_snapshots WHERE yearMonth = :yearMonth LIMIT 1")
    fun getSnapshot(yearMonth: String): Flow<MonthlySnapshotEntity?>

    @Query("SELECT * FROM monthly_snapshots WHERE yearMonth = :yearMonth LIMIT 1")
    suspend fun getSnapshotSync(yearMonth: String): MonthlySnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(snapshot: MonthlySnapshotEntity)

    @Query("SELECT * FROM monthly_snapshots ORDER BY yearMonth DESC")
    suspend fun getAllSnapshots(): List<MonthlySnapshotEntity>

    @Query("DELETE FROM monthly_snapshots")
    suspend fun clearAll()
}
