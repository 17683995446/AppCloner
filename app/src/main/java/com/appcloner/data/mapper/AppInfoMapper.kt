
package com.appcloner.data.mapper

import com.appcloner.core.model.AppInfo
import com.appcloner.data.local.entity.AppInfoEntity

object AppInfoMapper {
    
    fun toEntity(domain: AppInfo): AppInfoEntity {
        return AppInfoEntity(
            id = 0,
            appName = domain.appName,
            packageName = domain.packageName,
            versionName = domain.versionName,
            versionCode = domain.versionCode,
            isSystemApp = domain.isSystemApp,
            firstInstallTime = domain.firstInstallTime,
            lastUpdateTime = domain.lastUpdateTime,
            isFavorite = domain.isFavorite,
            isSavedApk = domain.isSavedApk,
            apkPath = domain.apkPath,
            appSize = domain.appSize,
            isCloned = domain.isCloned,
            cloneCount = domain.cloneCount,
            lastAccessedTime = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun toDomain(entity: AppInfoEntity): AppInfo {
        return AppInfo(
            appName = entity.appName,
            packageName = entity.packageName,
            versionName = entity.versionName,
            versionCode = entity.versionCode,
            isSystemApp = entity.isSystemApp,
            firstInstallTime = entity.firstInstallTime,
            lastUpdateTime = entity.lastUpdateTime,
            isFavorite = entity.isFavorite,
            isSavedApk = entity.isSavedApk,
            apkPath = entity.apkPath,
            appSize = entity.appSize,
            isCloned = entity.isCloned,
            cloneCount = entity.cloneCount
        )
    }
    
    fun toDomainList(entities: List&lt;AppInfoEntity&gt;): List&lt;AppInfo&gt; {
        return entities.map { toDomain(it) }
    }
    
    fun toEntityList(domains: List&lt;AppInfo&gt;): List&lt;AppInfoEntity&gt; {
        return domains.map { toEntity(it) }
    }
}
