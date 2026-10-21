import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;
public class BasicPassthroughTests {
 static int n;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 static byte[] entry(Path p,String name)throws Exception{try(var z=new ZipFile(p.toFile())){return z.getInputStream(z.getEntry(name)).readAllBytes();}}
 static boolean rejects(Runnable action){try{action.run();return false;}catch(IllegalStateException e){return true;}}
 public static void main(String[] args)throws Exception {
  Path run=Path.of(args[0]), input=Path.of(args[1]);Files.createDirectories(run);
  String name="lib/arm64-v8a/libopenxr_loader.so";
  byte[] original=entry(input,name), expected=original.clone();expected[0x18768]=(byte)0xed;
  var patch=PatchStore.INSTANCE.get(BasicPassthroughPatch.ID);
  check(patch!=null&&!patch.isRecommended()&&!patch.getRequiresOverportConfig(),"registered, optional, defaults off");
  Path raw=run.resolve("raw"),loader=raw.resolve("root/"+name),receipt=raw.resolve("root/assets/pfdmr-basic-passthrough.txt");
  Files.createDirectories(loader.getParent());Files.write(loader,original);
  BasicPassthroughPatch.apply(raw);check(Arrays.equals(expected,Files.readAllBytes(loader)),"exact known one-byte instruction change");
  BasicPassthroughPatch.apply(raw);check(Arrays.equals(expected,Files.readAllBytes(loader)),"repeat patch stable");
  Files.write(loader,new byte[20]);check(rejects(()->BasicPassthroughPatch.restore(raw))&&Files.exists(receipt),"tampered loader rejected with receipt retained");
  Files.write(loader,expected);BasicPassthroughPatch.restore(raw);check(Arrays.equals(original,Files.readAllBytes(loader))&&!Files.exists(receipt),"exact owned rollback");
  Files.write(loader,expected);BasicPassthroughPatch.apply(raw);BasicPassthroughPatch.restore(raw);check(Arrays.equals(expected,Files.readAllBytes(loader))&&!Files.exists(receipt),"external patch not claimed or undone");
  byte[] unknown=original.clone();unknown[0]^=1;Files.write(loader,unknown);check(rejects(()->BasicPassthroughPatch.apply(raw))&&Arrays.equals(unknown,Files.readAllBytes(loader))&&!Files.exists(receipt),"unknown loader rejected without mutations");
  Files.write(loader,original);Path other=raw.resolve("root/lib/x86/libopenxr_loader.so");Files.createDirectories(other.getParent());Files.write(other,original);check(rejects(()->BasicPassthroughPatch.apply(raw)),"multiple architectures rejected");
  var ctx=new PatcherContext("3.4.3-23204ea",run.resolve("cache").toFile(),"Owned.apk","{filename}.basic.{ext}");
  try(var in=Files.newInputStream(input)){ctx.prepare(in);}Path ws=ctx.getWorkingDir().toPath();
  String manifest=Files.readString(ws.resolve("AndroidManifest.xml.json"));
  ctx.patch(Map.of(BasicPassthroughPatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
  check(Files.readString(ws.resolve("AndroidManifest.xml.json")).equals(manifest),"manifest unchanged, no spatial permissions");
  Path output=run.resolve("basic-enabled.apk");try(var out=Files.newOutputStream(output)){ctx.export(out);}
  check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"signed export valid");
  check(Arrays.equals(expected,entry(output,name)),"export contains exact native fix");
  for(String untouched:List.of("lib/arm64-v8a/libUE4.so","assets/main.obb.png"))check(Arrays.equals(entry(input,untouched),entry(output,untouched)),"untouched "+untouched);
  ctx.cleanup();try(var in=Files.newInputStream(output)){ctx.prepare(in);}
  ctx.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
  check(Arrays.equals(original,Files.readAllBytes(ws.resolve("root/"+name))),"deselect after APK roundtrip restores loader");
  ctx.cleanup();System.out.println("ALL "+n+" BASIC PASSTHROUGH CHECKS PASSED");
 }
}
