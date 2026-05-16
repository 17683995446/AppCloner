
package com.appcloner.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.appcloner.R

class AppClonerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (ACTION_QUICK_CLONE == intent.action) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            if (packageName != null) {
                launchAppCloner(context, packageName)
            }
        }
    }

    companion object {
        const val ACTION_QUICK_CLONE = "com.appcloner.action.QUICK_CLONE"
        const val EXTRA_PACKAGE_NAME = "com.appcloner.extra.PACKAGE_NAME"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_app_cloner)
            
            views.setOnClickPendingIntent(
                R.id.widget_open_btn,
                getLaunchPendingIntent(context)
            )
            
            views.setOnClickPendingIntent(
                R.id.widget_clone_btn,
                getQuickClonePendingIntent(context)
            )
            
            views.setTextViewText(
                R.id.widget_title,
                "AppCloner 8.0"
            )
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getLaunchPendingIntent(context: Context): android.app.PendingIntent {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            return android.app.PendingIntent.getActivity(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun getQuickClonePendingIntent(context: Context): android.app.PendingIntent {
            val intent = Intent(context, AppClonerWidget::class.java).apply {
                action = ACTION_QUICK_CLONE
            }
            return android.app.PendingIntent.getBroadcast(
                context,
                1,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun launchAppCloner(context: Context, packageName: String? = null) {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            packageName?.let {
                intent?.putExtra("quick_clone_package", it)
            }
            intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}
