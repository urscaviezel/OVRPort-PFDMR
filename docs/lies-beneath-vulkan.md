# Vulkan heap compatibility

The recommended option `patch_pfdmr_vulkan_heap` appears in the GUI as **PFDMR Vulkan: missing memory heaps (verified games)**. GUI and CLI defaults enable it. Explicit CLI patch lists must include this ID.

Currently supported: `com.Drifter.Kodiak`, Lies Beneath 1.0-55578, ARM64. The full libUE4.so SHA-256 is pinned; unknown libraries for this package are rejected without modification. Other packages are left unchanged. This is a verified-build fix, not a universal Vulkan override. Add separately analyzed and tested profiles for other affected games.

The original engine selects a compatible Vulkan memory type but has no FOldResourceHeap for it. The 72-byte patch calls the existing allocator and constructor, records the heap, preserves memory-type bits/property requirements, and rejoins normal allocation. The verified original hash is `56b5f312acde60f4eb29c7a68f7a9bfe9ae7323776c40b89bef32640352ccf22`; patched hash is `7039aa9b53a71de33c3c1fad62ab2a5d3bf37092075e93fd6d688960571e34f0`.

A receipt allows deselection to restore only this option's own changes. Already manually repaired input is preserved. Modified receipt-bearing libraries stop restoration instead of overwriting subsequent changes.

On-device validation: PFDM MR / Android 14 reaches CLICK TO BEGIN and continuously submits Vulkan VR frames. Gameplay, controllers and audio have not been fully validated. Regression test: `tests/VulkanHeapTests.java`; supply local original and headset-tested fixed APKs as fixtures. No game binaries or signing keys are distributed.
