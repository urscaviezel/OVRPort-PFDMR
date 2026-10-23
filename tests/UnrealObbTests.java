import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;
public class UnrealObbTests {
 static int n;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 static String method(String s){int a=s.indexOf(".method public onCreate(Landroid/os/Bundle;)V");if(a<0)a=s.indexOf(".method protected onCreate(Landroid/os/Bundle;)V");return s.substring(a,s.indexOf(".end method",a)+11);}
 static Path activity(Path ws)throws Exception{try(var f=Files.walk(ws.resolve("smali"))){return f.filter(p->p.toString().replace('\\','/').endsWith("/com/epicgames/ue4/GameActivity.smali")).findFirst().orElseThrow();}}
 static void rejects(Runnable r,String s){try{r.run();}catch(IllegalStateException e){check(true,s);return;}throw new AssertionError(s);}
 public static void main(String[] a)throws Exception{
 Path root=Path.of(a[0]),input=Path.of(a[1]);Files.createDirectories(root);
 var ctx=new PatcherContext("3.4.3-23204ea",root.resolve("cache").toFile(),"OutOfScale.apk","{filename}.{ext}");
 var all=PatchStore.INSTANCE.all();int hz=all.indexOf(RefreshRatePatch.PATCH),vk=all.indexOf(VulkanHeapPatch.PATCH);
 check(!RefreshRatePatch.PATCH.isRecommended()&&all.get(hz+1).isRecommended(),"Hz optional immediately above default group");
 check(all.get(vk+1).getName().equals("patch_copy_libraries"),"Vulkan immediately above Copy Libraries");
 var indices=java.util.stream.IntStream.range(0,all.size()).filter(i->all.get(i).isRecommended()).toArray();
 check(indices[indices.length-1]-indices[0]+1==indices.length,"all defaults contiguous");
 check(UnrealObbPatch.PATCH.isRecommended(),"OBB fix defaults on");
 try(var in=Files.newInputStream(input)){ctx.prepare(in);}Path ws=ctx.getWorkingDir().toPath(),act=activity(ws);
 String original=Files.readString(act), expected=Files.readString(Path.of(a[2]));
 String manifest=Files.readString(ws.resolve("AndroidManifest.xml.json"));
 check(!UnrealObbPatch.apply(ws,"other.app")&&Files.readString(act).equals(original),"other packages unchanged");
 ctx.patch(Map.of(UnrealObbPatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
 check(UnrealObbPatch.fingerprint(method(Files.readString(act))).equals(UnrealObbPatch.fingerprint(method(expected))),"exact tested onCreate instructions and control flow");
 check(Files.readString(ws.resolve("AndroidManifest.xml.json")).equals(manifest),"manifest unchanged");
 String patched=Files.readString(act);UnrealObbPatch.apply(ws,"com.kurzgesagt.outofscale");check(Files.readString(act).equals(patched),"idempotent");
 Path helper=act.resolveSibling("b.smali");String helperText=Files.readString(helper);Files.writeString(helper,helperText.replace("array-length v1, v0","array-length v2, v0"));
 rejects(()->UnrealObbPatch.apply(ws,"com.kurzgesagt.outofscale"),"unknown OBB check rejected");Files.writeString(helper,helperText);
 check(Files.readString(act).equals(patched),"rejection preserves activity");
 Path output=root.resolve("patched.apk");ctx.exportAndClose(Files.newOutputStream(output));
 try(var before=new java.util.zip.ZipFile(input.toFile());var after=new java.util.zip.ZipFile(output.toFile())){
 var entries=before.entries();boolean same=true;while(entries.hasMoreElements()){var e=entries.nextElement();if(e.getName().startsWith("lib/")||e.getName().startsWith("assets/")){var out=after.getEntry(e.getName());same &= out!=null&&out.getSize()==e.getSize()&&out.getCrc()==e.getCrc();}}
 check(same,"native libraries and original assets unchanged");
 }
 check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"signed APK valid");ctx.cleanup();
 try(var in=Files.newInputStream(output)){ctx.prepare(in);}act=activity(ws);
 check(UnrealObbPatch.fingerprint(method(Files.readString(act))).equals(UnrealObbPatch.fingerprint(method(expected))),"exported DEX matches tested fix");
 ctx.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
 check(UnrealObbPatch.fingerprint(method(Files.readString(act))).equals(UnrealObbPatch.fingerprint(method(original))),"deselect restores after APK roundtrip");
 Files.writeString(act,patched);UnrealObbPatch.apply(ws,"com.kurzgesagt.outofscale");UnrealObbPatch.restore(ws);
 check(Files.readString(act).equals(patched),"existing manual fix preserved");
 Files.writeString(act,original.replace("->VersionCode:I","->Unexpected:I"));
 rejects(()->UnrealObbPatch.apply(ws,"com.kurzgesagt.outofscale"),"unknown startup pattern rejected");
 ctx.cleanup();System.out.println("ALL "+n+" OBB/ORDER CHECKS PASSED");
 }
}
