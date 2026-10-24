package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.reandroid.json.*;
import kotlin.Unit;
import moe.crx.overport.patching.*;

/** Exact Unity profile; includes the native scene adapters, without diagnostic wrappers. */
public final class SpatialZombiesPatch {
 public static final String ID="patch_pfdmr_spatial_zombies";
 public static final String LABEL="PFDMR compatibility: Spatial Zombies MR & controllers (experimental)";
 private static final String RECEIPT="root/assets/pfdmr-spatial-zombies",BUNDLE="root/assets/bin/Data/data.unity3d",META="root/assets/bin/Data/Managed/Metadata/global-metadata.dat",MRUK="root/lib/arm64-v8a/libmrutilitykitshared.so";
 private static final String CLASS="com/unity3d/player/UnityPlayerGameActivity.smali",HELPER="moe/crx/pfdmr/";
 private static final String[] HELPERS={"SpatialSettings.smali","SpatialSettings$1.smali","SpatialSettings$2.smali"};
 private static final String METHOD="startActivityForResult(Landroid/content/Intent;ILandroid/os/Bundle;)V";
 public static final Patch PATCH=new Patch(ID,false,false,(e,a)->{e.selectWorkspace(f->{apply(f.getFile().toPath());return Unit.INSTANCE;});return Unit.INSTANCE;});
 private static IllegalStateException fail(String s){return new IllegalStateException("Spatial Zombies: "+s);}
 private static byte[] resource(String name)throws IOException{try(var in=SpatialZombiesPatch.class.getResourceAsStream("/pfdmr/spatial-zombies/"+name)){if(in==null)throw fail("Missing resource: "+name);return in.readAllBytes();}}
 private static JSONObject profile()throws IOException{return new JSONObject(new String(resource("profile.json"),StandardCharsets.UTF_8));}
 private static Path activity(Path ws)throws IOException{try(var files=Files.walk(ws.resolve("smali"))){var matches=files.filter(Files::isRegularFile).filter(p->p.toString().replace('\\','/').endsWith("/"+CLASS)).toList();if(matches.size()!=1)throw fail("Expected one UnityPlayerGameActivity.");return matches.get(0);}}
 private static Path helperDir(Path activity){return activity.getParent().getParent().getParent().getParent().resolve(HELPER);}
 private static byte[] normalized(Path path)throws IOException{return Files.readString(path).replace("\r\n","\n").getBytes(StandardCharsets.UTF_8);}
 private static void check(Path path,String digest)throws IOException{if(digest==null||!UnityBundlePatch.hash(path).equals(digest))throw fail("Changed or unsupported file: "+path.getFileName());}
 private static Properties readReceipt(Path dir)throws IOException{Properties p=new Properties();try(var in=Files.newInputStream(dir.resolve("receipt.properties"))){p.load(in);}if(!"1".equals(p.getProperty("version")))throw fail("Invalid receipt.");return p;}
 public static void validateSelection(PatcherContext c,Map<String,List<String>> args){
  if(!args.containsKey(ID))return;
  if(!"com.mrf.TomFoerster.ZombiesMR".equals(c.appPackage())||!"1.2.8.5".equals(c.appVersionName())||!"154".equals(c.appVersionCode()))throw fail("Only ARM64 version 1.2.8.5 (154) is supported.");
  try{
   Path ws=c.getWorkingDir().toPath(),r=ws.resolve(RECEIPT);JSONObject p=profile();
   try(var files=Files.list(ws.resolve("root/lib/arm64-v8a"))){if(files.anyMatch(f->f.getFileName().toString().startsWith("libspatial_raw_")))throw fail("Standalone diagnostic build detected. Use the original APK.");}
   if(Files.exists(r)){verify(ws,r,readReceipt(r));return;}
   check(ws.resolve(BUNDLE),p.getString("bundle"));check(ws.resolve(META),p.getString("metadata"));
   MrukPosePatch.apply(Files.readAllBytes(ws.resolve(MRUK)),MrukPosePatch.ZOMBIES);
   inject(Files.readString(activity(ws)));
   for(String h:HELPERS)if(Files.exists(helperDir(activity(ws)).resolve(h)))throw fail("Settings helper collision.");
  }catch(IOException ex){throw fail(ex.toString());}
 }
 public static byte[] metadata(byte[] data,JSONObject profile){
  if(!StereoPatch.sha256(data).equals(profile.getString("metadata")))throw fail("Unsupported metadata.");
  byte[] out=data.clone();ByteBuffer b=ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);
  int table=b.getInt(8),tableBytes=b.getInt(12),count=b.getInt(16),start=b.getInt(20),size=b.getInt(24);
  if(b.getInt(0)!=0xfab11baf||b.getInt(4)!=106||count<26692||tableBytes!=(long)count*4||count!=b.getInt(28)+1||table<32||start<table+tableBytes||size<0||(long)start+size>out.length)throw fail("Unsupported IL2CPP header.");
  Map<Integer,String[]> replacements=Map.of(26683,new String[]{"com.oculus.panelapp.settings","com.yvr.spatialhome"},26684,new String[]{"com.oculus.panelapp.settings.SettingsActivity","com.yvr.launchersdk.UnityPlayerActivity"},26688,new String[]{"com.oculus.permission.USE_SCENE","com.yvr.permission.USE_SCENE"},26690,new String[]{"com.oculus.vrshell","com.yvr.spatialhome"});
  var blob=new ByteArrayOutputStream();int previous=0;
  for(int i=0;i<count-1;i++){int lo=b.getInt(table+i*4),hi=b.getInt(table+(i+1)*4);if(lo!=previous||hi<lo||hi>size)throw fail("Invalid literal range.");byte[] text=Arrays.copyOfRange(data,start+lo,start+hi);previous=hi;
   b.putInt(table+i*4,blob.size());if(replacements.containsKey(i)){String[] r=replacements.get(i);if(!Arrays.equals(text,r[0].getBytes(StandardCharsets.UTF_8)))throw fail("Literal mismatch.");text=r[1].getBytes(StandardCharsets.UTF_8);}blob.writeBytes(text);
  }
  if(previous!=size||blob.size()>size)throw fail("Invalid literal allocation.");b.putInt(table+(count-1)*4,blob.size());Arrays.fill(out,start,start+size,(byte)0);System.arraycopy(blob.toByteArray(),0,out,start,blob.size());
  if(!StereoPatch.sha256(out).equals(profile.getString("metadataAfter")))throw fail("Metadata result mismatch.");return out;
 }
 public static String inject(String text){
  if(!text.contains(".super Lcom/google/androidgamesdk/GameActivity;")||text.contains(METHOD)||text.contains("Lmoe/crx/pfdmr/SpatialSettings;"))throw fail("Unsupported or previously modified Unity activity.");
  return text+"\n.method public "+METHOD+"\n    .locals 0\n    invoke-static {p0, p1}, Lmoe/crx/pfdmr/SpatialSettings;->map(Landroid/app/Activity;Landroid/content/Intent;)Landroid/content/Intent;\n    move-result-object p1\n    if-eqz p1, :settings_handled\n    invoke-super {p0, p1, p2, p3}, Lcom/google/androidgamesdk/GameActivity;->"+METHOD+"\n    :settings_handled\n    return-void\n.end method\n";
 }
 private static Map<String,byte[]> canonical(Path temp,String activity)throws IOException{
  Path src=Files.createDirectory(temp.resolve("smali-src")),out=temp.resolve("smali-out"),dex=temp.resolve("helper.dex");
  Files.writeString(src.resolve("Activity.smali"),activity);for(String h:HELPERS)Files.write(src.resolve(h),resource(h));
  var options=new org.jf.smali.SmaliOptions();options.apiLevel=29;options.jobs=1;options.outputDexFile=dex.toString();if(!org.jf.smali.Smali.assemble(options,src.toString()))throw fail("Settings helper assembly failed.");
  var opts=new org.jf.baksmali.BaksmaliOptions();opts.apiLevel=29;opts.localsDirective=true;opts.sequentialLabels=true;opts.skipDuplicateLineNumbers=true;
  var file=new org.jf.dexlib2.dexbacked.DexBackedDexFile(org.jf.dexlib2.Opcodes.forApi(29),Files.readAllBytes(dex));if(!org.jf.baksmali.Baksmali.disassembleDexFile(file,out.toFile(),1,opts))throw fail("Settings helper verification failed.");
  Map<String,byte[]> result=new LinkedHashMap<>();result.put("activity",normalized(out.resolve(CLASS)));for(String h:HELPERS)result.put(h,normalized(out.resolve(HELPER+h)));return result;
 }
 private static void deleteTree(Path path)throws IOException{if(Files.exists(path))try(var files=Files.walk(path)){for(Path f:files.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}}
 public static void apply(Path ws){
  Path temp=null;
  try{
   if(Files.exists(ws.resolve(RECEIPT)))throw fail("Restore the profile before applying it again.");
   JSONObject profile=profile();check(ws.resolve(BUNDLE),profile.getString("bundle"));
   temp=Files.createTempDirectory(ws,"spatial-stage-");Path r=ws.resolve(RECEIPT),act=activity(ws),helpers=helperDir(act);
   Map<Path,byte[]> before=new LinkedHashMap<>(),after=new LinkedHashMap<>();
   before.put(ws.resolve(META),Files.readAllBytes(ws.resolve(META)));after.put(ws.resolve(META),metadata(before.get(ws.resolve(META)),profile));
   before.put(ws.resolve(MRUK),Files.readAllBytes(ws.resolve(MRUK)));after.put(ws.resolve(MRUK),MrukPosePatch.apply(before.get(ws.resolve(MRUK)),MrukPosePatch.ZOMBIES));
   before.put(act,normalized(act));Map<String,byte[]> smali=canonical(temp,inject(new String(before.get(act),StandardCharsets.UTF_8)));after.put(act,smali.get("activity"));
   for(String h:HELPERS)if(Files.exists(helpers.resolve(h)))throw fail("Helper collision.");
   Path candidate=temp.resolve("bundle"),rollback=temp.resolve("bundle.rollback");try(var bundle=new UnityBundlePatch(ws.resolve(BUNDLE))){bundle.patch(profile,candidate,rollback);}
   Properties receipt=new Properties();receipt.setProperty("version","1");receipt.setProperty("bundle.before",profile.getString("bundle"));receipt.setProperty("bundle.after",UnityBundlePatch.hash(candidate));receipt.setProperty("bundle.rollback",UnityBundlePatch.hash(rollback));
   int i=0;for(var e:before.entrySet()){receipt.setProperty("before"+i,StereoPatch.sha256(e.getValue()));receipt.setProperty("after"+i,StereoPatch.sha256(after.get(e.getKey())));i++;}
   for(String h:HELPERS)receipt.setProperty(h,StereoPatch.sha256(smali.get(h)));
   // Complete asset and bytecode validation before native adapter installation.
   boolean mrApplied=false,bundleMoved=false;Path originalBundle=temp.resolve("original-bundle");
   try{
    MrCompatibilityPatch.apply(ws,true);mrApplied=true;Files.createDirectories(r);i=0;for(byte[] data:before.values())Files.write(r.resolve("original"+(i++)),data,StandardOpenOption.CREATE_NEW);
    Files.move(rollback,r.resolve("bundle.rollback"));Files.move(ws.resolve(BUNDLE),originalBundle);bundleMoved=true;Files.move(candidate,ws.resolve(BUNDLE));
    for(var e:after.entrySet())Files.write(e.getKey(),e.getValue());Files.createDirectories(helpers);for(String h:HELPERS)Files.write(helpers.resolve(h),smali.get(h));
    try(var out=Files.newOutputStream(r.resolve("receipt.properties"))){receipt.store(out,"Spatial Zombies reversible profile");}
   }catch(Exception ex){
    for(var e:before.entrySet())Files.write(e.getKey(),e.getValue());if(bundleMoved)Files.move(originalBundle,ws.resolve(BUNDLE),StandardCopyOption.REPLACE_EXISTING);
    for(String h:HELPERS)Files.deleteIfExists(helpers.resolve(h));if(mrApplied)MrCompatibilityPatch.restore(ws);deleteTree(r);throw ex;
   }
  }catch(IOException ex){throw fail(ex.toString());}finally{if(temp!=null)try{deleteTree(temp);}catch(IOException ignored){}}
 }
 private static void verify(Path ws,Path r,Properties p)throws IOException{
  check(ws.resolve(BUNDLE),p.getProperty("bundle.after"));check(r.resolve("bundle.rollback"),p.getProperty("bundle.rollback"));
  Path act=activity(ws);Path[] targets={ws.resolve(META),ws.resolve(MRUK),act};
  for(int i=0;i<3;i++){check(r.resolve("original"+i),p.getProperty("before"+i));if(i==2){if(!StereoPatch.sha256(normalized(targets[i])).equals(p.getProperty("after"+i)))throw fail("Changed activity.");}else check(targets[i],p.getProperty("after"+i));}
  for(String h:HELPERS)if(!StereoPatch.sha256(normalized(helperDir(act).resolve(h))).equals(p.getProperty(h)))throw fail("Changed settings helper.");
 }
 public static void restore(Path ws){
  Path r=ws.resolve(RECEIPT);if(!Files.exists(r))return;Path candidate=null;
  try{
   Properties p=readReceipt(r);verify(ws,r,p);candidate=Files.createTempFile(ws,"spatial-restore-",".bundle");
   try(var b=new UnityBundlePatch(ws.resolve(BUNDLE))){b.restore(r.resolve("bundle.rollback"),candidate);}check(candidate,p.getProperty("bundle.before"));
   Path act=activity(ws);Path[] targets={ws.resolve(META),ws.resolve(MRUK),act};
   Files.move(candidate,ws.resolve(BUNDLE),StandardCopyOption.REPLACE_EXISTING);for(int i=0;i<3;i++)Files.copy(r.resolve("original"+i),targets[i],StandardCopyOption.REPLACE_EXISTING);
   for(String h:HELPERS)Files.delete(helperDir(act).resolve(h));deleteTree(r);
  }catch(IOException ex){throw fail(ex.toString());}finally{if(candidate!=null)try{Files.deleteIfExists(candidate);}catch(IOException ignored){}}
 }
}
