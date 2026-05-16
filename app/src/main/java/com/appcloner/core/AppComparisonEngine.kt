
package com.appcloner.core

import android.content.Context
import android.text.format.Formatter
import com.appcloner.data.model.AppAnalysis
import com.appcloner.utils.LogUtils
import java.io.File

class AppComparisonEngine(private val context: Context) {

    private val appAnalyzer = AppAnalyzer

    data class ComparisonResult(
        val app1Info: AppInfo,
        val app2Info: AppInfo,
        val app1Analysis: AppAnalysis?,
        val app2Analysis: AppAnalysis?,
        val versionComparison: VersionComparison,
        val sizeComparison: SizeComparison,
        val permissionComparison: PermissionComparison,
        val componentComparison: ComponentComparison,
        val sdkComparison: SdkComparison,
        val overallScore: Float
    )

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val versionName: String,
        val versionCode: Long,
        val apkSize: Long,
        val icon: android.graphics.drawable.Drawable?
    )

    data class VersionComparison(
        val isSameVersion: Boolean,
        val app1Version: String,
        val app2Version: String,
        val app1VersionCode: Long,
        val app2VersionCode: Long,
        val newerApp: Int // 1 or 2, 0 if same
    )

    data class SizeComparison(
        val app1Size: Long,
        val app2Size: Long,
        val sizeDifference: Long,
        val sizeDifferencePercent: Float,
        val largerApp: Int, // 1 or 2
        val formattedApp1Size: String,
        val formattedApp2Size: String,
        val formattedDifference: String
    )

    data class PermissionComparison(
        val commonPermissions: List<String>,
        val app1OnlyPermissions: List<String>,
        val app2OnlyPermissions: List<String>,
        val totalCommon: Int,
        val totalApp1Only: Int,
        val totalApp2Only: Int,
        val similarity: Float // 0-1
    )

    data class ComponentComparison(
        val activityComparison: ComponentCountComparison,
        val serviceComparison: ComponentCountComparison,
        val receiverComparison: ComponentCountComparison,
        val providerComparison: ComponentCountComparison
    )

    data class ComponentCountComparison(
        val app1Count: Int,
        val app2Count: Int,
        val common: Int,
        val difference: Int,
        val isIdentical: Boolean
    )

    data class SdkComparison(
        val app1TargetSdk: Int,
        val app2TargetSdk: Int,
        val app1MinSdk: Int,
        val app2MinSdk: Int,
        val sameTargetSdk: Boolean,
        val sameMinSdk: Boolean,
        val higherTargetSdk: Int,
        val higherMinSdk: Int
    )

    fun compare(packageName1: String, packageName2: String): ComparisonResult? {
        try {
            val analysis1 = appAnalyzer.analyzeApp(context, packageName1)
            val analysis2 = appAnalyzer.analyzeApp(context, packageName2)

            if (analysis1 == null || analysis2 == null) {
                LogUtils.w("AppComparison", "Failed to analyze one or both apps")
                return null
            }

            val appInfo1 = AppInfo(
                packageName = packageName1,
                appName = analysis1.appName,
                versionName = analysis1.versionName,
                versionCode = analysis1.versionCode,
                apkSize = analysis1.apkSize,
                icon = try {
                    context.packageManager.getApplicationIcon(packageName1)
                } catch (e: Exception) { null }
            )

            val appInfo2 = AppInfo(
                packageName = packageName2,
                appName = analysis2.appName,
                versionName = analysis2.versionName,
                versionCode = analysis2.versionCode,
                apkSize = analysis2.apkSize,
                icon = try {
                    context.packageManager.getApplicationIcon(packageName2)
                } catch (e: Exception) { null }
            )

            val versionComparison = compareVersions(appInfo1, appInfo2)
            val sizeComparison = compareSizes(appInfo1, appInfo2)
            val permissionComparison = comparePermissions(analysis1, analysis2)
            val componentComparison = compareComponents(analysis1, analysis2)
            val sdkComparison = compareSdk(analysis1, analysis2)

            val overallScore = calculateOverallScore(
                versionComparison,
                sizeComparison,
                permissionComparison,
                componentComparison,
                sdkComparison
            )

            return ComparisonResult(
                app1Info = appInfo1,
                app2Info = appInfo2,
                app1Analysis = analysis1,
                app2Analysis = analysis2,
                versionComparison = versionComparison,
                sizeComparison = sizeComparison,
                permissionComparison = permissionComparison,
                componentComparison = componentComparison,
                sdkComparison = sdkComparison,
                overallScore = overallScore
            )
        } catch (e: Exception) {
            LogUtils.e("AppComparison", "Comparison failed", e)
            return null
        }
    }

    private fun compareVersions(info1: AppInfo, info2: AppInfo): VersionComparison {
        val isSameVersion = info1.versionName == info2.versionName && 
                           info1.versionCode == info2.versionCode
        val newerApp = when {
            info1.versionCode > info2.versionCode -> 1
            info2.versionCode > info1.versionCode -> 2
            else -> 0
        }

        return VersionComparison(
            isSameVersion = isSameVersion,
            app1Version = info1.versionName,
            app2Version = info2.versionName,
            app1VersionCode = info1.versionCode,
            app2VersionCode = info2.versionCode,
            newerApp = newerApp
        )
    }

    private fun compareSizes(info1: AppInfo, info2: AppInfo): SizeComparison {
        val diff = info1.apkSize - info2.apkSize
        val largerApp = when {
            info1.apkSize > info2.apkSize -> 1
            info2.apkSize > info1.apkSize -> 2
            else -> 0
        }

        val percentDiff = if (info2.apkSize > 0) {
            (diff.toFloat() / info2.apkSize) * 100
        } else 0f

        return SizeComparison(
            app1Size = info1.apkSize,
            app2Size = info2.apkSize,
            sizeDifference = diff,
            sizeDifferencePercent = percentDiff,
            largerApp = largerApp,
            formattedApp1Size = formatSize(info1.apkSize),
            formattedApp2Size = formatSize(info2.apkSize),
            formattedDifference = formatSize(kotlin.math.abs(diff))
        )
    }

    private fun comparePermissions(analysis1: AppAnalysis, analysis2: AppAnalysis): PermissionComparison {
        val perms1 = analysis1.permissions.toSet()
        val perms2 = analysis2.permissions.toSet()

        val common = perms1.intersect(perms2).toList()
        val only1 = perms1.subtract(perms2).toList()
        val only2 = perms2.subtract(perms1).toList()

        val totalUnique = perms1.union(perms2).size
        val similarity = if (totalUnique > 0) {
            (common.size.toFloat() / totalUnique)
        } else 0f

        return PermissionComparison(
            commonPermissions = common.sorted(),
            app1OnlyPermissions = only1.sorted(),
            app2OnlyPermissions = only2.sorted(),
            totalCommon = common.size,
            totalApp1Only = only1.size,
            totalApp2Only = only2.size,
            similarity = similarity
        )
    }

    private fun compareComponents(analysis1: AppAnalysis, analysis2: AppAnalysis): ComponentComparison {
        return ComponentComparison(
            activityComparison = compareComponentCounts(
                analysis1.activities,
                analysis2.activities
            ),
            serviceComparison = compareComponentCounts(
                analysis1.services,
                analysis2.services
            ),
            receiverComparison = compareComponentCounts(
                analysis1.receivers,
                analysis2.receivers
            ),
            providerComparison = compareComponentCounts(
                analysis1.providers,
                analysis2.providers
            )
        )
    }

    private fun compareComponentCounts(list1: List<String>, list2: List<String>): ComponentCountComparison {
        val set1 = list1.toSet()
        val set2 = list2.toSet()
        val common = set1.intersect(set2)
        val diff = kotlin.math.abs(list1.size - list2.size)

        return ComponentCountComparison(
            app1Count = list1.size,
            app2Count = list2.size,
            common = common.size,
            difference = diff,
            isIdentical = list1.size == list2.size && common.size == list1.size
        )
    }

    private fun compareSdk(analysis1: AppAnalysis, analysis2: AppAnalysis): SdkComparison {
        return SdkComparison(
            app1TargetSdk = analysis1.targetSdkVersion,
            app2TargetSdk = analysis2.targetSdkVersion,
            app1MinSdk = analysis1.minSdkVersion,
            app2MinSdk = analysis2.minSdkVersion,
            sameTargetSdk = analysis1.targetSdkVersion == analysis2.targetSdkVersion,
            sameMinSdk = analysis1.minSdkVersion == analysis2.minSdkVersion,
            higherTargetSdk = if (analysis1.targetSdkVersion > analysis2.targetSdkVersion) 1 else 2,
            higherMinSdk = if (analysis1.minSdkVersion > analysis2.minSdkVersion) 1 else 2
        )
    }

    private fun calculateOverallScore(
        version: VersionComparison,
        size: SizeComparison,
        permissions: PermissionComparison,
        components: ComponentComparison,
        sdk: SdkComparison
    ): Float {
        var score = 0f
        var count = 0

        // Version similarity (40%)
        if (version.isSameVersion) {
            score += 1.0f
        } else {
            score += 0.5f
        }
        count++

        // Permission similarity (30%)
        score += permissions.similarity
        count++

        // Component similarity (15%)
        val componentSimilarity = calculateComponentSimilarity(components)
        score += componentSimilarity
        count++

        // SDK match (15%)
        if (sdk.sameTargetSdk && sdk.sameMinSdk) {
            score += 1.0f
        } else if (sdk.sameTargetSdk || sdk.sameMinSdk) {
            score += 0.5f
        }
        count++

        return score / count
    }

    private fun calculateComponentSimilarity(components: ComponentComparison): Float {
        val activitySim = if (components.activityComparison.isIdentical) 1f else 0f
        val serviceSim = if (components.serviceComparison.isIdentical) 1f else 0f
        val receiverSim = if (components.receiverComparison.isIdentical) 1f else 0f
        val providerSim = if (components.providerComparison.isIdentical) 1f else 0f
        
        return (activitySim + serviceSim + receiverSim + providerSim) / 4f
    }

    private fun formatSize(bytes: Long): String {
        return Formatter.formatFileSize(context, bytes)
    }

    fun generateComparisonReport(result: ComparisonResult): String {
        return buildString {
            appendLine("=" .repeat(60))
            appendLine("App Comparison Report")
            appendLine("=" .repeat(60))
            appendLine()
            
            appendLine("APPS:")
            appendLine("  App 1: ${result.app1Info.appName}")
            appendLine("  App 2: ${result.app2Info.appName}")
            appendLine()
            
            appendLine("VERSION COMPARISON:")
            appendLine("  ${result.app1Info.appName}: ${result.versionComparison.app1Version} (${result.versionComparison.app1VersionCode})")
            appendLine("  ${result.app2Info.appName}: ${result.versionComparison.app2Version} (${result.versionComparison.app2VersionCode})")
            appendLine("  Same version: ${result.versionComparison.isSameVersion}")
            appendLine()
            
            appendLine("SIZE COMPARISON:")
            appendLine("  ${result.app1Info.appName}: ${result.sizeComparison.formattedApp1Size}")
            appendLine("  ${result.app2Info.appName}: ${result.sizeComparison.formattedApp2Size}")
            appendLine("  Difference: ${result.sizeComparison.formattedDifference} (${String.format("%.1f", result.sizeComparison.sizeDifferencePercent)}%)")
            appendLine()
            
            appendLine("PERMISSION COMPARISON:")
            appendLine("  Common permissions: ${result.permissionComparison.totalCommon}")
            appendLine("  ${result.app1Info.appName} only: ${result.permissionComparison.totalApp1Only}")
            appendLine("  ${result.app2Info.appName} only: ${result.permissionComparison.totalApp2Only}")
            appendLine("  Similarity: ${String.format("%.1f", result.permissionComparison.similarity * 100)}%")
            appendLine()
            
            appendLine("SDK COMPARISON:")
            appendLine("  ${result.app1Info.appName}: Target ${result.sdkComparison.app1TargetSdk}, Min ${result.sdkComparison.app1MinSdk}")
            appendLine("  ${result.app2Info.appName}: Target ${result.sdkComparison.app2TargetSdk}, Min ${result.sdkComparison.app2MinSdk}")
            appendLine()
            
            appendLine("OVERALL SIMILARITY SCORE: ${String.format("%.1f", result.overallScore * 100)}%")
            appendLine("=" .repeat(60))
        }
    }
}
