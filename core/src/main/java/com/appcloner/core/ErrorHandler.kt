
package com.appcloner.core

import android.content.Context
import com.appcloner.utils.LogUtils
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 错误处理中心 - 产品级错误处理
 * 统一错误报告、错误恢复、用户友好提示
 */
class ErrorHandler private constructor(
    private val context: Context,
    private val analyticsManager: AnalyticsManager
) {
    companion object {
        private const val TAG = "ErrorHandler"
        
        @Volatile
        private var instance: ErrorHandler? = null
        
        fun getInstance(
            context: Context,
            analyticsManager: AnalyticsManager
        ): ErrorHandler {
            return instance ?: synchronized(this) {
                instance ?: ErrorHandler(context.applicationContext, analyticsManager)
                    .also { instance = it }
            }
        }
    }

    data class ErrorInfo(
        val errorType: String,
        val errorMessage: String,
        val stackTrace: String,
        val timestamp: Long,
        val userContext: String? = null
    )

    sealed class ErrorCategory {
        object UserActionRequired : ErrorCategory()
        object Temporary : ErrorCategory()
        object Fatal : ErrorCategory()
        object Unknown : ErrorCategory()
    }

    private val errorHistory = mutableListOf&lt;ErrorInfo&gt;()
    private val MAX_HISTORY = 50

    fun handleError(
        error: Throwable,
        userContext: String? = null,
        onUserAction: (() -&gt; Unit)? = null
    ): ErrorCategory {
        val errorInfo = createErrorInfo(error, userContext)
        
        // 记录到历史
        addToHistory(errorInfo)
        
        // 追踪到分析系统
        analyticsManager.trackError(errorInfo.errorType)
        
        // 记录日志
        LogUtils.e("Error occurred", error, TAG)
        
        // 分类并返回处理策略
        return categorizeError(error)
    }

    fun getUserFriendlyMessage(category: ErrorCategory, context: String? = null): String {
        return when (category) {
            ErrorCategory.UserActionRequired -&gt; {
                "需要您的操作才能继续，请检查权限或存储空间。"
            }
            ErrorCategory.Temporary -&gt; {
                "临时问题，请稍后重试。"
            }
            ErrorCategory.Fatal -&gt; {
                "发生了严重问题，请重新启动应用。"
            }
            ErrorCategory.Unknown -&gt; {
                "发生了未知错误，请尝试重新操作。"
            }
        }
    }

    fun canRecoverAutomatically(category: ErrorCategory): Boolean {
        return category == ErrorCategory.Temporary
    }

    fun getErrorHistory(): List&lt;ErrorInfo&gt; {
        return errorHistory.toList()
    }

    fun clearErrorHistory() {
        errorHistory.clear()
        LogUtils.w("Error history cleared", TAG)
    }

    private fun createErrorInfo(error: Throwable, userContext: String?): ErrorInfo {
        val stackTraceWriter = StringWriter()
        error.printStackTrace(PrintWriter(stackTraceWriter))
        
        return ErrorInfo(
            errorType = error.javaClass.simpleName,
            errorMessage = error.message ?: "未知错误",
            stackTrace = stackTraceWriter.toString(),
            timestamp = System.currentTimeMillis(),
            userContext = userContext
        )
    }

    private fun categorizeError(error: Throwable): ErrorCategory {
        return when {
            error is SecurityException || 
            error is android.net.UriPermissionException -&gt; {
                ErrorCategory.UserActionRequired
            }
            error is java.io.IOException -&gt; {
                ErrorCategory.Temporary
            }
            error is OutOfMemoryError -&gt; {
                ErrorCategory.Fatal
            }
            else -&gt; {
                ErrorCategory.Unknown
            }
        }
    }

    private fun addToHistory(info: ErrorInfo) {
        errorHistory.add(info)
        if (errorHistory.size &gt; MAX_HISTORY) {
            errorHistory.removeAt(0)
        }
    }
}
