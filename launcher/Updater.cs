// Portable Windows updater. Copyright 2026 PFDMR Edition contributors. GPL-3.0.
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Windows.Forms;

internal static class Updater {
    internal static readonly string[] Names = { "OVRPort-PFDMR.jar", "OVRPort-PFDMR.exe", "PFDMR-Updater.exe",
        "Start-OVRPort-PFDMR.cmd", "README.md", "README.de.md", "LICENSE", "THIRD-PARTY-NOTICES.md", "CHANGELOG.md", "BUILD-INFO.json", "jre", "licenses" };
    internal static bool Exists(string path) { return File.Exists(path) || Directory.Exists(path); }
    internal static void NoLinks(string path) {
        if (!Exists(path)) return;
        if ((File.GetAttributes(path) & FileAttributes.ReparsePoint) != 0) throw new IOException("Linked update paths are not supported.");
        if (Directory.Exists(path)) foreach (string child in Directory.GetFileSystemEntries(path)) NoLinks(child);
    }
    private static void Move(string from, string to) {
        for (int attempt=0;;attempt++) {
            try { if(Directory.Exists(from)) Directory.Move(from,to); else File.Move(from,to); return; }
            catch(IOException) { if(attempt>=20)throw; Thread.Sleep(250); }
        }
    }
    private static void Delete(string path) { if(Directory.Exists(path))Directory.Delete(path,true);else if(File.Exists(path))File.Delete(path); }
    internal static void Validate(string root,string stage) {
        if(Path.GetDirectoryName(stage)!=root || !Path.GetFileName(stage).StartsWith(".pfdmr-update-",StringComparison.Ordinal))throw new IOException("Invalid staging directory.");
        NoLinks(stage);
        for(string p=root; !String.IsNullOrEmpty(p); p=Path.GetDirectoryName(p))
            if ((File.GetAttributes(p) & FileAttributes.ReparsePoint)!=0)throw new IOException("Linked installation paths are not supported.");
        foreach(string name in Names) {
            string source=Path.Combine(stage,"payload",name);
            if(!Exists(source))throw new IOException("Incomplete update: "+name);
            NoLinks(Path.Combine(root,name));
        }
    }
    // All replacements are journaled in memory and backed up before moving new files.
    // On failure the backup is retained if any restoration itself fails.
    internal static void Install(string root,string stage, Action afterInstall) {
        Validate(root,stage);
        string backup=Path.Combine(stage,"backup");Directory.CreateDirectory(backup);
        var saved=new List<string>();var installed=new List<string>();
        try {
            foreach(string name in Names) {
                string dest=Path.Combine(root,name);
                if(Exists(dest)){Move(dest,Path.Combine(backup,name));saved.Add(name);}
                Move(Path.Combine(stage,"payload",name),dest);installed.Add(name);
            }
            afterInstall();
        } catch(Exception original) {
            try {
                installed.Reverse();foreach(string name in installed)Delete(Path.Combine(root,name));
                saved.Reverse();foreach(string name in saved)Move(Path.Combine(backup,name),Path.Combine(root,name));
            } catch(Exception rollback) {
                throw new IOException("Update failed. Recovery files remain at "+backup+". "+rollback.Message,original);
            }
            throw new IOException("Update failed; the previous version was restored. "+original.Message,original);
        }
    }
    [STAThread]
    private static void Main(string[] args) {
        string stage=null;
        try {
            if(args.Length!=3)throw new ArgumentException("Start updates from inside OVRPort PFDMR Edition.");
            string root=Path.GetFullPath(args[0]).TrimEnd(Path.DirectorySeparatorChar);
            stage=Path.GetFullPath(args[1]).TrimEnd(Path.DirectorySeparatorChar);
            Validate(root,stage);
            Process parent=Process.GetProcessById(Int32.Parse(args[2]));
            File.WriteAllText(Path.Combine(stage,"ready"),"ready");
            if(!parent.WaitForExit(120000))throw new IOException("The application did not close. Update cancelled.");
            using(var updateLock=new FileStream(Path.Combine(root,".pfdmr-update.lock"),FileMode.OpenOrCreate,FileAccess.ReadWrite,FileShare.None)) {
                Install(root,stage,delegate {
                    Process.Start(new ProcessStartInfo { FileName=Path.Combine(root,"OVRPort-PFDMR.exe"),
                        Arguments="--finish-update \""+stage+"\" "+Process.GetCurrentProcess().Id,
                        WorkingDirectory=root,UseShellExecute=false,CreateNoWindow=true });
                });
            }
        } catch(Exception error) {
            if(stage!=null && Directory.Exists(stage)) {
                try {File.WriteAllText(Path.Combine(stage,"error.txt"),error.ToString());}catch(IOException){}
            }
            MessageBox.Show(error.Message,"OVRPort PFDMR update",MessageBoxButtons.OK,MessageBoxIcon.Error);
        }
    }
}
