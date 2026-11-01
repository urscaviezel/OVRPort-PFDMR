package moe.crx.overport.pfdmr;
import java.nio.file.*;import java.util.*;import moe.crx.overport.patching.*;import com.reandroid.apk.ApkModule;import com.reandroid.archive.FileInputSource;
public class CommunityPipelineTests {
 public static void main(String[] args)throws Exception{
  Path run=Path.of(args[0]),base=Path.of(args[1]),runtime=Path.of(args[2]),fixture=Path.of(args[3]);Files.createDirectories(run);
  Path smali=run.resolve("input-smali");Files.createDirectories(smali);Files.writeString(smali.resolve("Entry.smali"),".class public Lfixture/Entry;\n.super Ljava/lang/Object;\n");Path dex=run.resolve("input.dex");var so=new org.jf.smali.SmaliOptions();so.apiLevel=29;so.outputDexFile=dex.toString();if(!org.jf.smali.Smali.assemble(so,smali.toString()))throw new AssertionError();
  String[][] targets={{CommunityProfiles.HAND,"com.CubicleNinjas.GuidedTaiChi","1274"},{CommunityProfiles.ROOM,"com.FireproofStudios.TheRoomVR","5011"},{CommunityProfiles.CLIMB,"com.crytek.climb","666"},{CommunityProfiles.ELEVEN,"quest.eleven.forfunlabs","210710133"},{CommunityProfiles.WANDER,"com.parkline.wander","3570"},{CommunityProfiles.SUPERHOT,"unity.SUPERHOT_Team.SUPERHOT_VR_QA","178"},{CommunityProfiles.SUPERHOT,"unity.SUPERHOT_Team.SUPERHOT_VR_QA","161","1.161"},{ClimbStabilityPatch.ID,"com.crytek.climb","666","666.0"}};
  int checks=0;for(String[] target:targets){Path original=run.resolve(target[1]+".apk");try(var a=ApkModule.loadApkFile(base.toFile())){a.getAndroidManifest().setPackageName(target[1]);a.getAndroidManifest().setVersionCode(Integer.parseInt(target[2]));if(target.length>3)a.getAndroidManifest().setVersionName(target[3]);a.refreshManifest();a.keepManifestChanges();for(String name:List.of("libopenxr_loader.so","libovrplatformloader.so"))a.add(new FileInputSource(runtime.resolve(name).toFile(),"lib/arm64-v8a/"+name));for(String name:List.of("libunity.so","libOVRPlugin.so","libvrapi.so"))a.add(new FileInputSource(fixture.toFile(),"lib/arm64-v8a/"+name));if(target[0].equals(CommunityProfiles.CLIMB))a.getAndroidManifest().getOrCreateApplicationElement().createAndroidAttribute("hasCode",16842764).setValueAsBoolean(false);else a.add(new FileInputSource(dex.toFile(),"classes.dex"));a.refreshManifest();a.keepManifestChanges();a.writeApk(original.toFile());}
   var c=new PatcherContext("3.4.3-aa54c3f",run.resolve("cache-"+target[2]).toFile(),"fixture.apk","{filename}.{ext}");try(var in=Files.newInputStream(original)){c.prepare(in);}Path ws=c.getWorkingDir().toPath();
   if(target[0].equals(CommunityProfiles.CLIMB)&&Files.isDirectory(ws.resolve("smali")))throw new AssertionError("Native fixture must not contain DEX");
   c.patch(Map.of(target[0],List.of()),(i,p)->kotlin.Unit.INSTANCE);if(!Files.exists(ws.resolve(CommunityProfiles.RECEIPT)))throw new AssertionError("Missing profile receipt");
   if(List.of(CommunityProfiles.HAND,CommunityProfiles.CLIMB,CommunityProfiles.ELEVEN).contains(target[0])&&!Files.exists(ws.resolve(CommunityProfiles.LIB+StereoPatch.BACKUP)))throw new AssertionError("Missing implicit stereo");
   if(target[0].equals(CommunityProfiles.WANDER)&&!Files.readString(ws.resolve(CommunityProfiles.LIB+"liboverport.config.so")).contains("1800"))throw new AssertionError("Missing implicit resolution");
   Path output=run.resolve(target[1]+"-signed.apk");c.exportAndClose(Files.newOutputStream(output));
   if(target[0].equals(CommunityProfiles.CLIMB)){try(var z=new java.util.zip.ZipFile(output.toFile())){if(z.getEntry("classes.dex")==null)throw new AssertionError("Native app helper DEX missing");}try(var a=ApkModule.loadApkFile(output.toFile())){var attrs=a.getAndroidManifest().getOrCreateApplicationElement().toJson().getJSONArray("attributes");boolean code=false;for(int i=0;i<attrs.length();i++){var attr=attrs.getJSONObject(i);if("hasCode".equals(attr.optString("name")))code=attr.getBoolean("data");}if(!code)throw new AssertionError("Native app must load Java helper");}}
   c.cleanup();try(var in=Files.newInputStream(output)){c.prepare(in);}c.patch(Map.of(target[0],List.of()),(i,p)->kotlin.Unit.INSTANCE); // restore and reapply the complete chain
   if(target[0].equals(ClimbStabilityPatch.ID)){
    if(!Files.readString(ws.resolve(ClimbStabilityPatch.FILE)).contains("enabled=true"))throw new AssertionError("Stability flag missing");
    c.patch(Map.of(CommunityProfiles.CLIMB,List.of()),(i,p)->kotlin.Unit.INSTANCE);
    if(!Files.readString(ws.resolve(ClimbStabilityPatch.FILE)).contains("enabled=false"))throw new AssertionError("Stability disable command missing");
   }
   c.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
   if(Files.exists(ws.resolve(ClimbStabilityPatch.FILE)))throw new AssertionError("Stability asset remains after deselection");
   for(String name:List.of("libunity.so","libOVRPlugin.so","libvrapi.so"))if(!Arrays.equals(Files.readAllBytes(fixture),Files.readAllBytes(ws.resolve(CommunityProfiles.LIB+name))))throw new AssertionError("Original library not restored: "+name);
   if(Files.exists(ws.resolve(CommunityProfiles.RECEIPT))||Files.exists(ws.resolve(CommunityProfiles.LIB+StereoPatch.BACKUP))||Files.readString(ws.resolve("AndroidManifest.xml.json")).contains("moe.crx.pfdmr.CommunityInit"))throw new AssertionError("Owned patch remains");
   c.cleanup();System.out.println("PASS fresh pipeline, implicit dependencies, signed roundtrip, reapply and deselect: "+target[1]);checks++;
  }System.out.println("ALL "+checks+" COMPLETE PROFILE PIPELINES PASSED");
 }
}
