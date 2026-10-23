package moe.crx.overport.pfdmr;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import kotlin.Unit;
import moe.crx.overport.patching.Patch;

/** Use the game's existing file/size check before entering its broken downloader path. */
public final class UnrealObbPatch {
    public static final String ID = "patch_pfdmr_unreal_obb";
    public static final String LABEL = "PFDMR Unreal: existing OBB startup (verified games)";
    private static final String RECEIPT = "root/assets/pfdmr-unreal-obb.txt";
    private static final String MARKER = "PFDMR_OUTOFSCALE_OBB_V1\n";
    private static final String GAME = "Lcom/epicgames/ue4/GameActivity;";
    private static final String CHECK_SHA = "e27ef66c75a04bbe4615eeb5de960cb218c3f6f72c9586678023df8321aa1c73";
    public static final Patch PATCH = new Patch(ID, true, false, (executor, args) -> {
        executor.selectWorkspace(file -> { apply(file.getFile().toPath(), executor.applicationPackage()); return Unit.INSTANCE; });
        return Unit.INSTANCE;
    });

    private static IllegalStateException fail(String reason) {
        return new IllegalStateException("PFDMR Unreal OBB: " + reason + " Only the verified Out of Scale startup is supported.");
    }
    private static Path activity(Path ws) throws Exception {
        try (var files = Files.walk(ws.resolve("smali"))) {
            var matches = files.filter(Files::isRegularFile).filter(p -> p.toString().replace('\\','/').endsWith("/com/epicgames/ue4/GameActivity.smali")).toList();
            if (matches.size()!=1) throw fail("Expected one GameActivity.");
            return matches.get(0);
        }
    }
    private static String method(String text, String signature) {
        Matcher m = Pattern.compile("(?m)^\\.method [^\\r\\n]*" + Pattern.quote(signature) + "\\r?$" ).matcher(text);
        if (!m.find()) throw fail("Missing " + signature);
        int end = text.indexOf(".end method",m.end());
        if (end<0) throw fail("Malformed method.");
        String body=text.substring(m.start(),end+11);
        if (m.find()) throw fail("Ambiguous method.");
        return body;
    }
    // Ignore disassembler label numbering and debug/register directives, not instructions.
    public static String fingerprint(String method) throws Exception {
        var lines = method.lines().map(String::trim).filter(s -> !s.isEmpty() && !s.matches("^(#|\\.line |\\.local |\\.end local|\\.restart local|\\.param |\\.prologue|\\.locals |\\.registers ).*")).toList();
        String text=String.join("\n",lines);
        var labels=new LinkedHashMap<String,String>();
        var matcher=Pattern.compile("(?<![A-Za-z0-9_;>]):[A-Za-z_][A-Za-z_0-9]*").matcher(text);
        text=matcher.replaceAll(m -> labels.computeIfAbsent(m.group(), key -> ":label"+labels.size()));
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static void validateCheck(Path activity) throws Exception {
        String check=method(Files.readString(activity.resolveSibling("b.smali")),"b(Landroid/app/Activity;I)Z");
        if (!CHECK_SHA.equals(fingerprint(check))) throw fail("Unrecognized OBB file/size check.");
    }
    private static Pattern site(boolean original) {
        String game=Pattern.quote(GAME);
        return Pattern.compile("iget-boolean v1, v8, "+game+"->O:Z\\s+if-nez v1, (?<label>:[A-Za-z_0-9]+)\\s+"
            +(original?"(?<gate>iget-boolean v1, v8, "+game+"->VerifyOBBOnStartUp:Z\\s+if-nez v1, \\k<label>\\s+)":"")
            +"(?<check>iget v1, v8, "+game+"->VersionCode:I\\s+invoke-static \\{v8, v1\\}, Lcom/epicgames/ue4/b;->b\\(Landroid/app/Activity;I\\)Z\\s+move-result v1\\s+iput-boolean v1, v8, "+game+"->O:Z\\s+\\k<label>\\s+new-instance v1, Landroid/widget/FrameLayout;)");
    }
    private static Matcher unique(String body, boolean original) {
        Matcher m=site(original).matcher(body);
        if (!m.find()) throw fail("Unrecognized startup instructions.");
        int start=m.start();
        if (m.find()) throw fail("Ambiguous startup instructions.");
        m.reset();m.find(start);return m;
    }
    public static boolean apply(Path ws,String packageName) {
        if (!"com.kurzgesagt.outofscale".equals(packageName)) return false;
        try {
            Path path=activity(ws);validateCheck(path);
            String text=Files.readString(path), body=method(text,"onCreate(Landroid/os/Bundle;)V");
            Path receipt=ws.resolve(RECEIPT);
            if (Files.exists(receipt)&&!MARKER.equals(Files.readString(receipt))) throw fail("Invalid patch receipt.");
            if (site(false).matcher(body).find()) { unique(body,false);return true; }
            Matcher m=unique(body,true);
            String updated=body.substring(0,m.start("gate"))+body.substring(m.end("gate"));
            Files.createDirectories(receipt.getParent());Files.writeString(receipt,MARKER);
            Files.writeString(path,text.replace(body,updated));return true;
        } catch (IllegalStateException e) {throw e;} catch(Exception e) {throw fail(e.toString());}
    }
    public static void restore(Path ws) {
        Path receipt=ws.resolve(RECEIPT);if (!Files.exists(receipt)) return;
        try {
            if (!MARKER.equals(Files.readString(receipt))) throw fail("Invalid patch receipt.");
            Path path=activity(ws);validateCheck(path);
            String text=Files.readString(path),body=method(text,"onCreate(Landroid/os/Bundle;)V");
            if (!site(true).matcher(body).find()) {
                Matcher m=unique(body,false);
                String gate="iget-boolean v1, v8, "+GAME+"->VerifyOBBOnStartUp:Z\n\n    if-nez v1, "+m.group("label")+"\n\n    ";
                String restored=body.substring(0,m.start("check"))+gate+body.substring(m.start("check"));
                Files.writeString(path,text.replace(body,restored));
            }
            Files.delete(receipt);
        } catch (IllegalStateException e) {throw e;} catch(Exception e) {throw fail(e.toString());}
    }
}
