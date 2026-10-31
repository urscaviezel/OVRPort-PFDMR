package moe.crx.overport.pfdmr;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.reandroid.json.*;
import kotlin.Unit;
import moe.crx.overport.patching.*;

/** Opt-in profiles based on the pinned community integration kit, not its installers. */
public final class CommunityProfiles {
 public static final String HAND="patch_pfdmr_hand_mesh", ROOM="patch_pfdmr_room_legacy", CLIMB="patch_pfdmr_climb_vrapi", ELEVEN="patch_pfdmr_eleven_orgid", WANDER="patch_pfdmr_wander_threads", SUPERHOT="patch_pfdmr_superhot_storage";
 public static final List<String> IDS=List.of(HAND,ROOM,CLIMB,ELEVEN,WANDER,SUPERHOT);
 static final String LIB="root/lib/arm64-v8a/", RECEIPT="root/assets/pfdmr-community-v1.json", PROVIDER="moe.crx.pfdmr.CommunityInit";
 static final Map<String,String> LABELS=Map.of(HAND,"Guided Tai Chi: hand mesh & stereo",ROOM,"The Room VR: legacy Unity frames",CLIMB,"The Climb: VrApi bridge, stereo & foveation",ELEVEN,"Eleven: platform callback & stereo",WANDER,"Wander: Unity thread keys & 1800 x 1664",SUPERHOT,"SUPERHOT: repair cloud-save permissions");
 public static String label(String id){return IDS.contains(id)?"PFDMR compatibility: "+LABELS.get(id)+" (experimental)":null;}
 public static List<Patch> patches(){List<Patch> result=new ArrayList<>();for(String id:IDS)result.add(new Patch(id,false,false,(e,a)->{e.selectWorkspace(f->{apply(f.getFile().toPath(),id,e.applicationPackage());return Unit.INSTANCE;});return Unit.INSTANCE;}));return result;}
 static IllegalStateException fail(String s){return new IllegalStateException("PFDMR community profile: "+s);}
 static byte[] resource(String n)throws IOException{try(var in=CommunityProfiles.class.getResourceAsStream("/pfdmr/community/"+n)){if(in==null)throw fail("Missing helper "+n);byte[] v=in.readAllBytes();if(n.endsWith(".so")){JSONObject hashes=new JSONObject(new String(resource("hashes.json"),StandardCharsets.UTF_8));if(!StereoPatch.sha256(v).equals(hashes.getString(n)))throw fail("Invalid helper "+n);}return v;}}
 public static void validateSelection(PatcherContext ctx,Map<String,List<String>> args){
  long count=IDS.stream().filter(args::containsKey).count();if(count>1)throw fail("Select only the matching game profile.");
  for(String id:IDS)if(args.containsKey(id)){
   String pkg=Map.of(HAND,"com.CubicleNinjas.GuidedTaiChi",ROOM,"com.FireproofStudios.TheRoomVR",CLIMB,"com.crytek.climb",ELEVEN,"quest.eleven.forfunlabs",WANDER,"com.parkline.wander",SUPERHOT,"unity.SUPERHOT_Team.SUPERHOT_VR_QA").get(id);
   String code=Map.of(HAND,"1274",ROOM,"5011",CLIMB,"666",ELEVEN,"210710133",WANDER,"3570",SUPERHOT,"178").get(id);
   if(!pkg.equals(ctx.appPackage())||!code.equals(ctx.appVersionCode()))throw fail("This profile supports only "+pkg+" version code "+code+".");
   if(args.containsKey(MrCompatibilityPatch.ID)||args.containsKey(BasicPassthroughPatch.ID)||args.containsKey(SpatialZombiesPatch.ID)||args.containsKey(Pinball110Patch.ID))throw fail("Unrelated MR/game profiles cannot be combined with this game profile.");
   if(id.equals(CLIMB)&&args.containsKey("patch_remove_vrapi"))throw fail("Disable Remove VrApi library when using The Climb bridge.");
  }
 }
 public static Map<String,List<String>> expand(Map<String,List<String>> args){Map<String,List<String>> out=new LinkedHashMap<>(args);if(args.containsKey(HAND)||args.containsKey(CLIMB)||args.containsKey(ELEVEN))out.putIfAbsent(StereoPatch.ID,List.of());if(args.containsKey(WANDER))out.putIfAbsent(MossResolutionPatch.ID,List.of());return out;}
 static void known(byte[] data,String hash){if(!StereoPatch.sha256(data).equals(hash))throw fail("Unsupported library; start with the original APK and runtime 3.4.3-aa54c3f.");}
 static Path safe(Path ws,String path){Path p=ws.resolve(path).normalize();if(!p.startsWith(ws.normalize())||!(path.startsWith(LIB)||path.startsWith("smali/")))throw fail("Invalid receipt path");return p;}
 static JSONObject app(JSONObject doc){JSONArray nodes=doc.getJSONArray("nodes");for(int i=0;i<nodes.length();i++){JSONObject n=nodes.optJSONObject(i);if(n!=null&&"manifest".equals(n.optString("name"))){JSONArray children=n.getJSONArray("nodes");for(int j=0;j<children.length();j++){JSONObject a=children.optJSONObject(j);if(a!=null&&"application".equals(a.optString("name")))return a;}}}throw fail("No manifest application");}
 static JSONObject attr(String name,int id,String type,Object value){return new JSONObject().put("name",name).put("id",id).put("uri","http://schemas.android.com/apk/res/android").put("prefix","android").put("value_type",type).put("data",value);}
 static boolean ourProvider(JSONObject node){if(node==null||!"provider".equals(node.optString("name")))return false;JSONArray a=node.optJSONArray("attributes");if(a!=null)for(int i=0;i<a.length();i++)if(PROVIDER.equals(a.getJSONObject(i).optString("data")))return true;return false;}
 static JSONObject provider(String pkg){return new JSONObject().put("node_type","element").put("name","provider").put("attributes",new JSONArray().put(attr("name",16842755,"STRING",PROVIDER)).put(attr("authorities",16842776,"STRING",pkg+".pfdmr.community.init")).put(attr("exported",16842768,"BOOLEAN",false)));}
 static byte[] canonicalHelper()throws IOException{
  Path temp=Files.createTempDirectory("pfdmr-community-smali-");try{
   Path src=Files.createDirectory(temp.resolve("src")),dex=temp.resolve("classes.dex"),out=temp.resolve("out");Files.write(src.resolve("CommunityInit.smali"),resource("CommunityInit.smali"));var options=new org.jf.smali.SmaliOptions();options.apiLevel=29;options.jobs=1;options.outputDexFile=dex.toString();if(!org.jf.smali.Smali.assemble(options,src.toString()))throw fail("Cannot assemble init helper");var bo=new org.jf.baksmali.BaksmaliOptions();bo.apiLevel=29;bo.localsDirective=true;bo.sequentialLabels=true;bo.skipDuplicateLineNumbers=true;var df=new org.jf.dexlib2.dexbacked.DexBackedDexFile(org.jf.dexlib2.Opcodes.forApi(29),Files.readAllBytes(dex));if(!org.jf.baksmali.Baksmali.disassembleDexFile(df,out.toFile(),1,bo))throw fail("Cannot verify init helper");return Files.readString(out.resolve("moe/crx/pfdmr/CommunityInit.smali")).replace("\r\n","\n").getBytes(StandardCharsets.UTF_8);
  }finally{try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
 }
 public static void apply(Path ws,String id,String pkg){try{
  if(!IDS.contains(id))throw fail("Unknown profile");if(Files.exists(ws.resolve(RECEIPT)))throw fail("Existing profile must be restored first");
  Path lib=ws.resolve(LIB);if(!Files.isDirectory(lib))throw fail("ARM64 native libraries required");
  try(var dirs=Files.list(ws.resolve("root/lib"))){if(dirs.anyMatch(p->!p.equals(lib)&&Files.isDirectory(p)))throw fail("Only ARM64-only APKs supported by these profiles");}
  Map<String,byte[]> changes=new LinkedHashMap<>();JSONObject manifest=null;
  if(id.equals(HAND)){
   String path=LIB+"libopenxr_loader.so";byte[] bytes=Files.readAllBytes(ws.resolve(path));known(bytes,"21821370b886ac25b048836573a68438dc45c0cf9f3e7e6c9f415e28e7578b05");CommunityElf elf=new CommunityElf(bytes);byte[] fixed=bytes.clone();ByteBuffer b=ByteBuffer.wrap(fixed).order(ByteOrder.LITTLE_ENDIAN);int at=elf.va(0x15108);if(b.getInt(at)!=0xaa1f03ea||b.getInt(at+4)!=0x14000004)throw fail("Hand loop signature mismatch");b.putInt(at,0xb4000169);b.putInt(at+4,0x9280000a);known(fixed,"e07f4f78d43c7f223f4b2ef4197fcebe6b53f8889e45230c760ebd814ff8c264");changes.put(path,fixed);
  }else if(id.equals(ROOM)){
   String unity=LIB+"libunity.so",plugin=LIB+"libOVRPlugin.so";changes.put(unity,new CommunityElf(Files.readAllBytes(ws.resolve(unity))).rodata(Map.of("com.oculus.systemactivities","android","ovrp_Update2","fpov_Update2","ovrp_BeginFrame","fpov_BeginFrame","ovrp_EndFrame","fpov_EndFrame")));changes.put(plugin,new CommunityElf(Files.readAllBytes(ws.resolve(plugin))).needed("libfp_ovrp.so"));addHelper(ws,changes,"libfp_ovrp.so");
  }else if(id.equals(CLIMB)){
   byte[] bridge=resource("libvrapi.so");Set<String> exports=new CommunityElf(bridge).symbols(true),imports=new TreeSet<>();try(var files=Files.list(lib)){for(Path p:files.filter(p->p.toString().endsWith(".so")&&!p.getFileName().toString().equals("libvrapi.so")).toList()){byte[] bytes=Files.readAllBytes(p);if(bytes.length>=4&&bytes[0]==0x7f&&bytes[1]=='E'&&bytes[2]=='L'&&bytes[3]=='F')for(String s:new CommunityElf(bytes).symbols(false))if(s.startsWith("vrapi_"))imports.add(s);}}
   if(imports.isEmpty()||!exports.containsAll(imports))throw fail("VrApi bridge does not satisfy game imports: "+imports);if(!Files.exists(lib.resolve("libopenxr_loader.so")))throw fail("Missing PFDMR loader");changes.put(LIB+"libvrapi.so",bridge);
  }else if(id.equals(ELEVEN)){
   String path=LIB+"libovrplatformloader.so";byte[] bytes=Files.readAllBytes(ws.resolve(path));known(bytes,"791a77fcefbd61766e7e7703f440e743084af40063c1e213730403414f460444");Set<String> exports=new HashSet<>(Arrays.asList(new String(resource("org-exports.txt"),StandardCharsets.UTF_8).split("\\R")));byte[] hidden=new CommunityElf(bytes).hide(exports);byte[] fixed=new CommunityElf(hidden).needed("libfp_langpack.so");known(fixed,"6fe337df98e340e573036cf69f32ad7549fc7e325b837b0d35c2cf1a35c8faab");changes.put(path,fixed);addHelper(ws,changes,"libfp_langpack.so");
  }else if(id.equals(WANDER)){
   String path=LIB+"libunity.so";byte[] renamed=new CommunityElf(Files.readAllBytes(ws.resolve(path))).imports(Map.of("pthread_key_create","fp_tkey_create","pthread_key_delete","fp_tkey_delete","pthread_getspecific","fp_tkey_get","pthread_setspecific","fp_tkey_set"));changes.put(path,new CommunityElf(renamed).needed("libfp_tkeys.so"));addHelper(ws,changes,"libfp_tkeys.so");
  }
  if(id.equals(CLIMB)||id.equals(SUPERHOT)){
   Path smali=ws.resolve("smali");List<Path> roots;try(var files=Files.list(smali)){roots=files.filter(Files::isDirectory).sorted().toList();}if(roots.isEmpty())throw fail("No DEX workspace for app initializer");try(var files=Files.walk(smali)){if(files.anyMatch(p->p.toString().replace('\\','/').endsWith("/moe/crx/pfdmr/CommunityInit.smali")))throw fail("Init helper already present");}
   String path=ws.relativize(roots.get(0).resolve("moe/crx/pfdmr/CommunityInit.smali")).toString().replace('\\','/');changes.put(path,canonicalHelper());manifest=new JSONObject(Files.readString(ws.resolve("AndroidManifest.xml.json")));JSONObject app=app(manifest);JSONArray nodes=app.optJSONArray("nodes");if(nodes==null){nodes=new JSONArray();app.put("nodes",nodes);}for(int i=0;i<nodes.length();i++)if(ourProvider(nodes.optJSONObject(i)))throw fail("Provider name collision");nodes.put(provider(pkg));
  }
  commit(ws,id,changes,manifest);
 }catch(IOException e){throw fail(e.toString());}}
 static void addHelper(Path ws,Map<String,byte[]> changes,String name)throws IOException{if(Files.exists(ws.resolve(LIB+name)))throw fail("Existing unowned helper "+name);changes.put(LIB+name,resource(name));}
 static void commit(Path ws,String id,Map<String,byte[]> changes,JSONObject manifest)throws IOException{
  JSONObject receipt=new JSONObject().put("version",1).put("profile",id).put("provider",manifest!=null);JSONArray records=new JSONArray();
  for(var e:changes.entrySet()){
   Path p=safe(ws,e.getKey());byte[] after=e.getValue(),before=Files.exists(p)?Files.readAllBytes(p):null;JSONObject r=new JSONObject().put("path",e.getKey()).put("after",StereoPatch.sha256(after)).put("length",before==null?-1:before.length);JSONArray spans=new JSONArray();if(before!=null){r.put("before",StereoPatch.sha256(before));for(int i=0;i<before.length;){if(i<after.length&&before[i]==after[i]){i++;continue;}int start=i++;while(i<before.length&&(i>=after.length||before[i]!=after[i]))i++;spans.put(new JSONObject().put("offset",start).put("bytes",Base64.getEncoder().encodeToString(Arrays.copyOfRange(before,start,i))));}}r.put("spans",spans);records.put(r);
  }receipt.put("files",records);Path rp=ws.resolve(RECEIPT);Files.createDirectories(rp.getParent());Files.writeString(rp,receipt.toString());
  for(var e:changes.entrySet()){Path p=safe(ws,e.getKey());Files.createDirectories(p.getParent());Files.write(p,e.getValue());}
  if(manifest!=null)Files.writeString(ws.resolve("AndroidManifest.xml.json"),manifest.toString());
 }
 public static void restore(Path ws){Path rp=ws.resolve(RECEIPT);if(!Files.exists(rp))return;try{
  JSONObject receipt=new JSONObject(Files.readString(rp));if(receipt.getInt("version")!=1||!IDS.contains(receipt.getString("profile")))throw fail("Unknown rollback receipt");JSONArray records=receipt.getJSONArray("files");Map<Path,byte[]> before=new LinkedHashMap<>();
  for(int i=0;i<records.length();i++){JSONObject r=records.getJSONObject(i);Path p=safe(ws,r.getString("path"));byte[] after=Files.readAllBytes(p);if(r.getString("path").endsWith(".smali"))after=new String(after,StandardCharsets.UTF_8).replace("\r\n","\n").getBytes(StandardCharsets.UTF_8);if(!StereoPatch.sha256(after).equals(r.getString("after")))throw fail("Changed profile file: "+p.getFileName()+". Use original APK.");int len=r.getInt("length");if(len<0){before.put(p,null);continue;}byte[] original=Arrays.copyOf(after,len);JSONArray spans=r.getJSONArray("spans");for(int j=0;j<spans.length();j++){JSONObject s=spans.getJSONObject(j);byte[] bytes=Base64.getDecoder().decode(s.getString("bytes"));int at=s.getInt("offset");if(at<0||(long)at+bytes.length>len)throw fail("Invalid rollback range");System.arraycopy(bytes,0,original,at,bytes.length);}known(original,r.getString("before"));before.put(p,original);}
  JSONObject manifest=null;if(receipt.optBoolean("provider",false)){Path path=ws.resolve("AndroidManifest.xml.json");manifest=new JSONObject(Files.readString(path));JSONArray nodes=app(manifest).getJSONArray("nodes");int count=0;for(int i=nodes.length()-1;i>=0;i--)if(ourProvider(nodes.optJSONObject(i))){nodes.remove(i);count++;}if(count!=1)throw fail("Initializer changed or missing");}
  for(var e:before.entrySet())if(e.getValue()==null)Files.delete(e.getKey());else Files.write(e.getKey(),e.getValue());if(manifest!=null)Files.writeString(ws.resolve("AndroidManifest.xml.json"),manifest.toString());Files.delete(rp);
 }catch(IOException e){throw fail(e.toString());}}
}
