import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;

public class Beta10PatchTests {
 static int n;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 interface Checked {void run()throws Exception;}
 static void reject(Checked c,String s)throws Exception{boolean rejected=false;try{c.run();}catch(IllegalStateException ex){rejected=true;}check(rejected,s);}
 static void patch(PatcherContext c,Map<String,List<String>> a){c.patch(a,(i,p)->kotlin.Unit.INSTANCE);}
 public static void main(String[] args)throws Exception{
  Path run=Path.of(args[1]);Files.createDirectories(run);
  for(String id:List.of(VaderAudioPatch.ID,SpatialZombiesPatch.ID)){var p=PatchStore.INSTANCE.get(id);check(p!=null&&!p.isRecommended(),id+" optional/default off");}
  if(args[0].equals("vader")){
   int[] offsets={0x58cf0f8,0x5ae2e38,0x5b55348};
   for(int i=0;i<3;i++){
    Path ws=run.resolve("ep"+(i+1)),lib=ws.resolve("root/lib/arm64-v8a/libUE4.so");Files.createDirectories(lib.getParent());byte[] original;
    try(var z=new ZipFile(args[2+i])){original=z.getInputStream(z.getEntry("lib/arm64-v8a/libUE4.so")).readAllBytes();}Files.write(lib,original);
    check(VaderAudioPatch.inspect(original)==offsets[i],"episode "+(i+1)+" unique executable signature");
    QuestGatePatch.apply(ws);byte[] quest=Files.readAllBytes(lib);VaderAudioPatch.apply(ws);byte[] fixed=Files.readAllBytes(lib);
    int changed=0;for(int j=0;j<fixed.length;j++)if(fixed[j]!=quest[j]){changed++;check(j>=offsets[i]&&j<offsets[i]+4,"edit inside mixer instruction");}check(changed==3,"one instruction changed (three differing bytes)");
    VaderAudioPatch.apply(ws);check(Arrays.equals(fixed,Files.readAllBytes(lib)),"idempotent mixer");
    VaderAudioPatch.restore(ws);check(Arrays.equals(quest,Files.readAllBytes(lib)),"mixer rollback preserves Quest edits");QuestGatePatch.restore(ws);check(Arrays.equals(original,Files.readAllBytes(lib)),"composed rollback exact");
    Files.write(lib,fixed);VaderAudioPatch.apply(ws);VaderAudioPatch.restore(ws);check(Arrays.equals(fixed,Files.readAllBytes(lib)),"independent mixer preserved");
    byte[] corrupt=original.clone();corrupt[offsets[i]+32]^=1;Files.write(lib,corrupt);reject(()->VaderAudioPatch.apply(ws),"unknown mixer body rejected");
   }
  }else{
   var ctx=new PatcherContext("3.4.3-23204ea",run.resolve("cache").toFile(),"Owned.apk","{filename}.beta10.{ext}");try(var in=Files.newInputStream(Path.of(args[2]))){ctx.prepare(in);}
   Path ws=ctx.getWorkingDir().toPath(),bundle=ws.resolve("root/assets/bin/Data/data.unity3d"),meta=ws.resolve("root/assets/bin/Data/Managed/Metadata/global-metadata.dat"),mruk=ws.resolve("root/lib/arm64-v8a/libmrutilitykitshared.so");
   patch(ctx,Map.of());String originalBundle=UnityBundlePatch.hash(bundle),originalMeta=UnityBundlePatch.hash(meta),originalMruk=UnityBundlePatch.hash(mruk);
   var selection=new LinkedHashMap<String,List<String>>();selection.put(RefreshRatePatch.ID,List.of());selection.put(SpatialZombiesPatch.ID,List.of());selection.put(MrCompatibilityPatch.ID,List.of());selection.put(BasicPassthroughPatch.ID,List.of());selection.put(StereoPatch.ID,List.of());
   var ran=new ArrayList<String>();ctx.patch(selection,(i,p)->{ran.add(p.getName());return kotlin.Unit.INSTANCE;});
   check(ran.equals(List.of(BasicPassthroughPatch.ID,SpatialZombiesPatch.ID,StereoPatch.ID,RefreshRatePatch.ID)),"one native scene layer; spatial before stereo/Hz");
   check(UnityBundlePatch.hash(mruk).equals(MrukPosePatch.ZOMBIES.after()),"MRUK matches working APK");
   check(UnityBundlePatch.hash(meta).equals("f6022d98e0e56aed1238d3e2585a3fdfa6624aed0ae2a91da49525414aed1d93"),"metadata matches working APK");
   String result=UnityBundlePatch.hash(bundle);check(!result.equals(originalBundle),"Unity bundle changed");
   check(Files.size(ws.resolve("root/assets/pfdmr-spatial-zombies/bundle.rollback"))<2000000,"compact bundle rollback, no full asset duplication");
   patch(ctx,selection);check(UnityBundlePatch.hash(bundle).equals(result),"repeat patch produces identical Unity bundle");
   Path output=run.resolve("spatial-beta10.apk");ctx.exportAndClose(Files.newOutputStream(output));check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"signed combined output verified");
   ctx.cleanup();try(var in=Files.newInputStream(output)){ctx.prepare(in);}patch(ctx,Map.of());
   check(UnityBundlePatch.hash(bundle).equals(originalBundle),"bundle exact rollback after export/import");check(UnityBundlePatch.hash(meta).equals(originalMeta)&&UnityBundlePatch.hash(mruk).equals(originalMruk),"metadata and MRUK exact rollback");
   check(!Files.exists(ws.resolve("root/assets/pfdmr-spatial-zombies"))&&!Files.exists(ws.resolve("root/assets/pfdmr-mr")),"all owned receipts removed");
   byte[] m=Files.readAllBytes(meta);m[32]^=1;Files.write(meta,m);reject(()->patch(ctx,selection),"unsupported metadata rejected before mutation");check(UnityBundlePatch.hash(bundle).equals(originalBundle),"failed preflight leaves bundle unchanged");
   ctx.cleanup();
  }
  System.out.println("ALL "+n+" BETA10 "+args[0]+" CHECKS PASSED");
 }
}
