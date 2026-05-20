
package com.appcloner.data.repository

import com.appcloner.core.model.CloneHistory
import kotlinx.coroutines.flow.Flow

interface CloneHistoryRepository {
    
    fun getAllHistory(): Flow&lt;List&lt;CloneHistory&gt;&gt;
    
    fun getHistoryByPackage(packageName: String): Flow&lt;List&lt;CloneHistory&gt;&gt;
    
    fun getRecentSuccessfulClones(limit: Int = 10): Flow&lt;List&lt;CloneHistory&gt;&gt;
    
    suspend fun saveHistory(history: CloneHistory)
    
    suspend fun deleteHistory(id: String): Boolean
    
    suspend fun deleteAllHistory()
    
    suspend fun getHistoryCount(): Int
    
    suspend fun getSuccessCount(): Int
}
