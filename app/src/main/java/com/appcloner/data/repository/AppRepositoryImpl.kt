
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
    
    override fun getAllApps(): Flow&lt;List&lt;AppInfo&gt;&gt; {
        return appInfoDao.getAllApps()
            .map { AppInfoMapper.toDomainList(it) }
    }
    
    override fun getFavoriteApps(): Flow&lt;List&lt;AppInfo&gt;&gt; {
        return appInfoDao.getFavoriteApps()
            .map { AppInfoMapper.toDomainList(it) }
    }
    
    override fun getClonedApps(): Flow&lt;List&lt;AppInfo&gt;&gt; {
        return appInfoDao.getClonedApps()
            .map { AppInfoMapper.toDomainList(it) }
    }
    
    override fun getUserApps(): Flow&lt;List&lt;AppInfo&gt;&gt; {
        return appInfoDao.getUserApps()
            .map { AppInfoMapper.toDomainList(it) }
    }
    
    override fun getAppByPackageName(packageName: String): Flow&lt;AppInfo?&gt; {
        return appInfoDao.getAppByPackageName(packageName)
            .map { it?.let { AppInfoMapper.toDomain(it) } }
    }
    
    override fun searchApps(query: String): Flow&lt;List&lt;AppInfo&gt;&gt; {
        return appInfoDao.searchApps(query)
            .map { AppInfoMapper.toDomainList(it) }
    }
    
    override suspend fun insertApp(app: AppInfo): Long = withContext(Dispatchers.IO) {
        val entity = AppInfoMapper.toEntity(app)
        appInfoDao.insertApp(entity)
    }
    
    override suspend fun insertApps(apps: List&lt;AppInfo&gt;): List&lt;Long&gt; = withContext(Dispatchers.IO) {
        val entities = AppInfoMapper.toEntityList(apps)
        appInfoDao.insertApps(entities)
    }
    
    override suspend fun updateApp(app: AppInfo) = withContext(Dispatchers.IO) {
        val entity = AppInfoMapper.toEntity(app)
        appInfoDao.updateApp(entity)
    }
    
    override suspend fun deleteApp(app: AppInfo) = withContext(Dispatchers.IO) {
        val entity = AppInfoMapper.toEntity(app)
        appInfoDao.deleteApp(entity)
    }
    
    override suspend fun deleteAppByPackageName(packageName: String) = withContext(Dispatchers.IO) {
        appInfoDao.deleteAppByPackageName(packageName)
    }
    
    override suspend fun deleteAllApps() = withContext(Dispatchers.IO) {
        appInfoDao.deleteAllApps()
    }
    
    override suspend fun toggleFavorite(packageName: String) = withContext(Dispatchers.IO) {
        val currentApp = appInfoDao.getAppByPackageNameSync(packageName)
        val isCurrentlyFavorite = currentApp?.isFavorite ?: false
        appInfoDao.updateFavorite(packageName, !isCurrentlyFavorite)
    }
    
    override suspend fun incrementCloneCount(packageName: String) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        appInfoDao.incrementCloneCount(packageName, timestamp)
    }
    
    override suspend fun updateLastAccessed(packageName: String) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        appInfoDao.updateLastAccessed(packageName, timestamp)
    }
    
    override suspend fun getAppCount(): Int = withContext(Dispatchers.IO) {
        appInfoDao.getAppCount()
    }
    
    override suspend fun getFavoriteAppCount(): Int = withContext(Dispatchers.IO) {
        appInfoDao.getFavoriteAppCount()
    }
    
    override suspend fun loadAndSyncInstalledApps(): Result&lt;Int&gt; = withContext(Dispatchers.IO) {
        try {
            Timber.d("Loading installed apps...")
            
            val apps = mutableListOf&lt;AppInfo&gt;()
            val packages = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }
            
            for (packageInfo in packages) {
                try {
                    val appInfo = getAppInfoFromPackage(packageInfo)
                    if (appInfo != null) {
                        apps.add(appInfo)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load app: ${packageInfo.packageName}")
                    continue
                }
            }
            
            val entities = AppInfoMapper.toEntityList(apps)
            appInfoDao.insertApps(entities)
            
            Timber.d("Successfully synced ${apps.size} apps")
            Result.success(apps.size)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync installed apps")
            Result.failure(e)
        }
    }
    
    private fun getAppInfoFromPackage(packageInfo: android.content.pm.PackageInfo): AppInfo? {
        return try {
            val applicationInfo = packageInfo.applicationInfo
            val appName = applicationInfo?.let {
                packageManager.getApplicationLabel(it).toString()
            } ?: packageInfo.packageName
            
            val isSystemApp = applicationInfo?.let {
                (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } ?: false
            
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            val appSize = try {
                applicationInfo?.sourceDir?.let { java.io.File(it).length() } ?: 0L
            } catch (e: Exception) {
                0L
            }
            
            AppInfo(
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
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse app info")
            null
        }
    }
}
