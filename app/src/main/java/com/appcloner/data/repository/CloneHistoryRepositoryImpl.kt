
package com.appcloner.data.repository

import com.appcloner.core.model.CloneHistory
import com.appcloner.data.local.dao.CloneHistoryDao
import com.appcloner.data.local.entity.CloneHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloneHistoryRepositoryImpl @Inject constructor(
    private val cloneHistoryDao: CloneHistoryDao
) : CloneHistoryRepository {
    
    override fun getAllHistory(): Flow&lt;List&lt;CloneHistory&gt;&gt; {
        return cloneHistoryDao.getAllHistory()
            .map { entities -&gt; entities.map { toDomain(it) } }
    }
    
    override fun getHistoryByPackage(packageName: String): Flow&lt;List&lt;CloneHistory&gt;&gt; {
        return cloneHistoryDao.getHistoryByPackage(packageName)
            .map { entities -&gt; entities.map { toDomain(it) } }
    }
    
    override fun getRecentSuccessfulClones(limit: Int): Flow&lt;List&lt;CloneHistory&gt;&gt; {
        return cloneHistoryDao.getRecentSuccessfulClones(limit)
            .map { entities -&gt; entities.map { toDomain(it) } }
    }
    
    override suspend fun saveHistory(history: CloneHistory) {
        val entity = toEntity(history)
        cloneHistoryDao.insert(entity)
        Timber.d("Saved clone history: ${history.clonedAppName}")
    }
    
    override suspend fun deleteHistory(id: String): Boolean {
        Timber.d("Deleting clone history: $id")
        val longId = id.toLongOrNull() ?: return false
        cloneHistoryDao.deleteById(longId)
        return true
    }
    
    override suspend fun deleteAllHistory() {
        Timber.d("Deleting all clone history")
        cloneHistoryDao.deleteAll()
    }
    
    override suspend fun getHistoryCount(): Int {
        return cloneHistoryDao.getCount()
    }
    
    override suspend fun getSuccessCount(): Int {
        return cloneHistoryDao.getSuccessCount()
    }
    
    private fun toDomain(entity: CloneHistoryEntity): CloneHistory {
        return CloneHistory(
            id = entity.id.toString(),
            originalPackageName = entity.originalPackageName,
            clonedPackageName = entity.clonedPackageName,
            clonedAppName = entity.clonedAppName,
            cloneTimestamp = entity.cloneTimestamp,
            apkPath = entity.apkPath,
            appSize = entity.appSize,
            isSuccessful = entity.isSuccessful,
            isActive = true,
            errorMessage = entity.errorMessage
        )
    }
    
    private fun toEntity(domain: CloneHistory): CloneHistoryEntity {
        return CloneHistoryEntity(
            id = domain.id.toLongOrNull() ?: 0L,
            originalPackageName = domain.originalPackageName,
            clonedPackageName = domain.clonedPackageName,
            clonedAppName = domain.clonedAppName,
            cloneTimestamp = domain.cloneTimestamp,
            apkPath = domain.apkPath ?: "",
            appSize = domain.appSize,
            isSuccessful = domain.isSuccessful,
            errorMessage = domain.errorMessage
        )
    }
}
