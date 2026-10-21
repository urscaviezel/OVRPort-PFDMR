package moe.crx.overport.pfdmr;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Bounded downloads and extraction for the portable Windows distribution. */
public final class UpdateFiles {
    public static final Set<String> FILES = Set.of("OVRPort-PFDMR.jar", "OVRPort-PFDMR.exe", "PFDMR-Updater.exe",
        "Start-OVRPort-PFDMR.cmd", "README.md", "README.de.md", "LICENSE", "THIRD-PARTY-NOTICES.md", "CHANGELOG.md", "BUILD-INFO.json");
    public static final Set<String> DIRECTORIES = Set.of("jre", "licenses");
    public static final String REPO = "https://github.com/urscaviezel/OVRPort-PFDMR";
    public static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(file)) { byte[] b = new byte[65536]; int n; while ((n=in.read(b))!=-1) digest.update(b,0,n); }
        return HexFormat.of().formatHex(digest.digest());
    }
    public static void download(String url, Path target, long maxBytes) throws Exception {
        URI uri = URI.create(url);
        for (int redirects=0; redirects<6; redirects++) {
            if (!uri.getScheme().equals("https")) throw new IOException("HTTPS required.");
            HttpURLConnection connection=(HttpURLConnection)uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(15000); connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "OVRPort-PFDMR-Updater");
            try {
                int code=connection.getResponseCode();
                if (code==301||code==302||code==303||code==307||code==308) { uri=uri.resolve(connection.getHeaderField("Location")); continue; }
                if (code!=200) throw new IOException("GitHub download failed (HTTP "+code+"). Please try again later.");
                try (var in=connection.getInputStream(); var out=Files.newOutputStream(target)) {
                    byte[] b=new byte[65536]; long total=0; int n;
                    while ((n=in.read(b))!=-1) { total+=n; if(total>maxBytes)throw new IOException("Download exceeds expected size.");out.write(b,0,n); }
                }
                return;
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many download redirects.");
    }
    public static void verify(Path zip, long size, String digest) throws Exception {
        if (Files.size(zip)!=size || !digest.matches("sha256:[0-9a-f]{64}") || !sha256(zip).equals(digest.substring(7)))
            throw new IOException("Update checksum mismatch. The current installation has not been changed.");
    }
    public static void extract(Path zip, Path target, String version) throws Exception {
        target=target.toAbsolutePath().normalize();
        String prefix="OVRPort-PFDMR-Edition-"+version+"-Windows/";
        Set<String> seen=new HashSet<>(); long total=0; int count=0;
        try (var entries=new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e=entries.getNextEntry())!=null) {
                if (++count>20000) throw new IOException("Too many archive entries.");
                String name=e.getName();
                if (!name.startsWith(prefix)) throw new IOException("Unexpected update archive layout.");
                String relative=name.substring(prefix.length());
                if (relative.isEmpty() && e.isDirectory()) continue;
                if (relative.endsWith("/")) relative=relative.substring(0,relative.length()-1);
                String[] parts=relative.split("/",-1);
                for (String part:parts) if (!part.matches("[a-zA-Z0-9_.+@() -]+") || part.equals(".") || part.equals("..") || part.endsWith(".") || part.endsWith(" ") || part.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?"))
                    throw new IOException("Unsafe update archive path.");
                if (!(DIRECTORIES.contains(parts[0]) || parts.length==1 && FILES.contains(parts[0]))) throw new IOException("Unexpected update file: "+relative);
                if (!seen.add(relative.toLowerCase(Locale.ROOT))) throw new IOException("Duplicate archive entry.");
                Path dest=target.resolve(relative).normalize();
                if (!dest.startsWith(target)) throw new IOException("Unsafe archive path.");
                if(e.isDirectory()) {Files.createDirectories(dest);continue;}
                Files.createDirectories(dest.getParent());
                try(var out=Files.newOutputStream(dest, StandardOpenOption.CREATE_NEW)) {
                    byte[] b=new byte[65536];int n;while((n=entries.read(b))!=-1){total+=n;if(total>1024L*1024*1024)throw new IOException("Expanded update too large.");out.write(b,0,n);}
                }
            }
        }
        for(String file:FILES) if(!Files.isRegularFile(target.resolve(file))) throw new IOException("Incomplete update: "+file);
        for(String file:List.of("jre/bin/javaw.exe","jre/release")) if(!Files.isRegularFile(target.resolve(file)))throw new IOException("Incomplete Java runtime.");
        if(!Files.isDirectory(target.resolve("licenses")))throw new IOException("Missing licenses.");
    }
    public static Path installation(Class<?> anchor) throws Exception {
        Path jar=Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
        if(!System.getProperty("os.name").startsWith("Windows") || !jar.getFileName().toString().equals("OVRPort-PFDMR.jar"))
            throw new IOException("Automatic installation requires the extracted Windows package. Download it from GitHub once.");
        Path root=jar.getParent().toRealPath();
        if(!Files.isRegularFile(root.resolve("PFDMR-Updater.exe")))throw new IOException("Update helper is missing. Extract the complete Windows package.");
        return root;
    }
}
