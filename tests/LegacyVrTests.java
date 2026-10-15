import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.reandroid.json.*;
import moe.crx.overport.patches.LauncherEntryPatchKt;
import moe.crx.overport.patching.*;
import moe.crx.overport.pfdmr.*;
public class LegacyVrTests {
 static int n;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 static JSONObject node(String name,JSONObject... children){JSONArray a=new JSONArray();for(var c:children)a.put(c);return new JSONObject().put("node_type","element").put("name",name).put("nodes",a);}
 static JSONObject filter(String action,String category){return node("intent-filter",LauncherEntryPatchKt.createAction(action),LauncherEntryPatchKt.createCategory(category));}
 static final String MAIN="android.intent.action.MAIN", INFO="android.intent.category.INFO", VR="com.yvr.intent.category.VR";
 static void synthetic(){
  var old=filter(MAIN,INFO);String keep=old.toString();var activity=node("activity",old);var doc=node("document",node("manifest",node("application",activity)));
  LauncherEntryPatchKt.ensureLegacyVrEntry(doc);
  check(activity.getJSONArray("nodes").length()==2,"legacy MAIN/INFO gains VR filter");
  check(old.toString().equals(keep),"original INFO filter preserved");
  String once=doc.toString();LauncherEntryPatchKt.ensureLegacyVrEntry(doc);check(once.equals(doc.toString()),"repatch does not duplicate VR filter");
  var deep=node("activity",filter("android.intent.action.VIEW",INFO));var normal=node("activity",filter(MAIN,"android.intent.category.LAUNCHER"));
  var have=node("activity",filter(MAIN,INFO),filter(MAIN,VR));
  var others=node("document",node("manifest",node("application",deep,normal,have)));String before=others.toString();
  LauncherEntryPatchKt.ensureLegacyVrEntry(others);check(before.equals(others.toString()),"deep links, normal launcher and existing VR entry unchanged");
 }
 public static void main(String[] args)throws Exception{
  synthetic();Path cache=Path.of(args[0]);var ctx=new PatcherContext("3.4.3-aa54c3f",cache.toFile(),"Vader.apk","{filename}.Fixed.{ext}");
  try(var in=Files.newInputStream(Path.of(args[1]))){ctx.prepare(in);}
  ctx.patch(Map.of(QuestGatePatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
  Path out=Path.of(args[2]);try(var stream=Files.newOutputStream(out)){ctx.export(stream);}
  var v=new com.android.apksig.ApkVerifier.Builder(out.toFile()).build().verify();check(v.isVerified(),"corrected APK signature verifies");
  var previous=new com.android.apksig.ApkVerifier.Builder(Path.of(args[1]).toFile()).build().verify();
  check(Arrays.equals(v.getSignerCertificates().get(0).getEncoded(),previous.getSignerCertificates().get(0).getEncoded()),"same signing certificate permits data-preserving update");
  try(var a=new ZipFile(args[1]);var b=new ZipFile(out.toFile())){
   for(var entry:Collections.list(a.entries()))if(entry.getName().startsWith("lib/")&&!entry.isDirectory())check(Arrays.equals(a.getInputStream(entry).readAllBytes(),b.getInputStream(b.getEntry(entry.getName())).readAllBytes()),"native library unchanged: "+entry.getName());
  }
  ctx.cleanup();System.out.println("ALL "+n+" LEGACY VR CHECKS PASSED");
 }
}
