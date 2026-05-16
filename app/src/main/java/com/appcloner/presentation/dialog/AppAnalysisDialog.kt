
package com.appcloner.presentation.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.appcloner.R
import com.appcloner.core.AppAnalyzer
import com.appcloner.data.model.AppAnalysis
import com.appcloner.data.model.AppFeature
import java.text.SimpleDateFormat
import java.util.*

class AppAnalysisDialog(
    private val ctx: Context,
    private val packageName: String
) : Dialog(ctx) {

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_app_analysis)
        
        val analysis = AppAnalyzer.analyzeApp(ctx, packageName)
        if (analysis != null) {
            populateAnalysis(analysis)
        } else {
            showError()
        }
        
        findViewById&lt;View&gt;(R.id.closeButton).setOnClickListener {
            dismiss()
        }
    }

    private fun populateAnalysis(analysis: AppAnalysis) {
        findViewById&lt;TextView&gt;(R.id.appName).text = analysis.appName
        findViewById&lt;TextView&gt;(R.id.packageName).text = analysis.packageName
        
        val versionInfo = "${analysis.versionName} (${analysis.versionCode})"
        findViewById&lt;TextView&gt;(R.id.versionInfo).text = versionInfo
        
        val sdkInfo = "SDK: ${analysis.minSdkVersion} → ${analysis.targetSdkVersion}"
        findViewById&lt;TextView&gt;(R.id.sdkInfo).text = sdkInfo
        
        val sizeInfo = "Size: ${analysis.getFormattedSize(ctx)}"
        findViewById&lt;TextView&gt;(R.id.sizeInfo).text = sizeInfo
        
        val timeInfo = "Installed: ${sdf.format(Date(analysis.installTime))}\n" +
                       "Updated: ${sdf.format(Date(analysis.updateTime))}"
        findViewById&lt;TextView&gt;(R.id.timeInfo).text = timeInfo
        
        val flagsInfo = "System: ${if (analysis.isSystemApp) "Yes" else "No"} | " +
                        "Debug: ${if (analysis.isDebuggable) "Yes" else "No"}"
        findViewById&lt;TextView&gt;(R.id.flagsInfo).text = flagsInfo
        
        populatePermissions(analysis)
        populateComponents(analysis)
        populateFeatures(analysis)
    }

    private fun populatePermissions(analysis: AppAnalysis) {
        val permStats = analysis.getPermissionStats()
        val permText = "Permissions: ${permStats.totalPermissions} total, " +
                       "${permStats.dangerousPermissions} dangerous"
        findViewById&lt;TextView&gt;(R.id.permSummary).text = permText
        
        val permListLayout = findViewById&lt;LinearLayout&gt;(R.id.permListLayout)
        permListLayout.removeAllViews()
        
        analysis.permissions.take(15).forEach { permission -&gt;
            val textView = TextView(ctx).apply {
                text = "• ${permission.split(".").lastOrNull() ?: permission}"
                textSize = 12f
                setPadding(16, 8, 16, 8)
                if (analysis.dangerousPermissions.contains(permission)) {
                    setTextColor(ctx.resources.getColor(android.R.color.holo_red_dark))
                }
            }
            permListLayout.addView(textView)
        }
        
        if (analysis.permissions.size &gt; 15) {
            val moreView = TextView(ctx).apply {
                text = "• ... (${analysis.permissions.size - 15} more)"
                textSize = 12f
                setPadding(16, 8, 16, 8)
                setTextColor(ctx.resources.getColor(android.R.color.darker_gray))
            }
            permListLayout.addView(moreView)
        }
    }

    private fun populateComponents(analysis: AppAnalysis) {
        val compText = "Activities: ${analysis.activities.size} | " +
                       "Services: ${analysis.services.size} | " +
                       "Receivers: ${analysis.receivers.size} | " +
                       "Providers: ${analysis.providers.size}"
        findViewById&lt;TextView&gt;(R.id.compSummary).text = compText
    }

    private fun populateFeatures(analysis: AppAnalysis) {
        val featuresList = mutableListOf&lt;String&gt;()
        
        if (analysis.hasFeature(AppFeature.ACTIVITIES)) {
            featuresList.add("📱 Activities")
        }
        if (analysis.hasFeature(AppFeature.SERVICES)) {
            featuresList.add("🔧 Services")
        }
        if (analysis.hasFeature(AppFeature.RECEIVERS)) {
            featuresList.add("📡 Receivers")
        }
        if (analysis.hasFeature(AppFeature.PROVIDERS)) {
            featuresList.add("📦 Providers")
        }
        if (analysis.hasFeature(AppFeature.NATIVE_LIBS)) {
            featuresList.add("🔬 Native Libs (${analysis.nativeLibraries.size})")
        }
        
        val featuresLayout = findViewById&lt;LinearLayout&gt;(R.id.featuresLayout)
        featuresLayout.removeAllViews()
        
        featuresList.forEach { feature -&gt;
            val textView = TextView(ctx).apply {
                text = "• $feature"
                textSize = 12f
                setPadding(16, 8, 16, 8)
            }
            featuresLayout.addView(textView)
        }
    }

    private fun showError() {
        findViewById&lt;View&gt;(R.id.analysisContent).visibility = View.GONE
        findViewById&lt;View&gt;(R.id.errorLayout).visibility = View.VISIBLE
    }
}
