import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import moe.crx.overport.pfdmr.UnrealAudioPatch;
import moe.crx.overport.patching.*;
public class AudioPatchTests {
 static int passed;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);System.out.println("PASS "+why);passed++;}
 public static void main(String[] a)throws Exception {
  var patch=PatchStore.INSTANCE.get(UnrealAudioPatch.ID);
  check(patch!=null&&!patch.isRecommended()&&!patch.getRequiresOverportConfig(),"optional patch registered without runtime dependency");
  check(PatchStore.INSTANCE.recommended().stream().noneMatch(p->p.getName().equals(UnrealAudioPatch.ID)),"disabled by default");
  var composer=(androidx.compose.runtime.Composer)java.lang.reflect.Proxy.newProxyInstance(AudioPatchTests.class.getClassLoader(),new Class<?>[]{androidx.compose.runtime.Composer.class},(proxy,method,values)->method.getReturnType()==boolean.class?false:method.getReturnType()==int.class?0:null);
  check(moe.crx.overport.app.util.PatchLocalizer.INSTANCE.localizedPatch(patch,composer,0).equals(UnrealAudioPatch.LABEL),"GUI displays the audio switch label");
  String synthetic=".method public onCreate(Landroid/os/Bundle;)V\n    .registers 24\n    return-void\n.end method\n";
  String injected=UnrealAudioPatch.inject(synthetic);
  check(injected.contains("invoke-static/range {p0 .. p0}"),"range invocation supports large register counts");
  check(injected.equals(UnrealAudioPatch.inject(injected)),"hook injection is idempotent");
  try{UnrealAudioPatch.inject(".class public LUnity;\n");throw new AssertionError("Unknown activity accepted");}catch(IllegalStateException expected){check(true,"unsupported activity rejected");}
  Path root=Path.of(a[0]);Files.createDirectories(root);
  var ctx=new PatcherContext("3.4.3-aa54c3f",root.resolve("cache").toFile(),"ActionHero.apk","{filename}.Audio48.{ext}");
  try(var in=Files.newInputStream(Path.of(a[1]))){ctx.prepare(in);}
  ctx.patch(Map.of(UnrealAudioPatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
  Path ws=ctx.getWorkingDir().toPath();
  String activity=Files.readString(ws.resolve("smali/classes/com/epicgames/unreal/GameActivity.smali"));
  check(activity.indexOf("Lmoe/crx/pfdmr/Audio48;->apply") < activity.indexOf("invoke-super {p0, p1}, Landroid/app/NativeActivity;->onCreate"),"hook runs before native engine onCreate");
  String config=Files.readString(ws.resolve("root/assets/pfdmr-audio48.properties"));
  check(config.contains("enabled=true")&&config.contains("UnrealGame/Solero/Solero/Saved/Config/Android/Engine.ini"),"Action Hero project path detected");
  check(Files.exists(ws.resolve("smali/classes/moe/crx/pfdmr/Audio48.smali")),"helper placed in same DEX as activity");
  UnrealAudioPatch.apply(ws);check(Files.readString(ws.resolve("smali/classes/com/epicgames/unreal/GameActivity.smali")).equals(activity),"repatch does not duplicate hook");
  Path output=root.resolve("ActionHero-Audio48-test.apk");try(var out=Files.newOutputStream(output)){ctx.export(out);}
  check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"rebuilt test APK signature verifies");
  try(var input=new ZipFile(a[1]);var apk=new ZipFile(output.toFile())){
   for(String name:List.of("lib/arm64-v8a/libUnreal.so","assets/UECommandLine.txt"))
    check(Arrays.equals(input.getInputStream(input.getEntry(name)).readAllBytes(),apk.getInputStream(apk.getEntry(name)).readAllBytes()),"unchanged "+name);
  }
  ctx.cleanup();try(var in=Files.newInputStream(output)){ctx.prepare(in);}
  ctx.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
  check(Files.readString(ws.resolve("root/assets/pfdmr-audio48.properties")).contains("enabled=false"),"unchecking on repatch enables runtime restoration");
  Path off=root.resolve("ActionHero-Audio48-off-test.apk");try(var out=Files.newOutputStream(off)){ctx.export(out);}
  check(new com.android.apksig.ApkVerifier.Builder(off.toFile()).build().verify().isVerified(),"disabled APK signature verifies");
  ctx.cleanup();System.out.println("ALL "+passed+" AUDIO PATCH CHECKS PASSED");
 }
}
