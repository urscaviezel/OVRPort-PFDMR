import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import moe.crx.overport.pfdmr.StereoPatch;
import moe.crx.overport.patching.*;
import moe.crx.overport.app.util.PatchLocalizer;

public class StereoTests {
    static int passed;
    static void check(boolean good, String label) {
        if (!good) throw new AssertionError(label);
        System.out.println("PASS " + label); passed++;
    }
    static void rejected(Runnable run, String label) {
        try { run.run(); } catch (IllegalStateException expected) { check(true, label); return; }
        throw new AssertionError("Expected rejection: " + label);
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]); Files.createDirectories(root);
        byte[] original = Files.readAllBytes(Path.of(args[1]));
        var registered = PatchStore.INSTANCE.get(StereoPatch.ID);
        check(registered != null && !registered.isRecommended() && !registered.getRequiresOverportConfig(), "optional patch registered, disabled by default, no unrelated config dependency");
        check(PatchStore.INSTANCE.all().size() == 23, "all 22 v4 patches preserved");
        check(PatchStore.INSTANCE.recommended().stream().noneMatch(p -> p.getName().equals(StereoPatch.ID)), "stereo excluded from defaults");
        var composer = (androidx.compose.runtime.Composer) java.lang.reflect.Proxy.newProxyInstance(
            StereoTests.class.getClassLoader(), new Class<?>[]{androidx.compose.runtime.Composer.class},
            (proxy, method, values) -> method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? 0 : null);
        check(PatchLocalizer.INSTANCE.localizedPatch(registered, composer, 0).equals(StereoPatch.LABEL), "actual UI localizer displays descriptive stereo label");
        check(StereoPatch.exports(original).size() == 56, "reference loader ELF exports parsed");
        rejected(() -> StereoPatch.exports(new byte[100]), "invalid ELF rejected");
        Path ws = root.resolve("workspace"), lib = ws.resolve("root/lib/arm64-v8a"); Files.createDirectories(lib);
        Files.write(lib.resolve(StereoPatch.LOADER), original);
        StereoPatch.apply(ws);
        byte[] wrapper = Files.readAllBytes(lib.resolve(StereoPatch.LOADER));
        check(!Arrays.equals(wrapper, original), "wrapper installed");
        check(Arrays.equals(Files.readAllBytes(lib.resolve(StereoPatch.BACKUP)), original), "original loader preserved byte for byte");
        check(StereoPatch.exports(wrapper).containsAll(StereoPatch.exports(original)), "all original direct OpenXR exports preserved");
        StereoPatch.apply(ws);
        check(Arrays.equals(Files.readAllBytes(lib.resolve(StereoPatch.BACKUP)), original), "repeat patch does not nest wrappers");
        StereoPatch.restore(ws);
        check(Arrays.equals(Files.readAllBytes(lib.resolve(StereoPatch.LOADER)), original) && !Files.exists(lib.resolve(StereoPatch.BACKUP)), "restore returns exact original loader");
        check(!Files.exists(ws.resolve("root/assets/pfdmr-stereo-fix.txt")), "restore removes stereo receipt");
        Path foreign = ws.resolve("root/lib/armeabi-v7a"); Files.createDirectories(foreign);
        Files.write(foreign.resolve(StereoPatch.LOADER), original);
        rejected(() -> StereoPatch.apply(ws), "mixed OpenXR ABIs rejected");
        check(Arrays.equals(Files.readAllBytes(lib.resolve(StereoPatch.LOADER)), original), "rejection leaves loader unchanged");
        Files.delete(foreign.resolve(StereoPatch.LOADER));
        Files.write(lib.resolve("libmanta_overport.so"), original);
        rejected(() -> StereoPatch.apply(ws), "old Batman wrapper rejected with actionable message");
        Files.delete(lib.resolve("libmanta_overport.so"));
        Files.write(lib.resolve(StereoPatch.BACKUP), original);
        rejected(() -> StereoPatch.apply(ws), "backup collision rejected");
        Files.delete(lib.resolve(StereoPatch.BACKUP));
        rejected(() -> StereoPatch.apply(root.resolve("no-loader")), "missing loader rejected");

        // Real v4 decode -> selected patch pipeline -> APK build/sign/verify.
        Path cache = root.resolve("cache"); Files.createDirectories(cache);
        PatcherContext ctx = new PatcherContext("3.4.3-aa54c3f", cache.toFile(), "Manta.apk", "{filename}.StereoFix.{ext}");
        try (var in = Files.newInputStream(Path.of(args[2]))) { ctx.prepare(in); }
        Map<String,List<String>> options = new LinkedHashMap<>();
        options.put(StereoPatch.ID, List.of()); options.put("patch_pfdmr_openxr_permission", List.of());
        var ordered = StereoPatch.prepare(ctx, options);
        check(new ArrayList<>(ordered.keySet()).get(ordered.size()-1).equals(StereoPatch.ID), "stereo always executes last regardless of selection order");
        List<String> ran = new ArrayList<>();
        ctx.patch(options, (i, patch) -> { ran.add(patch.getName()); return kotlin.Unit.INSTANCE; });
        check(ran.get(ran.size()-1).equals(StereoPatch.ID), "integrated v4 pipeline runs stereo last");
        Path apk = root.resolve("roundtrip-stereo.apk");
        try (var out = Files.newOutputStream(apk)) { ctx.export(out); }
        try (ZipFile z = new ZipFile(apk.toFile())) {
            check(Arrays.equals(z.getInputStream(z.getEntry("lib/arm64-v8a/"+StereoPatch.LOADER)).readAllBytes(), wrapper), "exported APK contains stereo wrapper");
            check(Arrays.equals(z.getInputStream(z.getEntry("lib/arm64-v8a/"+StereoPatch.BACKUP)).readAllBytes(), original), "exported APK contains exact original loader backup");
            check(z.getEntry("assets/pfdmr-stereo-fix.txt") != null, "exported APK carries stereo receipt");
        }
        var verify = new com.android.apksig.ApkVerifier.Builder(apk.toFile()).build().verify();
        check(verify.isVerified(), "exported APK signature verifies");
        ctx.cleanup();
        try (var in = Files.newInputStream(apk)) { ctx.prepare(in); }
        ctx.patch(Map.of("patch_pfdmr_openxr_permission", List.of()), (i, patch) -> kotlin.Unit.INSTANCE);
        Path off = root.resolve("roundtrip-without-stereo.apk");
        try (var out = Files.newOutputStream(off)) { ctx.export(out); }
        try (ZipFile z = new ZipFile(off.toFile())) {
            check(Arrays.equals(z.getInputStream(z.getEntry("lib/arm64-v8a/"+StereoPatch.LOADER)).readAllBytes(), original), "unchecking stereo restores original in exported APK");
            check(z.getEntry("lib/arm64-v8a/"+StereoPatch.BACKUP) == null, "disabled export contains no wrapper backup");
            check(z.getEntry("assets/pfdmr-stereo-fix.txt") == null, "disabled export contains no active stereo receipt");
        }
        check(new com.android.apksig.ApkVerifier.Builder(off.toFile()).build().verify().isVerified(), "disabled APK signature verifies");
        ctx.cleanup();
        System.out.println("ALL " + passed + " CHECKS PASSED");
    }
}
