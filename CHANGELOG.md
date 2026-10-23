# Changelog

## 0.1.0-beta.8

- Add optional HMD refresh-rate preference using the saved headset setting; leave SpaceWarp unchanged.
- Add Vulkan heap compatibility for a verified engine build.
- Prevent multiple desktop instances sharing the same workspace.
- Release exported APK files immediately and close archives before cleanup.

## 0.1.0-beta.7

- Check for new GitHub releases and update the Windows app in place after confirmation.
- Verify downloads, preserve user data, and restore the previous files if installation fails.

- Add optional basic passthrough capability correction for a verified ARM64 loader.
- Keep scene and spatial compatibility as a separate, combinable option.
- Restore the minimal loader edit when deselected, including combined MR/stereo repatching.

## 0.1.0-beta.6

- Keep MR compatibility active when applications reopen OpenXR loaders or query the resolver again.
- Add an Android loader-identity regression test.

## 0.1.0-beta.5

- Add optional MR passthrough, scene-permission and room-query compatibility, disabled by default.
- Preserve original loaders and permission code for reversible repatching.

## 0.1.0-beta.4

- Recognize legacy MAIN/INFO launch entries and add the missing PFDMR VR entry.
- Include this startup correction with the optional Unreal Quest fix.

## 0.1.0-beta.3

- Optional legacy Unreal Quest startup and controller compatibility fix, disabled by default.
- Validates both native code sites before patching; supports restoring its own edits when deselected.


## 0.1.0-beta.2 — 2026-10-13

- Add optional Unreal 48 kHz audio correction, disabled by default.
- Preserve existing settings and support restoration of the previous sample rate.
- Shorten release documentation; compatibility information is maintained separately on PFDhub.

## 0.1.0-beta.1 — 2026-10-11

- First public experimental PFDMR Edition release.
- PFDMR OpenXR compatibility and optional parallel-eye stereo correction.
- Retain upstream patch options, original attribution and GPLv3 source availability.
