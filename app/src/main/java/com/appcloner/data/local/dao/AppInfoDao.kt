
package com.appcloner.data.local.dao

import androidx.room.*
import com.appcloner.data.local.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {
    
    @Query("SELECT * FROM apps ORDER BY appName ASC")
    fun getAllApps(): Flow&lt;List&lt;AppInfoEntity&gt;&gt;
    
    @Query("SELECT * FROM apps WHERE isFavorite = 1 ORDER BY appName ASC")
    fun getFavoriteApps(): Flow&lt;List&lt;AppInfoEntity&gt;&gt;
    
    @Query("SELECT * FROM apps WHERE isCloned = 1 ORDER BY lastUpdateTime DESC")
    fun getClonedApps(): Flow&lt;List&lt;AppInfoEntity&gt;&gt;
    
    @Query("SELECT * FROM apps WHERE isSystemApp = 0 ORDER BY appName ASC")
    fun getUserApps(): Flow&lt;List&lt;AppInfoEntity&gt;&gt;
    
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    fun getAppByPackageName(packageName: String): Flow&lt;AppInfoEntity?&gt;
    
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackageNameSync(packageName: String): AppInfoEntity?
    
    @Query("SELECT * FROM apps WHERE appName LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%' ORDER BY appName ASC")
    fun searchApps(query: String): Flow&lt;List&lt;AppInfoEntity&gt;&gt;
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfoEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List&lt;AppInfoEntity&gt;): List&lt;Long&gt;
    
    @Update
    suspend fun updateApp(app: AppInfoEntity)
    
    @Delete
    suspend fun deleteApp(app: AppInfoEntity)
    
    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteAppByPackageName(packageName: String)
    
    @Query("DELETE FROM apps")
    suspend fun deleteAllApps()
    
    @Query("UPDATE apps SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun updateFavorite(packageName: String, isFavorite: Boolean)
    
    @Query("UPDATE apps SET cloneCount = cloneCount + 1, isCloned = 1, lastAccessedTime = :timestamp WHERE packageName = :packageName")
    suspend fun incrementCloneCount(packageName: String, timestamp: Long)
    
    @Query("UPDATE apps SET lastAccessedTime = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastAccessed(packageName: String, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM apps")
    suspend fun getAppCount(): Int
    
    @Query("SELECT COUNT(*) FROM apps WHERE isFavorite = 1")
    suspend fun getFavoriteAppCount(): Int
}
