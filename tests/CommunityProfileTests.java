package moe.crx.overport.pfdmr;
import java.nio.file.*;
import java.util.*;
import moe.crx.overport.patching.*;
import com.reandroid.json.*;
public class CommunityProfileTests {
 static int n;static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 static void rejects(Runnable r,String s){try{r.run();}catch(IllegalStateException e){check(true,s);return;}throw new AssertionError(s);}
 public static void main(String[] args)throws Exception {
  System.out.println("CLASS "+CommunityProfiles.class.getProtectionDomain().getCodeSource());
  Path run=Path.of(args[0]),runtime=Path.of(args[2]);Files.createDirectories(run);
  for(String id:CommunityProfiles.IDS)check(PatchStore.INSTANCE.get(id)!=null&&!PatchStore.INSTANCE.get(id).isRecommended(),"optional profile "+id);
  check(CommunityProfiles.expand(Map.of(CommunityProfiles.WANDER,List.of())).containsKey(MossResolutionPatch.ID),"Wander includes resolution");
  check(CommunityProfiles.expand(Map.of(CommunityProfiles.HAND,List.of())).containsKey(StereoPatch.ID),"hand profile includes stereo");
  byte[] fixture=Files.readAllBytes(Path.of(args[3]));var elf=new CommunityElf(fixture);
  byte[] needed=elf.needed("libfp_ovrp.so");Files.write(run.resolve("needed.so"),needed);
  check(new CommunityElf(needed).needed().get(0).equals("libfp_ovrp.so"),"first dependency added");
  byte[] twice=new CommunityElf(needed).needed("libfp_tkeys.so");Files.write(run.resolve("needed-twice.so"),twice);check(new CommunityElf(twice).needed().get(0).equals("libfp_tkeys.so"),"second dependency with relocated headers");
  byte[] renamed=elf.imports(Map.of("pthread_key_create","fp_tkey_create","pthread_key_delete","fp_tkey_delete","pthread_getspecific","fp_tkey_get","pthread_setspecific","fp_tkey_set"));Files.write(run.resolve("renamed.so"),renamed);check(new CommunityElf(renamed).symbols(false).contains("fp_tkey_create"),"four Unity imports renamed");
  byte[] rodata=elf.rodata(Map.of("com.oculus.systemactivities","android","ovrp_Update2","fpov_Update2","ovrp_BeginFrame","fpov_BeginFrame","ovrp_EndFrame","fpov_EndFrame"));Files.write(run.resolve("rodata.so"),rodata);
  var ctx=new PatcherContext("3.4.3-aa54c3f",run.resolve("cache").toFile(),"Fixture.apk","{filename}.{ext}");try(var in=Files.newInputStream(Path.of(args[1]))){ctx.prepare(in);}Path ws=ctx.getWorkingDir().toPath(),lib=ws.resolve(CommunityProfiles.LIB);Files.createDirectories(lib);
  rejects(()->CommunityProfiles.validateSelection(ctx,Map.of(CommunityProfiles.SUPERHOT,List.of())),"wrong game rejected");
  byte[] platform=Files.readAllBytes(runtime.resolve("libovrplatformloader.so"));
  Set<String> ex=new HashSet<>(Files.readAllLines(Path.of("overportcli/src/main/resources/pfdmr/community/org-exports.txt")));
  Files.write(run.resolve("org-fixed.so"),new CommunityElf(new CommunityElf(platform).hide(ex)).needed("libfp_langpack.so"));
  for(String id:List.of(CommunityProfiles.HAND,CommunityProfiles.ELEVEN)){
   String name=id.equals(CommunityProfiles.HAND)?"libopenxr_loader.so":"libovrplatformloader.so";byte[] before=Files.readAllBytes(runtime.resolve(name));Files.write(lib.resolve(name),before);CommunityProfiles.apply(ws,id,"test");
   check(!Arrays.equals(before,Files.readAllBytes(lib.resolve(name))),"runtime edited "+id);
   if(id.equals(CommunityProfiles.HAND)){StereoPatch.apply(ws);StereoPatch.restore(ws);check(true,"hand fix composes with stereo wrapper");}
   CommunityProfiles.restore(ws);check(Arrays.equals(before,Files.readAllBytes(lib.resolve(name))),"exact rollback "+id);
  }
  for(String id:List.of(CommunityProfiles.ROOM,CommunityProfiles.WANDER)){
   Files.write(lib.resolve("libunity.so"),fixture);Files.write(lib.resolve("libOVRPlugin.so"),fixture);CommunityProfiles.apply(ws,id,"test");CommunityProfiles.restore(ws);check(Arrays.equals(fixture,Files.readAllBytes(lib.resolve("libunity.so"))),"Unity rollback "+id);
  }
  Files.createDirectories(ws.resolve("smali/classes"));
  Files.write(lib.resolve("libunity.so"),fixture);Files.write(lib.resolve("libvrapi.so"),fixture);
  CommunityProfiles.apply(ws,CommunityProfiles.CLIMB,"com.crytek.climb");
  check(new CommunityElf(Files.readAllBytes(lib.resolve("libvrapi.so"))).symbols(true).contains("vrapi_GetTimeInSeconds"),"Climb bridge satisfies direct imports");
  StereoPatch.apply(ws);StereoPatch.restore(ws);CommunityProfiles.restore(ws);
  check(Arrays.equals(fixture,Files.readAllBytes(lib.resolve("libvrapi.so"))),"Climb stereo and app initializer rollback together");
  CommunityProfiles.apply(ws,CommunityProfiles.SUPERHOT,"unity.SUPERHOT_Team.SUPERHOT_VR_QA");Path apk=run.resolve("init.apk");ctx.exportAndClose(Files.newOutputStream(apk));check(new com.android.apksig.ApkVerifier.Builder(apk.toFile()).build().verify().isVerified(),"initializer APK signed");ctx.cleanup();try(var in=Files.newInputStream(apk)){ctx.prepare(in);}
  check(Files.readString(ws.resolve("AndroidManifest.xml.json")).contains("moe.crx.pfdmr.CommunityInit"),"private initializer survives manifest encoding");CommunityProfiles.restore(ws);check(!Files.readString(ws.resolve("AndroidManifest.xml.json")).contains("moe.crx.pfdmr.CommunityInit"),"initializer removed after signed reimport");
  CommunityProfiles.apply(ws,CommunityProfiles.HAND,"test");Files.write(lib.resolve("libopenxr_loader.so"),new byte[]{1});rejects(()->CommunityProfiles.restore(ws),"modified native file blocks unsafe rollback");ctx.cleanup();System.out.println("ALL "+n+" COMMUNITY CHECKS PASSED");
 }
}
