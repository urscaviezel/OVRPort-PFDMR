import moe.crx.overport.pfdmr.DesktopInstance;
import java.nio.file.*;
public class DesktopInstanceTests {
 public static void main(String[] args)throws Exception {
  Path root=Path.of(args[0]);
  if(args.length>1){try(var lock=DesktopInstance.acquire(root)){if(lock==null){System.out.println("BUSY");System.exit(23);}System.out.println("ACQUIRED");System.out.flush();if(args[1].equals("hold"))Thread.sleep(30000);}return;}
  String java=Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),cp=System.getProperty("java.class.path");
  try(var lock=DesktopInstance.acquire(root)){
   if(lock==null||DesktopInstance.acquire(root)!=null)throw new AssertionError("same-JVM exclusion");
   var second=new ProcessBuilder(java,"-cp",cp,"DesktopInstanceTests",root.toString(),"try").inheritIO().start();
   if(second.waitFor()!=23)throw new AssertionError("second process not blocked");
  }
  var next=new ProcessBuilder(java,"-cp",cp,"DesktopInstanceTests",root.toString(),"hold").redirectErrorStream(true).start();
  if(!new java.io.BufferedReader(new java.io.InputStreamReader(next.getInputStream())).readLine().equals("ACQUIRED"))throw new AssertionError("relaunch blocked");
  next.destroyForcibly().waitFor();
  try(var lock=DesktopInstance.acquire(root)){if(lock==null)throw new AssertionError("stale lock after crash");}
  System.out.println("PASS same-JVM and cross-process exclusion, relaunch and crash recovery");
 }
}
