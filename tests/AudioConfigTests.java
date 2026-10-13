import java.nio.file.*;
import java.util.*;
import moe.crx.pfdmr.Audio48;
public class AudioConfigTests {
 static int passed;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);System.out.println("PASS "+why);passed++;}
 public static void main(String[] args)throws Exception {
  Path root=Path.of(args[0]);Files.createDirectories(root);
  String rel="UnrealGame/Solero/Solero/Saved/Config/Android/Engine.ini";
  Path ini=root.resolve(rel);Path backup=Path.of(ini+".pfdmr-audio48.original");
  String section="[/Script/AndroidRuntimeSettings.AndroidRuntimeSettings]";
  Audio48.update(root.toFile(),rel,false);check(!Files.exists(ini),"disabled without backup does nothing");
  Files.createDirectories(ini.getParent());
  String original="\uFEFF; settings\r\n"+section+"\r\nAudioSampleRate=44100\r\nAudioNumBuffersToEnqueue=4\r\n[Other]\r\nAudioSampleRate=22050\r\n";
  Files.writeString(ini,original);Audio48.update(root.toFile(),rel,true);
  String changed=Files.readString(ini);
  check(changed.contains("AudioSampleRate=48000"),"sets 48 kHz");
  check(changed.contains("AudioSampleRate=22050")&&changed.contains("AudioNumBuffersToEnqueue=4"),"preserves unrelated settings");
  check(changed.startsWith("\uFEFF")&&changed.contains("\r\n"),"preserves UTF-8 BOM and CRLF");
  check(Files.readString(backup).equals(original),"backs up original bytes");
  Audio48.update(root.toFile(),rel,true);check(Files.readString(ini).equals(changed),"repeat start is idempotent");
  check(Files.readString(backup).equals(original),"repeat start keeps initial backup");
  Files.writeString(ini,changed.replace("AudioNumBuffersToEnqueue=4","AudioNumBuffersToEnqueue=8"));
  Audio48.update(root.toFile(),rel,false);String restored=Files.readString(ini);
  check(restored.contains("AudioSampleRate=44100")&&!restored.contains("48000"),"disable restores original rate");
  check(restored.contains("AudioNumBuffersToEnqueue=8"),"disable preserves later unrelated changes");
  check(!Files.exists(backup),"disable clears owned backup");
  Files.delete(ini);Audio48.update(root.toFile(),rel,true);check(Files.readString(ini).contains("48000"),"fresh installation creates config");
  Audio48.update(root.toFile(),rel,false);check(!Files.readString(ini).contains("AudioSampleRate"),"fresh installation rollback removes override");
  String duplicates=section+"\nAudioSampleRate=44100\n"+section+"\nAudioSampleRate=32000\n";
  String merged=Audio48.rewrite(duplicates,List.of("AudioSampleRate=48000"));
  check(merged.split("AudioSampleRate",-1).length==2,"duplicate keys and sections produce one override");
  try{Audio48.update(root.toFile(),"../Engine.ini",true);throw new AssertionError("Traversal accepted");}catch(java.io.IOException expected){check(true,"rejects unsafe project path");}
  byte[] invalid={(byte)0xff,(byte)0xfe,0,0};Files.write(ini,invalid);
  try{Audio48.update(root.toFile(),rel,true);throw new AssertionError("Encoding accepted");}catch(java.io.IOException expected){check(Arrays.equals(invalid,Files.readAllBytes(ini))&&!Files.exists(backup),"unsupported encoding leaves original untouched");}
  System.out.println("ALL "+passed+" AUDIO CONFIG CHECKS PASSED");
 }
}
