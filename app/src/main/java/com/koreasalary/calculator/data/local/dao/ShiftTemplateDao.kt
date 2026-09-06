package com.koreasalary.calculator.data.local.dao

import androidx.room.*
import com.koreasalary.calculator.data.local.entity.ShiftTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftTemplateDao {
    @Query("SELECT * FROM shift_templates ORDER BY orderIndex ASC, id ASC")
    fun getAllTemplates(): Flow<List<ShiftTemplateEntity>>

    @Query("SELECT * FROM shift_templates ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllTemplatesSync(): List<ShiftTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: ShiftTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<ShiftTemplateEntity>)

    @Update
    suspend fun update(template: ShiftTemplateEntity)

    @Delete
    suspend fun delete(template: ShiftTemplateEntity)

    @Query("DELETE FROM shift_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shift_templates")
    suspend fun clearAll()
}
