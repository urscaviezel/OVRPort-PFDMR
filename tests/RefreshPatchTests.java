import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;
public class RefreshPatchTests {
 static int n;static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 static byte[] entry(Path p,String name)throws Exception{try(var z=new ZipFile(p.toFile())){return z.getInputStream(z.getEntry(name)).readAllBytes();}}
 public static void main(String[] args)throws Exception {
  Path run=Path.of(args[0]),input=Path.of(args[1]);Files.createDirectories(run);
  var patch=PatchStore.INSTANCE.get(RefreshRatePatch.ID);check(patch!=null&&!patch.isRecommended()&&!patch.getRequiresOverportConfig(),"optional and defaults off");
  var ctx=new PatcherContext("3.4.3-23204ea",run.resolve("cache").toFile(),"Owned.apk","{filename}.refresh.{ext}");
  try(var in=Files.newInputStream(input)){ctx.prepare(in);}Path ws=ctx.getWorkingDir().toPath();
  String manifest=Files.readString(ws.resolve("AndroidManifest.xml.json"));
  byte[] loader=entry(input,"lib/arm64-v8a/libopenxr_loader.so"),config=entry(input,"lib/arm64-v8a/liboverport.config.so");
  ctx.patch(Map.of(RefreshRatePatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
  check(Arrays.equals(config,Files.readAllBytes(ws.resolve("root/lib/arm64-v8a/liboverport.config.so"))),"config including SpaceWarp unchanged");
  check(Files.readString(ws.resolve("AndroidManifest.xml.json")).equals(manifest),"manifest unchanged");
  ctx.patch(Map.of(RefreshRatePatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
  check(Arrays.equals(loader,Files.readAllBytes(ws.resolve("root/lib/arm64-v8a/libpfdmr_refresh_original.so"))),"repeat patch preserves original without nesting");
  Path output=run.resolve("refresh.apk");var stream=Files.newOutputStream(output);ctx.exportAndClose(stream);
  boolean closed=false;try{stream.write(1);}catch(java.io.IOException expected){closed=true;}check(closed,"desktop export closes output stream immediately");
  Path renamed=run.resolve("renamed.apk");Files.move(output,renamed);Files.move(renamed,output);check(true,"exported APK immediately renameable on Windows");
  check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"signed APK valid");
  ctx.cleanup();try(var in=Files.newInputStream(output)){ctx.prepare(in);}
  ctx.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
  check(Arrays.equals(loader,Files.readAllBytes(ws.resolve("root/lib/arm64-v8a/libopenxr_loader.so"))),"deselect after APK roundtrip restores original");
  ctx.patch(Map.of(RefreshRatePatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
  Path backup=ws.resolve("root/lib/arm64-v8a/libpfdmr_refresh_original.so");Files.write(backup,new byte[1]);boolean rejected=false;
  try{RefreshRatePatch.restore(ws);}catch(IllegalStateException expected){rejected=true;}
  check(rejected&&Files.exists(ws.resolve("root/assets/pfdmr-refresh.properties")),"tampered backup rejected without discarding receipt");
  ctx.cleanup();Files.delete(output);check(true,"output remains deletable after cleanup");
  // A failed export must also close the owned destination stream.
  class Tracking extends java.io.ByteArrayOutputStream {boolean closedFlag;public void close(){closedFlag=true;}}
  var tracking=new Tracking();try{ctx.exportAndClose(tracking);}catch(Exception expected){}
  check(tracking.closedFlag,"failed export closes owned stream");
  System.out.println("ALL "+n+" REFRESH/FILE LIFETIME CHECKS PASSED");
 }
}
