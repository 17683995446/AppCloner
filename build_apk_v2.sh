#!/bin/bash
# App Cloner 构建脚本
# 用于在本地或 CI 环境中构建 APK

set -e

echo "================================"
echo "App Cloner 构建脚本"
echo "================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1 已安装"
        return 0
    else
        echo -e "${RED}✗${NC} $1 未安装"
        return 1
    fi
}

# 检查环境
echo "1. 检查环境..."
echo "--------------------------------"

check_command java
check_command gradle
check_command wget

echo ""
echo "2. 设置 Android SDK..."
echo "--------------------------------"

# 检查 ANDROID_HOME
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
    else
        echo -e "${YELLOW}!${NC} ANDROID_HOME 未设置，尝试自动安装..."
        mkdir -p /opt/android-sdk
        export ANDROID_HOME="/opt/android-sdk"
    fi
fi

echo "ANDROID_HOME: $ANDROID_HOME"

echo ""
echo "3. 准备 Gradle Wrapper..."
echo "--------------------------------"

if [ ! -f "gradlew" ]; then
    echo "创建 Gradle Wrapper..."
    gradle wrapper --gradle-version 8.2
fi

chmod +x gradlew

echo ""
echo "4. 构建 Debug APK..."
echo "--------------------------------"

./gradlew assembleDebug --no-daemon --stacktrace

echo ""
echo "5. 验证 APK..."
echo "--------------------------------"

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo -e "${GREEN}✓${NC} Debug APK 构建成功！"
    ls -lh "$APK_PATH"
    
    # 复制到根目录
    cp "$APK_PATH" AppCloner-debug.apk
    echo -e "${GREEN}✓${NC} APK 已复制到: AppCloner-debug.apk"
else
    echo -e "${RED}✗${NC} Debug APK 构建失败！"
    exit 1
fi

echo ""
echo "================================"
echo -e "${GREEN}构建完成！${NC}"
echo "================================"
