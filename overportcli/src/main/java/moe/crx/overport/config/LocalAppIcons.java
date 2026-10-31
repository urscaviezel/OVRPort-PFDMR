package moe.crx.overport.config;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;

/** Optional flat icon folder: exact package filenames only, never guessed by title. */
public final class LocalAppIcons {
 private static final String SETTING="localIconDirectory";
 private static Preferences preferences(){return Preferences.userRoot().node("moe/crx/overport/pfdmr");}
 public static void setDirectory(File dir){if(!dir.isDirectory())throw new IllegalArgumentException("Select an icon folder");preferences().put(SETTING,dir.getAbsolutePath());}
 public static File directory(){
  String override=System.getProperty("overport.iconCache");if(override!=null)return new File(override);
  try{String saved=preferences().get(SETTING,"");if(!saved.isBlank()&&new File(saved).isDirectory())return new File(saved);}catch(SecurityException ignored){}
  // Portable folder next to the app; also find the project's folder in local test builds.
  List<File> starts=new ArrayList<>();starts.add(new File(System.getProperty("user.dir")));
  try{starts.add(new File(LocalAppIcons.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile());}catch(Exception ignored){}
  for(File start:starts)for(int i=0;i<8&&start!=null;i++,start=start.getParentFile()){File candidate=new File(start,"app_icon");if(candidate.isDirectory())return candidate;}
  return null;
 }
 public static boolean validPackage(String pkg){return pkg!=null&&pkg.length()<=255&&pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+");}
 public static boolean image(byte[] bytes){return bytes.length>=12&&((bytes[0]==(byte)0x89&&bytes[1]=='P'&&bytes[2]=='N'&&bytes[3]=='G')||(bytes[0]==(byte)0xff&&bytes[1]==(byte)0xd8)||(bytes[0]=='R'&&bytes[1]=='I'&&bytes[2]=='F'&&bytes[3]=='F'&&bytes[8]=='W'&&bytes[9]=='E'&&bytes[10]=='B'&&bytes[11]=='P')||(bytes[0]=='G'&&bytes[1]=='I'&&bytes[2]=='F'));}
 public static byte[] read(String pkg){
  if(!validPackage(pkg))return null;
  File dir=directory();if(dir==null)return null;
  for(String ext:List.of(".webp",".png",".jpg",".jpeg",".gif")){
   Path path=dir.toPath().resolve(pkg+ext);
   try{long size=Files.size(path);if(!Files.isRegularFile(path)||size<=0||size>16*1024*1024)continue;byte[] bytes=Files.readAllBytes(path);
    if(image(bytes))return bytes;
   }catch(java.io.IOException ignored){}
  }return null;
 }
}
