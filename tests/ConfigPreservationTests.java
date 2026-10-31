import java.nio.file.*;
import java.util.*;
import kotlin.Unit;
import moe.crx.overport.patching.*;
import moe.crx.overport.pfdmr.MossResolutionPatch;
import com.reandroid.json.JSONObject;
public class ConfigPreservationTests {
 static int n;
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("PASS "+message);n++;}
 public static void main(String[] args)throws Exception {
  Path run=Path.of(args[0]);
  var ctx=new PatcherContext("test",run.resolve("cache").toFile(),"Fixture.apk","{filename}.{ext}");
  try(var in=Files.newInputStream(Path.of(args[1]))){ctx.prepare(in);}
  Path ws=ctx.getWorkingDir().toPath(), file=ws.resolve("root/lib/arm64-v8a/liboverport.config.so");
  Files.createDirectories(file.getParent()); Files.deleteIfExists(file);
  ctx.patch(Map.of(MossResolutionPatch.ID,List.of()),(i,p)->Unit.INSTANCE);
  var cfg=new JSONObject(Files.readString(file));
  check(cfg.getInt("suggested_texture_width")==1800&&cfg.getInt("version")==1,"fresh APK gets config before resolution profile");
  ctx.patch(Map.of(),(i,p)->Unit.INSTANCE);
  check(!new JSONObject(Files.readString(file)).has("suggested_texture_width"),"fresh profile rollback restores absent dimensions");
  String raw="{\"version\":1,\"disable_space_warp\":1,\"extension\":{\"value\":9007199254740993},\"database\":[{\"app_id\":\"__current__\",\"products\":[],\"unknown_app\":true,\"assets\":[{\"asset_id\":12345678901234567,\"file_path\":\"/existing.bundle\",\"unknown_asset\":42}]}]}";
  Files.writeString(file,raw);
  var executor=new PatchExecutor(run.toFile(),ws.toFile(),new com.reandroid.apk.ApkModule());
  executor.selectConfig(c->{c.setSuggestedCpuLevel(3);return Unit.INSTANCE;});
  cfg=new JSONObject(Files.readString(file));
  check(cfg.getJSONObject("extension").getLong("value")==9007199254740993L,"unknown field and integer precision preserved");
  check(cfg.getJSONArray("database").toString().equals(new JSONObject(raw).getJSONArray("database").toString()),"database including unknown nested fields and asset IDs preserved");
  check(cfg.getInt("disable_space_warp")==1&&cfg.getInt("suggested_cpu_level")==3,"SpaceWarp retained while requested config changes apply");
  executor.selectConfig(c->{c.getDatabase().get(0).getAssets().get(0).setMetadata("updated");return Unit.INSTANCE;});
  var asset=new JSONObject(Files.readString(file)).getJSONArray("database").getJSONObject(0).getJSONArray("assets").getJSONObject(0);
  check(asset.getInt("unknown_asset")==42&&asset.getString("metadata").equals("updated"),"nested asset update retains extension fields");
  executor.selectConfig(c->{c.setSuggestedCpuLevel(0);return Unit.INSTANCE;});
  check(!new JSONObject(Files.readString(file)).has("suggested_cpu_level"),"known setting can reset to default");
  Files.writeString(file,"{broken");
  boolean rejected=false;try{executor.selectConfig(c->Unit.INSTANCE);}catch(Exception e){rejected=true;}
  check(rejected&&Files.readString(file).equals("{broken"),"malformed configuration rejected without resetting data");
  ctx.cleanup();System.out.println("ALL "+n+" CONFIG CHECKS PASSED");
 }
}
