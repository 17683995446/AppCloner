#!/bin/bash

# Android AppCloner - 编译脚本
# 功能：自动配置环境并编译Debug APK

echo "================================"
echo "Android 应用分身 - APK 编译"
echo "================================"
echo ""

# 检查Java
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装"
    exit 1
fi

echo "✅ Java 环境: $(java -version 2>&1 | head -1)"

# 检查Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME=/opt/android-sdk
        export ANDROID_SDK_ROOT=/opt/android-sdk
        echo "✅ Android SDK: $ANDROID_HOME"
    else
        echo "❌ Android SDK 未找到"
        exit 1
    fi
else
    echo "✅ Android SDK: $ANDROID_HOME"
fi

# 清除代理设置（避免网络问题）
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY
echo "✅ 已清除代理设置"

# 进入项目目录
cd "$(dirname "$0")"
PROJECT_DIR=$(pwd)
echo "✅ 项目目录: $PROJECT_DIR"

# 清理旧构建
echo ""
echo "正在清理旧构建..."
rm -rf app/build
echo "✅ 清理完成"

# 开始构建
echo ""
echo "================================"
echo "开始编译 APK..."
echo "================================"
echo ""

# 使用Gradle编译
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew assembleDebug
else
    gradle assembleDebug
fi

# 检查结果
if [ $? -eq 0 ]; then
    echo ""
    echo "================================"
    echo "🎉 编译成功！"
    echo "================================"
    echo ""
    APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo "✅ APK 文件位置: $APK_PATH"
        ls -lh "$APK_PATH"
    else
        echo "⚠️ APK 文件未找到，尝试其他位置..."
        find "$PROJECT_DIR" -name "*.apk" -type f 2>/dev/null | head -5
    fi
else
    echo ""
    echo "================================"
    echo "❌ 编译失败"
    echo "================================"
    echo ""
    echo "可能的问题："
    echo "1. 网络连接问题 - 请检查网络"
    echo "2. Android SDK 缺失 - 请安装 Android SDK"
    echo "3. 依赖下载失败 - 尝试重新运行此脚本"
    exit 1
fi
