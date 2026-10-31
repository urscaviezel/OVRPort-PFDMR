package moe.crx.overport.config;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import com.reandroid.json.*;
import moe.crx.overport.pfdmr.StereoPatch;

/** Fetch only missing package icons from the separate GitHub icon-cache branch. */
public final class RemoteAppIcons {
 public static final String BASE="https://raw.githubusercontent.com/urscaviezel/OVRPort-PFDMR/icon-cache/";
 public interface Fetcher { byte[] get(URI url,int limit)throws IOException; }
 private final Path cache;private final URI base;private final Fetcher fetcher;private JSONObject index;private boolean attempted;
 public RemoteAppIcons(Path cache,URI base,Fetcher fetcher){if(!"https".equals(base.getScheme())||!base.toString().endsWith("/"))throw new IllegalArgumentException("HTTPS icon base required");this.cache=cache;this.base=base;this.fetcher=fetcher;}
 private static final class Default {static final RemoteAppIcons INSTANCE=new RemoteAppIcons(Path.of(System.getProperty("user.home"),"overport","app-icon-cache"),URI.create(BASE),RemoteAppIcons::download);}
 public static byte[] read(String pkg){return Default.INSTANCE.load(pkg);}
 public static byte[] cached(String pkg){return Default.INSTANCE.cachedImage(pkg);}
 private static byte[] download(URI url,int limit)throws IOException{
  HttpURLConnection c=(HttpURLConnection)url.toURL().openConnection();c.setConnectTimeout(5000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","OVRPort-PFDMR-Icons");
  try{if(c.getResponseCode()!=200)throw new IOException("Icon HTTP "+c.getResponseCode());if(c.getContentLengthLong()>limit)throw new IOException("Icon response too large");try(var in=c.getInputStream()){byte[] data=in.readNBytes(limit+1);if(data.length>limit)throw new IOException("Icon response too large");return data;}}finally{c.disconnect();}
 }
 private static void write(Path target,byte[] data)throws IOException{Files.createDirectories(target.getParent());Path temp=Files.createTempFile(target.getParent(),"icon-",".tmp");try{Files.write(temp,data);Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}}
 private synchronized JSONObject index(){
  if(attempted)return index;attempted=true;Path file=cache.resolve("icons-index.json");
  try{
   boolean fromDisk=Files.isRegularFile(file)&&Files.size(file)<=8*1024*1024&&System.currentTimeMillis()-Files.getLastModifiedTime(file).toMillis()<86400000;byte[] data;if(fromDisk)data=Files.readAllBytes(file);
   else{data=fetcher.get(base.resolve("icons-index.json"),8*1024*1024);if(data.length>8*1024*1024)throw new IOException("Icon index too large");}
   JSONObject doc=new JSONObject(new String(data,java.nio.charset.StandardCharsets.UTF_8));if(doc.getInt("version")!=1)throw new IOException("Unsupported icon index");index=doc.getJSONObject("icons");if(!fromDisk)write(file,data);
  }catch(Exception ignored){index=null;}
  return index;
 }
 private byte[] cachedImage(String pkg){
  if(!LocalAppIcons.validPackage(pkg))return null;Path file=cache.resolve(pkg+".image");
  try{if(Files.isRegularFile(file)&&Files.size(file)<=16*1024*1024){byte[] data=Files.readAllBytes(file);if(LocalAppIcons.image(data))return data;}}catch(IOException ignored){}
  return null;
 }
 public byte[] load(String pkg){
  if(!LocalAppIcons.validPackage(pkg))return null;byte[] saved=cachedImage(pkg);if(saved!=null)return saved;Path file=cache.resolve(pkg+".image");
  try{
   JSONObject entries=index();if(entries==null)return null;JSONObject entry=entries.optJSONObject(pkg);if(entry==null)return null;
   String name=entry.getString("file"),hash=entry.getString("sha256");int size=entry.getInt("size");
   if(!List.of(pkg+".webp",pkg+".png",pkg+".jpg",pkg+".jpeg",pkg+".gif").contains(name)||!hash.matches("[0-9a-f]{64}")||size<=0||size>16*1024*1024)return null;
   byte[] data=fetcher.get(base.resolve("images/"+name),size);if(data.length!=size||!StereoPatch.sha256(data).equals(hash)||!LocalAppIcons.image(data))return null;write(file,data);return data;
  }catch(Exception ignored){return null;}
 }
}
