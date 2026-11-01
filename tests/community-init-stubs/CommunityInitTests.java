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
  Path assets=base.resolve("assets");Files.createDirectories(assets);Path flag=assets.resolve("pfdmr-climb-stability.txt");String header="PFDMR_CLIMB_STABILITY_V1\n";
  Files.writeString(cfg,"keep = 42\nr_MultiThreaded = 1\n");Files.writeString(flag,header+"enabled=true\n");new CommunityInit().onCreate();check(Files.readString(cfg).contains("r_MultiThreaded = 0"),"opt-in Climb stability applied");
  Path snapshot=base.resolve("user.cfg.pfdmr-stability-before");String previous=Files.readString(snapshot);new CommunityInit().onCreate();check(Files.readString(snapshot).equals(previous),"repeated stability startup preserves original backup");
  Files.writeString(cfg,Files.readString(cfg)+"new_setting = 7\n");Files.writeString(flag,header+"enabled=false\n");new CommunityInit().onCreate();check(Files.readString(cfg).contains("r_MultiThreaded = 1")&&Files.readString(cfg).contains("new_setting = 7"),"disable restores only owned stability setting");check(!Files.exists(snapshot),"completed stability backup cleared");
  Files.writeString(cfg,"keep = 42\n");Files.writeString(flag,header+"enabled=true\n");new CommunityInit().onCreate();Files.writeString(flag,header+"enabled=false\n");new CommunityInit().onCreate();check(!Files.readString(cfg).contains("r_MultiThreaded"),"missing original setting restored by removing owned key");
  Files.writeString(flag,header+"enabled=true\n");new CommunityInit().onCreate();Files.writeString(cfg,Files.readString(cfg).replace("r_MultiThreaded = 0","r_MultiThreaded = 2"));Files.writeString(flag,header+"enabled=false\n");new CommunityInit().onCreate();check(Files.readString(cfg).contains("r_MultiThreaded = 2"),"externally edited stability setting preserved");
  c.pkg="unity.SUPERHOT_Team.SUPERHOT_VR_QA";Path saves=base.resolve("cloud/data");Files.createDirectories(saves);Path save=saves.resolve("existing.sav");Files.writeString(save,"keep");new CommunityInit().onCreate();
  check(Files.readString(save).equals("keep"),"existing saves unchanged");
  check(android.system.Os.calls.equals(java.util.List.of(base.resolve("cloud").toFile().getAbsolutePath(),saves.toFile().getAbsolutePath())),"only cloud and data receive 0770");
  try(var files=Files.list(saves)){check(files.count()==1,"write probe removed");}
  c.pkg="other.game";android.system.Os.calls.clear();new CommunityInit().onCreate();check(android.system.Os.calls.isEmpty(),"other games unaffected");
  System.out.println("ALL "+n+" APP INIT CHECKS PASSED");
 }
}