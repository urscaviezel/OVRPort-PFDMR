package moe.crx.overport.pfdmr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import kotlin.Unit;
import moe.crx.overport.patching.Patch;

/** Automatic compatibility fix for the headset-verified Lies Beneath build only.
 * Uses the existing allocator and FOldResourceHeap constructor for a valid memory
 * type without a heap. Does not weaken Vulkan memory-type/property requirements.
 * The full-library digest pins all PC-relative calls and the ARM64 ABI assumptions.
 */
public final class VulkanHeapPatch {
    public static final String ID = "patch_pfdmr_vulkan_heap";
    public static final String LABEL = "PFDMR Vulkan: missing memory heaps (verified games)";
    private static final String RECEIPT = "root/assets/pfdmr-vulkan-heap.txt";
    private static final String MARKER = "PFDMR_VULKAN_HEAP_KODIAK_55578_V1\n";
    public static final Patch PATCH = new Patch(ID, true, false, (executor, args) -> {
        executor.selectWorkspace(file -> { apply(file.getFile().toPath(), executor.applicationPackage()); return Unit.INSTANCE; });
        return Unit.INSTANCE;
    });
    private static final String PACKAGE = "com.Drifter.Kodiak";
    private static final String BEFORE_SHA = "56b5f312acde60f4eb29c7a68f7a9bfe9ae7323776c40b89bef32640352ccf22";
    private static final String AFTER_SHA = "7039aa9b53a71de33c3c1fad62ab2a5d3bf37092075e93fd6d688960571e34f0";
    private static final int OFFSET = 0x3877808;
    private static final byte[] BEFORE = HexFormat.of().parseHex("ca0a40f9ba1240b9f7761b12480140b97a02003448020034f9031faae903182a4a110091eb031a2aeb0000363f0119eba00000544c0140b98c01170a9f01176be008005439070091");
    private static final byte[] AFTER = HexFormat.of().parseHex("000e80524d6bfd97fb0300aae10316aae203182a0340a05260faff97c80a40f9084d388b080540b9090d0353693300390911045369370039c80e40f91bd938f8f903182a8b000014");

    private VulkanHeapPatch() {}

    private static IllegalStateException unsupported(String detail) {
        return new IllegalStateException("Lies Beneath Vulkan fix: " + detail
                + " Only the verified ARM64 build 1.0-55578 is supported; no library changes were written.");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Returns true for both newly repaired and already repaired supported input. */
    public static boolean apply(Path workspace, String packageName) {
        if (!PACKAGE.equals(packageName)) return false;
        Path library = workspace.resolve("root/lib/arm64-v8a/libUE4.so");
        if (!Files.isRegularFile(library)) throw unsupported("Expected lib/arm64-v8a/libUE4.so.");
        try {
            byte[] bytes = Files.readAllBytes(library);
            String digest = sha256(bytes);
            if (AFTER_SHA.equals(digest)) return true;
            if (!BEFORE_SHA.equals(digest)) throw unsupported("Unrecognized or modified engine library.");
            if (!Arrays.equals(BEFORE, Arrays.copyOfRange(bytes, OFFSET, OFFSET + BEFORE.length)))
                throw unsupported("Unexpected instructions at the patch site.");
            System.arraycopy(AFTER, 0, bytes, OFFSET, AFTER.length);
            if (!AFTER_SHA.equals(sha256(bytes))) throw unsupported("Patched library validation failed.");
            Path receipt = workspace.resolve(RECEIPT);
            Files.createDirectories(receipt.getParent());
            Files.writeString(receipt, MARKER);
            Files.write(library, bytes);
            return true;
        } catch (IOException error) {
            throw new IllegalStateException("Lies Beneath Vulkan fix: Cannot read or write the engine library.", error);
        }
    }
    /** Restore only edits owned by this option; preserve previously patched input. */
    public static void restore(Path workspace) {
        Path receipt = workspace.resolve(RECEIPT);
        if (!Files.exists(receipt)) return;
        Path library = workspace.resolve("root/lib/arm64-v8a/libUE4.so");
        try {
            if (!MARKER.equals(Files.readString(receipt))) throw unsupported("Invalid patch receipt.");
            byte[] bytes = Files.readAllBytes(library);
            if (!AFTER_SHA.equals(sha256(bytes))) throw unsupported("Engine changed after patching; cannot restore safely.");
            System.arraycopy(BEFORE, 0, bytes, OFFSET, BEFORE.length);
            if (!BEFORE_SHA.equals(sha256(bytes))) throw unsupported("Restoration validation failed.");
            Files.write(library, bytes);
            Files.delete(receipt);
        } catch (IOException error) {
            throw new IllegalStateException("Lies Beneath Vulkan fix: Cannot restore engine library.", error);
        }
    }

}
