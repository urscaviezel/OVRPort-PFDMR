# Beta 13 local compatibility test

Based on the owner-supplied integration kit and Red Matter handoff dated 2026-11-01. No publication authorized.

## Red Matter 1

Default-off patch_pfdmr_redmatter_signer: Red Matter 1 1.0.013 / versionCode 38, com.VerticalRobot.RedMatter, ARM64 only, exact libUE4.so SHA-256 fd0449158d06fc076d863c4bc6d6a01ed1c2c9268b98be89d750c2352fdc6e61. The profile also selects existing 1800 x 1664 resolution compatibility. Run after engine/library edits.

The profile replaces only two immediate-load words in the documented signer comparison, mapped through PT_LOAD. The following compare, conditional branch, mismatch path and platform behavior stay intact. Expected value is Arrays.hashCode(DER certificate) for the selected output signer, not the contributor's fixed example value. No private key is serialized into the workspace/receipt. Input must have a valid single signer without rotation.

PatcherContext pins the existing per-package SignerConfig before applying this profile and reuses the exact object for output. Pin resets on new input and cleanup. A small receipt stores the original eight bytes, before/after engine digests, public certificate SHA-256 and signature hash. Restore verifies all bytes by original full-engine hash before reapply/deselect. Export verifies the actual final certificate, engine digest and comparison immediates before returning APK bytes; mismatches fail the export.

Initial implementation used reference data before the matching original game APK was provided. Kernel tests use a small synthetic ARM64 ELF with nonidentity VA/offset mapping and retain the production exact-hash guard. Actual APK signers, signer pinning, multi-signer rejection and final output checks are exercised using disposable signed metadata fixtures. Twenty-six signer checks pass. The actual Quest backup was subsequently validated as described below; the owner subsequently confirmed working headset gameplay. Red Matter 2 is not covered.

## The Climb

Default-off patch_pfdmr_climb_stability includes the existing Climb bridge/stereo profile and retains the user-confirmed Beta 12 grip pose. Package/version gating follows the existing Climb 666 profile. The additional APK asset enables an app-owned startup merge of r_MultiThreaded = 0. The setting is experimental; upstream reports improved progression after render/loading-thread crashes, with no proven root cause or long-session performance result.

The helper saves a separate before-stability user.cfg copy only when needed. On a subsequent patch with the base Climb profile retained and stability unchecked, it restores prior r_MultiThreaded lines or removes the key if originally absent. Unrelated current settings and externally edited values are preserved. General preexisting user.cfg backup is not overwritten. Removing all Climb profiles also removes the initializer, so app-data restoration requires running the base Climb profile with stability off.

Fifteen JVM app-init tests cover old behavior, opt-in, backup retention, toggle-off, absent original key, externally edited values and save-data preservation. Eight complete synthetic profile pipelines cover existing profiles plus Climb stability: fresh apply, signed reimport, reapply and deselect. Config-preservation tests (8) and resolution tests (11) pass. Windows package/update verification is recorded in BUILD.md after packaging.

## Actual Quest-backup validation

The owner supplied the 2026-11-01 Quest backup after packaging. Red Matter 1 1.0.013 / code 38 has the exact expected 187,026,672-byte engine and SHA-256 above. Eleven actual-APK checks passed using the exact packaged Beta 13 JAR: full recommended defaults plus signer profile; automatic 1800 x 1664; verified output signature and pinned certificate; changes limited to the eight signer-load bytes; all original game assets byte-identical; signed reimport/same-key reapply; exact original-engine restoration on deselect; fresh-key reapplication and verified output; source APK unchanged. Test keys and outputs are isolated under tests/build/beta13-real-redmatter, not the owner's installation/signing cache. No device installation or launch. Beta 13 binary unchanged. Red Matter 2 backup is version 1.0.021 / code 75 with a different engine digest and is not eligible for this signer profile.

## Release validation

The owner confirmed Red Matter 1 works and reported no further Climb errors in the short test. Extended Climb stability and Red Matter 2 testing remain open. Beta 13 is released as an experimental prerelease; the tested application JAR is unchanged.
