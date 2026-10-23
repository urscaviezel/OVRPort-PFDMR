# Pinball FX VR 1.10 integration review

Date: 2026-10-24. Initial review snapshot. The separate local Beta 9 implementation and validation are documented in BUILD.md; nothing has been published.

## Evidence and scope

Read the current v20 handoff, confirmed-v20-controller source snapshot, MRUK patch script, retained runtime logs and existing native/mr adapters. REA inspected the confirmed APK: ev_cfbe57175162d29f58d32d128f75f0d942664a29018759c3021150a2ca0833f9. Package com.zenstudios.PFXVRQuest, version 1.10/code 7380, manifest engine 5.6.1. Confirmed APK SHA-256 3389c916ff70d1756d9dc3fa0fa7a667f243b8d8c5bf2973e7514b3abedd28af. REA metadata is static, not a fresh runtime test; working gameplay is user-reported.

Compared the base basic+scene/spatial APK against v20. Outside signing entries, ZIP inventory/CRC comparison identified changes only to the top OpenXR loader and MRUK, plus added libpfx_base.so. Byte comparison confirmed that this backup equals the original top loader. Full hashes confirm libUnreal.so and libOVRPlugin.so remain unchanged on disk. Both targeted Unreal hook prologs match the handoff. MRUK differs by one byte at file offset 0x233375 (0x0d to 0x05), implementing the documented instruction change.

Original library identities:
- Unreal: 6b383c0e833d39d2084cec12662695b925fa7729846aa7b131abed85fdfb7228
- OVRPlugin: 4618643a0eb6712a4dd8e559afbb136bc5ef736b28fa3a59b78264e4170aab6a
- MRUK: fc2df0e83599bbd932680fae2859c06e189c8d6f18cc96babd605eed65897eb8
- Patched MRUK: 38ff3719a3d5abc81c9f6c5bdfd9bfce2df67895974e8bbad9c9eacb02f26f04

## Changes and integration boundaries

1. VR projection: clear source-alpha/unpremultiplied-alpha flags on copied projection layers only when no passthrough layer is submitted.
2. MR projection: invert the existing alpha scale/bias on copied structures only with submitted passthrough; preserve RGB and the remaining next-chain. Do not apply this globally.
3. MRUK anchors: accept both VALID pose bits without requiring TRACKED, retaining successful xrLocateSpace checks. Do not fabricate tracking flags at the API boundary. This consumer-side correction is a candidate for other matching MRUK builds, but is not yet validated for other games.
4. VR return height: temporarily suppress the MRUK world-lock field during Tick when no passthrough was submitted, then restore it. This requires the exact Unreal layout and guarded runtime hook at VA 0x5cf5500; tracking-origin changes were too late in the recorded experiment.
5. DLC crash prevention: guard only the identified task class/status combination in GetTableDownloadRatio at VA 0x6076644. Return zero progress instead of the invalid virtual call, preserving other statuses and original execution. This neither grants purchases nor makes unavailable DLC playable. The precise reason for remaining DLC failures is unresolved.
6. Controller grip: track the relevant Touch grip-pose actions and postmultiply their action-space orientation by local X +90 degrees, preserving position and head pose. This is game/profile-specific; neither global controller-offset disabling nor grip-to-aim replacement reproduced the fix.

Beta 8 scene permission/discovery/buffer/passthrough gating remains the foundation. Its current EndFrame path does not perform the new projection corrections, and its binding forwarding does not apply the new grip orientation. The v20 additions therefore do not imply that all existing spatial behavior should be replaced.

## Proposed patcher design

Add a narrowly recognized PinballFX 1.10 compatibility profile, keeping the existing optional scene/spatial adapter intact. A dedicated game-compatibility option can package all six verified adjustments; identify package, version and library hashes before applying. Do not silently make global rendering or controller changes under the general spatial switch. Preserve v1.9 and other apps. Refuse unknown variants of the selected profile, preserve originals and support removal/repatching. Next test version is beta 9 because beta 8 is published.

Keep the new layer above the existing spatial chain at the same API boundary as the tested shim; integrate deliberately with stereo/refresh wrappers, direct exports and resolver lookups. Scope runtime hooks to the verified libraries and expected prologs, make installation one-time and safe against concurrent calls, and handle partial installation failures. The test trampoline copies only the verified prologs and is not a general relocator.

Production work required before distribution: remove logging-only hooks and debug property dependencies; replace fixed eight-action and sixteen-layer limits; track action/action-set/instance/session destruction and handle reuse; define safe behavior for unfamiliar extension chains without mutating caller memory; reset passthrough state on session transitions. Preserve functional action recognition and the two corrective Unreal hooks while removing diagnostics.

Required validation: focused native tests for alpha/chain copying and untouched input, pose-validity combinations, VR/MR world-lock gating, guarded/unguarded task states, grip quaternion composition and lifecycle/handle reuse; APK roundtrip/rollback plus existing MR/stereo/refresh regression checks. Then fresh user patch/install and HMD tests of VR, MR, controllers, repeated transitions, height, restart and unavailable-table attempts. No new headset actions were performed in this review.
