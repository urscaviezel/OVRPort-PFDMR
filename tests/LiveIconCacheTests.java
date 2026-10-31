import java.nio.file.*;import java.util.*;import java.net.*;import java.io.*;import javax.imageio.ImageIO;
import moe.crx.overport.config.*;import moe.crx.overport.app.SkiaIconResizer;import moe.crx.overport.pfdmr.StereoPatch;import com.reandroid.json.*;
/** Explicit integration test against the published icon branch, with an isolated home/cache. */
public class LiveIconCacheTests {
 public static void main(String[] args)throws Exception {
  boolean offline=args[0].equals("offline");Path run=Path.of(args[1]);Files.createDirectories(run);Path empty=run.resolve("empty-local");Files.createDirectories(empty);System.setProperty("overport.iconCache",empty.toAbsolutePath().toString());
  if(offline)ProxySelector.setDefault(new ProxySelector(){public java.util.List<Proxy> select(URI uri){throw new AssertionError("Unexpected network request while using cache: "+uri);}public void connectFailed(URI u,SocketAddress s,IOException e){}});
  JSONObject index=new JSONObject(Files.readString(Path.of(args[2]))).getJSONObject("icons");
  Path disk=Path.of(System.getProperty("user.home"),"overport","app-icon-cache");
  for(String pkg:List.of("com.ILMxLAB.VaderImmortal.ep1","com.crytek.climb","com.zenstudios.PFXVRQuest")){
   if(LocalAppIcons.read(pkg)!=null)throw new AssertionError("Local folder must not supply this icon");
   if(!offline&&pkg.equals("com.ILMxLAB.VaderImmortal.ep1")){
    var first=ApplicationMetaInfoKt.getApplicationInfo(pkg);if(first==null||!first.getMetaId().equals("icon-cache"))throw new AssertionError("Expected automatic GitHub fallback for missing Vader metadata");System.out.println("PASS normal metadata lookup falls back to live GitHub without local icons");
   }
   byte[] bytes=RemoteAppIcons.read(pkg);if(bytes==null)throw new AssertionError("GitHub icon unavailable: "+pkg);
   JSONObject entry=index.getJSONObject(pkg);if(bytes.length!=entry.getInt("size")||!StereoPatch.sha256(bytes).equals(entry.getString("sha256")))throw new AssertionError("Live icon hash mismatch");
   var info=ApplicationMetaInfoKt.getApplicationInfo(pkg);if(info==null||!info.getMetaId().equals("icon-cache"))throw new AssertionError("Normal metadata resolver did not reuse downloaded image");
   byte[] png=SkiaIconResizer.INSTANCE.resize(Base64.getDecoder().decode(info.getImages().get(0).getUri()),512,512);var image=ImageIO.read(new ByteArrayInputStream(png));if(image==null||image.getWidth()!=512)throw new AssertionError("PNG conversion failed");Files.write(run.resolve(pkg+".png"),png);
   System.out.println("PASS "+(offline?"network disabled, reused disk cache ":"live GitHub download, SHA256, metadata and PNG ")+pkg+" ("+bytes.length+" bytes)");
  }
  try(var files=Files.list(disk)){var list=files.toList();if(list.size()!=4)throw new AssertionError("Expected only index plus three icons: "+list);System.out.println("PASS exactly one index and three individual icons cached; total "+list.stream().mapToLong(p->{try{return Files.size(p);}catch(IOException e){throw new RuntimeException(e);}}).sum()+" bytes");}
 }
}
