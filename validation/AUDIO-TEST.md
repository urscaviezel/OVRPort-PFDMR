# Local audio test build: 0.1.0-beta.2-audio.1

2026-10-13. Not a published release. No app was installed or changed on the headset during this build.

- 15 audio configuration checks passed: 48 kHz, preservation of other sections/settings, BOM/CRLF, backup, repeat execution, rollback, fresh config, duplicate keys, path validation and unsupported encoding.
- 15 patch integration checks passed against Action Hero 1.00.109160: disabled default, GUI label, startup hook before native initialization, project path, valid rebuilt APK signatures, repeated patching, unchanged Unreal library/command line and rollback mode when deselected.
- The helper is compiled from Java source with Android API 34 stubs, translated by D8 (minimum API 26), then disassembled into smali for the existing APK rebuild pipeline.
- Native/in-headset audio playback with the new startup integration still requires the user's test. The previous manual Engine.ini setting was confirmed effective in Action Hero; it must not mask this test.

See docs/AUDIO-TEST.de.md for test and restoration instructions. Test APKs use an isolated disposable signing key and are not included in the distribution.
