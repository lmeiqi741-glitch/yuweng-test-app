#!/bin/sh
export LD_PRELOAD=/tmp/jvm_mprotect.so
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
exec gradle "$@"
