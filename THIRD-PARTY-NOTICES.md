# Third-party notices and source availability


An independent modification of [ovrport/app](https://github.com/ovrport/app), based on commit `1c7cb335c81369fbfd2957ad0edcb238956a8e3c`. Credit remains with the original developers. The application and this fork's additions are distributed under GPLv3; see `LICENSE`.

## Matching application source

The **v0.1.0-beta.5** repository tag and the application source archive next to the binaries in the [release](https://github.com/urscaviezel/OVRPort-PFDMR/releases/tag/v0.1.0-beta.5) include the native wrapper, Android audio helper, launcher and build scripts. See the [source build notes](https://github.com/urscaviezel/OVRPort-PFDMR/blob/v0.1.0-beta.5/docs/BUILD.md).

## Bundled Java runtime

**Eclipse Temurin 17.0.20.1+1, Windows x64 JRE**, by Eclipse Adoptium/OpenJDK contributors, is provided under GPLv2 with the Classpath Exception and associated component terms. All notices remain in `jre/legal/`; `jre/release` identifies the exact source revisions.

- [Official runtime release](https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.20.1%2B1)
- [Matching complete OpenJDK source](https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk-sources_17.0.20.1_1.tar.gz)
- [Source repository](https://github.com/adoptium/jdk17u)
- [Recorded build-script revision](https://github.com/adoptium/temurin-build/tree/e6ba7dec3d07654074559310376a3ae89da5f4ac)

The matching OpenJDK source archive is also hosted as an asset of this fork's release.

## Upstream vendored libraries

| Component | Version | License / notices | Source |
|---|---|---|---|
| ARSCLib, REAndroid contributors | 1.4.0 | Apache-2.0; `licenses/ARSCLib-LICENSE.txt` | [V1.4.0](https://github.com/REAndroid/ARSCLib/tree/V1.4.0) |
| smali/baksmali/dexlib2, Ben Gruver and contributors | 2.5.2 | BSD and component terms; `licenses/smali-LICENSE.txt` | [v2.5.2](https://github.com/JesusFreke/smali/tree/v2.5.2) |

These upstream JARs remain in `overportcli/libs`. Matching source archives are provided with the release. The smali bundle includes support libraries with their upstream terms.

## OpenXR and native toolchain

The vendored OpenXR headers retain Khronos copyright and `Apache-2.0 OR MIT` identifiers. This distribution uses the Apache-2.0 option; see `licenses/OpenXR-LICENSE.txt`. Headers came from the YVR OpenXR mobile SDK 2.0.0 used for local tests. No vendor runtime library from that SDK is bundled.

The wrapper is built with Android NDK 27.2.12479018. LLVM/libc++ and toolchain notices are in `licenses/Android-NDK-toolchain-NOTICE.txt`. Android platform libraries are loaded from the device, not redistributed here.

## Maven dependencies

The source build uses Kotlin, Compose Multiplatform/AndroidX, Skiko, Kotlin coroutines/serialization, Android apksig, Bouncy Castle, FlatLaf and transitive dependencies. Versions are recorded in the build files and generated dependency inventory. Copyright/license/notice files found in dependency JARs are retained separately under `licenses/dependencies/` as well as in packaged metadata.

Additional original license and notice texts are in `licenses/`: Kotlin and Compose (JetBrains and contributors, Apache-2.0), Skiko (JetBrains and contributors, Apache-2.0), JBR API (JetBrains, Apache-2.0), JSpecify (Apache-2.0), and Bouncy Castle (Legion of the Bouncy Castle, MIT-style license). AndroidX and apksig retain Android Open Source Project Apache-2.0 notices. FlatLaf retains its Apache-2.0 license. Exact original download URLs and hashes are recorded in `additional-provenance.json`.

The Gradle wrapper is retained from upstream. The Windows .NET Framework launcher prerequisite is not redistributed.

## Legacy Unreal Quest compatibility research

The Quest startup/controller compatibility finding was informed by [FramePort](https://github.com/spoopyghosty0/frameport/blob/main/src/frameport/patches/frame/unreal_santacruz.py), including the investigation credited there to Klownicle. OVRPort-PFDMR uses its own Java implementation with ELF validation and reversible edits. No FramePort executable or Python module is bundled.
