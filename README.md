# OVRPort PFDMR Edition

**0.1.0-beta.1 · Experimental pre-release · Windows x64**

An unofficial PFDMR fork of [ovrport/app](https://github.com/ovrport/app), adding optional parallel-eye stereo compatibility for Play For Dream MR.

Credit and thanks to the original OVRPort developers and dependency contributors. This fork is independently maintained and is not endorsed by OVRPort, Play For Dream, Meta, or game developers.

[Deutsche Anleitung](README.de.md) · [Download pre-release](https://github.com/urscaviezel/OVRPort-PFDMR/releases) · [Build from source](docs/BUILD.md) · [Report results](https://github.com/urscaviezel/OVRPort-PFDMR/issues)

## Getting started

1. Download the Windows ZIP from Releases and **extract the entire folder**.
2. Run `OVRPort-PFDMR.exe`. Java is included.
3. Alternatively, use `Start-OVRPort-PFDMR.cmd`.

Windows 10/11 x64. The EXE launcher uses the Windows .NET Framework 4.x. Internet access is used for Overport library discovery/downloads and release checks. As upstream does, this application uses `%USERPROFILE%\overport\cache`, including existing per-application signing keys.

## Applying the optional stereo fix

1. Open an APK you own and select an Overport library version.
2. Enable **PFDMR OpenXR compatibility (minimum)** first. This clears other selections and adds the OpenXR system permission.
3. Then enable **PFDMR Stereo-Fix: parallel eyes (experimental)**.
4. Enable any other patches your application needs, such as copying Overport libraries.
5. Select **Confirm** and save a separate file, e.g. `Game.PFDMR.StereoFix.apk`.
6. Install it on the PFDMR with your usual tools and test in the headset.

Stereo is **off by default**, with no automatic game detection. The suggested filename includes `.PFDMR`; add `.StereoFix` yourself. The minimum PFDMR option is a manifest-only change, not a conversion of every Quest API.

## How it works

The original Batman investigation measured approximately 8° between the reported eye orientations. The wrapper replaces the two orientations returned by `xrLocateViews` with their sign-corrected, normalized quaternion midpoint. **Eye positions, IPD, FOV and image assignment remain unchanged. This remains stereoscopic 3D.**

The wrapper works inside the selected APK. It preserves the original loader as `libpfdmr_original_openxr.so` and forwards the OpenXR entry points. No firmware or system-wide runtime is replaced. Stereo runs after other selected patches so library replacement cannot overwrite it.

## Status and limitations

- The underlying workaround eliminated double images in a private play test of **Batman: Arkham Shadow 1.4.1-350961**. This was not a complete playthrough or confirmation that this public beta works with other applications.
- Automated checks cover APK rebuild/signature verification, repeated patching and original-loader restoration. Native tests use a **mock OpenXR runtime** on a PFDMR, not visual game testing.
- Only ARM64 APKs with a compatible `libopenxr_loader.so` are supported. Missing loaders, unsupported direct OpenXR exports and mixed OpenXR architectures are rejected.
- FOV values are preserved; this is not a full FOV-expansion implementation. Check image edges, depth and head movements per application.
- Other causes of double images may remain. No Android, Linux or macOS patcher builds are released with this beta.

## Comparing or removing the fix

Patch the same input with identical selections except stereo, and keep separate outputs. An APK previously patched by this edition can be reopened: leaving stereo disabled restores the backup. Reapplying stereo does not nest wrappers.

Do not stack this on the older private Batman wrapper (`libmanta_overport.so`); use the input without that older stereo fix.

## Reporting compatibility

Include application version, PFDMR firmware, Overport library version, patch selections, and observations with/without stereo. Check double images, depth, image edges and head movements. **Do not upload APKs, OBBs or private signing keys.**

## Provenance and licensing

Based on [ovrport/app](https://github.com/ovrport/app), commit `1c7cb335c81369fbfd2957ad0edcb238956a8e3c`. PFDMR Edition versions are independent of upstream patcher and runtime-library versions. Earlier v1–v5 were private test labels.

The application is distributed under **GPLv3**; see [LICENSE](LICENSE). Original notices are retained. [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) identifies dependencies, notices and source locations.

The complete application source, PFDMR additions, native wrapper, Windows launcher and build scripts are in this repository. No private v4/v5 binary is required. Releases also provide application and bundled Java runtime source archives.

Use only game files you legally own. No game APKs or private signing keys are included.
