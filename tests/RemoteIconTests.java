import java.nio.file.*;import java.net.URI;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;import com.reandroid.json.*;import moe.crx.overport.config.*;import moe.crx.overport.pfdmr.StereoPatch;
public class RemoteIconTests {
 static int n;static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 public static void main(String[] args)throws Exception{
  Path run=Path.of(args[0]);byte[] image=Files.readAllBytes(Path.of(args[1]));String pkg="com.ILMxLAB.VaderImmortal.ep1",name=pkg+".webp";URI base=URI.create("https://example.invalid/icon-cache/");
  JSONObject entry=new JSONObject().put("file",name).put("size",image.length).put("sha256",StereoPatch.sha256(image));byte[] index=new JSONObject().put("version",1).put("icons",new JSONObject().put(pkg,entry)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  AtomicInteger requests=new AtomicInteger();RemoteAppIcons.Fetcher fetch=(url,limit)->{requests.incrementAndGet();if(url.equals(base.resolve("icons-index.json")))return index;if(url.equals(base.resolve("images/"+name)))return image;throw new AssertionError("Unexpected request "+url);};
  var cache=new RemoteAppIcons(run.resolve("normal"),base,fetch);
  check(Arrays.equals(cache.load(pkg),image),"only requested package downloaded and hash checked");check(requests.get()==2,"one index and one image request");
  check(Arrays.equals(cache.load(pkg),image)&&requests.get()==2,"repeated preview/patch uses disk cache");
  var offline=new RemoteAppIcons(run.resolve("normal"),base,(u,l)->{throw new AssertionError("Offline cache must not request network");});check(Arrays.equals(offline.load(pkg),image),"download remains available offline after restart");
  check(cache.load("other.missing")==null&&requests.get()==2,"absent package makes no image request");check(cache.load("../evil")==null&&requests.get()==2,"unsafe package never requested");
  Path manifest=run.resolve("normal/icons-index.json");long time=System.currentTimeMillis()-600000;Files.setLastModifiedTime(manifest,java.nio.file.attribute.FileTime.fromMillis(time));
  var reread=new RemoteAppIcons(run.resolve("normal"),base,(u,l)->{throw new AssertionError("Fresh index must be reused");});check(reread.load("other.missing")==null&&Files.getLastModifiedTime(manifest).toMillis()==time,"reading cached index does not postpone refresh indefinitely");
  var corrupt=new RemoteAppIcons(run.resolve("corrupt"),base,(u,l)->u.toString().endsWith(".json")?index:new byte[image.length]);check(corrupt.load(pkg)==null&&!Files.exists(run.resolve("corrupt/"+pkg+".image")),"checksum mismatch rejected without storing image");
  byte[] bad=new JSONObject().put("version",1).put("icons",new JSONObject().put(pkg,new JSONObject(entry.toString()).put("file","../outside.png"))).toString().getBytes();AtomicInteger badRequests=new AtomicInteger();var unsafe=new RemoteAppIcons(run.resolve("unsafe"),base,(u,l)->{badRequests.incrementAndGet();return bad;});check(unsafe.load(pkg)==null&&badRequests.get()==1,"index path traversal cannot request image");
  try{new RemoteAppIcons(run,URI.create("http://example.invalid/"),fetch);throw new AssertionError();}catch(IllegalArgumentException ok){check(true,"HTTPS required");}
  System.out.println("ALL "+n+" REMOTE ICON CHECKS PASSED");
 }
}
