package com.appcloner

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import java.io.*
import java.security.KeyStore
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val clonedApps = mutableListOf<AppInfo>()
    private val originalApps = mutableListOf<AppInfo>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        private const val PREFS_NAME = "AppClonerPrefs"
        private const val KEY_CLONED_APPS = "cloned_apps"
        private const val REQUEST_CODE_INSTALL = 1001
        private const val KEYSTORE_PASSWORD = "AppCloner2024Secret"
        private const val KEY_ALIAS = "AppClonerCloneKey"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadClonedApps()
        generateSigningKeyIfNeeded()

        setupViewPager()
        loadAppList()
    }

    private fun generateSigningKeyIfNeeded() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.app_list)
                1 -> getString(R.string.cloned_apps)
                else -> ""
            }
        }.attach()
    }

    private fun loadAppList() {
        scope.launch {
            val apps = withContext(Dispatchers.IO) {
                getInstalledApps()
            }
            originalApps.clear()
            originalApps.addAll(apps)
            updateFragments()
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val packageManager = packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                try {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo.packageName)
                    apps.add(AppInfo(appName, appInfo.packageName, icon))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return apps.sortedBy { it.appName }
    }

    private fun loadClonedApps() {
        val clonedAppsString = sharedPreferences.getString(KEY_CLONED_APPS, "") ?: ""
        if (clonedAppsString.isNotEmpty()) {
            val packages = clonedAppsString.split(",").filter { it.isNotEmpty() }
            for (packageInfo in packages) {
                val parts = packageInfo.split("|")
                if (parts.size >= 2) {
                    val packageName = parts[0]
                    val cloneId = parts[1].toIntOrNull() ?: 0
                    val appName = parts.getOrElse(2) { "Clone $cloneId" }

                    try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val icon = packageManager.getApplicationIcon(appInfo)
                        clonedApps.add(AppInfo(appName, packageName, icon, true, cloneId))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun saveClonedApps() {
        val clonedAppsString = clonedApps.joinToString(",") {
            "${it.packageName}|${it.cloneId}|${it.appName}"
        }
        sharedPreferences.edit().putString(KEY_CLONED_APPS, clonedAppsString).apply()
    }

    fun cloneApp(appInfo: AppInfo) {
        scope.launch {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage(getString(R.string.cloning))
            progressDialog.setCancelable(false)
            progressDialog.show()

            val result = withContext(Dispatchers.IO) {
                performClone(appInfo)
            }

            progressDialog.dismiss()

            if (result.first) {
                Toast.makeText(this@MainActivity, getString(R.string.clone_success), Toast.LENGTH_SHORT).show()
                updateFragments()
            } else {
                Toast.makeText(this@MainActivity, result.second, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performClone(appInfo: AppInfo): Pair<Boolean, String> {
        return try {
            val sourceDir = packageManager.getApplicationInfo(appInfo.packageName, 0).sourceDir
            val cloneId = getNextCloneId(appInfo.packageName)
            val newPackageName = "${appInfo.packageName}.clone$cloneId"

            val outputApk = File(cacheDir, "cloned_app_$cloneId.apk")

            val cloneResult = cloneApkWithNewPackage(
                sourceApk = File(sourceDir),
                outputApk = outputApk,
                originalPackage = appInfo.packageName,
                newPackage = newPackageName,
                appLabel = appInfo.appName
            )

            if (cloneResult) {
                val clonedApp = AppInfo(
                    appName = "${appInfo.appName} (Clone)",
                    packageName = newPackageName,
                    icon = appInfo.icon,
                    isCloned = true,
                    cloneId = cloneId
                )

                clonedApps.add(clonedApp)
                saveClonedApps()

                requestInstallApk(outputApk)

                Pair(true, "Success")
            } else {
                Pair(false, "Clone failed - APK signing error")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Error: ${e.message}")
        }
    }

    private fun cloneApkWithNewPackage(
        sourceApk: File,
        outputApk: File,
        originalPackage: String,
        newPackage: String,
        appLabel: String
    ): Boolean {
        var tempDir: File? = null
        try {
            tempDir = File(cacheDir, "apk_temp_${System.currentTimeMillis()}")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            unzipApk(sourceApk, tempDir)

            val manifestFile = File(tempDir, "AndroidManifest.xml")
            if (manifestFile.exists()) {
                var manifestContent = manifestFile.readBytes()
                manifestContent = binaryReplace(manifestContent, originalPackage, newPackage)
                manifestFile.writeBytes(manifestContent)
            }

            val smaliDir = File(tempDir, "smali")
            if (smaliDir.exists()) {
                replaceInSmaliFiles(smaliDir, originalPackage, newPackage)
            }

            val resDir = File(tempDir, "res")
            if (resDir.exists()) {
                replaceInResourceFiles(resDir, originalPackage, newPackage)
            }

            val arscFile = File(tempDir, "resources.arsc")
            if (arscFile.exists()) {
                var arscContent = arscFile.readBytes()
                arscContent = binaryReplace(arscContent, originalPackage, newPackage)
                arscFile.writeBytes(arscContent)
            }

            val unsignedApk = File(cacheDir, "unsigned_$System.currentTimeMillis().apk")
            zipDirectory(tempDir, unsignedApk)

            signApkProperly(unsignedApk, outputApk)

            unsignedApk.delete()

            return outputApk.exists() && outputApk.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            tempDir?.deleteRecursively()
        }
    }

    private fun replaceInSmaliFiles(dir: File, oldPackage: String, newPackage: String) {
        val oldPath = oldPackage.replace(".", "/")
        val newPath = newPackage.replace(".", "/")

        dir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "smali") {
                var content = file.readText()
                if (content.contains(oldPackage)) {
                    content = content.replace(oldPackage, newPackage)
                    content = content.replace(oldPath, newPath)
                    file.writeText(content)
                }
            }
        }

        val oldDir = File(dir, oldPath)
        if (oldDir.exists()) {
            val newDir = File(dir, newPath)
            if (!newDir.exists()) {
                newDir.mkdirs()
            }
            oldDir.listFiles()?.forEach { file ->
                val destFile = File(newDir, file.name)
                file.copyTo(destFile, overwrite = true)
                replaceInSmaliFiles(newDir, oldPackage, newPackage)
            }
        }
    }

    private fun replaceInResourceFiles(dir: File, oldPackage: String, newPackage: String) {
        dir.walkTopDown().forEach { file ->
            if (file.isFile && (file.extension == "xml" || file.extension == "png")) {
                try {
                    if (file.extension == "xml") {
                        var content = file.readText()
                        if (content.contains(oldPackage)) {
                            content = content.replace(oldPackage, newPackage)
                            file.writeText(content)
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun unzipApk(zipFile: File, destDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun zipDirectory(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(sourceDir).path
                    zos.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun signApkProperly(unsignedApk: File, signedApk: File) {
        try {
            val keystoreFile = File(cacheDir, "clone.keystore")
            if (!keystoreFile.exists()) {
                createKeystore(keystoreFile)
            }

            signWithJarsigner(unsignedApk, signedApk, keystoreFile)
        } catch (e: Exception) {
            e.printStackTrace()
            unsignedApk.copyTo(signedApk, overwrite = true)
        }
    }

    private fun createKeystore(keystoreFile: File) {
        Runtime.getRuntime().exec(arrayOf(
            "keytool", "-genkeypair",
            "-keystore", keystoreFile.absolutePath,
            "-alias", KEY_ALIAS,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-storepass", KEYSTORE_PASSWORD,
            "-keypass", KEYSTORE_PASSWORD,
            "-dname", "CN=AppCloner, OU=AppCloner, O=AppCloner, L=Beijing, ST=Beijing, C=CN"
        )).waitFor()
    }

    private fun signWithJarsigner(unsignedApk: File, signedApk: File, keystoreFile: File) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "jarsigner",
                "-keystore", keystoreFile.absolutePath,
                "-storepass", KEYSTORE_PASSWORD,
                "-keypass", KEYSTORE_PASSWORD,
                "-signedjar", signedApk.absolutePath,
                unsignedApk.absolutePath,
                KEY_ALIAS
            ))
            val result = process.waitFor()

            if (result != 0) {
                val error = process.errorStream.bufferedReader().readText()
                throw Exception("jarsigner failed: $error")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            unsignedApk.copyTo(signedApk, overwrite = true)
        }
    }

    private fun binaryReplace(data: ByteArray, oldStr: String, newStr: String): ByteArray {
        if (oldStr.length != newStr.length) {
            return data
        }

        val result = data.copyOf()
        val oldBytes = oldStr.toByteArray()
        val newBytes = newStr.toByteArray()

        for (i in 0..result.size - oldBytes.size) {
            var match = true
            for (j in oldBytes.indices) {
                if (result[i + j] != oldBytes[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                for (j in newBytes.indices) {
                    result[i + j] = newBytes[j]
                }
            }
        }
        return result
    }

    private fun getNextCloneId(packageName: String): Int {
        var maxId = 0
        clonedApps.forEach {
            if (it.packageName.startsWith(packageName)) {
                if (it.cloneId > maxId) maxId = it.cloneId
            }
        }
        return maxId + 1
    }

    private fun requestInstallApk(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE_INSTALL)
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW)
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
    }

    fun launchClonedApp(appInfo: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "无法启动应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "启动失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun showDeleteConfirmDialog(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_clone)
            .setMessage(R.string.delete_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteClonedApp(appInfo)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun deleteClonedApp(appInfo: AppInfo) {
        try {
            clonedApps.remove(appInfo)
            saveClonedApps()

            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:${appInfo.packageName}")
            startActivity(intent)

            updateFragments()
            Toast.makeText(this, "分身已删除", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFragments() {
        viewPagerAdapter.getFragment(0).updateAppList(originalApps)
        viewPagerAdapter.getFragment(1).updateAppList(clonedApps)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_INSTALL) {
            Toast.makeText(this, "请重新尝试分身操作", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
