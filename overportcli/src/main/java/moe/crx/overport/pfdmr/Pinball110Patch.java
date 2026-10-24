package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import kotlin.Unit;
import moe.crx.overport.patching.*;

/** Exact-build compatibility profile layered above the scene/spatial adapter. */
public final class Pinball110Patch {
    public static final String ID="patch_pfdmr_pinball110";
    public static final String LABEL="PFDMR compatibility: Pinball FX VR 1.10 (requires scene & spatial)";
    private static final String PACKAGE="com.zenstudios.PFXVRQuest";
    private static final String LIB="root/lib/arm64-v8a/", LOADER="libopenxr_loader.so", BACKUP="libpfdmr_pinball110_original.so";
    private static final String RECEIPT="root/assets/pfdmr-pinball110.properties", MARKER="PFDMR_PINBALL110_V1";
    private static final String UNREAL="6b383c0e833d39d2084cec12662695b925fa7729846aa7b131abed85fdfb7228";
    private static final String OVR="4618643a0eb6712a4dd8e559afbb136bc5ef736b28fa3a59b78264e4170aab6a";
    private static final String MRUK_BEFORE="fc2df0e83599bbd932680fae2859c06e189c8d6f18cc96babd605eed65897eb8";
    private static final String MRUK_AFTER="38ff3719a3d5abc81c9f6c5bdfd9bfce2df67895974e8bbad9c9eacb02f26f04";
    private static final int OFFSET=0x233374;
    private static final byte[] BEFORE=HexFormat.of().parseHex("1f0d40f2"), AFTER=HexFormat.of().parseHex("1f0540f2");
    public static final Patch PATCH=new Patch(ID,false,false,(executor,args)->{
        executor.selectWorkspace(file->{apply(file.getFile().toPath(),executor.applicationPackage());return Unit.INSTANCE;});return Unit.INSTANCE;
    });
    private static IllegalStateException fail(String message){return new IllegalStateException("Pinball FX VR 1.10: "+message);}
    private static boolean marked(byte[] data){return new String(data,StandardCharsets.ISO_8859_1).contains(MARKER);}
    private static String hash(Path file)throws IOException{return StereoPatch.sha256(Files.readAllBytes(file));}
    private static void identity(Path ws,String packageName)throws IOException {
        if(!PACKAGE.equals(packageName))throw fail("This option supports only com.zenstudios.PFXVRQuest 1.10.");
        if(Files.exists(ws.resolve(LIB+"libpfx_base.so")))throw fail("Legacy test wrapper detected. Use the APK before the standalone test fixes.");
        if(!UNREAL.equals(hash(ws.resolve(LIB+"libUnreal.so"))) || !OVR.equals(hash(ws.resolve(LIB+"libOVRPlugin.so"))))throw fail("Unrecognized engine or OVRPlugin; only the verified ARM64 build is supported.");
        String mruk=hash(ws.resolve(LIB+"libmrutilitykitshared.so"));
        if(!MRUK_BEFORE.equals(mruk)&&!MRUK_AFTER.equals(mruk))throw fail("Unrecognized MRUK library.");
        try(var dirs=Files.list(ws.resolve("root/lib"))){
            if(dirs.anyMatch(p->!p.getFileName().toString().equals("arm64-v8a")&&Files.exists(p.resolve(LOADER))))throw fail("Multiple OpenXR architectures are unsupported.");
        }
    }
    /** Reject invalid combinations before restoring or applying any selected patch. */
    public static void validateSelection(PatcherContext context,Map<String,List<String>> args) {
        if(!args.containsKey(ID))return;
        if(!args.containsKey(MrCompatibilityPatch.ID))throw fail("Also enable PFDMR MR: scene & spatial compatibility.");
        if(!"1.10".equals(context.appVersionName()) || !"7380".equals(context.appVersionCode()))throw fail("Only version 1.10 (7380) is supported.");
        try{identity(context.getWorkingDir().toPath(),context.appPackage());}catch(IOException e){throw fail(e.toString());}
    }
    public static void restore(Path ws) {
        Path receipt=ws.resolve(RECEIPT),loader=ws.resolve(LIB+LOADER),backup=ws.resolve(LIB+BACKUP),mruk=ws.resolve(LIB+"libmrutilitykitshared.so");
        try {
            if(!Files.exists(receipt)) {
                if(Files.exists(backup)||Files.isRegularFile(loader)&&marked(Files.readAllBytes(loader)))throw fail("Missing rollback receipt. Use the original APK.");
                return;
            }
            Properties p=new Properties();try(var in=Files.newInputStream(receipt)){p.load(in);}
            byte[] original=Files.readAllBytes(backup),current=Files.readAllBytes(loader),m=Files.readAllBytes(mruk);
            String before=p.getProperty("mruk.before"),mh=StereoPatch.sha256(m);
            if(!"1".equals(p.getProperty("version")) || !StereoPatch.sha256(original).equals(p.getProperty("loader.before")) ||
                !(StereoPatch.sha256(current).equals(p.getProperty("loader.after"))||Arrays.equals(current,original)) ||
                !(MRUK_BEFORE.equals(before)||MRUK_AFTER.equals(before)) || !(MRUK_AFTER.equals(mh)||before.equals(mh)))
                throw fail("Loader, MRUK or backup changed; refusing unsafe restoration.");
            if(MRUK_BEFORE.equals(before))System.arraycopy(BEFORE,0,m,OFFSET,BEFORE.length);
            if(!before.equals(StereoPatch.sha256(m)))throw fail("MRUK restoration validation failed.");
            Files.write(loader,original);Files.write(mruk,m);Files.delete(backup);Files.delete(receipt);
        }catch(IOException e){throw fail(e.toString());}
    }
    public static void apply(Path ws,String packageName) {
        try {
            identity(ws,packageName);
            if(!Files.isDirectory(ws.resolve("root/assets/pfdmr-mr")))throw fail("The scene & spatial adapter must be applied first.");
            restore(ws);
            Path loader=ws.resolve(LIB+LOADER),backup=ws.resolve(LIB+BACKUP),receipt=ws.resolve(RECEIPT),mruk=ws.resolve(LIB+"libmrutilitykitshared.so");
            byte[] original=Files.readAllBytes(loader),m=Files.readAllBytes(mruk),wrapper;
            String before=StereoPatch.sha256(m);
            m=MrukPosePatch.apply(m,MrukPosePatch.PINBALL);
            try(var in=Pinball110Patch.class.getResourceAsStream("/pfdmr/arm64-v8a/libpfdmr_pinball110.so")){
                if(in==null)throw fail("Bundled compatibility wrapper missing.");wrapper=in.readAllBytes();
            }
            if(!marked(wrapper))throw fail("Bundled compatibility wrapper marker missing.");
            Set<String> exports=StereoPatch.exports(original),missing=new TreeSet<>(exports);
            if(!exports.contains("xrGetInstanceProcAddr"))throw fail("OpenXR resolver missing.");
            missing.removeAll(StereoPatch.exports(wrapper));if(!missing.isEmpty())throw fail("Unsupported direct OpenXR exports: "+missing);
            Properties p=new Properties();p.setProperty("version","1");p.setProperty("loader.before",StereoPatch.sha256(original));p.setProperty("loader.after",StereoPatch.sha256(wrapper));p.setProperty("mruk.before",before);
            Files.createDirectories(receipt.getParent());Files.write(backup,original,StandardOpenOption.CREATE_NEW);
            try(var out=Files.newOutputStream(receipt)){p.store(out,"Pinball FX VR 1.10 compatibility; original libraries retained");}
            Files.write(mruk,m);Files.write(loader,wrapper);
        }catch(IOException e){throw fail(e.toString());}
    }
}
