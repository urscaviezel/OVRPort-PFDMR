package moe.crx.overport.pfdmr;
import java.nio.file.*;import java.util.*;import moe.crx.overport.patching.*;import com.reandroid.apk.ApkModule;import com.reandroid.archive.FileInputSource;
public class CommunityPipelineTests {
 public static void main(String[] args)throws Exception{
  Path run=Path.of(args[0]),base=Path.of(args[1]),runtime=Path.of(args[2]),fixture=Path.of(args[3]);Files.createDirectories(run);
  Path smali=run.resolve("input-smali");Files.createDirectories(smali);Files.writeString(smali.resolve("Entry.smali"),".class public Lfixture/Entry;\n.super Ljava/lang/Object;\n");Path dex=run.resolve("input.dex");var so=new org.jf.smali.SmaliOptions();so.apiLevel=29;so.outputDexFile=dex.toString();if(!org.jf.smali.Smali.assemble(so,smali.toString()))throw new AssertionError();
  String[][] targets={{CommunityProfiles.HAND,"com.CubicleNinjas.GuidedTaiChi","1274"},{CommunityProfiles.ROOM,"com.FireproofStudios.TheRoomVR","5011"},{CommunityProfiles.CLIMB,"com.crytek.climb","666"},{CommunityProfiles.ELEVEN,"quest.eleven.forfunlabs","210710133"},{CommunityProfiles.WANDER,"com.parkline.wander","3570"},{CommunityProfiles.SUPERHOT,"unity.SUPERHOT_Team.SUPERHOT_VR_QA","178"}};
  int checks=0;for(String[] target:targets){Path original=run.resolve(target[1]+".apk");try(var a=ApkModule.loadApkFile(base.toFile())){a.getAndroidManifest().setPackageName(target[1]);a.getAndroidManifest().setVersionCode(Integer.parseInt(target[2]));a.refreshManifest();a.keepManifestChanges();for(String name:List.of("libopenxr_loader.so","libovrplatformloader.so"))a.add(new FileInputSource(runtime.resolve(name).toFile(),"lib/arm64-v8a/"+name));for(String name:List.of("libunity.so","libOVRPlugin.so","libvrapi.so"))a.add(new FileInputSource(fixture.toFile(),"lib/arm64-v8a/"+name));a.add(new FileInputSource(dex.toFile(),"classes.dex"));a.writeApk(original.toFile());}
   var c=new PatcherContext("3.4.3-aa54c3f",run.resolve("cache-"+target[2]).toFile(),"fixture.apk","{filename}.{ext}");try(var in=Files.newInputStream(original)){c.prepare(in);}Path ws=c.getWorkingDir().toPath();
   c.patch(Map.of(target[0],List.of()),(i,p)->kotlin.Unit.INSTANCE);if(!Files.exists(ws.resolve(CommunityProfiles.RECEIPT)))throw new AssertionError("Missing profile receipt");
   if(List.of(CommunityProfiles.HAND,CommunityProfiles.CLIMB,CommunityProfiles.ELEVEN).contains(target[0])&&!Files.exists(ws.resolve(CommunityProfiles.LIB+StereoPatch.BACKUP)))throw new AssertionError("Missing implicit stereo");
   if(target[0].equals(CommunityProfiles.WANDER)&&!Files.readString(ws.resolve(CommunityProfiles.LIB+"liboverport.config.so")).contains("1800"))throw new AssertionError("Missing implicit resolution");
   Path output=run.resolve(target[1]+"-signed.apk");c.exportAndClose(Files.newOutputStream(output));c.cleanup();try(var in=Files.newInputStream(output)){c.prepare(in);}c.patch(Map.of(target[0],List.of()),(i,p)->kotlin.Unit.INSTANCE); // restore and reapply the complete chain
   c.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
   for(String name:List.of("libunity.so","libOVRPlugin.so","libvrapi.so"))if(!Arrays.equals(Files.readAllBytes(fixture),Files.readAllBytes(ws.resolve(CommunityProfiles.LIB+name))))throw new AssertionError("Original library not restored: "+name);
   if(Files.exists(ws.resolve(CommunityProfiles.RECEIPT))||Files.exists(ws.resolve(CommunityProfiles.LIB+StereoPatch.BACKUP))||Files.readString(ws.resolve("AndroidManifest.xml.json")).contains("moe.crx.pfdmr.CommunityInit"))throw new AssertionError("Owned patch remains");
   c.cleanup();System.out.println("PASS fresh pipeline, implicit dependencies, signed roundtrip, reapply and deselect: "+target[1]);checks++;
  }System.out.println("ALL "+checks+" COMPLETE PROFILE PIPELINES PASSED");
 }
}
