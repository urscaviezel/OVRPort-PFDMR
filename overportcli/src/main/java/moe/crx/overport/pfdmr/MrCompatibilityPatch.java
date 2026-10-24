package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.reandroid.json.*;
import kotlin.Unit;
import moe.crx.overport.patching.*;

/** Opt-in YVR passthrough/scene compatibility; preserves originals for repatching. */
public final class MrCompatibilityPatch {
    public static final String ID = "patch_pfdmr_mr_scene";
    public static final String LABEL = "PFDMR MR: scene & spatial compatibility (experimental)";
    private static final String PERMISSION = "com.yvr.permission.USE_SCENE";
    private static final String CLASS = "com/google/vr/sdk/samples/permission/";
    private static final String BRIDGE = "PfdmrScenePermissionBridge";
    private static final String PREFIX = "L"+CLASS;
    private static final String[] LOADERS = {"libopenxr_loader.so", "libopenxr_loader_yvr.so"};
    private static final String[] BACKUPS = {"libpfdmr_mr_original_openxr.so", "libpfdmr_mr_original_yvr.so"};
    private static final String[] RESOURCES = {"libpfdmr_mr_dispatch.so", "libpfdmr_mr_yvr.so"};
    private static final String[] CLASSES = {"PermissionHelper", "PermissionFragment"};
    private static final String RECEIPT = "root/assets/pfdmr-mr";
    public static final Patch PATCH = new Patch(ID, false, false, (executor,args) -> {
        executor.selectWorkspace(file -> { apply(file.getFile().toPath()); return Unit.INSTANCE; });
        return Unit.INSTANCE;
    });
    private static IllegalStateException fail(String s) {
        return new IllegalStateException("PFDMR MR: "+s+" Use an APK without an older MR test patch, or disable this option.");
    }
    private static byte[] resource(String name) throws IOException {
        try(var in=MrCompatibilityPatch.class.getResourceAsStream("/pfdmr/"+name)) {
            if(in==null)throw fail("Missing bundled resource: "+name);
            return in.readAllBytes();
        }
    }
    private static Path findClass(Path ws,String name) throws IOException {
        List<Path> matches;
        try(var files=Files.walk(ws.resolve("smali"))) {
            matches=files.filter(Files::isRegularFile).filter(p->p.toString().replace('\\','/').endsWith("/"+CLASS+name+".smali")).toList();
        }
        if(matches.size()!=1)throw fail("Unsupported or ambiguous permission bridge: "+name);
        return matches.get(0);
    }
    private static String replaceOnce(String text,String from,String to) {
        if(text.indexOf(from)<0 || text.indexOf(from)!=text.lastIndexOf(from))throw fail("Unrecognized permission method.");
        return text.replace(from,to);
    }
    public static String patchHelper(String text) {
        String head=".method public static checkPermission(Ljava/lang/String;)Z\n    .locals 4";
        return replaceOnce(text,head,head+"\n\n    invoke-static {p0}, "+PREFIX+BRIDGE+";->map(Ljava/lang/String;)Ljava/lang/String;\n    move-result-object p0");
    }
    public static String patchFragment(String text) {
        if(text.contains("pfdmrOriginalPermissions") || text.contains("pinballOriginalPermissions"))throw fail("Permission bridge already modified.");
        text=replaceOnce(text,"# direct methods",".field private pfdmrOriginalPermissions:[Ljava/lang/String;\n\n# direct methods");
        String head=".method public acquirePermissions([Ljava/lang/String;)V\n    .locals 1";
        text=replaceOnce(text,head,head+"\n\n    iput-object p1, p0, "+PREFIX+"PermissionFragment;->pfdmrOriginalPermissions:[Ljava/lang/String;\n"+
            "    invoke-static {p1}, "+PREFIX+BRIDGE+";->mapArray([Ljava/lang/String;)[Ljava/lang/String;\n    move-result-object p1");
        head=".method public onRequestPermissionsResult(I[Ljava/lang/String;[I)V\n    .locals 2";
        // Two original locals v0/v1. v2 preserves 'this' because the original method reuses p0.
        text=replaceOnce(text,head,head.replace(".locals 2",".locals 3")+"\n\n    move-object v2, p0");
        head="    invoke-static {p2, p3}, "+PREFIX+"PermissionHelper;->onAcquirePermissions([Ljava/lang/String;[I)V";
        return replaceOnce(text,head,"    iget-object v0, v2, "+PREFIX+"PermissionFragment;->pfdmrOriginalPermissions:[Ljava/lang/String;\n"+
            "    if-eqz v0, :pfdmr_original_done\n    move-object p2, v0\n    const/4 v0, 0x0\n"+
            "    iput-object v0, v2, "+PREFIX+"PermissionFragment;->pfdmrOriginalPermissions:[Ljava/lang/String;\n"+
            "    :pfdmr_original_done\n"+head);
    }
    private static JSONObject manifest(Path ws) throws IOException {
        return new JSONObject(Files.readString(ws.resolve("AndroidManifest.xml.json")));
    }
    private static JSONArray rootNodes(JSONObject doc) {
        JSONArray nodes=doc.optJSONArray("nodes");
        if(nodes!=null)for(int i=0;i<nodes.length();i++) {
            JSONObject n=nodes.optJSONObject(i);
            if(n!=null && "manifest".equals(n.optString("name")))return n.getJSONArray("nodes");
        }
        throw fail("Unsupported manifest.");
    }
    private static boolean isPermission(JSONObject node) {
        if(node==null || !"uses-permission".equals(node.optString("name")))return false;
        JSONArray attrs=node.optJSONArray("attributes");
        if(attrs!=null)for(int i=0;i<attrs.length();i++) {
            JSONObject a=attrs.optJSONObject(i);
            if(a!=null && "name".equals(a.optString("name")) && PERMISSION.equals(a.optString("data")))return true;
        }
        return false;
    }
    private static boolean addPermission(JSONObject doc) {
        JSONArray nodes=rootNodes(doc);
        for(int i=0;i<nodes.length();i++)if(isPermission(nodes.optJSONObject(i)))return false;
        nodes.put(moe.crx.overport.patches.VRMetadataPatchKt.createUsesPermission(PERMISSION));
        return true;
    }
    private static void checkHash(byte[] data,Properties p,String key) {
        if(!StereoPatch.sha256(data).equals(p.getProperty(key)))throw fail("Modified or missing rollback data: "+key);
    }
    private static byte[] normalized(Path p) throws IOException { return Files.readString(p).replace("\r\n","\n").getBytes(java.nio.charset.StandardCharsets.UTF_8); }
    private static byte[][] canonicalize(Path ws,byte[] helper,byte[] fragment,byte[] bridge) throws IOException {
        Path temp=Files.createTempDirectory(ws,"mr-smali-");
        try {
            Path src=Files.createDirectory(temp.resolve("src")), dex=temp.resolve("classes.dex"), out=temp.resolve("out");
            Files.write(src.resolve("PermissionHelper.smali"),helper);
            Files.write(src.resolve("PermissionFragment.smali"),fragment);
            Files.write(src.resolve(BRIDGE+".smali"),bridge);
            var options=new org.jf.smali.SmaliOptions();options.apiLevel=29;options.jobs=1;options.outputDexFile=dex.toString();
            if(!org.jf.smali.Smali.assemble(options,src.toString()))throw fail("Permission bridge could not be assembled.");
            var decoded=new org.jf.baksmali.BaksmaliOptions();
            decoded.apiLevel=29;decoded.localsDirective=true;decoded.sequentialLabels=true;decoded.skipDuplicateLineNumbers=true;
            var file=new org.jf.dexlib2.dexbacked.DexBackedDexFile(org.jf.dexlib2.Opcodes.forApi(29),Files.readAllBytes(dex));
            if(!org.jf.baksmali.Baksmali.disassembleDexFile(file,out.toFile(),1,decoded))throw fail("Permission bridge could not be verified.");
            return new byte[][] {normalized(out.resolve(CLASS+"PermissionHelper.smali")),
                normalized(out.resolve(CLASS+"PermissionFragment.smali")),normalized(out.resolve(CLASS+BRIDGE+".smali"))};
        } finally {
            try(var files=Files.walk(temp)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
        }
    }
    public static void restore(Path ws) {
        Path receipt=ws.resolve(RECEIPT), lib=ws.resolve("root/lib/arm64-v8a");
        try {
            if(!Files.exists(receipt)) {
                for(String name:BACKUPS)if(Files.exists(lib.resolve(name)))throw fail("Unowned MR loader backup.");
                return;
            }
            Properties p=new Properties();try(var in=Files.newInputStream(receipt.resolve("receipt.properties"))){p.load(in);}
            if(!"1".equals(p.getProperty("version")))throw fail("Unknown rollback format.");
            int count="true".equals(p.getProperty("nativeOnly"))?2:4;
            byte[][] originals=new byte[count][];
            Path[] targets=new Path[count];
            for(int i=0;i<count;i++) {
                targets[i]=i<2?lib.resolve(LOADERS[i]):findClass(ws,CLASSES[i-2]);
                originals[i]=Files.readAllBytes(i<2?lib.resolve(BACKUPS[i]):receipt.resolve(CLASSES[i-2]+".original"));
                checkHash(originals[i],p,"before"+i);
                byte[] current=i<2?Files.readAllBytes(targets[i]):Files.readString(targets[i]).replace("\r\n","\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                checkHash(current,p,"after"+i);
            }
            Path bridge=count==4?targets[2].resolveSibling(BRIDGE+".smali"):null;
            if(bridge!=null)checkHash(Files.readString(bridge).replace("\r\n","\n").getBytes(java.nio.charset.StandardCharsets.UTF_8),p,"bridge");
            JSONObject doc=manifest(ws);
            if("true".equals(p.getProperty("addedPermission"))) {
                JSONArray nodes=rootNodes(doc);
                for(int i=nodes.length()-1;i>=0;i--)if(isPermission(nodes.optJSONObject(i)))nodes.remove(i);
            }
            // All checks completed before touching any original.
            for(int i=0;i<count;i++)Files.write(targets[i],originals[i]);
            Files.writeString(ws.resolve("AndroidManifest.xml.json"),doc.toString());
            if(bridge!=null)Files.delete(bridge);
            for(String name:BACKUPS)Files.delete(lib.resolve(name));
            if(count==4)for(String name:CLASSES)Files.delete(receipt.resolve(name+".original"));
            Files.delete(receipt.resolve("receipt.properties"));Files.delete(receipt);
        } catch(IOException ex){throw fail(ex.toString());}
    }
    public static void apply(Path ws) { apply(ws,false); }
    public static void apply(Path ws,boolean nativeOnly) {
        Path lib=ws.resolve("root/lib/arm64-v8a"), receipt=ws.resolve(RECEIPT);
        try {
            restore(ws);
            try(var dirs=Files.list(ws.resolve("root/lib"))) {
                if(dirs.anyMatch(p->!p.equals(lib)&&Files.exists(p.resolve(LOADERS[0]))))throw fail("Only a single ARM64 loader is supported.");
            }
            for(String name:List.of("liboverport_original.so","libopenxr_loader_yvr_original.so",StereoPatch.BACKUP))
                if(Files.exists(lib.resolve(name)))throw fail("Existing wrapper chain; restore it before applying MR compatibility.");
            int count=nativeOnly?2:4;
            byte[][] original=new byte[count][], updated=new byte[count][];
            Path[] targets=new Path[count];
            for(int i=0;i<2;i++) {
                targets[i]=lib.resolve(LOADERS[i]);original[i]=Files.readAllBytes(targets[i]);
                String image=new String(original[i],java.nio.charset.StandardCharsets.ISO_8859_1);
                if(image.contains(BACKUPS[0]) || image.contains(BACKUPS[1]) || image.contains(StereoPatch.MARKER) ||
                    image.contains("liboverport_original.so") || image.contains("libopenxr_loader_yvr_original.so"))
                    throw fail("An existing compatibility wrapper has no valid rollback receipt.");
                updated[i]=resource("arm64-v8a/"+RESOURCES[i]);
                Set<String> exports=StereoPatch.exports(original[i]);
                if(!exports.contains("xrGetInstanceProcAddr") || !StereoPatch.exports(updated[i]).containsAll(exports))throw fail("Unsupported direct loader exports.");
            }
            for(int i=2;i<count;i++) {
                targets[i]=findClass(ws,CLASSES[i-2]);
                original[i]=Files.readString(targets[i]).replace("\r\n","\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                String s=new String(original[i],java.nio.charset.StandardCharsets.UTF_8);
                updated[i]=(i==2?patchHelper(s):patchFragment(s)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            Path bridge=null;byte[] bridgeBytes=null;
            if(!nativeOnly){
            if(!targets[2].getParent().equals(targets[3].getParent()))throw fail("Split permission classes are unsupported.");
            bridge=targets[2].resolveSibling(BRIDGE+".smali");
            if(Files.exists(bridge))throw fail("Permission helper collision.");
            bridgeBytes=resource("mr/"+BRIDGE+".smali");
            byte[][] canonical=canonicalize(ws,updated[2],updated[3],bridgeBytes);
            updated[2]=canonical[0];updated[3]=canonical[1];bridgeBytes=canonical[2];
            }
            JSONObject doc=manifest(ws);boolean added=addPermission(doc);
            Properties p=new Properties();p.setProperty("version","1");p.setProperty("addedPermission",""+added);p.setProperty("nativeOnly",""+nativeOnly);
            for(int i=0;i<count;i++){p.setProperty("before"+i,StereoPatch.sha256(original[i]));p.setProperty("after"+i,StereoPatch.sha256(updated[i]));}
            if(bridgeBytes!=null)p.setProperty("bridge",StereoPatch.sha256(bridgeBytes));
            // Preflight complete; originals persist inside the APK for reversible re-patching.
            Files.createDirectories(receipt);
            for(int i=0;i<count;i++)Files.write(i<2?lib.resolve(BACKUPS[i]):receipt.resolve(CLASSES[i-2]+".original"),original[i],StandardOpenOption.CREATE_NEW);
            try(var out=Files.newOutputStream(receipt.resolve("receipt.properties"))){p.store(out,"PFDMR MR rollback");}
            for(int i=0;i<count;i++)Files.write(targets[i],updated[i]);
            if(bridge!=null)Files.write(bridge,bridgeBytes);Files.writeString(ws.resolve("AndroidManifest.xml.json"),doc.toString());
        } catch(IOException ex){throw fail(ex.toString());}
    }
}
