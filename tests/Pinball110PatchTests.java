import java.nio.file.*;
import java.util.*;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;

public class Pinball110PatchTests {
 static int n;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 interface Checked {void run()throws Exception;}
 static void reject(Checked c,String s)throws Exception{boolean rejected=false;try{c.run();}catch(IllegalStateException expected){rejected=true;}check(rejected,s);}
 static void patch(PatcherContext c,Map<String,List<String>> a){c.patch(a,(i,p)->kotlin.Unit.INSTANCE);}
 public static void main(String[] args)throws Exception {
  Path run=Path.of(args[0]),input=Path.of(args[1]);Files.createDirectories(run);
  var option=PatchStore.INSTANCE.get(Pinball110Patch.ID);
  check(option!=null&&!option.isRecommended()&&!option.getRequiresOverportConfig(),"separate option defaults off");
  var store=PatchStore.INSTANCE.all();check(store.indexOf(option)+1==store.indexOf(PatchStore.INSTANCE.get(RefreshRatePatch.ID)),"profile immediately above Hz option");
  var ctx=new PatcherContext("3.4.3-23204ea",run.resolve("cache").toFile(),"Owned.apk","{filename}.pinball.{ext}");
  try(var in=Files.newInputStream(input)){ctx.prepare(in);}Path ws=ctx.getWorkingDir().toPath(),lib=ws.resolve("root/lib/arm64-v8a");
  Path receipt=ws.resolve("root/assets/pfdmr-pinball110.properties"),loader=lib.resolve("libopenxr_loader.so"),mruk=lib.resolve("libmrutilitykitshared.so");
  byte[] withMr=Files.readAllBytes(loader);
  reject(()->patch(ctx,Map.of(Pinball110Patch.ID,List.of())),"missing scene selection rejected before restoration");
  check(Arrays.equals(withMr,Files.readAllBytes(loader)),"dependency rejection preserves existing scene loader");
  reject(()->Pinball110Patch.apply(ws,"another.game"),"other package rejected");
  Files.write(lib.resolve("libpfx_base.so"),new byte[1]);
  reject(()->Pinball110Patch.apply(ws,ctx.appPackage()),"standalone legacy shim rejected");Files.delete(lib.resolve("libpfx_base.so"));
  patch(ctx,Map.of());
  byte[] original=Files.readAllBytes(loader),originalMruk=Files.readAllBytes(mruk),config=Files.readAllBytes(lib.resolve("liboverport.config.so"));
  reject(()->Pinball110Patch.apply(ws,ctx.appPackage()),"missing applied scene adapter rejected");
  var selection=new LinkedHashMap<String,List<String>>();selection.put(RefreshRatePatch.ID,List.of());selection.put(Pinball110Patch.ID,List.of());selection.put(StereoPatch.ID,List.of());selection.put(MrCompatibilityPatch.ID,List.of());selection.put(BasicPassthroughPatch.ID,List.of());
  var ran=new ArrayList<String>();ctx.patch(selection,(i,p)->{ran.add(p.getName());return kotlin.Unit.INSTANCE;});
  check(ran.equals(List.of(BasicPassthroughPatch.ID,MrCompatibilityPatch.ID,Pinball110Patch.ID,StereoPatch.ID,RefreshRatePatch.ID)),"basic, scene, profile, stereo, Hz order independent of selection order");
  check(StereoPatch.sha256(Files.readAllBytes(mruk)).equals("38ff3719a3d5abc81c9f6c5bdfd9bfce2df67895974e8bbad9c9eacb02f26f04"),"MRUK exact patched digest");
  check(Arrays.equals(config,Files.readAllBytes(lib.resolve("liboverport.config.so"))),"SpaceWarp and config unchanged");
  byte[] once=Files.readAllBytes(lib.resolve("libpfdmr_pinball110_original.so"));patch(ctx,selection);
  check(Arrays.equals(once,Files.readAllBytes(lib.resolve("libpfdmr_pinball110_original.so"))),"repeat patch retains original without nesting");
  Path output=run.resolve("pinball110.apk");ctx.exportAndClose(Files.newOutputStream(output));
  check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"signed combined APK verified");
  ctx.cleanup();try(var in=Files.newInputStream(output)){ctx.prepare(in);}patch(ctx,Map.of());
  check(Arrays.equals(original,Files.readAllBytes(loader))&&Arrays.equals(originalMruk,Files.readAllBytes(mruk)),"signed roundtrip deselection restores loader and MRUK exactly");
  check(!Files.exists(receipt)&&!Files.exists(lib.resolve("libpfdmr_pinball110_original.so")),"owned rollback metadata removed");
  patch(ctx,Map.of(MrCompatibilityPatch.ID,List.of(),Pinball110Patch.ID,List.of()));
  byte[] validMruk=Files.readAllBytes(mruk);Files.write(mruk,new byte[1]);
  reject(()->Pinball110Patch.restore(ws),"tampered MRUK blocks rollback");
  check(Files.exists(receipt),"failed restoration retains receipt");Files.write(mruk,validMruk);
  Path backup=lib.resolve("libpfdmr_pinball110_original.so");byte[] validBackup=Files.readAllBytes(backup);Files.write(backup,new byte[1]);
  reject(()->Pinball110Patch.restore(ws),"tampered loader backup blocks rollback");Files.write(backup,validBackup);
  Pinball110Patch.restore(ws);
  Files.write(mruk,validMruk); // Independently prepatched MRUK must survive this option's removal.
  Pinball110Patch.apply(ws,ctx.appPackage());Pinball110Patch.restore(ws);
  check(Arrays.equals(validMruk,Files.readAllBytes(mruk)),"prepatched MRUK preserved on deselection");
  Files.write(mruk,new byte[1]);reject(()->Pinball110Patch.apply(ws,ctx.appPackage()),"unknown MRUK rejected before receipt creation");
  check(!Files.exists(receipt),"unknown build leaves no profile receipt");
  ctx.cleanup();System.out.println("ALL "+n+" PINBALL 1.10 CHECKS PASSED");
 }
}
