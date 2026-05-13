#!/usr/bin/env python3
import os
import sys
import subprocess
import urllib.request
import tempfile

def download_gradle_wrapper():
    """Download gradle-wrapper.jar from a working source"""
    wrapper_url = "https://github.com/nicerloop/android-gradle-wrapper/raw/master/gradle/wrapper/gradle-wrapper.jar"
    wrapper_path = "/workspace/gradle/wrapper/gradle-wrapper.jar"
    
    print(f"Downloading gradle-wrapper.jar...")
    try:
        urllib.request.urlretrieve(wrapper_url, wrapper_path)
        print(f"Downloaded to {wrapper_path}")
        return True
    except Exception as e:
        print(f"Download failed: {e}")
        return False

def check_java():
    """Check Java installation"""
    try:
        result = subprocess.run(['java', '-version'], capture_output=True, text=True)
        print(f"Java version: {result.stderr.split()[2]}")
        return True
    except Exception as e:
        print(f"Java not found: {e}")
        return False

def main():
    print("=" * 60)
    print("Android AppCloner APK Builder")
    print("=" * 60)
    
    # Check Java
    if not check_java():
        print("ERROR: Java is not installed")
        sys.exit(1)
    
    # Set environment
    os.environ['ANDROID_HOME'] = '/opt/android-sdk'
    os.environ['ANDROID_SDK_ROOT'] = '/opt/android-sdk'
    
    # Try to download gradle wrapper
    download_gradle_wrapper()
    
    # Try to build
    print("\nAttempting to build APK...")
    try:
        result = subprocess.run(
            ['./gradlew', 'assembleDebug'],
            cwd='/workspace',
            env=os.environ,
            capture_output=True,
            text=True,
            timeout=300
        )
        print(result.stdout)
        if result.returncode == 0:
            print("\n✅ Build successful!")
            print("APK location: /workspace/app/build/outputs/apk/debug/app-debug.apk")
        else:
            print(f"\n❌ Build failed")
            print(result.stderr)
    except Exception as e:
        print(f"Build error: {e}")

if __name__ == '__main__':
    main()
