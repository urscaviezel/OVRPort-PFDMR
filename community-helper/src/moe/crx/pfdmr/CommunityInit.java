package moe.crx.pfdmr;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.system.Os;
import android.util.Log;
import java.io.*;
import java.util.regex.*;
/** App-owned startup maintenance; scoped to explicitly selected game profiles. */
public final class CommunityInit extends ContentProvider {
 private static String read(InputStream in)throws IOException{try(InputStream stream=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=stream.read(buf))!=-1){if(out.size()+n>1024*1024)throw new IOException("Unexpected configuration size");out.write(buf,0,n);}return new String(out.toByteArray(),"UTF-8");}}
 private static String read(File file)throws IOException{return file.exists()?read(new FileInputStream(file)):"";}
 private static void write(File file,String text)throws IOException{try(FileOutputStream out=new FileOutputStream(file)){out.write(text.getBytes("UTF-8"));out.getFD().sync();}}
 private static Pattern setting(String key){return Pattern.compile("(?im)^[\\t ]*"+Pattern.quote(key)+"[\\t ]*=[^\\r\\n]*\\r?$");}
 private static String set(String text,String key,String value){Matcher m=setting(key).matcher(text);if(m.find())return m.replaceAll(Matcher.quoteReplacement(key+" = "+value));return text+(text.isEmpty()||text.endsWith("\n")?"":"\n")+key+" = "+value+"\n";}
 private static boolean stability(Context c)throws IOException{
  String flag;try{flag=read(c.getAssets().open("pfdmr-climb-stability.txt"));}catch(FileNotFoundException missing){return false;}
  if(flag.equals("PFDMR_CLIMB_STABILITY_V1\nenabled=true\n"))return true;
  if(flag.equals("PFDMR_CLIMB_STABILITY_V1\nenabled=false\n"))return false;
  throw new IOException("Unknown Climb stability command");
 }
 private static void climb(Context c,File base)throws IOException,android.system.ErrnoException {
  File cfg=new File(base,"user.cfg"),snapshot=new File(base,"user.cfg.pfdmr-stability-before");String old=read(cfg),next=set(old,"r_vr_foveated","0");boolean restored=false;
  if(stability(c)){
   String stable=set(next,"r_MultiThreaded","0");if(!stable.equals(next)&&!snapshot.exists())write(snapshot,next);next=stable;
  }else if(snapshot.exists()){
   Matcher current=setting("r_MultiThreaded").matcher(next);boolean found=false,owned=true;
   while(current.find()){found=true;if(!current.group().trim().equalsIgnoreCase("r_MultiThreaded = 0"))owned=false;}
   if(found&&owned){String before=read(snapshot);Matcher prior=setting("r_MultiThreaded").matcher(before);StringBuilder values=new StringBuilder();while(prior.find())values.append(prior.group()).append('\n');next=setting("r_MultiThreaded").matcher(next).replaceAll("");if(values.length()>0)next+=(next.endsWith("\n")?"":"\n")+values;restored=true;}
   else Log.i("PFDMRCommunity","Keeping externally changed r_MultiThreaded setting");
  }
  if(!next.equals(old)){
   File backup=new File(base,"user.cfg.pfdmr-before");if(cfg.exists()&&!backup.exists())write(backup,old);
   File temp=new File(base,"user.cfg.pfdmr-tmp");write(temp,next);Os.rename(temp.getAbsolutePath(),cfg.getAbsolutePath());
  }
  if(restored&&!snapshot.delete())throw new IOException("Cannot clear completed Climb stability backup");
 }
 public boolean onCreate(){try{
  Context c=getContext();String pkg=c.getPackageName();File base=c.getExternalFilesDir(null);if(base==null)throw new IOException("External files unavailable");
  if(pkg.equals("com.crytek.climb"))climb(c,base);
  else if(pkg.equals("unity.SUPERHOT_Team.SUPERHOT_VR_QA")){
   File cloud=new File(base,"cloud"),data=new File(cloud,"data");if(!cloud.exists()&&!cloud.mkdir())throw new IOException("Cannot create cloud");Os.chmod(cloud.getAbsolutePath(),0770);if(!data.exists()&&!data.mkdir())throw new IOException("Cannot create data");Os.chmod(data.getAbsolutePath(),0770);
   File probe=File.createTempFile("pfdmr-probe-",".tmp",data);try(FileOutputStream out=new FileOutputStream(probe)){out.write(1);}finally{probe.delete();}
  }
  Log.i("PFDMRCommunity","App-local preparation complete");
 }catch(Exception e){Log.e("PFDMRCommunity","App-local preparation failed",e);}return true;}
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}public String getType(Uri u){return null;}public Uri insert(Uri u,ContentValues v){return null;}public int delete(Uri u,String s,String[] a){return 0;}public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}
