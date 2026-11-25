<!-- Modified for Inbo from Clear SMS. See LICENSE and NOTICE. -->

# Build Inbo

Use JDK 17 with Android SDK Platform 35, Build-Tools 36.0.0, and Platform-Tools. If you use the Android Command-Line Tools, install the SDK packages and accept their licenses.

```sh
sdkmanager "platforms;android-35" "build-tools;36.0.0" "platform-tools"
sdkmanager --licenses
```

Clone Inbo and use the checked-in Gradle wrapper. Export `ANDROID_HOME` or set `sdk.dir` in the ignored root `local.properties` file.

```sh
git clone https://github.com/harishkotra/inbo.git
cd inbo
./gradlew assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/Inbo-debug.apk
```

Use `gradlew.bat` on Windows. On a device with SMS support, open Inbo and follow Android’s prompt to choose it as your default SMS app. You can also open **Settings → Messages → Default SMS app**. Updating an installed APK requires the same signing key.

Run the CI checks locally.

```sh
./gradlew ktlintCheck lintDebug testDebugUnitTest
```

A [synthetic corpus](../scripts/demo/README.md) and the [rule audit](../scripts/audit_rule_coverage.py) are available for exercising classification with invented messages.

```sh
python3 scripts/audit_rule_coverage.py scripts/demo/messages.jsonl
```

## Build a release

`./gradlew assembleRelease` writes `app/build/outputs/apk/release/Inbo.apk`. The release build uses R8 and resource shrinking with obfuscation disabled. Configure [release signing](release-signing.md) to produce an installable APK. The [Android CI workflow](../.github/workflows/android.yml) runs checks on pull requests and builds release artifacts for version tags.
