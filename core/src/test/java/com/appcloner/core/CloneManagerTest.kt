
package com.appcloner.core

import com.appcloner.apkprocessor.*
import io.mockk.*
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlinx.coroutines.test.*
import org.junit.Assert.*

/**
 * CloneManager 集成测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CloneManagerTest {

    private lateinit var cloneManager: CloneManager
    private lateinit var mockApkTool: IApkToolService
    private lateinit var mockSigner: ISignerService
    private lateinit var mockModifier: PackageNameModifier
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        mockApkTool = mockk()
        mockSigner = mockk()
        mockModifier = mockk()
        
        cloneManager = CloneManager(
            apkToolService = mockApkTool,
            signerService = mockSigner,
            packageNameModifier = mockModifier
        )
    }

    @Test
    fun `test clone manager success flow`() = runTest(testDispatcher) {
        // 准备测试数据
        val testApk = File("test.apk")
        val outputDir = File("output")
        val config = CloneConfig(
            originalApk = testApk,
            newPackageName = "com.test.clone",
            newAppName = "Test Clone",
            outputDir = outputDir
        )

        // 模拟行为
        every { mockApkTool.decode(any(), any()) } returns Result.success(true)
        every { mockModifier.modifyPackageName(any(), any(), any()) } returns Result.success(true)
        every { mockApkTool.build(any(), any()) } returns Result.success(true)
        every { mockSigner.signApk(any(), any()) } returns Result.success(true)

        // 执行测试
        val result = cloneManager.cloneApp(config)

        // 验证
        assertTrue(result.isSuccess)
        verify(exactly = 1) { mockApkTool.decode(any(), any()) }
        verify(exactly = 1) { mockModifier.modifyPackageName(any(), any(), any()) }
        verify(exactly = 1) { mockApkTool.build(any(), any()) }
        verify(exactly = 1) { mockSigner.signApk(any(), any()) }
    }

    @Test
    fun `test clone manager failure on decode`() = runTest(testDispatcher) {
        // 准备
        val testApk = File("test.apk")
        val config = CloneConfig(
            originalApk = testApk,
            newPackageName = "com.test.clone",
            newAppName = "Test Clone",
            outputDir = File("output")
        )

        // 模拟解码失败
        every { mockApkTool.decode(any(), any()) } returns Result.failure(Exception("Decode failed"))

        // 执行
        val result = cloneManager.cloneApp(config)

        // 验证
        assertTrue(result.isFailure)
    }

    @Test
    fun `test progress listener is called`() = runTest(testDispatcher) {
        // 准备
        val testApk = File("test.apk")
        val config = CloneConfig(
            originalApk = testApk,
            newPackageName = "com.test.clone",
            newAppName = "Test Clone",
            outputDir = File("output")
        )

        // 模拟
        every { mockApkTool.decode(any(), any()) } returns Result.success(true)
        every { mockModifier.modifyPackageName(any(), any(), any()) } returns Result.success(true)
        every { mockApkTool.build(any(), any()) } returns Result.success(true)
        every { mockSigner.signApk(any(), any()) } returns Result.success(true)

        // 监听进度
        val progressSteps = mutableListOf&lt;Int&gt;()
        val progressListener = object : CloneProgressListener {
            override fun onProgress(step: Int, total: Int, message: String) {
                progressSteps.add(step)
            }

            override fun onSuccess(clonedApk: File) {}
            override fun onError(error: String) {}
        }

        // 执行
        cloneManager.cloneApp(config, progressListener)

        // 验证进度被调用
        assertTrue(progressSteps.isNotEmpty())
    }
}
