
# App Cloner - APKTool集成配置

## 概述

本项目集成了APKTool，用于APK的反编译、重打包和签名处理。

## APKTool版本

当前版本: **2.9.0**

## 文件说明

### apktool_2.9.0.jar
- 位置: `app/src/main/assets/apktool_2.9.0.jar`
- 用途: APK反编译和重打包

###uber-apk-signer.jar
- 位置: `app/src/main/assets/uber-apk-signer-1.2.1.jar`
- 用途: APK签名

## 使用方法

### 在代码中调用

```kotlin
// 1. 反编译APK
val apkToolService = RealApkToolService(context)
apkToolService.decode(sourceApk, outputDir)

// 2. 修改内容
// ... 进行包名、应用名等修改 ...

// 3. 重打包
apkToolService.build(sourceDir, unsignedApk)

// 4. 签名
val signerService = RealSignerService(context)
signerService.signApk(unsignedApk, signedApk)
```

## 完整克隆流程

```
1. 获取源APK → sourceDir
2. 反编译 → decompiledDir (APKTool decode)
3. 修改AndroidManifest.xml (包名、应用名)
4. 修改DEX/Smali文件
5. 重打包 → unsigned.apk (APKTool build)
6. 签名 → signed.apk (Uber ApkSigner)
7. 安装 → InstallIntent
```

## 命令行使用

### 反编译
```bash
java -jar apktool_2.9.0.jar d input.apk -o output_dir
```

### 重打包
```bash
java -jar apktool_2.9.0.jar b input_dir -o output.apk
```

### 签名
```bash
java -jar uber-apk-signer.jar --apks signed.apk --out out_dir
```

## 依赖下载

### APKTool
从 [APKTool官网](https://ibotpeaches.github.io/Apktool/) 下载最新版本

### Uber ApkSigner
从 [GitHub](https://github.com/patrickfav/uber-apk-signer) 下载

## 注意事项

1. APKTool需要Java环境 (JDK 8+)
2. 部分加固应用可能无法反编译
3. 签名时需要有效的签名密钥
4. Android 9.0+应用需要正确的签名配置

## 法律声明

本工具仅用于学习和研究目的。请遵守相关法律法规，不要用于非法用途。
