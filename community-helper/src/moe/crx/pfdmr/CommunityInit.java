package moe.crx.pfdmr;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.system.Os;
import android.util.Log;
import java.io.*;
import java.util.regex.Pattern;
/** App-owned startup maintenance, scoped to the two explicitly selected games. */
public final class CommunityInit extends ContentProvider {
 public boolean onCreate() {
  try {
   Context c=getContext();String pkg=c.getPackageName();File base=c.getExternalFilesDir(null);
   if(base==null)throw new IOException("External files unavailable");
   if(pkg.equals("com.crytek.climb")) {
    File cfg=new File(base,"user.cfg");String old="";
    if(cfg.exists()){if(cfg.length()>1024*1024)throw new IOException("Unexpected user.cfg size");ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(cfg)){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}old=new String(out.toByteArray(),"UTF-8");}
    String next=Pattern.compile("(?im)^\\s*r_vr_foveated\\s*=.*$").matcher(old).replaceAll("r_vr_foveated = 0");
    if(next.equals(old)&&!Pattern.compile("(?im)^\\s*r_vr_foveated\\s*=").matcher(old).find())next=old+(old.endsWith("\n")||old.isEmpty()?"":"\n")+"r_vr_foveated = 0\n";
    if(!next.equals(old)) {
     File backup=new File(base,"user.cfg.pfdmr-before");if(cfg.exists()&&!backup.exists())try(FileOutputStream out=new FileOutputStream(backup)){out.write(old.getBytes("UTF-8"));out.getFD().sync();}
     File temp=new File(base,"user.cfg.pfdmr-tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(next.getBytes("UTF-8"));out.getFD().sync();}Os.rename(temp.getAbsolutePath(),cfg.getAbsolutePath());
    }
   } else if(pkg.equals("unity.SUPERHOT_Team.SUPERHOT_VR_QA")) {
    File cloud=new File(base,"cloud"),data=new File(cloud,"data");
    if(!cloud.exists()&&!cloud.mkdir())throw new IOException("Cannot create cloud");Os.chmod(cloud.getAbsolutePath(),0770);
    if(!data.exists()&&!data.mkdir())throw new IOException("Cannot create data");Os.chmod(data.getAbsolutePath(),0770);
    File probe=File.createTempFile("pfdmr-probe-",".tmp",data);try(FileOutputStream out=new FileOutputStream(probe)){out.write(1);}finally{probe.delete();}
   }
   Log.i("PFDMRCommunity","App-local preparation complete");
  }catch(Exception e){Log.e("PFDMRCommunity","App-local preparation failed",e);}
  return true;
 }
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
 public String getType(Uri u){return null;} public Uri insert(Uri u,ContentValues v){return null;}
 public int delete(Uri u,String s,String[] a){return 0;}public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}
