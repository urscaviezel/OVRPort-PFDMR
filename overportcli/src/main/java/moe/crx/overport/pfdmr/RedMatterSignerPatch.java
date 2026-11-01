// SPDX-License-Identifier: GPL-3.0-only
package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.security.MessageDigest;
import com.android.apksig.ApkVerifier;
import com.reandroid.json.*;
import kotlin.Unit;
import moe.crx.overport.patching.*;

/** Exact build-38 signer compatibility, adapted from the community Nov-1 reference.
 * Retains the game's comparison, rejection path and platform callbacks. */
public final class RedMatterSignerPatch {
 public static final String ID="patch_pfdmr_redmatter_signer";
 public static final String LABEL="PFDMR compatibility: Red Matter 1 signer & 1800 x 1664 (experimental)";
 static final String GAME="root/lib/arm64-v8a/libUE4.so",RECEIPT="root/assets/pfdmr-redmatter-signer.json";
 static final String ORIGINAL="fd0449158d06fc076d863c4bc6d6a01ed1c2c9268b98be89d750c2352fdc6e61";
 static final long SITE=0x6b8c428L;static final int EXPECTED=0xb7df0a6f;
 public static final Patch PATCH=new Patch(ID,false,false,(e,a)->{e.selectWorkspace(f->{apply(f.getFile().toPath(),e.signingCertificate());return Unit.INSTANCE;});return Unit.INSTANCE;});
 static IllegalStateException fail(String message){return new IllegalStateException("Red Matter signer compatibility: "+message);}
 public static void validateSelection(PatcherContext c,Map<String,List<String>> args){
  if(!args.containsKey(ID))return;
  if(!"com.VerticalRobot.RedMatter".equals(c.appPackage())||!"38".equals(c.appVersionCode())||!"1.0.013".equals(c.appVersionName()))throw fail("Only Red Matter 1, ARM64 version 1.0.013 (38), is supported.");
  try{
   Path libs=c.getWorkingDir().toPath().resolve("root/lib");try(var paths=Files.list(libs)){if(paths.anyMatch(p->!p.getFileName().toString().equals("arm64-v8a")&&Files.isDirectory(p)))throw fail("Only ARM64-only APKs supported.");}
   ApkVerifier.Result result=new ApkVerifier.Builder(c.getInputApk()).build().verify();
   if(!result.isVerified()||result.getSignerCertificates().size()!=1||(result.getSigningCertificateLineage()!=null&&result.getSigningCertificateLineage().size()>1))throw fail("A valid single-signer input without certificate rotation is required.");
  }catch(Exception ex){if(ex instanceof IllegalStateException)throw (IllegalStateException)ex;throw fail(ex.toString());}
 }
 public static boolean active(Path ws){return Files.exists(ws.resolve(RECEIPT));}
 static String hash(byte[] bytes){return StereoPatch.sha256(bytes);}
 static int low(int value){return 0x52800008|((value&0xffff)<<5);}
 static int high(int value){return 0x72a00008|((value>>>16)<<5);}
 static int offset(byte[] bytes,long address){
  if(bytes.length<64)throw fail("Truncated ELF");ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
  if(b.getInt(0)!=0x464c457f||b.get(4)!=2||b.get(5)!=1||b.getShort(18)!=183)throw fail("Expected ARM64 ELF64 LE");
  long ph=b.getLong(32);int size=Short.toUnsignedInt(b.getShort(54)),count=Short.toUnsignedInt(b.getShort(56));if(size!=56||count==0||ph<0||ph+(long)size*count>bytes.length)throw fail("Invalid ELF headers");
  for(int i=0;i<count;i++){int p=Math.toIntExact(ph+(long)i*size);if(b.getInt(p)!=1)continue;long file=b.getLong(p+8),base=b.getLong(p+16),length=b.getLong(p+32);
   if(base>=0&&length>=16&&address>=base&&address-base<=length-16){long at=file+address-base;if(file<0||at<0||at>bytes.length-16)throw fail("Unbacked comparison site");return Math.toIntExact(at);}
  }throw fail("Comparison not mapped in PT_LOAD");
 }
 static byte[] edit(byte[] bytes,int signerHash,String allowedHash){
  if(!hash(bytes).equals(allowedHash))throw fail("Unsupported engine or existing external modification. Use the original APK.");int at=offset(bytes,SITE);ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
  if(b.getInt(at)!=low(EXPECTED)||b.getInt(at+4)!=high(EXPECTED)||b.getInt(at+8)!=0x6b08001f||b.getInt(at+12)!=0x54000040)throw fail("Signer comparison signature changed");
  byte[] out=bytes.clone();ByteBuffer w=ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);w.putInt(at,low(signerHash));w.putInt(at+4,high(signerHash));return out;
 }
 public static void apply(Path ws,byte[] certificate){try{
  if(certificate==null||certificate.length==0)throw fail("Final output certificate is not pinned");restore(ws);Path game=ws.resolve(GAME);byte[] before=Files.readAllBytes(game);int signerHash=Arrays.hashCode(certificate);byte[] after=edit(before,signerHash,ORIGINAL);int at=offset(before,SITE);
  JSONObject receipt=new JSONObject().put("version",1).put("profile",ID).put("before_sha256",ORIGINAL).put("after_sha256",hash(after)).put("offset",at).put("original_bytes",Base64.getEncoder().encodeToString(Arrays.copyOfRange(before,at,at+8))).put("certificate_sha256",hash(certificate)).put("signature_hash",signerHash);
  Path record=ws.resolve(RECEIPT);Files.createDirectories(record.getParent());Files.writeString(record,receipt.toString());Files.write(game,after);
 }catch(IOException ex){throw fail(ex.toString());}}
 static JSONObject receipt(Path ws)throws IOException{
  JSONObject r=new JSONObject(Files.readString(ws.resolve(RECEIPT)));if(r.getInt("version")!=1||!ID.equals(r.getString("profile"))||!ORIGINAL.equals(r.getString("before_sha256")))throw fail("Unknown rollback receipt");return r;
 }
 public static void restore(Path ws){if(!active(ws))return;try{
  JSONObject r=receipt(ws);Path game=ws.resolve(GAME);byte[] bytes=Files.readAllBytes(game);byte[] original=restored(bytes,r,ORIGINAL);if(!Arrays.equals(bytes,original))Files.write(game,original);Files.delete(ws.resolve(RECEIPT));
 }catch(IOException ex){throw fail(ex.toString());}}
 static byte[] restored(byte[] bytes,JSONObject r,String originalHash){
  if(hash(bytes).equals(originalHash))return bytes;
  if(!hash(bytes).equals(r.getString("after_sha256")))throw fail("Engine changed after patching; cannot restore safely");
  int at=offset(bytes,SITE);if(at!=r.getInt("offset"))throw fail("Rollback site changed");byte[] old=Base64.getDecoder().decode(r.getString("original_bytes"));if(old.length!=8)throw fail("Invalid rollback bytes");byte[] out=bytes.clone();System.arraycopy(old,0,out,at,8);if(!hash(out).equals(originalHash))throw fail("Original engine verification failed");return out;
 }
 static byte[] onlyCertificate(ApkVerifier.Result result){try{
  if(!result.isVerified()||result.getSignerCertificates().size()!=1||(result.getSigningCertificateLineage()!=null&&result.getSigningCertificateLineage().size()>1))throw fail("Final signature verification failed or has unsupported rotation");return result.getSignerCertificates().get(0).getEncoded();
 }catch(java.security.cert.CertificateEncodingException ex){throw fail(ex.toString());}}
 public static void verifyOutput(Path ws,Path apk,byte[] pinned){if(!active(ws))return;try{
  JSONObject r=receipt(ws);byte[] actual=onlyCertificate(new ApkVerifier.Builder(apk.toFile()).build().verify());
  if(!Arrays.equals(actual,pinned)||!hash(actual).equals(r.getString("certificate_sha256"))||Arrays.hashCode(actual)!=r.getInt("signature_hash"))throw fail("Final signer does not match the profile target");
  try(var zip=new ZipFile(apk.toFile())){
   var entry=zip.getEntry("lib/arm64-v8a/libUE4.so");if(entry==null)throw fail("Output engine missing");MessageDigest digest=MessageDigest.getInstance("SHA-256");try(var in=zip.getInputStream(entry)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);}if(!HexFormat.of().formatHex(digest.digest()).equals(r.getString("after_sha256")))throw fail("Output engine differs from verified profile");
   try(var in=zip.getInputStream(entry)){in.skipNBytes(r.getInt("offset"));byte[] site=in.readNBytes(16);if(site.length!=16)throw fail("Output comparison truncated");ByteBuffer b=ByteBuffer.wrap(site).order(ByteOrder.LITTLE_ENDIAN);int target=Arrays.hashCode(actual);if(b.getInt()!=low(target)||b.getInt()!=high(target)||b.getInt()!=0x6b08001f||b.getInt()!=0x54000040)throw fail("Output signer comparison mismatch");}
  }
 }catch(Exception ex){if(ex instanceof IllegalStateException)throw (IllegalStateException)ex;throw fail(ex.toString());}}
}
