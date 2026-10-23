import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import moe.crx.overport.pfdmr.UpdateFiles;
import moe.crx.overport.app.model.AppUpdates;

public class UpdateTests {
 static int count;
 interface Action { void run() throws Exception; }
 static void check(boolean b,String label){if(!b)throw new AssertionError(label);System.out.println("PASS "+label);count++;}
 static boolean fails(Action a){try{a.run();return false;}catch(Exception expected){return true;}}
 static String release(String v,boolean draft,boolean complete) {
  String name="OVRPort-PFDMR-Edition-"+v+"-Windows.zip";
  return "{\"tag_name\":\"v"+v+"\",\"html_url\":\"https://github.com/urscaviezel/OVRPort-PFDMR/releases/tag/v"+v+"\",\"name\":\"test\",\"draft\":"+draft+",\"assets\":"+(complete?"[{\"name\":\""+name+"\",\"browser_download_url\":\""+UpdateFiles.REPO+"/releases/download/v"+v+"/"+name+"\",\"size\":123,\"digest\":\"sha256:"+"a".repeat(64)+"\"}]":"[]")+"}";
 }
 static void zip(Path path,String... names)throws Exception{try(var out=new ZipOutputStream(Files.newOutputStream(path))){for(String n:names){out.putNextEntry(new ZipEntry(n));out.write(new byte[]{1});out.closeEntry();}}}
 public static void main(String[] args)throws Exception {
  var u=AppUpdates.INSTANCE;Path run=Path.of(args[0]).toAbsolutePath();Files.createDirectories(run);
  check(u.compareVersions("v0.1.0-beta.10","0.1.0-beta.8")>0,"numeric beta ordering");
  check(u.compareVersions("0.1.0","0.1.0-beta.99")>0,"stable follows prerelease");
  check(u.compareVersions("1.0.0-beta.1","0.9.9")>0,"major version ordering");
  check(u.compareVersions("v0.1.0-beta.7","0.1.0-beta.7")==0,"tag prefix equivalent");
  String data="["+release("0.1.0-beta.6",false,true)+","+release("0.1.0-beta.10",true,true)+","+release("0.1.0-beta.9",false,false)+","+release("0.1.0-beta.8",false,true)+"]";
  check(u.selectRelease(data,"0.1.0-beta.7").getTagName().equals("v0.1.0-beta.8"),"newer complete beta selected; drafts and incomplete releases skipped");
  check(u.selectRelease(data,"0.1.0-beta.8")==null,"no equal-version reinstall or downgrade");
  check(u.selectRelease("["+release("0.1.0-beta.8",false,true).replace("\"name\":\"test\"","\"name\":null")+"]","0.1.0-beta.7")!=null,"release without optional display title accepted");
  check(u.selectRelease("["+release("0.1.0-beta.8",false,true).replace("https://github.com/urscaviezel/OVRPort-PFDMR/releases/download/","https://example.com/")+"]","0.1.0-beta.7")==null,"foreign asset URL rejected");
  check(u.selectRelease("["+release("0.1.0-beta.8",false,true).replace("sha256:","sha1:")+"]","0.1.0-beta.7")==null,"missing trustworthy checksum rejected");
  Path archive=run.resolve("test.zip");String prefix="OVRPort-PFDMR-Edition-0.1.0-beta.8-Windows/";
  for(String bad:List.of("../outside", "jre/../../outside", "jre/C:drive", "jre/CON.txt", "jre/a\\b", "jre/trailing. ", "workspace/key.keystore")) {
   zip(archive,prefix+bad);check(fails(()->UpdateFiles.extract(archive,run.resolve("bad-"+count),"0.1.0-beta.8")),"reject unsafe archive "+bad);
  }
  zip(archive,prefix+"README.md",prefix+"readme.md");check(fails(()->UpdateFiles.extract(archive,run.resolve("duplicate"),"0.1.0-beta.8")),"duplicate/case-conflicting entries rejected");
  zip(archive,prefix+"OVRPort-PFDMR.jar");check(fails(()->UpdateFiles.extract(archive,run.resolve("incomplete"),"0.1.0-beta.8")),"incomplete package rejected");
  String digest="sha256:"+UpdateFiles.sha256(archive);UpdateFiles.verify(archive,Files.size(archive),digest);check(true,"matching size and SHA256 accepted");
  check(fails(()->UpdateFiles.verify(archive,Files.size(archive),"sha256:"+"0".repeat(64))),"corrupt download rejected");
  check(fails(()->UpdateFiles.verify(archive,Files.size(archive)+1,digest)),"truncated download rejected");
  check(fails(()->UpdateFiles.download("http://example.com",run.resolve("http"),10)),"unencrypted redirect/download rejected");
  if(args.length>1){UpdateFiles.extract(Path.of(args[1]),run.resolve("real-package"),args.length>2?args[2]:"0.1.0-beta.7");check(true,"real packaged update extracts successfully");}
  System.out.println("ALL "+count+" UPDATE CHECKS PASSED");
 }
}
