
package com.appcloner.security

import android.content.Context
import android.content.pm.PackageManager
import com.appcloner.utils.LogUtils

object PermissionAuditTool {

    private const val TAG = "PermissionAuditTool"

    data class PermissionAudit(
        val packageName: String,
        val appName: String,
        val permissions: List&lt;PermissionInfo&gt;,
        val overallRisk: RiskLevel,
        val recommendations: List&lt;String&gt;
    )

    data class PermissionInfo(
        val name: String,
        val group: String?,
        val level: RiskLevel,
        val description: String?,
        val isGranted: Boolean
    )

    enum class RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private val dangerousPermissions = mapOf(
        "android.permission.READ_CALENDAR" to "Calendar",
        "android.permission.WRITE_CALENDAR" to "Calendar",
        "android.permission.CAMERA" to "Camera",
        "android.permission.READ_CONTACTS" to "Contacts",
        "android.permission.WRITE_CONTACTS" to "Contacts",
        "android.permission.GET_ACCOUNTS" to "Contacts",
        "android.permission.ACCESS_FINE_LOCATION" to "Location",
        "android.permission.ACCESS_COARSE_LOCATION" to "Location",
        "android.permission.RECORD_AUDIO" to "Microphone",
        "android.permission.READ_PHONE_STATE" to "Phone",
        "android.permission.CALL_PHONE" to "Phone",
        "android.permission.READ_CALL_LOG" to "Phone",
        "android.permission.WRITE_CALL_LOG" to "Phone",
        "android.permission.ADD_VOICEMAIL" to "Phone",
        "android.permission.USE_SIP" to "Phone",
        "android.permission.PROCESS_OUTGOING_CALLS" to "Phone",
        "android.permission.BODY_SENSORS" to "Sensors",
        "android.permission.SEND_SMS" to "SMS",
        "android.permission.RECEIVE_SMS" to "SMS",
        "android.permission.READ_SMS" to "SMS",
        "android.permission.RECEIVE_WAP_PUSH" to "SMS",
        "android.permission.RECEIVE_MMS" to "SMS",
        "android.permission.READ_EXTERNAL_STORAGE" to "Storage",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "Storage",
        "android.permission.ACCESS_MEDIA_LOCATION" to "Location",
        "android.permission.ACTIVITY_RECOGNITION" to "Activity"
    )

    private val permissionDescriptions = mapOf(
        "android.permission.READ_CALENDAR" to "Read calendar events",
        "android.permission.WRITE_CALENDAR" to "Modify calendar events",
        "android.permission.CAMERA" to "Access camera for taking pictures and videos",
        "android.permission.READ_CONTACTS" to "Read your contacts",
        "android.permission.WRITE_CONTACTS" to "Modify your contacts",
        "android.permission.GET_ACCOUNTS" to "Get a list of accounts on the device",
        "android.permission.ACCESS_FINE_LOCATION" to "Access precise location",
        "android.permission.ACCESS_COARSE_LOCATION" to "Access approximate location",
        "android.permission.RECORD_AUDIO" to "Record audio",
        "android.permission.READ_PHONE_STATE" to "Read phone status and identity",
        "android.permission.CALL_PHONE" to "Make phone calls",
        "android.permission.READ_CALL_LOG" to "Read call log",
        "android.permission.WRITE_CALL_LOG" to "Write call log",
        "android.permission.ADD_VOICEMAIL" to "Add voicemail",
        "android.permission.USE_SIP" to "Use SIP",
        "android.permission.PROCESS_OUTGOING_CALLS" to "Process outgoing calls",
        "android.permission.BODY_SENSORS" to "Access body sensors",
        "android.permission.SEND_SMS" to "Send SMS messages",
        "android.permission.RECEIVE_SMS" to "Receive SMS messages",
        "android.permission.READ_SMS" to "Read SMS messages",
        "android.permission.RECEIVE_WAP_PUSH" to "Receive WAP push messages",
        "android.permission.RECEIVE_MMS" to "Receive MMS messages",
        "android.permission.READ_EXTERNAL_STORAGE" to "Read external storage",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "Write external storage",
        "android.permission.ACCESS_MEDIA_LOCATION" to "Access media location",
        "android.permission.ACTIVITY_RECOGNITION" to "Recognize physical activity"
    )

    fun auditApp(context: Context, packageName: String): PermissionAudit? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
            )
            
            val appName = try {
                context.packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
            } catch (e: Exception) {
                packageName
            }
            
            val permissions = packageInfo.requestedPermissions?.map { permissionName -&gt;
                val isGranted = try {
                    context.packageManager.checkPermission(
                        permissionName,
                        packageName
                    ) == PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    false
                }
                
                val group = dangerousPermissions[permissionName]
                val level = determineRiskLevel(permissionName)
                val description = permissionDescriptions[permissionName]
                
                PermissionInfo(
                    name = permissionName,
                    group = group,
                    level = level,
                    description = description,
                    isGranted = isGranted
                )
            } ?: emptyList()
            
            val overallRisk = calculateOverallRisk(permissions)
            val recommendations = generateRecommendations(permissions)
            
            PermissionAudit(
                packageName = packageName,
                appName = appName,
                permissions = permissions,
                overallRisk = overallRisk,
                recommendations = recommendations
            )
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to audit app: $packageName", e)
            null
        }
    }

    private fun determineRiskLevel(permission: String): RiskLevel {
        return when {
            permission.contains("LOCATION") || 
            permission.contains("CAMERA") || 
            permission.contains("SMS") || 
            permission.contains("CALL") ||
            permission.contains("READ_SMS") ||
            permission.contains("READ_PHONE") -&gt; RiskLevel.HIGH
            
            permission.contains("MICROPHONE") || 
            permission.contains("CONTACTS") ||
            permission.contains("CALENDAR") ||
            permission.contains("SENSORS") -&gt; RiskLevel.MEDIUM
            
            permission.contains("STORAGE") || 
            permission.contains("ACTIVITY") -&gt; RiskLevel.LOW
            
            else -&gt; RiskLevel.LOW
        }
    }

    private fun calculateOverallRisk(permissions: List&lt;PermissionInfo&gt;): RiskLevel {
        val grantedPermissions = permissions.filter { it.isGranted }
        
        return when {
            grantedPermissions.any { it.level == RiskLevel.HIGH } || 
            grantedPermissions.count { it.level == RiskLevel.MEDIUM } &gt;= 3 -&gt; RiskLevel.HIGH
            
            grantedPermissions.any { it.level == RiskLevel.MEDIUM } -&gt; RiskLevel.MEDIUM
            
            else -&gt; RiskLevel.LOW
        }
    }

    private fun generateRecommendations(permissions: List&lt;PermissionInfo&gt;): List&lt;String&gt; {
        val recommendations = mutableListOf&lt;String&gt;()
        val grantedPermissions = permissions.filter { it.isGranted }
        
        // Location permissions warning
        val hasLocationPermission = grantedPermissions.any { 
            it.name.contains("LOCATION") || 
            it.group == "Location"
        }
        if (hasLocationPermission) {
            recommendations.add("This app can access your location. Consider revoking if not needed.")
        }
        
        // Camera and microphone warning
        val hasCameraPermission = grantedPermissions.any { 
            it.name.contains("CAMERA") || 
            it.group == "Camera"
        }
        if (hasCameraPermission) {
            recommendations.add("This app has camera access. Review periodically.")
        }
        
        val hasMicrophonePermission = grantedPermissions.any { 
            it.name.contains("RECORD_AUDIO") || 
            it.group == "Microphone"
        }
        if (hasMicrophonePermission) {
            recommendations.add("This app can record audio. Be cautious when granting.")
        }
        
        // SMS and phone permissions
        val hasSmsPermission = grantedPermissions.any { 
            it.name.contains("SMS") || 
            it.group == "SMS"
        }
        if (hasSmsPermission) {
            recommendations.add("This app can access SMS. High privacy risk.")
        }
        
        val hasCallPermission = grantedPermissions.any { 
            it.name.contains("CALL") || 
            it.group == "Phone"
        }
        if (hasCallPermission) {
            recommendations.add("This app can make phone calls. Review carefully.")
        }
        
        // Contacts and calendar
        val hasContactsPermission = grantedPermissions.any { 
            it.name.contains("CONTACTS") || 
            it.group == "Contacts"
        }
        if (hasContactsPermission) {
            recommendations.add("This app can read your contacts. Privacy-sensitive.")
        }
        
        val hasCalendarPermission = grantedPermissions.any { 
            it.name.contains("CALENDAR") || 
            it.group == "Calendar"
        }
        if (hasCalendarPermission) {
            recommendations.add("This app can access your calendar.")
        }
        
        // High risk permissions
        val highRiskCount = grantedPermissions.count { it.level == RiskLevel.HIGH }
        if (highRiskCount &gt;= 3) {
            recommendations.add("This app requires many high-risk permissions. Consider alternatives.")
        }
        
        // No recommendations if app is clean
        if (recommendations.isEmpty()) {
            recommendations.add("This app has minimal permissions. Good privacy practices!")
        }
        
        return recommendations
    }

    fun getPermissionGroup(permission: String): String? {
        return dangerousPermissions[permission]
    }

    fun getPermissionDescription(permission: String): String? {
        return permissionDescriptions[permission]
    }

    fun isDangerousPermission(permission: String): Boolean {
        return dangerousPermissions.containsKey(permission)
    }

    fun getDangerousPermissions(permissions: List&lt;PermissionInfo&gt;): List&lt;PermissionInfo&gt; {
        return permissions.filter { dangerousPermissions.containsKey(it.name) }
    }

    fun getHighRiskPermissions(permissions: List&lt;PermissionInfo&gt;): List&lt;PermissionInfo&gt; {
        return permissions.filter { it.level == RiskLevel.HIGH }
    }

    fun getGrantedPermissions(permissions: List&lt;PermissionInfo&gt;): List&lt;PermissionInfo&gt; {
        return permissions.filter { it.isGranted }
    }

    fun getRiskScore(permissions: List&lt;PermissionInfo&gt;): Int {
        var score = 0
        val granted = permissions.filter { it.isGranted }
        
        granted.forEach {
            score += when (it.level) {
                RiskLevel.HIGH -&gt; 10
                RiskLevel.MEDIUM -&gt; 5
                RiskLevel.LOW -&gt; 2
                RiskLevel.CRITICAL -&gt; 20
            }
        }
        
        return score.coerceIn(0, 100)
    }

    fun getPrivacyRating(permissions: List&lt;PermissionInfo&gt;): String {
        val score = getRiskScore(permissions)
        return when {
            score == 0 -&gt; "Excellent"
            score &lt; 10 -&gt; "Good"
            score &lt; 25 -&gt; "Fair"
            score &lt; 50 -&gt; "Poor"
            else -&gt; "Bad"
        }
    }

    fun generateAuditReport(audit: PermissionAudit): String {
        return buildString {
            appendLine("=")
            appendLine("App Privacy Audit Report")
            appendLine("=")
            appendLine()
            appendLine("App: ${audit.appName}")
            appendLine("Package: ${audit.packageName}")
            appendLine("Risk Level: ${audit.overallRisk}")
            appendLine("Privacy Rating: ${getPrivacyRating(audit.permissions)}")
            appendLine("Risk Score: ${getRiskScore(audit.permissions)}")
            appendLine()
            appendLine("Permissions:")
            appendLine()
            
            val granted = getGrantedPermissions(audit.permissions)
            val dangerous = getDangerousPermissions(audit.permissions)
            
            appendLine("Total: ${audit.permissions.size}")
            appendLine("Granted: ${granted.size}")
            appendLine("Dangerous: ${dangerous.size}")
            appendLine()
            
            if (audit.recommendations.isNotEmpty()) {
                appendLine("Recommendations:")
                audit.recommendations.forEach { recommendation -&gt;
                    appendLine("  - $recommendation")
                }
                appendLine()
            }
            
            appendLine("Permission Details:")
            audit.permissions.sortedByDescending { it.level }.forEach { info -&gt;
                val status = if (info.isGranted) "GRANTED" else "NOT GRANTED"
                val level = info.level
                val group = info.group ?: "None"
                appendLine("  $status [$level] ${info.name} ($group)")
                info.description?.let {
                    appendLine("      $it")
                }
            }
            appendLine()
            appendLine("=")
        }
    }
}

