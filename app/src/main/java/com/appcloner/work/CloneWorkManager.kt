
package com.appcloner.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.appcloner.core.model.AppInfo
import com.appcloner.core.model.CloneConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloneWorkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val workManager = WorkManager.getInstance(context)
    
    fun startClone(
        app: AppInfo,
        clonedPackageName: String? = null,
        clonedAppName: String? = null,
        delaySeconds: Long = 0L
    ): String {
        val actualClonedPackageName = clonedPackageName 
            ?: CloneConfig.generateClonePackageName(app.packageName)
        val actualClonedAppName = clonedAppName 
            ?: CloneConfig.generateCloneAppName(app.appName)
        
        Timber.i("Queueing clone job: ${app.appName}")
        Timber.i("Original: ${app.packageName} -&gt; Cloned: $actualClonedPackageName")
        
        val inputData = Data.Builder()
            .putString(CloneWorker.KEY_ORIGINAL_PACKAGE_NAME, app.packageName)
            .putString(CloneWorker.KEY_CLONED_PACKAGE_NAME, actualClonedPackageName)
            .putString(CloneWorker.KEY_CLONED_APP_NAME, actualClonedAppName)
            .putLong(CloneWorker.KEY_APP_SIZE, app.appSize)
            .build()
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder&lt;CloneWorker&gt;()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        
        workManager.enqueueUniqueWork(
            "clone_${app.packageName}_${System.currentTimeMillis()}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        
        Timber.i("Clone job queued with ID: ${workRequest.id}")
        return workRequest.id.toString()
    }
    
    fun cancelClone(workId: String): Boolean {
        Timber.i("Cancelling clone job: $workId")
        workManager.cancelWorkById(androidx.work.WorkerId.fromString(workId))
        return true
    }
    
    fun cancelAllClones() {
        Timber.i("Cancelling all clone jobs")
        workManager.cancelAllWork()
    }
    
    fun getWorkInfo(workId: String): Flow&lt;WorkInfo?&gt; {
        return workManager.getWorkInfoByIdFlow(androidx.work.WorkerId.fromString(workId))
    }
    
    fun getAllWorkInfo(): Flow&lt;List&lt;WorkInfo&gt;&gt; {
        return workManager.getWorkInfosByTagFlow("clone_task")
    }
    
    fun getActiveWorkCount(): Flow&lt;Int&gt; {
        return workManager.getWorkInfosByTagFlow("clone_task")
            .map { list -&gt; list.count { it.state == WorkInfo.State.RUNNING } }
    }
    
    companion object {
        const val TAG = "CloneWorkManager"
    }
}
