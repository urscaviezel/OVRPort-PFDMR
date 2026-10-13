package moe.crx.pfdmr;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

/** GPL-3.0. Optional app-local Unreal configuration, applied before native onCreate. */
public final class Audio48 {
    private static final String SECTION = "[/Script/AndroidRuntimeSettings.AndroidRuntimeSettings]";
    public static void apply(Context context) {
        try {
            Properties config = new Properties();
            try (InputStream in = context.getAssets().open("pfdmr-audio48.properties")) { config.load(in); }
            update(context.getFilesDir(), config.getProperty("path"), "true".equals(config.getProperty("enabled")));
            Log.i("PFDMRAudio48", "Audio profile processed: enabled=" + config.getProperty("enabled") + "; " + config.getProperty("path"));
        } catch (Exception failure) {
            // A compatibility option must never prevent the game from starting.
            Log.w("PFDMRAudio48", "Audio configuration not applied", failure);
        }
    }

    public static void update(File files, String relative, boolean enabled) throws IOException {
        if (relative == null || !relative.matches("(UnrealGame|UE4Game)/([A-Za-z0-9_]+)/\\2/Saved/Config/Android/Engine\\.ini"))
            throw new IOException("Unsupported Unreal config path");
        File ini = new File(files, relative);
        File backup = new File(ini.getPath() + ".pfdmr-audio48.original");
        if (!ini.getCanonicalPath().startsWith(files.getCanonicalPath() + File.separator))
            throw new IOException("Config path outside app files");
        if (!enabled && !backup.isFile()) return;
        byte[] current = ini.isFile() ? Files.readAllBytes(ini.toPath()) : new byte[0];
        String text = decode(current);
        String original = backup.isFile() ? decode(Files.readAllBytes(backup.toPath())) : text;
        String updated = rewrite(text, enabled ? Collections.singletonList("AudioSampleRate=48000") : audioLines(original));
        if (enabled) {
            if (!ini.getParentFile().isDirectory() && !ini.getParentFile().mkdirs()) throw new IOException("Cannot create config directory");
            if (!backup.exists()) Files.write(backup.toPath(), current, StandardOpenOption.CREATE_NEW);
        }
        if (!updated.equals(text)) {
            Path temp = new File(ini.getPath() + ".pfdmr-audio48.tmp").toPath();
            Files.write(temp, updated.getBytes(StandardCharsets.UTF_8));
            Files.move(temp, ini.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        if (!enabled) Files.delete(backup.toPath());
    }

    private static String decode(byte[] bytes) throws IOException {
        try { return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(); }
        catch (CharacterCodingException e) { throw new IOException("Unsupported INI encoding; original preserved", e); }
    }
    private static boolean audioKey(String line) {
        int equals = line.indexOf('=');
        return equals >= 0 && line.substring(0, equals).trim().equalsIgnoreCase("AudioSampleRate");
    }
    private static List<String> audioLines(String text) {
        List<String> result = new ArrayList<>(); boolean inSection = false;
        for (String line : text.replace("\uFEFF", "").split("\\r?\\n", -1)) {
            String trim = line.trim();
            if (trim.startsWith("[") && trim.endsWith("]")) inSection = trim.equalsIgnoreCase(SECTION);
            else if (inSection && audioKey(line)) result.add(line);
        }
        return result;
    }
    /** Preserve unrelated settings, comments, BOM and newline style; handle duplicate keys/sections. */
    public static String rewrite(String text, List<String> replacement) {
        String bom = text.startsWith("\uFEFF") ? "\uFEFF" : "";
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        List<String> result = new ArrayList<>(); boolean inSection = false, inserted = false;
        for (String line : text.substring(bom.length()).split("\\r?\\n", -1)) {
            String trim = line.trim();
            if (trim.startsWith("[") && trim.endsWith("]")) {
                if (inSection && !inserted) { result.addAll(replacement); inserted = true; }
                inSection = trim.equalsIgnoreCase(SECTION);
            }
            if (!(inSection && audioKey(line))) result.add(line);
        }
        if (inSection && !inserted) { result.addAll(replacement); inserted = true; }
        if (!inserted && !replacement.isEmpty()) { result.add(SECTION); result.addAll(replacement); }
        return bom + String.join(newline, result);
    }
}
