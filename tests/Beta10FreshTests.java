import java.nio.file.*;
import java.util.*;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;
public class Beta10FreshTests {
 public static void main(String[] args)throws Exception{
  Path run=Path.of(args[0]),cache=run.resolve("cache"),source=Path.of(args[2]);String version=args[3];Files.createDirectories(cache);
  Files.copy(source.resolve("installed.json"),cache.resolve("installed.json"),StandardCopyOption.REPLACE_EXISTING);
  Path libs=source.resolve("libraries/"+version),dest=cache.resolve("libraries/"+version);
  try(var files=Files.walk(libs)){for(Path p:files.toList()){Path target=dest.resolve(libs.relativize(p));if(Files.isDirectory(p))Files.createDirectories(target);else Files.copy(p,target,StandardCopyOption.REPLACE_EXISTING);}}
  var c=new PatcherContext(version,cache.toFile(),"Owned.apk","{filename}.beta10.{ext}");try(var in=Files.newInputStream(Path.of(args[1]))){c.prepare(in);}
  var selection=new LinkedHashMap<String,List<String>>();for(var p:PatchStore.INSTANCE.recommended())selection.put(p.getName(),List.of());
  boolean vader=args.length>4&&args[4].equals("vader");
  if(vader){selection.put(QuestGatePatch.ID,List.of());selection.put(VaderAudioPatch.ID,List.of());}else selection.put(SpatialZombiesPatch.ID,List.of());c.patch(selection,(i,p)->{System.out.println("PATCH "+p.getName());return kotlin.Unit.INSTANCE;});
  Path ws=c.getWorkingDir().toPath();
  if(vader){
   byte[] lib=Files.readAllBytes(ws.resolve("root/lib/arm64-v8a/libUE4.so"));int at=VaderAudioPatch.inspect(lib);
   if(java.nio.ByteBuffer.wrap(lib).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(at)!=0x14000046)throw new AssertionError("mixer");
  }else{
   if(!Files.readString(ws.resolve("AndroidManifest.xml.json")).contains("com.yvr.permission.USE_SCENE"))throw new AssertionError("scene permission missing");
   if(!UnityBundlePatch.hash(ws.resolve("root/lib/arm64-v8a/libmrutilitykitshared.so")).equals(MrukPosePatch.ZOMBIES.after()))throw new AssertionError("MRUK");
   if(Files.exists(ws.resolve("root/lib/arm64-v8a/libspatial_raw_dispatch.so")))throw new AssertionError("diagnostic wrapper");
  }
  Path out=run.resolve(vader?"vader-fresh-beta10.apk":"spatial-fresh-beta10.apk");c.exportAndClose(Files.newOutputStream(out));
  if(!new com.android.apksig.ApkVerifier.Builder(out.toFile()).build().verify().isVerified())throw new AssertionError("signature");
  if(vader){
   c.cleanup();try(var in=Files.newInputStream(out)){c.prepare(in);}
   c.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
   if(Files.exists(c.getWorkingDir().toPath().resolve("root/assets/pfdmr-vader-audio.properties")))throw new AssertionError("mixer receipt");
  }
  c.cleanup();System.out.println("PASS original APK + default patches + " + (vader?"Vader/Quest profiles, signed reimport and rollback":"Spatial profile only; signed export, scene permission, MRUK, no diagnostic wrappers"));
 }
}
