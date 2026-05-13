#!/bin/bash

echo "Fixing gradle-wrapper.jar..."

# 下载真实的 gradle-wrapper.jar
cd /workspace/gradle/wrapper

# 使用一个可靠的源下载
curl -L -o gradle-wrapper.jar "https://raw.githubusercontent.com/nicerloop/android-gradle-wrapper/master/gradle/wrapper/gradle-wrapper.jar"

# 检查文件类型
file gradle-wrapper.jar

# 如果下载的是HTML文件，使用备用方法
if file gradle-wrapper.jar | grep -q "HTML"; then
    echo "Download failed, trying alternative source..."
    # 使用 Maven Central 下载
    wget -q -O gradle-wrapper.jar "https://repo.maven.apache.org/maven2/org/gradle/gradle-wrapper/8.2/gradle-wrapper-8.2.jar"
fi

# 验证
file gradle-wrapper.jar
ls -lh gradle-wrapper.jar

echo "Done!"
