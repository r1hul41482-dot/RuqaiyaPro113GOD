#!/data/data/com.termux/files/usr/bin/bash
# RuqaiyaPro Android 11 On-Device Builder for Boss Rubel
# Target: Infinix HOT 12 Play X6816C

echo "=========================================================="
echo "   RuqaiyaPro 113 GOD FINAL FIX FOR BOSS RUBEL            "
echo "   Infinix HOT 12 Play (Android 11, API 30, 4+3GB RAM)   "
echo "=========================================================="

pkg update -y
pkg install openjdk-17 gradle -y

export JAVA_HOME="/data/data/com.termux/files/usr/lib/jvm/openjdk-17"
chmod +x ./gradlew

echo "Starting assembleDebug..."
./gradlew assembleDebug --no-daemon --stacktrace

if [ $? -eq 0 ]; then
    echo "=========================================================="
    echo " BUILD SUCCESSFUL! APK generated at:                     "
    echo " app/build/outputs/apk/debug/app-debug.apk                "
    echo "=========================================================="
    cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/RuqaiyaPro-BossRubel.apk 2>/dev/null
    echo "Copied to /sdcard/Download/RuqaiyaPro-BossRubel.apk"
else
    echo "Build failed. Check Logcat or Regex repair engine."
fi
