import java.nio.file.*;
import java.util.*;
import com.reandroid.json.JSONObject;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;
public class MossResolutionTests {
 static int n;static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 static String config="root/lib/arm64-v8a/liboverport.config.so";
 static void rejected(Runnable fn,String s){try{fn.run();}catch(IllegalStateException e){check(true,s);return;}throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  Path run=Path.of(args[0]),ws=run.resolve("direct"),file=ws.resolve(config);Files.createDirectories(file.getParent());
  var option=PatchStore.INSTANCE.get(MossResolutionPatch.ID);check(option!=null&&!option.isRecommended(),"Moss switch registered and off by default");
  String original="{\"version\":1,\"suggested_texture_width\":2048,\"disable_space_warp\":1,\"custom_unknown\":{\"keep\":[1,2]},\"suggested_cpu_level\":3}";
  Files.writeString(file,original);MossResolutionPatch.apply(ws);
  var updated=new JSONObject(Files.readString(file));check(updated.getInt("suggested_texture_width")==1800&&updated.getInt("suggested_texture_height")==1664,"exact requested resolution");
  updated.put("suggested_texture_width",2048);updated.remove("suggested_texture_height");check(updated.toString().equals(new JSONObject(original).toString()),"all other keys including SpaceWarp and unknown fields preserved");
  String once=Files.readString(file);MossResolutionPatch.apply(ws);check(once.equals(Files.readString(file)),"repeat application stable");
  MossResolutionPatch.restore(ws);check(new JSONObject(Files.readString(file)).toString().equals(new JSONObject(original).toString()),"rollback restores values and missing keys");
  Files.writeString(file,once);MossResolutionPatch.apply(ws);MossResolutionPatch.restore(ws);check(once.equals(Files.readString(file)),"independently configured 1800x1664 preserved");
  Files.delete(file);rejected(()->MossResolutionPatch.apply(ws),"missing config rejected");
  Files.writeString(file,original);MossResolutionPatch.apply(ws);var tampered=new JSONObject(Files.readString(file)).put("suggested_texture_width",1700);Files.writeString(file,tampered.toString());rejected(()->MossResolutionPatch.restore(ws),"changed resolution blocks unsafe rollback");
  var ctx=new PatcherContext("test",run.resolve("cache").toFile(),"Fixture.apk","{filename}.{ext}");try(var in=Files.newInputStream(Path.of(args[1]))){ctx.prepare(in);}Path cf=ctx.getWorkingDir().toPath().resolve(config);Files.createDirectories(cf.getParent());Files.writeString(cf,original);
  ctx.patch(Map.of(MossResolutionPatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
  Path output=run.resolve("resolution-fixture.apk");ctx.exportAndClose(Files.newOutputStream(output));check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"APK export and signature verified");
  ctx.cleanup();try(var in=Files.newInputStream(output)){ctx.prepare(in);}check(new JSONObject(Files.readString(cf)).getInt("suggested_texture_height")==1664,"resolution survives APK roundtrip");ctx.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
  check(new JSONObject(Files.readString(cf)).toString().equals(new JSONObject(original).toString()),"deselecting after signed reimport restores original config");ctx.cleanup();
  System.out.println("ALL "+n+" MOSS RESOLUTION CHECKS PASSED");
 }
}
