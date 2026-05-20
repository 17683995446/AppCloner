package com.appcloner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appcloner.data.local.entity.CloneHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloneHistoryDao {

    @Query("SELECT * FROM clone_history ORDER BY cloneTimestamp DESC")
    fun getAllHistory(): Flow<List<CloneHistoryEntity>>

    @Query("SELECT * FROM clone_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): CloneHistoryEntity?

    @Query("SELECT * FROM clone_history WHERE originalPackageName = :packageName ORDER BY cloneTimestamp DESC")
    fun getHistoryByPackage(packageName: String): Flow<List<CloneHistoryEntity>>

    @Query("SELECT * FROM clone_history WHERE isSuccessful = 1 ORDER BY cloneTimestamp DESC LIMIT :limit")
    fun getRecentSuccessfulClones(limit: Int = 10): Flow<List<CloneHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: CloneHistoryEntity): Long

    @Update
    suspend fun update(history: CloneHistoryEntity)

    @Delete
    suspend fun delete(history: CloneHistoryEntity)

    @Query("DELETE FROM clone_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clone_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM clone_history")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM clone_history WHERE isSuccessful = 1")
    suspend fun getSuccessCount(): Int
}
