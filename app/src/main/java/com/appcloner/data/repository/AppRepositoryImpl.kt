
package com.appcloner.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.appcloner.core.model.AppInfo
import com.appcloner.data.local.dao.AppInfoDao
import com.appcloner.data.mapper.AppInfoMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    private val context: Context,
    private val appInfoDao: AppInfoDao
) : AppRepository {
    
    private val packageManager: PackageManager = context.packageManager
    
    init {
        Timber.d("[AppRepositoryImpl] Initializing AppRepositoryImpl")
    }
    
    override fun getAllApps(): Flow&lt;List&lt;AppInfo&gt;&gt; {
        Timber.d("[AppRepositoryImpl] getAllApps() called")
        return appInfoDao.getAllApps()
            .map { 
                Timber.d("[AppRepositoryImpl] Mapping ${it.size} entities to domain models")
                AppInfoMapper.toDomainList(it) 
            }
    }
    
    override fun getFavoriteApps(): Flow&lt;List&lt;AppInfo&gt;&gt; {
        Timber.d("[AppRepositoryImpl] getFavoriteApps() called")
        return appInfoDao.getFavoriteApps()
            .map { 
                Timber.d("[AppRepositoryImpl] Mapping ${it.size} favorite entities to domain models")
                AppInfoMapper.toDomainList(it) 
            }
    }
    
    override fun getClonedApps(): Flow&lt;List&lt;AppInfo&gt;&gt; {
        Timber.d("[AppRepositoryImpl] getClonedApps() called")
        return appInfoDao.getClonedApps()
            .map { 
                Timber.d("[AppRepositoryImpl] Mapping ${it.size} cloned entities to domain models")
                AppInfoMapper.toDomainList(it) 
            }
    }
    
    override fun getUserApps(): Flow&lt;List&lt;AppInfo&gt;&gt; {
        Timber.d("[AppRepositoryImpl] getUserApps() called")
        return appInfoDao.getUserApps()
            .map { 
                Timber.d("[AppRepositoryImpl] Mapping ${it.size} user app entities to domain models")
                AppInfoMapper.toDomainList(it) 
            }
    }
    
    override fun getAppByPackageName(packageName: String): Flow&lt;AppInfo?&gt; {
        Timber.d("[AppRepositoryImpl] getAppByPackageName() called for: $packageName")
        return appInfoDao.getAppByPackageName(packageName)
            .map { 
                Timber.d("[AppRepositoryImpl] Found entity for $packageName: ${it != null}")
                it?.let { AppInfoMapper.toDomain(it) } 
            }
    }
    
    override fun searchApps(query: String): Flow&lt;List&lt;AppInfo&gt;&gt; {
        Timber.d("[AppRepositoryImpl] searchApps() called with query: '$query'")
        return appInfoDao.searchApps(query)
            .map { 
                Timber.d("[AppRepositoryImpl] Found ${it.size} apps matching query")
                AppInfoMapper.toDomainList(it) 
            }
    }
    
    override suspend fun insertApp(app: AppInfo): Long = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] insertApp() called for: ${app.packageName}")
        val entity = AppInfoMapper.toEntity(app)
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.insertApp()")
        val id = appInfoDao.insertApp(entity)
        Timber.d("[AppRepositoryImpl] insertApp completed, id: $id")
        id
    }
    
    override suspend fun insertApps(apps: List&lt;AppInfo&gt;): List&lt;Long&gt; = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] insertApps() called for ${apps.size} apps")
        val entities = AppInfoMapper.toEntityList(apps)
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.insertApps()")
        val ids = appInfoDao.insertApps(entities)
        Timber.d("[AppRepositoryImpl] insertApps completed, inserted ${ids.size} apps")
        ids
    }
    
    override suspend fun updateApp(app: AppInfo) = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] updateApp() called for: ${app.packageName}")
        val entity = AppInfoMapper.toEntity(app)
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.updateApp()")
        appInfoDao.updateApp(entity)
        Timber.d("[AppRepositoryImpl] updateApp completed")
    }
    
    override suspend fun deleteApp(app: AppInfo) = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] deleteApp() called for: ${app.packageName}")
        val entity = AppInfoMapper.toEntity(app)
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.deleteApp()")
        appInfoDao.deleteApp(entity)
        Timber.d("[AppRepositoryImpl] deleteApp completed")
    }
    
    override suspend fun deleteAppByPackageName(packageName: String) = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] deleteAppByPackageName() called for: $packageName")
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.deleteAppByPackageName()")
        appInfoDao.deleteAppByPackageName(packageName)
        Timber.d("[AppRepositoryImpl] deleteAppByPackageName completed")
    }
    
    override suspend fun deleteAllApps() = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] deleteAllApps() called")
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.deleteAllApps()")
        appInfoDao.deleteAllApps()
        Timber.d("[AppRepositoryImpl] deleteAllApps completed")
    }
    
    override suspend fun toggleFavorite(packageName: String) = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] toggleFavorite() called for: $packageName")
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.getAppByPackageNameSync()")
        val currentApp = appInfoDao.getAppByPackageNameSync(packageName)
        val isCurrentlyFavorite = currentApp?.isFavorite ?: false
        Timber.d("[AppRepositoryImpl] Current favorite status: $isCurrentlyFavorite")
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.updateFavorite()")
        appInfoDao.updateFavorite(packageName, !isCurrentlyFavorite)
        Timber.d("[AppRepositoryImpl] toggleFavorite completed, new status: ${!isCurrentlyFavorite}")
    }
    
    override suspend fun incrementCloneCount(packageName: String) = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] incrementCloneCount() called for: $packageName")
        val timestamp = System.currentTimeMillis()
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.incrementCloneCount() with timestamp: $timestamp")
        appInfoDao.incrementCloneCount(packageName, timestamp)
        Timber.d("[AppRepositoryImpl] incrementCloneCount completed")
    }
    
    override suspend fun updateLastAccessed(packageName: String) = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] updateLastAccessed() called for: $packageName")
        val timestamp = System.currentTimeMillis()
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.updateLastAccessed() with timestamp: $timestamp")
        appInfoDao.updateLastAccessed(packageName, timestamp)
        Timber.d("[AppRepositoryImpl] updateLastAccessed completed")
    }
    
    override suspend fun getAppCount(): Int = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] getAppCount() called")
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.getAppCount()")
        val count = appInfoDao.getAppCount()
        Timber.d("[AppRepositoryImpl] getAppCount returned: $count")
        count
    }
    
    override suspend fun getFavoriteAppCount(): Int = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] getFavoriteAppCount() called")
        Timber.d("[AppRepositoryImpl] Calling appInfoDao.getFavoriteAppCount()")
        val count = appInfoDao.getFavoriteAppCount()
        Timber.d("[AppRepositoryImpl] getFavoriteAppCount returned: $count")
        count
    }
    
    override suspend fun loadAndSyncInstalledApps(): Result&lt;Int&gt; = withContext(Dispatchers.IO) {
        Timber.d("[AppRepositoryImpl] loadAndSyncInstalledApps() called")
        try {
            Timber.d("[AppRepositoryImpl] Loading installed apps...")
            
            val apps = mutableListOf&lt;AppInfo&gt;()
            val packages = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.TIRAMISU) {
                Timber.d("[AppRepositoryImpl] Using TIRAMISU API to get installed packages")
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                Timber.d("[AppRepositoryImpl] Using legacy API to get installed packages")
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }
            Timber.d("[AppRepositoryImpl] Found ${packages.size} packages")
            
            for (packageInfo in packages) {
                try {
                    Timber.d("[AppRepositoryImpl] Processing package: ${packageInfo.packageName}")
                    val appInfo = getAppInfoFromPackage(packageInfo)
                    if (appInfo != null) {
                        apps.add(appInfo)
                        Timber.d("[AppRepositoryImpl] Added app: ${appInfo.appName}")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "[AppRepositoryImpl] Failed to load app: ${packageInfo.packageName}")
                    continue
                }
            }
            
            Timber.d("[AppRepositoryImpl] Processed ${apps.size} apps, calling AppInfoMapper.toEntityList()")
            val entities = AppInfoMapper.toEntityList(apps)
            Timber.d("[AppRepositoryImpl] Calling appInfoDao.insertApps()")
            appInfoDao.insertApps(entities)
            
            Timber.d("[AppRepositoryImpl] Successfully synced ${apps.size} apps")
            Result.success(apps.size)
            
        } catch (e: Exception) {
            Timber.e(e, "[AppRepositoryImpl] Failed to sync installed apps")
            Result.failure(e)
        }
    }
    
    private fun getAppInfoFromPackage(packageInfo: android.content.pm.PackageInfo): AppInfo? {
        Timber.d("[AppRepositoryImpl] getAppInfoFromPackage() called for: ${packageInfo.packageName}")
        return try {
            val applicationInfo = packageInfo.applicationInfo
            Timber.d("[AppRepositoryImpl] Getting app name")
            val appName = applicationInfo?.let {
                packageManager.getApplicationLabel(it).toString()
            } ?: packageInfo.packageName
            Timber.d("[AppRepositoryImpl] App name: $appName")
            
            Timber.d("[AppRepositoryImpl] Checking if system app")
            val isSystemApp = applicationInfo?.let {
                (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } ?: false
            Timber.d("[AppRepositoryImpl] Is system app: $isSystemApp")
            
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            Timber.d("[AppRepositoryImpl] Version: $versionName ($versionCode)")
            
            val appSize = try {
                applicationInfo?.sourceDir?.let { java.io.File(it).length() } ?: 0L
            } catch (e: Exception) {
                Timber.w(e, "[AppRepositoryImpl] Failed to get app size, defaulting to 0")
                0L
            }
            Timber.d("[AppRepositoryImpl] App size: $appSize bytes")
            
            val appInfo = AppInfo(
                appName = appName,
                packageName = packageInfo.packageName,
                versionName = versionName,
                versionCode = versionCode,
                isSystemApp = isSystemApp,
                firstInstallTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime,
                apkPath = applicationInfo?.sourceDir,
                appSize = appSize
            )
            Timber.d("[AppRepositoryImpl] Successfully created AppInfo for: ${packageInfo.packageName}")
            appInfo
        } catch (e: Exception) {
            Timber.w(e, "[AppRepositoryImpl] Failed to parse app info")
            null
        }
    }
}
