import java.nio.file.*;import android.content.*;import moe.crx.pfdmr.CommunityInit;
public class CommunityInitTests {
 static int n;static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 public static void main(String[] args)throws Exception{
  Path base=Path.of(args[0]);Files.createDirectories(base);Context c=new Context();c.dir=base.toFile();ContentProvider.context=c;c.pkg="com.crytek.climb";
  Path cfg=base.resolve("user.cfg");String old="other_setting = 42\nr_vr_foveated = 1\n";Files.writeString(cfg,old);
  new CommunityInit().onCreate();String next=Files.readString(cfg);
  check(next.equals("other_setting = 42\nr_vr_foveated = 0\n"),"only Climb foveation value changes");
  check(Files.readString(base.resolve("user.cfg.pfdmr-before")).equals(old),"existing config backup retained");
  new CommunityInit().onCreate();check(Files.readString(cfg).equals(next),"Climb initialization idempotent");
  check(Files.readString(base.resolve("user.cfg.pfdmr-before")).equals(old),"second launch preserves first backup");
  Files.delete(cfg);new CommunityInit().onCreate();check(Files.readString(cfg).equals("r_vr_foveated = 0\n"),"fresh config created");
  c.pkg="unity.SUPERHOT_Team.SUPERHOT_VR_QA";Path saves=base.resolve("cloud/data");Files.createDirectories(saves);Path save=saves.resolve("existing.sav");Files.writeString(save,"keep");new CommunityInit().onCreate();
  check(Files.readString(save).equals("keep"),"existing saves unchanged");
  check(android.system.Os.calls.equals(java.util.List.of(base.resolve("cloud").toFile().getAbsolutePath(),saves.toFile().getAbsolutePath())),"only cloud and data receive 0770");
  try(var files=Files.list(saves)){check(files.count()==1,"write probe removed");}
  c.pkg="other.game";android.system.Os.calls.clear();new CommunityInit().onCreate();check(android.system.Os.calls.isEmpty(),"other games unaffected");
  System.out.println("ALL "+n+" APP INIT CHECKS PASSED");
 }
}