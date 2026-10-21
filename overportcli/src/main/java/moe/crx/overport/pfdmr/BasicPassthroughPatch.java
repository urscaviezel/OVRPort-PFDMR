package moe.crx.overport.pfdmr;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import kotlin.Unit;
import moe.crx.overport.patching.Patch;

/** Correct the basic passthrough capability in one verified ARM64 OverPort loader.
 * Does not enable scene access, change engine gates, or advertise color/depth support.
 */
public final class BasicPassthroughPatch {
    public static final String ID = "patch_pfdmr_basic_passthrough";
    public static final String LABEL = "PFDMR MR: basic passthrough (experimental)";
    private static final String LOADER = "root/lib/arm64-v8a/libopenxr_loader.so";
    private static final String RECEIPT = "root/assets/pfdmr-basic-passthrough.txt";
    private static final String BEFORE = "21821370b886ac25b048836573a68438dc45c0cf9f3e7e6c9f415e28e7578b05";
    private static final String AFTER = "3035befb0d493fea89309b6ec2f79a453a90f9fd467fe425cb43ee06be391c6a";
    private static final String RECORD = "PFDMR_BASIC_PASSTHROUGH_V1\n" + BEFORE + "\n" + AFTER + "\n";
    public static final Patch PATCH = new Patch(ID, false, false, (executor, args) -> {
        executor.selectWorkspace(file -> { apply(file.getFile().toPath()); return Unit.INSTANCE; });
        return Unit.INSTANCE;
    });
    private static IllegalStateException fail(String detail) {
        return new IllegalStateException("PFDMR basic passthrough: " + detail + " Use the original APK or disable this option.");
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    public static void restore(Path workspace) {
        Path receipt = workspace.resolve(RECEIPT), loader = workspace.resolve(LOADER);
        if (!Files.exists(receipt)) return;
        try {
            if (!Files.readString(receipt).equals(RECORD)) throw fail("Unknown rollback receipt.");
            byte[] bytes = Files.readAllBytes(loader);
            String current = hash(bytes);
            // Also recover an interrupted write after the receipt was saved.
            if (!current.equals(BEFORE)) {
                if (!current.equals(AFTER)) throw fail("Loader changed since patching; cannot restore safely.");
                bytes[0x18768] = (byte)0xff;
                if (!hash(bytes).equals(BEFORE)) throw fail("Original loader verification failed.");
                Files.write(loader, bytes);
            }
            Files.delete(receipt);
        } catch (IOException e) { throw fail(e.toString()); }
    }
    public static void apply(Path workspace) {
        restore(workspace);
        Path loader = workspace.resolve(LOADER), receipt = workspace.resolve(RECEIPT);
        try {
            if (!Files.isRegularFile(loader)) throw fail("No supported ARM64 OpenXR loader found.");
            try (var dirs = Files.list(workspace.resolve("root/lib"))) {
                if (dirs.anyMatch(p -> !p.getFileName().toString().equals("arm64-v8a") && Files.exists(p.resolve("libopenxr_loader.so"))))
                    throw fail("Multiple OpenXR architectures are not supported.");
            }
            byte[] bytes = Files.readAllBytes(loader);
            String current = hash(bytes);
            if (current.equals(AFTER)) return; // Already fixed externally: do not claim ownership.
            if (!current.equals(BEFORE)) throw fail("Unsupported OpenXR loader version.");
            // str xzr,[x23,#0x10] -> str x13,[x23,#0x10]; x13 is known to be 1.
            bytes[0x18768] = (byte)0xed;
            if (!hash(bytes).equals(AFTER)) throw fail("Patched loader verification failed.");
            Files.createDirectories(receipt.getParent());
            Files.writeString(receipt, RECORD);
            Files.write(loader, bytes);
        } catch (IOException e) { throw fail(e.toString()); }
    }
}
