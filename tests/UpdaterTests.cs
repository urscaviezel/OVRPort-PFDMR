using System;
using System.IO;
internal static class UpdaterTests {
 static int count;
 static void Check(bool ok,string message){if(!ok)throw new Exception(message);Console.WriteLine("PASS "+message);count++;}
 static string Setup(string root) {
  Directory.CreateDirectory(root);string stage=Path.Combine(root,".pfdmr-update-test");Directory.CreateDirectory(Path.Combine(stage,"payload"));
  foreach(string name in Updater.Names) {
   if(name=="jre"||name=="licenses") {
    Directory.CreateDirectory(Path.Combine(root,name));File.WriteAllText(Path.Combine(root,name,"test"),"old");
    Directory.CreateDirectory(Path.Combine(stage,"payload",name));File.WriteAllText(Path.Combine(stage,"payload",name,"test"),"new");
   } else {File.WriteAllText(Path.Combine(root,name),"old");File.WriteAllText(Path.Combine(stage,"payload",name),"new");}
  }
  Directory.CreateDirectory(Path.Combine(root,"workspace"));File.WriteAllText(Path.Combine(root,"workspace","key.keystore"),"private");
  return stage;
 }
 static void Main(string[] args) {
  string root=Path.GetFullPath(args[0]);string stage=Setup(Path.Combine(root,"success"));string install=Path.GetDirectoryName(stage);
  Updater.Install(install,stage,delegate {});
  Check(File.ReadAllText(Path.Combine(install,"OVRPort-PFDMR.jar"))=="new","new files installed");
  Check(File.ReadAllText(Path.Combine(stage,"backup","OVRPort-PFDMR.jar"))=="old","old files backed up until restart");
  Check(File.ReadAllText(Path.Combine(install,"workspace","key.keystore"))=="private","unmanaged workspace and signing key preserved");
  stage=Setup(Path.Combine(root,"rollback"));install=Path.GetDirectoryName(stage);
  bool failed=false;try{Updater.Install(install,stage,delegate{throw new IOException("simulated restart failure");});}catch(IOException){failed=true;}
  Check(failed && File.ReadAllText(Path.Combine(install,"OVRPort-PFDMR.jar"))=="old" && File.ReadAllText(Path.Combine(install,"jre","test"))=="old","restart failure restores original jar and runtime");
  stage=Setup(Path.Combine(root,"locked"));install=Path.GetDirectoryName(stage);
  using(var locked=new FileStream(Path.Combine(install,"README.md"),FileMode.Open,FileAccess.Read,FileShare.None)) {
   failed=false;try{Updater.Install(install,stage,delegate{});}catch(IOException){failed=true;}
   Check(failed && File.ReadAllText(Path.Combine(install,"OVRPort-PFDMR.jar"))=="old","locked file rolls back earlier replacements");
  }
  stage=Setup(Path.Combine(root,"missing"));install=Path.GetDirectoryName(stage);File.Delete(Path.Combine(stage,"payload","OVRPort-PFDMR.jar"));
  failed=false;try{Updater.Install(install,stage,delegate{});}catch(IOException){failed=true;}
  Check(failed && File.ReadAllText(Path.Combine(install,"OVRPort-PFDMR.jar"))=="old","incomplete payload rejected before changes");
  failed=false;try{Updater.Validate(install,root);}catch(IOException){failed=true;}Check(failed,"outside staging path rejected");
  Console.WriteLine("ALL "+count+" WINDOWS UPDATER CHECKS PASSED");
 }
}
