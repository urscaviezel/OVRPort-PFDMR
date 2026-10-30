package moe.crx.overport.pfdmr;

import java.io.IOException;
import java.nio.file.*;
import com.reandroid.json.JSONObject;
import kotlin.Unit;
import moe.crx.overport.patching.Patch;

/** Explicit opt-in render-size configuration; no game code or runtime hooks. */
public final class MossResolutionPatch {
    public static final String ID = "patch_pfdmr_moss_resolution";
    public static final String LABEL = "PFDMR compatibility: Moss render resolution 1800 x 1664 (experimental)";
    private static final String CONFIG = "root/lib/arm64-v8a/liboverport.config.so";
    private static final String RECEIPT = "root/assets/pfdmr-moss-resolution.json";
    private static final String WIDTH = "suggested_texture_width", HEIGHT = "suggested_texture_height";
    public static final Patch PATCH = new Patch(ID, false, false, (executor, args) -> {
        executor.selectWorkspace(file -> { apply(file.getFile().toPath()); return Unit.INSTANCE; });
        return Unit.INSTANCE;
    });
    private static IllegalStateException fail(String reason) {
        return new IllegalStateException("Moss resolution: " + reason);
    }
    private static JSONObject config(Path ws) throws IOException {
        Path path = ws.resolve(CONFIG);
        if (!Files.isRegularFile(path)) throw fail("ARM64 OverPort configuration missing. Enable Copy Overport Libraries when patching an original APK.");
        String text = Files.readString(path).trim();
        if (!text.startsWith("{") || !text.endsWith("}")) throw fail("Invalid OverPort JSON configuration.");
        return new JSONObject(text);
    }
    public static void apply(Path ws) {
        try {
            restore(ws);
            JSONObject config = config(ws);
            if (config.optInt(WIDTH, -1) == 1800 && config.optInt(HEIGHT, -1) == 1664) return;
            JSONObject before = new JSONObject();
            for (String key : new String[] {WIDTH, HEIGHT}) if (config.has(key)) before.put(key, config.get(key));
            JSONObject receipt = new JSONObject().put("version", 1).put("before", before);
            config.put(WIDTH, 1800).put(HEIGHT, 1664);
            Path path = ws.resolve(RECEIPT); Files.createDirectories(path.getParent());
            Files.writeString(path, receipt.toString());
            Files.writeString(ws.resolve(CONFIG), config.toString());
        } catch (IOException ex) { throw fail(ex.toString()); }
    }
    public static void restore(Path ws) {
        Path receipt = ws.resolve(RECEIPT);
        if (!Files.exists(receipt)) return;
        try {
            JSONObject saved = new JSONObject(Files.readString(receipt)), config = config(ws);
            if (saved.optInt("version", -1) != 1 || config.optInt(WIDTH, -1) != 1800 || config.optInt(HEIGHT, -1) != 1664)
                throw fail("Resolution or rollback data changed; use the original APK.");
            JSONObject before = saved.getJSONObject("before");
            for (String key : new String[] {WIDTH, HEIGHT}) {
                if (before.has(key)) config.put(key, before.get(key)); else config.remove(key);
            }
            Files.writeString(ws.resolve(CONFIG), config.toString()); Files.delete(receipt);
        } catch (IOException ex) { throw fail(ex.toString()); }
    }
}
