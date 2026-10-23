# Building 0.1.0-beta.8 from source

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
python native/mr/build_mr.py --ndk 'C:\path\to\ndk\27.2.12479018' --tests
python audio-helper/build_helper.py --jdk $env:JAVA_HOME --sdk 'C:\path\to\android-sdk'
.\gradlew.bat :composeApp:packageUberJarForCurrentOS --no-daemon
.\gradlew.bat :composeApp:dependencyInventory --no-daemon
python scripts/collect_licenses.py
```

**Build native first.** Its output in `overportcli/src/main/resources/pfdmr/arm64-v8a/` is embedded in the desktop JAR under `composeApp/build/compose/jars/`.

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar 'composeApp/build/compose/jars/composeApp-windows-x64-0.1.0-beta.8.jar'
python scripts/package_windows.py --jar 'composeApp/build/compose/jars/composeApp-windows-x64-0.1.0-beta.8.jar' --jre 'C:\path\to\jre-17.0.20.1+1' --output dist
```

Packaging compiles `launcher/Launcher.cs`, copies the runtime/notices and verifies the ZIP. The Windows EXE is not signed. APK signing uses the upstream per-package signing store.

## Validation

Run against the final JAR, a disposable workspace, a reference ARM64 loader and a local APK you own. Tests create a disposable signing key and never install the APK:

```powershell
$jar = 'composeApp/build/compose/jars/composeApp-windows-x64-0.1.0-beta.8.jar'
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

Build version: **0.1.0-beta.8**. `VersionManager.VERSION` deliberately remains **1.2.3** for upstream library compatibility checks; `EDITION_VERSION` is the fork's release version.

## Optional MR validation

`native/mr/build_mr.py --tests` builds four isolated Android ARM64 tests for passthrough state, scene buffers, discovery batches and component filtering. Run the executables from `native/mr/build/` in a disposable device directory. They use mock calls and do not launch a game or read real room data.

`tests/MrPatchTests.java` exercises a local owned APK with the supported permission bridge, including signed export and restoring original code/loaders. Compile against the final desktop JAR; arguments are a disposable test directory and the local APK. Never distribute test APKs or signing keys.

The optional adapter requires ARM64 OverPort/YVR loaders and the recognized `com.google.vr.sdk.samples.permission` bridge. It rejects unsupported layouts rather than guessing. It wraps the two API boundaries, preserving original libraries, and runs after library replacement but before the stereo wrapper. Repatching restores stereo first, MR second, and the basic passthrough edit last. No Unreal address hooks or room/pose diagnostic logging are included. Actual VR/MR transitions and permission prompts still require headset testing.

`tests/ScenePermissionTest.smali` is an optional Android-runtime smoke test for the permission name mapping. It assembles with the bundled helper. On the current headset, standalone `dalvikvm` fails during vendor-library preload before the test runs; the native mock tests and signed APK roundtrip remain independently runnable.

The MR loader resources must have the installed SONAMEs `libopenxr_loader.so` and `libopenxr_loader_yvr.so`, despite their distinct resource filenames. Otherwise a second Android `dlopen` may select an original loader and bypass the adapter. `test_loader_identity` reproduces this with mock originals carrying the same SONAMEs. Place the two production wrappers under their installed names beside the mock originals and run with `LD_LIBRARY_PATH=.`. Both repeated library lookup and querying the resolver must retain the wrapper. Beta 5 fails this test; the corrected wrappers pass.

## Basic passthrough validation

`tests/BasicPassthroughTests.java` takes a disposable directory and a local APK containing the supported loader. It checks the exact native change, rejection of unknown versions, ownership-aware rollback, signed APK export and preservation of engine/assets/manifest. `MrPatchTests` also checks combined basic + scene/spatial + stereo export and rollback. The basic fix only changes the basic capability bit reported through `XR_TYPE_SYSTEM_PASSTHROUGH_PROPERTIES2_FB`; it does not add permissions, color/depth support or game-specific input changes.

## Windows application updates

The app checks the public fork release API at startup and on request. Published prereleases are included, tags are compared numerically, and equal/older versions, drafts and missing/incomplete Windows ZIP assets are excluded. Updates use the asset's SHA-256 digest from GitHub. Keep the existing versioned ZIP naming convention and BUILD-INFO.json version/JAR digest when publishing future releases. Never use the upstream compatibility version for this comparison.

The Windows package includes PFDMR-Updater.exe compiled from launcher/Updater.cs. Download and extraction finish before shutdown. The helper waits for the Java process, replaces only known package files/directories, rolls back on installation/restart errors and launches the app again. The launcher removes the old-file backup/staging directory after the helper exits. User cache/settings/signing keys live outside these managed files and are preserved. The installation must be writable; no administrator elevation is requested. Failed downloads leave the installed version running. Manual JAR/development launches do not support automatic installation. Power loss or a forcibly terminated helper can leave recovery files in the .pfdmr-update-* staging directory; these should be retained for recovery.

Compile tests/UpdateTests.java against the desktop JAR and run with a new disposable directory and optionally the Windows ZIP. It checks release selection, version ordering, digests, traversal/invalid archives and real package extraction. Compile launcher/Updater.cs plus tests/UpdaterTests.cs with the framework C# compiler and /main:UpdaterTests to exercise rollback and preservation in a new disposable directory. tests/UpdaterProcessTests.ps1 -Package <extracted-package> -TestRoot <new-disposable-directory> checks the real helper/launcher handshake, waiting for app exit, replacement, restart and automatic cleanup using a harmless stub Java process; it does not update the installed application or publish a simulated release.

## Beta 8 refresh preference and desktop lifecycle

The optional ARM64 refresh wrapper reads `Settings.System.SpatialHome_SystemFrequencyMode` through the Android context supplied to OpenXR instance creation or loader initialization. It forwards the selected 72, 80 or 90 Hz in place of an app preference. Missing/unreadable/unknown values fall back to 0 (OpenXR: no application preference). The original Beta 8 only sent 0 and the user observed 90 Hz despite a 72 Hz HMD setting. This is not an unconditional 72/80/90 Hz override: firmware policy, alternate APIs, or a game-specific frame limiter can still determine the result. Actual HMD-setting behavior requires an on-device game test. Actual-rate queries, frame pacing, SpaceWarp configuration and submission data are not changed. Native mock tests cover direct calls, resolver calls, result/error forwarding and repeated loader lookup. See `native/refresh_tests.cpp`; build with `native/build_native.py --tests` and run in an isolated device directory with the mock original and production wrapper under its installed name.

Patch order is base replacements, basic passthrough, scene/spatial, stereo, then refresh. Restore wrappers in reverse order. `RefreshPatchTests.java` covers signed export, immediate Windows file release and rollback; `MrPatchTests.java` covers all four optional loader layers together. `DesktopInstanceTests.java` tests workspace-wide locking across JVMs and after a forced process exit. The lock is acquired before the desktop UI/cache is opened and released by process exit, so the update helper can restart the app normally.

Keep release artifacts in `dist/releases/<version>/` inside this repository. Archived pre-release experiments live under `dist/archive/`; game projects must not contain patcher distributions.

Refresh follow-up: native mock JNI tests cover 72/80/90 Hz, changes after startup, invalid settings and cleared exceptions. A standalone real-Context shell probe (`RefreshJniProbe.java`, `refresh_jni_probe.cpp`) was killed by the headset runtime before reporting a result; it does not validate app-context access. Test the corrected build in-game and inspect the `PFDMRRefresh` log tag if the actual display rate differs. No system settings are written.

Refresh session-start follow-up: Lies Beneath runtime logs showed a 72-to-90 Hz transition at launch without a PFDMRRefresh request log. The optional wrapper now enables XR_FB_display_refresh_rate when advertised, resolves its request function per session and applies the saved HMD setting after successful xrBeginSession, including session restarts. Missing settings/functions leave startup behavior unchanged; refresh errors never replace the session-start result. Session dispatch is removed on successful destruction. Native tests (27 checks) cover the no-app-request path, extension deduplication, restart, failed start and cleanup in addition to existing SpaceWarp/data forwarding. In-game acceptance remains pending; inspect the Session start log and headset overlay.


Beta 8 validation update (2026-10-23): the user confirmed that Ghost Town, Witchblood and Lies Beneath all accept the session-start refresh fix and run at the selected 72 Hz. SpaceWarp remains unchanged.

## Existing OBB startup compatibility

`UnrealObbPatch` is enabled by default, applies only to `com.kurzgesagt.outofscale` with the verified startup pattern and fingerprinted OBB check, and makes no changes to other packages. It removes only the VerifyOBBOnStartUp guard around the existing path/size check in GameActivity.onCreate. Missing or incorrectly sized OBBs still use the original downloader path; the additional downloader CRC pass is skipped for an existing correctly sized OBB. The OBB itself is neither modified nor bundled. A receipt supports restoring owned edits after export/reimport; preexisting manual fixes are retained. Unknown check code or startup instructions fail closed.

Reference input: Out of Scale versionCode 3244131, APK SHA-256 a00e3d7cf1ee6f4e27269d5e3e63ef35525965997eff31be9d9137a0a46916f5. REA static evidence: package ev_61301123d271204f4f43f75a876f4f78c0f245388605845b1327ae2b19337a49; startup ev_731786b7479ab7b9c179178a17906fda042bf5a6f014f7fa81289a5942db75d6; existing check ev_b7f77113a5c24d9c0a698223be5f5088a0ad4bd2c5259df1b55d4a3313e9fb20. Comparison against the user's working OBBFix APK confirms the two removed instructions, with downstream labels adjusted by assembly. Static evidence does not itself prove runtime behavior; the user confirmed the separate game-project patch works. Fresh patcher-output headset testing remains separate.

`tests/UnrealObbTests.java` checks the original/reference onCreate instructions, signed export and DEX roundtrip, rollback, preexisting fixes, rejection of unknown startup/check code, unrelated packages and the requested checkbox/default ordering. Arguments: disposable test directory, original local APK, reference GameActivity.smali disassembled from the working APK. No game code or APKs are included in distributions.


## Beta 9 Pinball FX VR 1.10 profile (local test build)

Build `native/pinball110/build.py --ndk <NDK>` before the desktop JAR. `--tests` also builds `native/build/pinball110/pinball-tests` and its mock original; run them in an isolated ARM64 Android directory with LD_LIBRARY_PATH set there. The production wrapper carries the installed SONAME libopenxr_loader.so and marker PFDMR_PINBALL110_V1. The mock-test executable bypasses production library installation but separately executes the same ARM64 relay/trampoline helper on a synthetic function.

The separate option defaults off and requires scene/spatial selected. Preflight checks package com.zenstudios.PFXVRQuest, version 1.10/code 7380, the known Unreal/OVRPlugin/MRUK digests, and rejects standalone test shims. Layer order is basic, scene/spatial, Pinball profile, stereo, refresh; restoration reverses this order. The MRUK validity instruction edit is hash-checked and reversible; independently prepatched MRUK is preserved. The profile never changes SpaceWarp, DLC purchases, OBBs or save files.

The native profile implements the six functional corrections described in PINBALLFX-110-REVIEW.md. Frame/layer/action storage is dynamic. Known ImageLayoutFB prefixes and ColorScaleBiasKHR are copied locally with their tail preserved; an unfamiliar predecessor chain is forwarded unchanged and produces a one-time diagnostic instead of copying an unknown structure size. Pose actions are scoped to their owning instance/action set, cleaned up at destruction, and grip-space input is never modified. Passthrough state is cleared at successful session end/destruction and instance destruction; camera state is published before forwarding a frame so concurrent game ticks see the VR/MR transition in time; failed submissions restore the previous state.

Unreal hook installation occurs before the underlying xrCreateInstance. Both prologs and executable ranges are checked before writes. Near relay pages allow one aligned atomic AArch64 branch write per hook instead of rewriting a four-instruction live prolog. Each trampoline relocates only the verified first non-PC-relative instruction. Originals are published before branch installation, code protection is restored, and the module is pinned for process lifetime. If preparation fails, instance creation returns an initialization error instead of silently running a partial profile. Hooks remain installed for process lifetime; removing the profile requires repatching and restarting the game, as with other APK options.

`Pinball110PatchTests.java` takes a fresh disposable directory and the local v1.10 base APK. It checks selection dependencies, guarded identity, wrapper order, combined signed export/reimport/rollback, unchanged config and tamper rejection. User headset validation of the newly integrated build is still required; the standalone v20 gameplay result alone does not validate this packaging.

Beta 9 validation: 62 native profile checks passed on the connected ARM64 headset, including an actual atomic-branch/relay/trampoline execution, pre-forward camera publication and completed-task handling without a stale pointer dereference. All 20 v1.9 MR pipeline regression checks passed; the refresh wrapper retained all 27 native checks. The profile combined APK test passed 20 checks (signed output, all five loader layers, rollback and tamper rejection). Fresh user headset gameplay testing remains pending.
