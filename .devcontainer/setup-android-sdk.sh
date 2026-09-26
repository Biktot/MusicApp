#!/bin/bash
set -e
SDK_ROOT=$HOME/android-sdk
mkdir -p $SDK_ROOT/cmdline-tools
cd $SDK_ROOT/cmdline-tools
curl -o tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q tools.zip
rm tools.zip
mv cmdline-tools latest
echo "sdk.dir=$SDK_ROOT" > /workspaces/MusicApp/local.properties
echo "export ANDROID_HOME=$SDK_ROOT" >> ~/.bashrc
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools" >> ~/.bashrc
yes | $SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null
$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
