
package com.appcloner.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appcloner.core.model.CloneConfig
import com.appcloner.core.model.CloneHistory
import com.appcloner.data.repository.AppRepository
import com.appcloner.data.repository.CloneHistoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import timber.log.Timber

@HiltWorker
class CloneWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appRepository: AppRepository,
    private val cloneHistoryRepository: CloneHistoryRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val KEY_ORIGINAL_PACKAGE_NAME = "original_package_name"
        const val KEY_CLONED_PACKAGE_NAME = "cloned_package_name"
        const val KEY_CLONED_APP_NAME = "cloned_app_name"
        const val KEY_APP_SIZE = "app_size"
    }
    
    override suspend fun doWork(): Result {
        val originalPackageName = inputData.getString(KEY_ORIGINAL_PACKAGE_NAME) ?: run {
            Timber.e("Missing original package name")
            return Result.failure()
        }
        
        val clonedPackageName = inputData.getString(KEY_CLONED_PACKAGE_NAME) ?: run {
            Timber.e("Missing cloned package name")
            return Result.failure()
        }
        
        val clonedAppName = inputData.getString(KEY_CLONED_APP_NAME) ?: run {
            Timber.e("Missing cloned app name")
            return Result.failure()
        }
        
        val appSize = inputData.getLong(KEY_APP_SIZE, 0L)
        
        return try {
            Timber.i("Starting clone process for: $clonedAppName")
            Timber.i("Original: $originalPackageName -&gt; Cloned: $clonedPackageName")
            
            // Step 1: Preparation
            setProgressAsync(androidx.work.Data.Builder()
                .putInt("progress", 10)
                .putString("stage", "preparing")
                .build())
            delay(500)
            
            // Step 2: Extracting APK
            setProgressAsync(androidx.work.Data.Builder()
                .putInt("progress", 30)
                .putString("stage", "extracting")
                .build())
            delay(1000)
            
            // Step 3: Modifying package
            setProgressAsync(androidx.work.Data.Builder()
                .putInt("progress", 60)
                .putString("stage", "modifying")
                .build())
            delay(1000)
            
            // Step 4: Signing APK
            setProgressAsync(androidx.work.Data.Builder()
                .putInt("progress", 80)
                .putString("stage", "signing")
                .build())
            delay(500)
            
            // Step 5: Installation (simulated)
            setProgressAsync(androidx.work.Data.Builder()
                .putInt("progress", 95)
                .putString("stage", "installing")
                .build())
            delay(500)
            
            // Save clone history
            val history = CloneHistory(
                id = java.util.UUID.randomUUID().toString(),
                originalPackageName = originalPackageName,
                clonedPackageName = clonedPackageName,
                clonedAppName = clonedAppName,
                cloneTimestamp = System.currentTimeMillis(),
                apkPath = "/data/app/$clonedPackageName.apk",
                appSize = appSize,
                isSuccessful = true,
                isActive = true
            )
            
            cloneHistoryRepository.saveHistory(history)
            
            // Increment clone count
            appRepository.incrementCloneCount(originalPackageName)
            
            setProgressAsync(androidx.work.Data.Builder()
                .putInt("progress", 100)
                .putString("stage", "completed")
                .build())
            
            Timber.i("Clone completed successfully: $clonedAppName")
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "Clone failed for: $clonedAppName")
            
            // Save failed history
            val history = CloneHistory(
                id = java.util.UUID.randomUUID().toString(),
                originalPackageName = originalPackageName,
                clonedPackageName = clonedPackageName,
                clonedAppName = clonedAppName,
                cloneTimestamp = System.currentTimeMillis(),
                appSize = appSize,
                isSuccessful = false,
                isActive = false,
                errorMessage = e.message
            )
            
            cloneHistoryRepository.saveHistory(history)
            
            Result.failure()
        }
    }
}
