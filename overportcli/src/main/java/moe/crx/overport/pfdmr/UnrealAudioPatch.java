package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import kotlin.Unit;
import moe.crx.overport.patching.*;

/** Optional Unreal audio configuration. No device-wide changes or binary audio hooks. */
public final class UnrealAudioPatch {
    public static final String ID = "patch_pfdmr_unreal_audio48";
    public static final String LABEL = "PFDMR Unreal Audio: 48 kHz (experimental)";
    private static final String CALL = "    invoke-static/range {p0 .. p0}, Lmoe/crx/pfdmr/Audio48;->apply(Landroid/content/Context;)V";
    private static final String CONFIG = "root/assets/pfdmr-audio48.properties";
    public static final Patch PATCH = new Patch(ID, false, false, (executor, args) -> {
        executor.selectWorkspace(file -> { apply(file.getFile().toPath()); return Unit.INSTANCE; });
        return Unit.INSTANCE;
    });
    private static IllegalStateException fail(String message) { return new IllegalStateException("PFDMR Unreal Audio: " + message); }
    public static void prepare(Path workspace, boolean enabled) {
        Path config = workspace.resolve(CONFIG);
        if (!enabled && Files.isRegularFile(config)) {
            try {
                String text = Files.readString(config);
                if (!text.startsWith("# PFDMR_AUDIO48_V1\n")) throw fail("Unknown audio profile; use the original APK.");
                Files.writeString(config, text.replace("enabled=true", "enabled=false"));
            } catch (IOException e) { throw fail(e.toString()); }
        }
    }
    public static void apply(Path workspace) {
        try {
            Path command = workspace.resolve("root/assets/UECommandLine.txt");
            if (!Files.isRegularFile(command)) throw fail("No Unreal project command line found. This option supports Unreal games only.");
            Matcher project = Pattern.compile("(?i)([A-Za-z0-9_]+)\\.uproject(?:[\\s\"']|$)").matcher(Files.readString(command));
            if (!project.find()) throw fail("Cannot identify the Unreal project safely.");
            String name = project.group(1);
            boolean ue5 = Files.exists(workspace.resolve("root/lib/arm64-v8a/libUnreal.so"));
            boolean ue4 = Files.exists(workspace.resolve("root/lib/arm64-v8a/libUE4.so"));
            if (ue5 == ue4) throw fail("Expected exactly one supported ARM64 Unreal engine.");
            List<Path> activities = new ArrayList<>();
            try (var files = Files.walk(workspace.resolve("smali"))) {
                files.filter(Files::isRegularFile).filter(p -> {
                    String path = p.toString().replace('\\','/');
                    return path.matches(".*/com/epicgames/(unreal|ue4)/GameActivity(?:\\.1)?\\.smali");
                }).forEach(activities::add);
            }
            if (activities.size() != 1) throw fail("Expected exactly one Unreal GameActivity.");
            Path activity = activities.get(0);
            Path helper = activity.getParent().getParent().getParent().getParent().resolve("moe/crx/pfdmr/Audio48.smali");
            Path config = workspace.resolve(CONFIG);
            String text = Files.readString(activity);
            boolean existing = Files.exists(config) && Files.readString(config).startsWith("# PFDMR_AUDIO48_V1\n");
            if (Files.exists(config) && !existing) throw fail("Audio profile filename already in use; use the original APK.");
            if ((Files.exists(helper) || text.contains("Lmoe/crx/pfdmr/Audio48;")) && !existing)
                throw fail("Helper name already in use; use the original APK.");
            String updated = inject(text);
            byte[] resource;
            try (InputStream in = UnrealAudioPatch.class.getResourceAsStream("/pfdmr/audio/Audio48.smali")) {
                if (in == null) throw fail("Bundled audio helper missing.");
                resource = in.readAllBytes();
            }
            Files.createDirectories(helper.getParent()); Files.createDirectories(config.getParent());
            Files.write(helper, resource);
            Files.writeString(activity, updated);
            Files.writeString(config, "# PFDMR_AUDIO48_V1\nenabled=true\npath=" + (ue5 ? "UnrealGame/" : "UE4Game/") + name + "/" + name + "/Saved/Config/Android/Engine.ini\n");
        } catch (IOException e) { throw fail(e.toString()); }
    }
    public static String inject(String text) {
        if (text.contains(CALL)) return text;
        Matcher method = Pattern.compile("(?m)^\\.method [^\\r\\n]*\\bonCreate\\(Landroid/os/Bundle;\\)V\\r?\\n").matcher(text);
        if (!method.find()) throw fail("No supported onCreate method found.");
        int end = text.indexOf(".end method", method.end());
        if (end < 0) throw fail("Malformed onCreate method.");
        String body = text.substring(method.end(), end);
        Matcher registers = Pattern.compile("(?m)^\\s*\\.(?:locals|registers) +[0-9]+[^\\r\\n]*\\r?\\n").matcher(body);
        if (!registers.find()) throw fail("Unsupported register declaration.");
        int at = method.end() + registers.end();
        return text.substring(0, at) + "\n" + CALL + "\n" + text.substring(at);
    }
}
