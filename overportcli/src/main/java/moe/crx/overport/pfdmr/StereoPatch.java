package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import kotlin.Unit;
import moe.crx.overport.patching.*;

/** Optional, application-local parallel-eye compatibility for PFDMR. */
public final class StereoPatch {
    public static final String ID = "patch_pfdmr_parallel_stereo";
    public static final String LABEL = "PFDMR Stereo-Fix: parallel eyes (experimental)";
    public static final String LOADER = "libopenxr_loader.so";
    public static final String BACKUP = "libpfdmr_original_openxr.so";
    public static final String MARKER = "PFDMR_PARALLEL_STEREO_V1";
    public static final Patch PATCH = new Patch(ID, false, false, (executor, args) -> {
        executor.selectWorkspace(file -> {
            apply(file.getFile().toPath());
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    });

    public static String label(String name) { return ID.equals(name) ? LABEL : name; }

    public static void register(Map<String, Patch> patches) {
        if (patches.containsKey(ID)) throw new IllegalStateException("Stereo patch already registered");
        Map<String, Patch> ordered = new LinkedHashMap<>();
        ordered.put(ID, PATCH); // v4 pins its PFDMR base option first; stereo follows it.
        for (var entry : patches.entrySet()) {
            ordered.put(entry.getKey(), entry.getValue());
        }
        patches.clear();
        patches.putAll(ordered);
    }

    // Called before the v4 patch pipeline. Restore our input wrapper first, then
    // run the stereo patch LAST so any selected library replacement has finished.
    public static Map<String, List<String>> prepare(PatcherContext context, Map<String, List<String>> args) {
        restore(context.getWorkingDir().toPath());
        Map<String, List<String>> ordered = new LinkedHashMap<>(args);
        if (ordered.containsKey(ID)) {
            List<String> options = ordered.remove(ID);
            ordered.put(ID, options);
        }
        return ordered;
    }

    private static IllegalStateException failure(String detail) {
        return new IllegalStateException("PFDMR Stereo-Fix: " + detail);
    }
    private static byte[] read(Path path) throws IOException { return Files.readAllBytes(path); }
    private static boolean marker(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1).contains(MARKER);
    }
    private static byte[] resource() throws IOException {
        try (InputStream in = StereoPatch.class.getResourceAsStream("/pfdmr/arm64-v8a/" + LOADER)) {
            if (in == null) throw failure("Die mitgelieferte Stereo-Bibliothek fehlt.");
            return in.readAllBytes();
        }
    }

    public static void restore(Path workspace) {
        Path lib = workspace.resolve("root/lib/arm64-v8a");
        Path loader = lib.resolve(LOADER), backup = lib.resolve(BACKUP);
        try {
            if (Files.isRegularFile(loader) && marker(read(loader))) {
                if (!Files.isRegularFile(backup) || marker(read(backup)))
                    throw failure("Original-Loader fehlt oder ist ungueltig. Bitte die urspruengliche APK verwenden.");
                exports(read(backup));
                Files.copy(backup, loader, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(backup);
                Files.deleteIfExists(workspace.resolve("root/assets/pfdmr-stereo-fix.txt"));
            } else if (Files.exists(backup)) {
                throw failure("Unerwartete Loader-Sicherung gefunden. Bitte die urspruengliche APK verwenden.");
            }
        } catch (IOException e) { throw failure(e.toString()); }
    }

    public static void apply(Path workspace) {
        Path libs = workspace.resolve("root/lib");
        Path lib = libs.resolve("arm64-v8a");
        Path loader = lib.resolve(LOADER), backup = lib.resolve(BACKUP);
        try {
            if (!Files.isRegularFile(loader))
                throw failure("Kein ARM64-OpenXR-Loader vorhanden. Diese APK wird vom Stereo-Fix nicht unterstuetzt.");
            try (var dirs = Files.list(libs)) {
                if (dirs.anyMatch(p -> !p.equals(lib) && Files.isRegularFile(p.resolve(LOADER))))
                    throw failure("Mehrere OpenXR-Architekturen gefunden. Diese Testversion unterstuetzt nur ARM64-OpenXR.");
            }
            if (Files.exists(lib.resolve("libmanta_overport.so")))
                throw failure("Batman-Testfix bereits vorhanden. Bitte die APK ohne alten Stereo-Fix verwenden.");
            byte[] wrapper = resource();
            byte[] original = read(loader);
            if (marker(original)) {
                // Also safe when invoked independently from the patch pipeline.
                if (!Files.isRegularFile(backup) || marker(read(backup))) throw failure("Ungueltige Loader-Sicherung.");
                original = read(backup);
            } else if (Files.exists(backup)) throw failure("Der Name der Loader-Sicherung ist bereits belegt.");
            Set<String> originalExports = exports(original);
            if (!originalExports.containsAll(Set.of("xrGetInstanceProcAddr", "xrLocateViews", "xrEndFrame")))
                throw failure("Der OpenXR-Loader besitzt nicht die erforderlichen Funktionen.");
            Set<String> missing = new TreeSet<>(originalExports);
            missing.removeAll(exports(wrapper));
            if (!missing.isEmpty()) throw failure("Nicht unterstuetzte direkte OpenXR-Exporte: " + missing);

            // Validation completed before any changes to the loader.
            if (!Files.exists(backup)) Files.write(backup, original, StandardOpenOption.CREATE_NEW);
            Files.write(loader, wrapper);
            Path receipt = workspace.resolve("root/assets/pfdmr-stereo-fix.txt");
            Files.createDirectories(receipt.getParent());
            Files.writeString(receipt, "PFDMR Stereo-Fix 0.1.0-beta.1 experimental\nMode: parallel eye orientations\n"
                + "Eye positions and FOV unchanged. ARM64 only.\nOriginal loader SHA-256: " + sha256(original) + "\n");
        } catch (IOException e) { throw failure(e.toString()); }
    }

    public static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    // Read defined public xr* symbols from the ELF64 dynamic symbol table.
    // Reject truncated/unsupported inputs rather than publishing a broken APK.
    public static Set<String> exports(byte[] bytes) {
        try {
            ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if (bytes.length < 64 || b.getInt(0) != 0x464c457f || b.get(4) != 2 || b.get(5) != 1
                || b.getShort(18) != 183) throw failure("Kein gueltiger ARM64-ELF-Loader.");
            int start = Math.toIntExact(b.getLong(40));
            int size = Short.toUnsignedInt(b.getShort(58)), count = Short.toUnsignedInt(b.getShort(60));
            if (size < 64 || count == 0) throw failure("ELF-Sektionstabelle fehlt.");
            Set<String> result = new TreeSet<>();
            for (int i = 0; i < count; i++) {
                int s = Math.addExact(start, Math.multiplyExact(i, size));
                if (b.getInt(s + 4) != 11) continue;
                int strings = Math.addExact(start, Math.multiplyExact(b.getInt(s + 40), size));
                int strOffset = Math.toIntExact(b.getLong(strings + 24));
                int strSize = Math.toIntExact(b.getLong(strings + 32));
                int offset = Math.toIntExact(b.getLong(s + 24));
                int length = Math.toIntExact(b.getLong(s + 32));
                int stride = Math.toIntExact(b.getLong(s + 56));
                if (stride < 24 || length % stride != 0 || (long)offset + length > bytes.length
                    || strOffset < 0 || strSize < 0 || (long)strOffset + strSize > bytes.length)
                    throw failure("Ungueltige ELF-Symbole.");
                for (int p = offset; p < offset + length; p += stride) {
                    int binding = Byte.toUnsignedInt(b.get(p + 4)) >> 4;
                    int visibility = b.get(p + 5) & 3;
                    if (b.getShort(p + 6) == 0 || (binding != 1 && binding != 2) || (visibility != 0 && visibility != 3)) continue;
                    int name = b.getInt(p);
                    if (name < 0 || name >= strSize) throw failure("Ungueltiger ELF-Symbolname.");
                    int end = strOffset + name;
                    while (end < strOffset + strSize && bytes[end] != 0) end++;
                    if (end == strOffset + strSize) throw failure("Unvollstaendiger ELF-Symbolname.");
                    String text = new String(bytes, strOffset + name, end - strOffset - name, StandardCharsets.US_ASCII);
                    if (text.startsWith("xr")) result.add(text);
                }
            }
            if (result.isEmpty()) throw failure("Keine exportierten OpenXR-Funktionen gefunden.");
            return result;
        } catch (IndexOutOfBoundsException | ArithmeticException e) { throw failure("Beschaedigter ELF-Loader."); }
    }
}
