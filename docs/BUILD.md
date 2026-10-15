# Building 0.1.0-beta.4 from source

This beta release builds from this repository, without binary overlay patching or a private v4/v5 JAR.

## Prerequisites

- Windows x64 and JDK 21 (tested with Eclipse Temurin 21.0.12).
- Python 3.10 or later.
- Android NDK **27.2.12479018**; Android SDK platform 34 and build-tools 34.0.0 for the audio helper.
- Internet access for Gradle 8.13 and Maven dependencies on first build.
- Portable packaging: Temurin **17.0.20.1+1 Windows x64 JRE** and the Windows .NET Framework C# compiler.

OpenXR headers are included in `native/include/openxr`, with original notices. `native/forward.cpp` contains the declarations for the 56 direct OpenXR entry points supported by the wrapper.

## Build

From the repository root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
python native/build_native.py --ndk 'C:\path\to\ndk\27.2.12479018' --tests
python audio-helper/build_helper.py --jdk $env:JAVA_HOME --sdk 'C:\path\to\android-sdk'
.\gradlew.bat :composeApp:packageUberJarForCurrentOS --no-daemon
.\gradlew.bat :composeApp:dependencyInventory --no-daemon
python scripts/collect_licenses.py
```

**Build native first.** Its output in `overportcli/src/main/resources/pfdmr/arm64-v8a/` is embedded in the desktop JAR under `composeApp/build/compose/jars/`.

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar 'composeApp/build/compose/jars/composeApp-windows-x64-0.1.0-beta.4.jar'
python scripts/package_windows.py --jar 'composeApp/build/compose/jars/composeApp-windows-x64-0.1.0-beta.4.jar' --jre 'C:\path\to\jre-17.0.20.1+1' --output dist
```

Packaging compiles `launcher/Launcher.cs`, copies the runtime/notices and verifies the ZIP. The Windows EXE is not signed. APK signing uses the upstream per-package signing store.

## Validation

Run against the final JAR, a disposable workspace, a reference ARM64 loader and a local APK you own. Tests create a disposable signing key and never install the APK:

```powershell
$jar = 'composeApp/build/compose/jars/composeApp-windows-x64-0.1.0-beta.4.jar'
& "$env:JAVA_HOME\bin\javac.exe" --release 17 -encoding UTF-8 -cp $jar -d tests/build tests/StereoTests.java
& "$env:JAVA_HOME\bin\java.exe" -Xverify:all -cp "tests/build;$jar" StereoTests test-work 'C:\path\to\libopenxr_loader.so' 'C:\path\to\owned-test.apk'
```

For the optional Quest startup/controller patch, use a local APK with the supported legacy Unreal functions. This checks detection, rejection, exact instruction changes, reversible export and APK signatures:

```powershell
& "$env:JAVA_HOME\bin\javac.exe" --release 17 -encoding UTF-8 -cp $jar -d tests/build tests/QuestGateTests.java
& "$env:JAVA_HOME\bin\java.exe" -Xmx3g -Xverify:all -cp "tests/build;$jar" QuestGateTests quest-test-work 'C:\path\to\owned-test.apk'
```

The native build's `--tests` option generates `native/build/native-tests` and a mock `libpfdmr_original_openxr.so`. Place them and the production wrapper in an isolated temporary directory on an ARM64 Android device and run `LD_LIBRARY_PATH=. ./native-tests`. This tests dispatch, math, preserved positions/FOV and error forwarding; it does not establish game compatibility.

The legacy VR regression test (`tests/LegacyVrTests.java`) covers MAIN/INFO handling, preservation of existing entries, idempotence and signed APK export. Its arguments are a disposable cache directory, an already-patched local test APK and an output APK. To test an update, copy that package's existing signing key into the disposable cache's `signatures` directory; the test requires matching signing certificates. Never include those private fixtures or keys in release assets.

## Provenance

Upstream history is retained from commit `1c7cb335c81369fbfd2957ad0edcb238956a8e3c`. The upstream vendored JARs are ARSCLib 1.4.0 and smali/baksmali 2.5.2; source archives accompany the release. Maven versions are pinned in `gradle/libs.versions.toml` and recorded in the generated dependency inventory. Runtime sources are identified in `THIRD-PARTY-NOTICES.md`.

Public version: **0.1.0-beta.4**. `VersionManager.VERSION` deliberately remains **1.2.3** for upstream library compatibility checks; `EDITION_VERSION` is the fork's release version.
