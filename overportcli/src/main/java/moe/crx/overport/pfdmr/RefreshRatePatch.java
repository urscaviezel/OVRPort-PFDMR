package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import kotlin.Unit;
import moe.crx.overport.patching.Patch;

/** Outermost optional loader wrapper: leave display preference to the runtime. */
public final class RefreshRatePatch {
    public static final String ID="patch_pfdmr_system_refresh";
    public static final String LABEL="PFDMR display: prefer headset refresh rate (experimental)";
    private static final String LIB="root/lib/arm64-v8a/", LOADER="libopenxr_loader.so", BACKUP="libpfdmr_refresh_original.so";
    private static final String RECEIPT="root/assets/pfdmr-refresh.properties", MARKER="PFDMR_SYSTEM_REFRESH_V1";
    public static final Patch PATCH=new Patch(ID,false,false,(executor,args)->{
        executor.selectWorkspace(file->{apply(file.getFile().toPath());return Unit.INSTANCE;});return Unit.INSTANCE;
    });
    private static IllegalStateException fail(String message){return new IllegalStateException("PFDMR refresh rate: "+message);}
    private static boolean marked(byte[] data){return new String(data,StandardCharsets.ISO_8859_1).contains(MARKER);}
    public static void restore(Path workspace) {
        Path receipt=workspace.resolve(RECEIPT),loader=workspace.resolve(LIB+LOADER),backup=workspace.resolve(LIB+BACKUP);
        try {
            if(!Files.exists(receipt)) {
                if(Files.exists(backup) || Files.isRegularFile(loader)&&marked(Files.readAllBytes(loader)))throw fail("Missing rollback receipt. Use the original APK.");
                return;
            }
            Properties p=new Properties();try(var in=Files.newInputStream(receipt)){p.load(in);}
            byte[] original=Files.readAllBytes(backup),current=Files.readAllBytes(loader);
            if(!"1".equals(p.getProperty("version")) || !StereoPatch.sha256(original).equals(p.getProperty("before")) ||
                !(StereoPatch.sha256(current).equals(p.getProperty("after")) || Arrays.equals(current,original)))
                throw fail("Loader or backup changed; refusing unsafe restoration.");
            Files.write(loader,original);Files.delete(backup);Files.delete(receipt);
        } catch(IOException e){throw fail(e.toString());}
    }
    public static void apply(Path workspace) {
        restore(workspace);
        Path loader=workspace.resolve(LIB+LOADER),backup=workspace.resolve(LIB+BACKUP),receipt=workspace.resolve(RECEIPT);
        try {
            if(!Files.isRegularFile(loader))throw fail("Requires an ARM64 OpenXR loader.");
            try(var dirs=Files.list(workspace.resolve("root/lib"))){
                if(dirs.anyMatch(p->!p.getFileName().toString().equals("arm64-v8a")&&Files.exists(p.resolve(LOADER))))throw fail("Multiple OpenXR architectures are unsupported.");
            }
            byte[] original=Files.readAllBytes(loader),wrapper;
            try(var in=RefreshRatePatch.class.getResourceAsStream("/pfdmr/arm64-v8a/libpfdmr_refresh.so")){
                if(in==null)throw fail("Bundled refresh-rate wrapper missing.");wrapper=in.readAllBytes();
            }
            Set<String> exports=StereoPatch.exports(original),missing=new TreeSet<>(exports);
            if(!exports.contains("xrGetInstanceProcAddr"))throw fail("OpenXR resolver missing.");
            missing.removeAll(StereoPatch.exports(wrapper));if(!missing.isEmpty())throw fail("Unsupported direct OpenXR exports: "+missing);
            Properties p=new Properties();p.setProperty("version","1");p.setProperty("before",StereoPatch.sha256(original));p.setProperty("after",StereoPatch.sha256(wrapper));
            Files.createDirectories(receipt.getParent());
            Files.write(backup,original,StandardOpenOption.CREATE_NEW);
            try(var out=Files.newOutputStream(receipt)){p.store(out,"PFDMR system refresh preference; SpaceWarp unchanged");}
            Files.write(loader,wrapper);
        } catch(IOException e){throw fail(e.toString());}
    }
}
