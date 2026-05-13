# Android AppCloner - Dockerfile 编译环境

这个Dockerfile可以在任何支持Docker的环境中快速构建Android APK。

## 使用方法

### 1. 构建 Docker 镜像

```bash
# 在项目目录下运行
docker build -t android-appcloner .

# 或者指定 Android SDK 版本
docker build -t android-appcloner --build-arg ANDROID_SDK_VERSION=34 .
```

### 2. 运行容器并编译

```bash
# 编译 Debug APK
docker run --rm -v "$(pwd)":/workspace android-appcloner ./gradlew assembleDebug

# 编译 Release APK
docker run --rm -v "$(pwd)":/workspace android-appcloner ./gradlew assembleRelease
```

### 3. 获取 APK

```bash
# APK 将生成在 app/build/outputs/apk/debug/ 或 app/build/outputs/apk/release/
ls -la app/build/outputs/apk/debug/
```

## Dockerfile 内容

```dockerfile
FROM ubuntu:22.04

# 避免交互式提示
ENV DEBIAN_FRONTEND=noninteractive

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 设置 JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

# 下载并安装 Android SDK
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    mkdir -p /opt/android-sdk/cmdline-tools && \
    unzip -q /tmp/cmdline-tools.zip -d /opt/android-sdk/cmdline-tools && \
    mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# 设置 Android SDK 环境变量
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 接受许可证
RUN yes | sdkmanager --licenses || true

# 安装必要的 SDK 组件
RUN sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 设置工作目录
WORKDIR /workspace

# 复制项目文件
COPY . .

# 授予 gradlew 执行权限
RUN chmod +x ./gradlew

# 编译 APK
CMD ["./gradlew", "assembleDebug"]
```

## 快速使用（一行命令）

```bash
# 在项目目录下运行这条命令，APK会自动编译
docker run --rm -v "$(pwd)":/workspace -w /workspace ubuntu:22.04 bash -c "apt-get update && apt-get install -y openjdk-17-jdk wget unzip curl && wget -q https://services.gradle.org/distributions/gradle-8.2-bin.zip -O /tmp/gradle.zip && unzip -q /tmp/gradle.zip -d /opt && export PATH=/opt/gradle-8.2/bin:\$PATH && ./gradlew assembleDebug"
```

## 注意事项

1. 确保 Docker 已安装并运行
2. 首次构建会下载大量依赖，请耐心等待
3. 编译后的 APK 位于 `app/build/outputs/apk/debug/` 目录
4. 如果需要 Release APK，请修改命令中的 `assembleDebug` 为 `assembleRelease`

## Windows 使用方法

```powershell
# 在 PowerShell 中运行
docker run --rm -v "${PWD}":/workspace android-appcloner .\gradlew.bat assembleDebug
```
