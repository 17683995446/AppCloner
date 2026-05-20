
package com.appcloner.data.repository

import com.appcloner.core.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    
    fun getAllApps(): Flow&lt;List&lt;AppInfo&gt;&gt;
    
    fun getFavoriteApps(): Flow&lt;List&lt;AppInfo&gt;&gt;
    
    fun getClonedApps(): Flow&lt;List&lt;AppInfo&gt;&gt;
    
    fun getUserApps(): Flow&lt;List&lt;AppInfo&gt;&gt;
    
    fun getAppByPackageName(packageName: String): Flow&lt;AppInfo?&gt;
    
    fun searchApps(query: String): Flow&lt;List&lt;AppInfo&gt;&gt;
    
    suspend fun insertApp(app: AppInfo): Long
    
    suspend fun insertApps(apps: List&lt;AppInfo&gt;): List&lt;Long&gt;
    
    suspend fun updateApp(app: AppInfo)
    
    suspend fun deleteApp(app: AppInfo)
    
    suspend fun deleteAppByPackageName(packageName: String)
    
    suspend fun deleteAllApps()
    
    suspend fun toggleFavorite(packageName: String)
    
    suspend fun incrementCloneCount(packageName: String)
    
    suspend fun updateLastAccessed(packageName: String)
    
    suspend fun getAppCount(): Int
    
    suspend fun getFavoriteAppCount(): Int
    
    suspend fun loadAndSyncInstalledApps(): Result&lt;Int&gt;
}
