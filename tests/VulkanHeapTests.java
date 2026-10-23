import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;

public class VulkanHeapTests {
    static int passed;
    static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); System.out.println("PASS " + name); passed++; }
    static void reject(Runnable action, String name) { try { action.run(); } catch (IllegalStateException expected) { check(expected.getMessage().contains("Vulkan fix"), name); return; } throw new AssertionError(name); }
    static byte[] engine(Path apk) throws Exception { try (var zip = new ZipFile(apk.toFile())) { return zip.getInputStream(zip.getEntry("lib/arm64-v8a/libUE4.so")).readAllBytes(); } }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]), input = Path.of(args[1]);
        byte[] original = engine(input), expected = engine(Path.of(args[2]));
        Path ws = root.resolve("direct"), lib = ws.resolve("root/lib/arm64-v8a/libUE4.so");
        Files.createDirectories(lib.getParent()); Files.write(lib, original);
        check(PatchStore.INSTANCE.recommended().contains(VulkanHeapPatch.PATCH), "enabled in GUI and CLI defaults");
        check(!VulkanHeapPatch.apply(ws, "other.game") && Arrays.equals(original, Files.readAllBytes(lib)), "other games unchanged");
        check(VulkanHeapPatch.apply(ws, "com.Drifter.Kodiak") && Arrays.equals(expected, Files.readAllBytes(lib)), "byte-identical to HMD-tested engine");
        VulkanHeapPatch.apply(ws, "com.Drifter.Kodiak"); check(Arrays.equals(expected, Files.readAllBytes(lib)), "idempotent");
        VulkanHeapPatch.restore(ws); check(Arrays.equals(original, Files.readAllBytes(lib)), "deselect restores exactly");
        Files.write(lib, expected); VulkanHeapPatch.apply(ws, "com.Drifter.Kodiak"); VulkanHeapPatch.restore(ws);
        check(Arrays.equals(expected, Files.readAllBytes(lib)), "preexisting manual patch preserved");
        byte[] modified = original.clone(); modified[100] ^= 1; Files.write(lib, modified);
        reject(() -> VulkanHeapPatch.apply(ws, "com.Drifter.Kodiak"), "unknown build rejected");
        check(Arrays.equals(modified, Files.readAllBytes(lib)), "rejection leaves input intact");
        Files.write(lib, original); VulkanHeapPatch.apply(ws, "com.Drifter.Kodiak");
        byte[] changed = expected.clone(); changed[100] ^= 1; Files.write(lib, changed);
        reject(() -> VulkanHeapPatch.restore(ws), "changed patched library blocks unsafe restoration");
        var ctx = new PatcherContext("3.4.3-aa54c3f", root.resolve("cache").toFile(), "LiesBeneath.apk", "{filename}.{ext}");
        try (var in = Files.newInputStream(input)) { ctx.prepare(in); }
        ctx.patch(Map.of(VulkanHeapPatch.ID, List.of()), (i,p) -> kotlin.Unit.INSTANCE);
        Path output = root.resolve("overport-heapfix.apk");
        try (var out = Files.newOutputStream(output)) { ctx.export(out); }
        check(Arrays.equals(expected, engine(output)), "real OverPort decode-patch-export matches HMD-tested fix");
        check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(), "exported APK signature valid");
        ctx.cleanup(); try (var in = Files.newInputStream(output)) { ctx.prepare(in); }
        ctx.patch(Map.of(), (i,p) -> kotlin.Unit.INSTANCE);
        Path disabled = root.resolve("overport-disabled.apk");
        try (var out = Files.newOutputStream(disabled)) { ctx.export(out); }
        check(Arrays.equals(original, engine(disabled)), "deselect works through full pipeline");
        ctx.cleanup(); System.out.println("ALL " + passed + " VULKAN HEAP CHECKS PASSED");
    }
}
