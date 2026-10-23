import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;
public class MrPatchTests {
 static int n;static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 static byte[] entry(Path p,String name)throws Exception{try(var z=new ZipFile(p.toFile())){return z.getInputStream(z.getEntry(name)).readAllBytes();}}
 static Map<String,String> smali(Path root)throws Exception{var m=new TreeMap<String,String>();try(var files=Files.walk(root.resolve("smali"))){for(var f:files.filter(Files::isRegularFile).toList())if(f.toString().contains("permission"))m.put(root.relativize(f).toString(),Files.readString(f).replace("\r\n","\n"));}return m;}
 public static void main(String[] args)throws Exception {
  Path run=Path.of(args[0]),input=Path.of(args[1]);Files.createDirectories(run);
  var patch=PatchStore.INSTANCE.get(MrCompatibilityPatch.ID);check(patch!=null&&!patch.isRecommended()&&!patch.getRequiresOverportConfig(),"registered, optional, defaults off");
  check(PatchStore.INSTANCE.all().size()==29,"all patches retained");
  var ctx=new PatcherContext("3.4.3-23204ea",run.resolve("cache").toFile(),"Owned.apk","{filename}.MR.{ext}");
  try(var in=Files.newInputStream(input)){ctx.prepare(in);}Path ws=ctx.getWorkingDir().toPath();var before=smali(ws);
  byte[] engine=entry(input,"lib/arm64-v8a/libUnreal.so");

  for(String[] names:new String[][]{{"libopenxr_loader.so","libpfdmr_mr_dispatch.so"},{"libopenxr_loader_yvr.so","libpfdmr_mr_yvr.so"}}) {
   var missing=new TreeSet<>(StereoPatch.exports(entry(input,"lib/arm64-v8a/"+names[0])));
   try(var in=MrCompatibilityPatch.class.getResourceAsStream("/pfdmr/arm64-v8a/"+names[1])){missing.removeAll(StereoPatch.exports(in.readAllBytes()));}
   check(missing.isEmpty(),"all direct exports preserved: "+names[0]);
  }
  ctx.patch(Map.of(MrCompatibilityPatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
  check(Files.readString(ws.resolve("AndroidManifest.xml.json")).contains("com.yvr.permission.USE_SCENE"),"real scene permission requested");
  check(Arrays.equals(engine,Files.readAllBytes(ws.resolve("root/lib/arm64-v8a/libUnreal.so"))),"Unreal untouched, no runtime trampolines");
  var once=smali(ws);ctx.patch(Map.of(MrCompatibilityPatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);check(smali(ws).equals(once),"repeat patch stable");
  Path output=run.resolve("mr-enabled.apk");try(var out=Files.newOutputStream(output)){ctx.export(out);}
  check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"enabled APK signature valid");
  ctx.cleanup();try(var in=Files.newInputStream(output)){ctx.prepare(in);}
  ctx.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
  check(smali(ws).equals(before),"deselect after APK roundtrip restores original permission code");
  check(!Files.exists(ws.resolve("root/assets/pfdmr-mr")),"rollback receipt removed");
  check(!Files.readString(ws.resolve("AndroidManifest.xml.json")).contains("com.yvr.permission.USE_SCENE"),"only added scene permission removed");
  Path off=run.resolve("mr-disabled.apk");try(var out=Files.newOutputStream(off)){ctx.export(out);}
  for(String lib:List.of("libopenxr_loader.so","libopenxr_loader_yvr.so","libUnreal.so"))check(Arrays.equals(entry(input,"lib/arm64-v8a/"+lib),entry(off,"lib/arm64-v8a/"+lib)),"exact restore "+lib);
  check(new com.android.apksig.ApkVerifier.Builder(off.toFile()).build().verify().isVerified(),"disabled APK signature valid");
  List<String> ran=new ArrayList<>();
  var selected=new LinkedHashMap<String,List<String>>();selected.put(StereoPatch.ID,List.of());selected.put(MrCompatibilityPatch.ID,List.of());selected.put(BasicPassthroughPatch.ID,List.of());selected.put(RefreshRatePatch.ID,List.of());
  ctx.patch(selected,(i,p)->{ran.add(p.getName());return kotlin.Unit.INSTANCE;});
  check(ran.equals(List.of(BasicPassthroughPatch.ID,MrCompatibilityPatch.ID,StereoPatch.ID,RefreshRatePatch.ID)),"MR executes after library replacement and before stereo regardless of selection order");
  Path combined=run.resolve("mr-stereo.apk");try(var out=Files.newOutputStream(combined)){ctx.export(out);}
  ctx.cleanup();try(var in=Files.newInputStream(combined)){ctx.prepare(in);}
  ctx.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
  check(Arrays.equals(entry(input,"lib/arm64-v8a/libopenxr_loader.so"),Files.readAllBytes(ws.resolve("root/lib/arm64-v8a/libopenxr_loader.so"))),"combined wrappers restore in reverse order without nesting");
  check(smali(ws).equals(before),"combined APK restores permission bridge");
  Path helper;try(var files=Files.walk(ws.resolve("smali"))){helper=files.filter(p->p.toString().endsWith("PermissionHelper.smali")).findFirst().orElseThrow();}
  String helperText=Files.readString(helper);Files.writeString(helper,helperText.replace(".locals 4",".locals 5"));
  boolean rejected=false;try{MrCompatibilityPatch.apply(ws);}catch(IllegalStateException expected){rejected=true;}
  check(rejected&&!Files.exists(ws.resolve("root/assets/pfdmr-mr")),"unknown permission layout rejected before mutations");Files.writeString(helper,helperText);
  ctx.patch(Map.of(MrCompatibilityPatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
  Path backup=ws.resolve("root/lib/arm64-v8a/libpfdmr_mr_original_openxr.so");byte[] backupBytes=Files.readAllBytes(backup);Files.write(backup,new byte[1]);
  rejected=false;try{MrCompatibilityPatch.restore(ws);}catch(IllegalStateException expected){rejected=true;}
  check(rejected&&Files.exists(ws.resolve("root/assets/pfdmr-mr")),"tampered backup blocks rollback without discarding receipt");Files.write(backup,backupBytes);
  ctx.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
  ctx.cleanup();System.out.println("ALL "+n+" MR PIPELINE CHECKS PASSED");
 }
}
