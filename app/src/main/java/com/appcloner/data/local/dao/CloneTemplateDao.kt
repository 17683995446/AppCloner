package com.appcloner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appcloner.data.local.entity.CloneTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloneTemplateDao {

    @Query("SELECT * FROM clone_templates ORDER BY usageCount DESC, createdAt DESC")
    fun getAllTemplates(): Flow<List<CloneTemplateEntity>>

    @Query("SELECT * FROM clone_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): CloneTemplateEntity?

    @Query("SELECT * FROM clone_templates WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultTemplate(): CloneTemplateEntity?

    @Query("SELECT * FROM clone_templates WHERE name LIKE '%' || :query || '%'")
    fun searchTemplates(query: String): Flow<List<CloneTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: CloneTemplateEntity): Long

    @Update
    suspend fun update(template: CloneTemplateEntity)

    @Delete
    suspend fun delete(template: CloneTemplateEntity)

    @Query("UPDATE clone_templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: Long)

    @Query("SELECT COUNT(*) FROM clone_templates")
    suspend fun getCount(): Int
}
