# Changelog

## 0.1.0-beta.2-audio.1 — local test, 2026-10-13

- Optional Unreal 48 kHz audio switch, disabled by default; independent of Meta XR Audio and stereo correction.
- Detect the Unreal project and insert an app-local INI helper before native GameActivity startup. Preserve unrelated settings and back up the original configuration.
- Repatching the audio test APK with the switch off restores the original sample-rate entries on the next game launch.
- Local test build only; not published. In-headset validation of this APK integration is pending.

## 0.1.0-beta.1 — 2026-10-11

First public experimental PFDMR Edition release.

- Minimum PFDMR OpenXR manifest compatibility, retaining the internal editions' selection-reset behavior.
- Optional parallel-eye stereo fix, disabled by default, preserving eye positions/IPD and FOV.
- Keep the original loader for restoration; prevent nested wrappers and apply stereo last.
- Validate ARM64 loader exports and reject incompatible inputs before loader replacement.
- Retain upstream patch options and the signing-key workflow.
- Publish full application/native/launcher source, build instructions, notices and validation details.

Previous v1–v5 names were private test labels. The private Batman test supports the underlying workaround; wider visual compatibility remains unconfirmed.
