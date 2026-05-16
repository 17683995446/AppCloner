
package com.appcloner.data.apk

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.appcloner.utils.LogUtils
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ApkResourceViewer {

    private const val TAG = "ApkResourceViewer"

    data class ApkInfo(
        val packageName: String,
        val appName: String,
        val versionName: String,
        val versionCode: Long,
        val minSdk: Int,
        val targetSdk: Int,
        val size: Long,
        val installTime: Long,
        val updateTime: Long,
        val icon: Drawable?
    )

    data class ResourceInfo(
        val name: String,
        val path: String,
        val size: Long,
        val type: ResourceType,
        val isCompressed: Boolean
    )

    enum class ResourceType {
        MANIFEST,
        XML,
        DEX,
        ARSC,
        PNG,
        JPEG,
        GIF,
        SO,
        FONT,
        JSON,
        TEXT,
        UNKNOWN
    }

    data class ManifestInfo(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val minSdk: Int,
        val targetSdk: Int,
        val permissions: List&lt;String&gt;,
        val activities: List&lt;String&gt;,
        val services: List&lt;String&gt;,
        val receivers: List&lt;String&gt;,
        val providers: List&lt;String&gt;
    )

    fun loadApkFromPackage(context: Context, packageName: String): ApkInfo? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val appInfo = packageInfo.applicationInfo
            val apkFile = File(appInfo.sourceDir)
            
            ApkInfo(
                packageName = packageName,
                appName = context.packageManager.getApplicationLabel(appInfo).toString(),
                versionName = packageInfo.versionName ?: "unknown",
                versionCode = if (android.os.Build.VERSION.SDK_INT &gt;= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                },
                minSdk = if (android.os.Build.VERSION.SDK_INT &gt;= android.os.Build.VERSION_CODES.N) {
                    appInfo.minSdkVersion
                } else {
                    1
                },
                targetSdk = appInfo.targetSdkVersion,
                size = apkFile.length(),
                installTime = packageInfo.firstInstallTime,
                updateTime = packageInfo.lastUpdateTime,
                icon = context.packageManager.getApplicationIcon(appInfo)
            )
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to load APK info for $packageName", e)
            null
        }
    }

    fun listResources(context: Context, packageName: String): List&lt;ResourceInfo&gt; {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val apkFile = File(appInfo.sourceDir)
            
            listResourcesFromApk(apkFile)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to list resources for $packageName", e)
            emptyList()
        }
    }

    fun listResourcesFromApk(apkFile: File): List&lt;ResourceInfo&gt; {
        val resources = mutableListOf&lt;ResourceInfo&gt;()
        
        try {
            ZipFile(apkFile).use { zipFile -&gt;
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    resources.add(
                        ResourceInfo(
                            name = entry.name.substringAfterLast("/"),
                            path = entry.name,
                            size = entry.size,
                            type = determineResourceType(entry.name),
                            isCompressed = entry.method == ZipEntry.DEFLATED
                        )
                    )
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to list resources from APK", e)
        }
        
        return resources
    }

    fun getResourceStream(context: Context, packageName: String, resourcePath: String): InputStream? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val apkFile = File(appInfo.sourceDir)
            getResourceStreamFromApk(apkFile, resourcePath)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to get resource stream for $resourcePath", e)
            null
        }
    }

    fun getResourceStreamFromApk(apkFile: File, resourcePath: String): InputStream? {
        return try {
            ZipFile(apkFile).use { zipFile -&gt;
                val entry = zipFile.getEntry(resourcePath)
                if (entry != null) {
                    // We need to make a copy because the ZipFile will be closed
                    zipFile.getInputStream(entry).readBytes().inputStream()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to get resource stream for $resourcePath", e)
            null
        }
    }

    private fun determineResourceType(path: String): ResourceType {
        return when {
            path == "AndroidManifest.xml" -&gt; ResourceType.MANIFEST
            path.endsWith(".xml") -&gt; ResourceType.XML
            path.endsWith(".dex") -&gt; ResourceType.DEX
            path.endsWith(".arsc") -&gt; ResourceType.ARSC
            path.endsWith(".png") -&gt; ResourceType.PNG
            path.endsWith(".jpg") || path.endsWith(".jpeg") -&gt; ResourceType.JPEG
            path.endsWith(".gif") -&gt; ResourceType.GIF
            path.endsWith(".so") -&gt; ResourceType.SO
            path.endsWith(".ttf") || path.endsWith(".otf") -&gt; ResourceType.FONT
            path.endsWith(".json") -&gt; ResourceType.JSON
            path.endsWith(".txt") -&gt; ResourceType.TEXT
            else -&gt; ResourceType.UNKNOWN
        }
    }

    fun getResourcesByType(resources: List&lt;ResourceInfo&gt;, type: ResourceType): List&lt;ResourceInfo&gt; {
        return resources.filter { it.type == type }
    }

    fun getTotalSize(resources: List&lt;ResourceInfo&gt;): Long {
        return resources.sumOf { it.size }
    }

    fun getLargestResources(resources: List&lt;ResourceInfo&gt;, limit: Int = 10): List&lt;ResourceInfo&gt; {
        return resources.sortedByDescending { it.size }.take(limit)
    }

    fun getResourceTypeStats(resources: List&lt;ResourceInfo&gt;): Map&lt;ResourceType, Int&gt; {
        return resources.groupBy { it.type }.mapValues { it.value.size }
    }

    fun getResourceTypeSizeStats(resources: List&lt;ResourceInfo&gt;): Map&lt;ResourceType, Long&gt; {
        return resources.groupBy { it.type }.mapValues { it.value.sumOf { resource -&gt; resource.size } }
    }

    fun searchResources(resources: List&lt;ResourceInfo&gt;, query: String): List&lt;ResourceInfo&gt; {
        val lowerQuery = query.lowercase()
        return resources.filter { 
            it.name.lowercase().contains(lowerQuery) || 
            it.path.lowercase().contains(lowerQuery)
        }
    }

    fun exportResource(context: Context, packageName: String, resourcePath: String, targetFile: File): Boolean {
        return try {
            val input = getResourceStream(context, packageName, resourcePath)
            if (input != null) {
                input.use { inputStream -&gt;
                    targetFile.outputStream().use { outputStream -&gt;
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to export resource: $resourcePath", e)
            false
        }
    }

    fun extractAllResources(context: Context, packageName: String, targetDir: File): Int {
        var count = 0
        val resources = listResources(context, packageName)
        
        resources.forEach { resource -&gt;
            try {
                val targetFile = File(targetDir, resource.path)
                targetFile.parentFile?.mkdirs()
                
                if (exportResource(context, packageName, resource.path, targetFile)) {
                    count++
                }
            } catch (e: Exception) {
                LogUtils.w(TAG, "Failed to extract resource: ${resource.path}", e)
            }
        }
        
        return count
    }
}

