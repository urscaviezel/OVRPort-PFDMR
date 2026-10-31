import java.nio.file.*;import java.util.*;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;import java.io.*;
import moe.crx.overport.config.*;import moe.crx.overport.app.SkiaIconResizer;import moe.crx.overport.patching.*;import moe.crx.overport.patches.ReplaceIconLabelPatchKt;
public class LocalIconTests {
 static Map<String,String> code(Path ws)throws Exception{Map<String,String> result=new TreeMap<>();Path root=ws.resolve("smali");try(var paths=Files.walk(root)){for(Path p:paths.filter(Files::isRegularFile).toList())if(p.toString().endsWith(".smali"))result.put(root.relativize(p).toString(),Files.readString(p).replace("\r\n","\n"));}return result;}
 static int n;static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);n++;}
 public static void main(String[] args)throws Exception{
  Path run=Path.of(args[0]);Files.createDirectories(run);System.setProperty("overport.iconCache",Path.of(args[1]).toAbsolutePath().toString());
  for(String pkg:List.of("com.ILMxLAB.VaderImmortal.ep1","com.ILMxLAB.VaderImmortal.ep2","com.ILMxLAB.VaderImmortal.ep3","com.crytek.climb","com.polyarc.MossGame","com.FireproofStudios.TheRoomVR")){
   byte[] raw=LocalAppIcons.read(pkg);check(raw!=null,"exact local match "+pkg);byte[] png=SkiaIconResizer.INSTANCE.resize(raw,512,512);BufferedImage image=ImageIO.read(new ByteArrayInputStream(png));check(image!=null&&image.getWidth()==512&&image.getHeight()==512,"WebP becomes actual 512px PNG "+pkg);Files.write(run.resolve(pkg+".png"),png);
  }
  check(LocalAppIcons.read("../com.ILMxLAB.VaderImmortal.ep1")==null,"path traversal rejected");check(LocalAppIcons.read("com.DefaultCompany.ClimbingGame")==null,"empty fail marker ignored");
  var info=ApplicationMetaInfoKt.getApplicationInfo("com.ILMxLAB.VaderImmortal.ep1");check(info!=null&&info.getMetaId().equals("local")&&info.getDisplayName().isEmpty(),"offline metadata resolves without inventing app title");
  ReplaceIconLabelPatchKt.setPLATFORM_ICON_RESIZER(SkiaIconResizer.INSTANCE);
  var ctx=new PatcherContext("3.4.3-aa54c3f",run.resolve("cache").toFile(),"Vader.apk","{filename}.{ext}");try(var in=Files.newInputStream(Path.of(args[2]))){ctx.prepare(in);}String title=ctx.appName();var originalCode=code(ctx.getWorkingDir().toPath());var e=new PatchExecutor(run.toFile(),ctx.getWorkingDir(),ctx.getWorkingApk());ReplaceIconLabelPatchKt.getPATCH_REPLACE_ICON_LABEL().getExecutor().invoke(e,List.of());
  Path output=run.resolve("Vader-icon-test.apk");ctx.exportAndClose(Files.newOutputStream(output));check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"icon APK signature verified");ctx.cleanup();try(var in=Files.newInputStream(output)){ctx.prepare(in);}
  check(ctx.appName().equals(title),"original title preserved");var manifest=ctx.getWorkingApk().getAndroidManifest();check(manifest.getIconResourceId()!=0&&manifest.getIconResourceId()==manifest.getRoundIconResourceId(),"normal and round icon both updated");
  BufferedImage image=ImageIO.read(ctx.appIcon());check(image!=null&&image.getWidth()==512,"icon resolves from signed reimport");
  try(var original=new java.util.zip.ZipFile(args[2]);var patched=new java.util.zip.ZipFile(output.toFile())){for(var entries=original.entries();entries.hasMoreElements();){var entry=entries.nextElement();String name=entry.getName();if(name.startsWith("lib/")||name.startsWith("assets/")){if(!Arrays.equals(original.getInputStream(entry).readAllBytes(),patched.getInputStream(patched.getEntry(name)).readAllBytes()))throw new AssertionError("Unexpected changed game payload: "+name);}}}
  check(originalCode.equals(code(ctx.getWorkingDir().toPath())),"all disassembled DEX code identical after reimport");check(true,"all original native libraries and assets byte-identical");ctx.cleanup();System.out.println("ALL "+n+" LOCAL ICON CHECKS PASSED");
 }
}
