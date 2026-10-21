// OVRPort PFDMR Edition launcher. Copyright 2026 PFDMR Edition contributors. GPL-3.0.
using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Windows.Forms;

internal static class Launcher {
    // Quote Windows argv without invoking a command shell.
    private static string Quote(string value) {
        var result = new StringBuilder("\""); int slashes = 0;
        foreach (char c in value) {
            if (c == '\\') { slashes++; continue; }
            if (c == '"') result.Append('\\', slashes * 2 + 1);
            else result.Append('\\', slashes);
            result.Append(c); slashes = 0;
        }
        return result.Append('\\', slashes * 2).Append('"').ToString();
    }
    [STAThread]
    private static void Main(string[] args) {
        try {
            string root = AppDomain.CurrentDomain.BaseDirectory;
            if (args.Length == 3 && args[0] == "--finish-update") {
                string stage = Path.GetFullPath(args[1]).TrimEnd(Path.DirectorySeparatorChar);
                string directory = Path.GetFullPath(root).TrimEnd(Path.DirectorySeparatorChar);
                if (Path.GetDirectoryName(stage) != directory || !Path.GetFileName(stage).StartsWith(".pfdmr-update-", StringComparison.Ordinal))
                    throw new IOException("Invalid update cleanup path.");
                try { if (!Process.GetProcessById(Int32.Parse(args[2])).WaitForExit(120000)) throw new IOException("Update is still running."); }
                catch (ArgumentException) { /* Helper already exited. */ }
                // Only remove the validated private staging directory after its helper exits.
                if (Directory.Exists(stage)) { CheckNoLinks(stage); Directory.Delete(stage, true); }
                args = new string[0];
            }
            string java = Path.Combine(root, "jre", "bin", "javaw.exe");
            string jar = Path.Combine(root, "OVRPort-PFDMR.jar");
            if (!File.Exists(java) || !File.Exists(jar)) throw new FileNotFoundException("Please extract the complete ZIP first. The jre folder and OVRPort-PFDMR.jar must remain beside this launcher.");
            var arguments = new StringBuilder("-jar ").Append(Quote(jar));
            foreach (string arg in args) arguments.Append(' ').Append(Quote(arg));
            Process.Start(new ProcessStartInfo { FileName = java, Arguments = arguments.ToString(), WorkingDirectory = root, UseShellExecute = false, CreateNoWindow = true });
        } catch (Exception error) {
            MessageBox.Show(error.Message, "OVRPort PFDMR Edition", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }
    private static void CheckNoLinks(string path) {
        if ((File.GetAttributes(path) & FileAttributes.ReparsePoint) != 0) throw new IOException("Linked update directory.");
        if (Directory.Exists(path)) foreach (string child in Directory.GetFileSystemEntries(path)) CheckNoLinks(child);
    }
}
