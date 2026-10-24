# Beta 10 compatibility review

Date: 2026-10-24. Analysis only; no implementation, version bump or release.

## Recommendation

Add two optional, default-off compatibility profiles: Vader Immortal audio mixer and Spatial Zombies. Share the known-build MRUK valid-pose patch implementation with Pinball internally, while retaining separate application profiles. Do not enable game-specific asset, input or settings changes globally.

## Vader Immortal

Evidence root: `C:/Users/cavi/Documents/Codex/2026-10-24/inzwischen-startet-vader-immortal-ja-ich`.
Sources: `outputs/Vader-Dojo-Fix-Bericht.txt`, `outputs/Vader-Dojo-Fix-Manifest.json`, `work/audio-fix/build-fixed-apks.py`, `work/audio-fix/test-mixer.c`, `work/audio-fix/native-test-results.txt`.

The constant-gain SIMD audio mixer processes 32-float blocks without correctly handling the remainder. At 1008 samples it accesses 16 extra floats (64 bytes). The fix redirects execution into the existing scalar path, which respects the sample count. This is independent of the existing 48-kHz configuration patch.

Independent ZIP payload comparison of the supplied before/after APKs confirms only the expected four bytes in `lib/arm64-v8a/libUE4.so` changed in each episode, excluding signature metadata. No `libdiag.so` is included. The original 372-byte function is identical and unique in each library, SHA-256 `823ec881d767c32e73e28ea4bfde03c8538bb162de5f12278aca67955bf5a0aa`.

| Package | Version / code | Instruction offset |
| --- | --- | --- |
| com.ILMxLAB.VaderImmortal.ep1 | 1.1.1+667256.cl.387770 / 387770 | 0x58cf0f8 |
| com.ILMxLAB.VaderImmortal.ep2 | 2.0.3+667261.cl.387778 / 387778 | 0x5ae2e38 |
| com.ILMxLAB.VaderImmortal.ep3 | 3.0.3+667263.cl.387932 / 387932 | 0x5b55348 |

Instruction bytes: `c1 08 00 54` to `46 00 00 14` (conditional to unconditional branch to scalar path). Require known package/version, executable ARM64 ELF range and the complete unique function signature; never patch by offset or four bytes alone. Existing Quest/shader modifications mean an original whole-library hash alone is not an appropriate post-patching gate.

Use reversible receipts and reverse composition order: if the mixer patch follows QuestGatePatch, restore the mixer before QuestGatePatch checks its whole-file receipt. Preserve independently prepatched inputs.

Recorded native tests: 345 checks, zero failures, including overflow reproduction, output reference checks, gain variants, aliasing, sample lengths and guard pages. These tests were inspected, not rerun in this review. The recorded isolated mixer benchmark is approximately 8.6 times slower on the scalar path; this is not a whole-game performance measurement. The user reports the bug fixed, but this does not establish gameplay coverage of all three episodes.

## Spatial Zombies

Evidence root: `C:/AI-Projects/Spatial-Zombies`. Primary sources: `WORKING-BASELINE.txt`, `analysis/build_detached_off.py`, `analysis/mr/build_permission.py`, `analysis/mr/build_room.py`, `analysis/build_valid_pose.py`, `analysis/room-preview/build.py`, `analysis/settings-resume/PfdmrSettingsBridge.java`, and the corresponding findings/patch reports.

Confirmed package: `com.mrf.TomFoerster.ZombiesMR`, version 1.2.8.5, code 154. Working APK: `com.mrf.tomfoerster.zombiesmr.PFDMR.settings-resume.apk`, SHA-256 `27482ae6e74adffb99a107e094f9952cb141c947656b8906fd57708c93af7e8e` (independently checked).

The working profile combines:

1. Disable only the three Unity detached controller profiles (Touch Plus, Touch Pro, Oculus Touch). This removes unsupported action paths; ordinary controller profiles remain. Controller pitch was adjusted in the game's settings, so do not add Pinball's automatic grip rotation.
2. Translate the IL2CPP scene-permission literal to `com.yvr.permission.USE_SCENE` and add the corresponding manifest permission. Existing Java-only permission adaptations do not cover this Unity call path.
3. Reuse the existing MR dispatch and YVR scene adapters. Their bytes in the working APK match the repository resources exactly.
4. Accept valid MRUK anchor poses without demanding the additional TRACKED bits. In this build, `FillLocatableData` changes `tst x8,#15` to `tst x8,#3` at file offset 0x23faf4; success checking remains. Before/after library SHA-256: `0a41fd207e149da64826bb9134ce05a3cb0d95dc2d091fd3b43e439321cc8961` / `aaec9e296488201bb54babaac481b2a0f2829b739bd407ac8489abf125ef2dd7`. Share known-build matching and reversible patch logic with Pinball; do not globally fabricate tracked flags.
5. Change exactly the two room-preview materials in `sharedassets0.assets`: RoomOutlineActive (163) and RoomOutlineInactive (164). Reuse shader 1209, disable culling/depth writes, use always-pass depth testing and suitable queues/grid colors. The working experiment changes these together; it does not isolate one shader setting as the sole cause.
6. Adapt the AppPermissions settings route and preserve return to the game. The working Java bridge uses the PFDM strategy service's focus-aware transition, then routes Spatial Home to native Spatial Data settings. Three additional IL2CPP settings literals change. Keep this firmware-specific behavior scoped to the profile, with a normal settings fallback; avoid launching the previous explicit Spatial Home activity that caused display/input problems. Delayed widget navigation needs lifecycle/debounce handling during integration.

The working APK still contains front/scene tracing wrappers: `libopenxr_loader.so` and `libpfdmr_mr_original_yvr.so` are diagnostic layers, with the real adapters/original loader under `libspatial_raw_dispatch.so` and `libspatial_raw_yvr.so`. Reconstruct the functional chain from repository resources and original payloads; do not ship these diagnostic layers as part of the profile.

The baseline reports working controllers, visible/selectable room previews, MR gameplay, interactive native Spatial Data settings and immediate return to game input. Long-session/full-feature coverage is not established. No new device installs or runtime tests were performed in this review.

## Integration checks

- Preflight all required Unity object, metadata and native signatures before changing anything; reject unsupported versions clearly.
- Keep profile changes transactional and reversible, including already-patched inputs and composition with MR, refresh and existing engine patches.
- Preserve metadata literals and Unity objects outside the explicit allowlist; verify payload diffs.
- Test fresh patch/install flows, room selection, permission denial/grant, settings return and controller behavior on the headset after implementation.
- Keep patcher artifacts in this repository; do not publish Beta 10 until authorized.
