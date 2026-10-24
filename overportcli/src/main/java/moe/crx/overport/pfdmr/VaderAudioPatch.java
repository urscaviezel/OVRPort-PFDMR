package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import kotlin.Unit;
import moe.crx.overport.patching.*;

/** Redirect the verified Vader mixer to its existing bounds-correct scalar path. */
public final class VaderAudioPatch {
 public static final String ID="patch_pfdmr_vader_audio";
 public static final String LABEL="PFDMR compatibility: Vader Immortal audio mixer (experimental)";
 private static final String LIB="root/lib/arm64-v8a/libUE4.so", RECEIPT="root/assets/pfdmr-vader-audio.properties";
 private static final String SIGNATURE="823ec881d767c32e73e28ea4bfde03c8538bb162de5f12278aca67955bf5a0aa";
 private static final int ORIGINAL=0x540008c1, FIXED=0x14000046;
 private static final Map<String,String> VERSIONS=Map.of("com.ILMxLAB.VaderImmortal.ep1","1.1.1+667256.cl.387770/387770","com.ILMxLAB.VaderImmortal.ep2","2.0.3+667261.cl.387778/387778","com.ILMxLAB.VaderImmortal.ep3","3.0.3+667263.cl.387932/387932");
 public static final Patch PATCH=new Patch(ID,false,false,(e,a)->{e.selectWorkspace(f->{apply(f.getFile().toPath());return Unit.INSTANCE;});return Unit.INSTANCE;});
 private static IllegalStateException fail(String s){return new IllegalStateException("Vader audio mixer: "+s);}
 public static void validateSelection(PatcherContext c,Map<String,List<String>> args){
  if(!args.containsKey(ID))return;
  if(!Objects.equals(VERSIONS.get(c.appPackage()),c.appVersionName()+"/"+c.appVersionCode()))throw fail("Unsupported episode/version. Use a verified ARM64 build.");
  try{inspect(Files.readAllBytes(c.getWorkingDir().toPath().resolve(LIB)));}catch(IOException ex){throw fail(ex.toString());}
 }
 public static int inspect(byte[] data){
  try {
   ByteBuffer b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
   if(data.length<64||b.getInt(0)!=0x464c457f||b.get(4)!=2||b.get(5)!=1||b.getShort(18)!=183||b.getShort(16)!=3)throw fail("Expected ARM64 ELF shared library.");
   int ph=Math.toIntExact(b.getLong(32)),stride=Short.toUnsignedInt(b.getShort(54)),count=Short.toUnsignedInt(b.getShort(56));
   if(stride<56||count==0||ph<0||(long)ph+(long)stride*count>data.length)throw fail("Invalid ELF headers.");
   Set<Integer> matches=new HashSet<>();
   for(int i=0;i<count;i++){
    int h=ph+i*stride;if(b.getInt(h)!=1||(b.getInt(h+4)&1)==0)continue;
    long off=b.getLong(h+8),len=b.getLong(h+32);
    if(off<0||len<0||off>data.length||len>data.length-off)throw fail("Invalid executable range.");
    for(int at=(int)((off+3)&~3L);at<=off+len-372;at+=4){
     if(b.getInt(at+20)!=ORIGINAL&&b.getInt(at+20)!=FIXED)continue;
     byte[] candidate=Arrays.copyOfRange(data,at,at+372);
     ByteBuffer.wrap(candidate).order(ByteOrder.LITTLE_ENDIAN).putInt(20,ORIGINAL);
     if(SIGNATURE.equals(StereoPatch.sha256(candidate)))matches.add(at+20);
    }
   }
   if(matches.size()!=1)throw fail("Unrecognized or ambiguous complete mixer function.");
   return matches.iterator().next();
  }catch(IndexOutOfBoundsException|ArithmeticException ex){throw fail("Malformed ELF.");}
 }
 public static void apply(Path ws){
  try{
   restore(ws);Path lib=ws.resolve(LIB),receipt=ws.resolve(RECEIPT);byte[] d=Files.readAllBytes(lib);int at=inspect(d);
   ByteBuffer b=ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);if(b.getInt(at)==FIXED)return; // independently fixed input
   Properties p=new Properties();p.setProperty("version","1");p.setProperty("before",StereoPatch.sha256(d));p.setProperty("offset",""+at);
   b.putInt(at,FIXED);p.setProperty("after",StereoPatch.sha256(d));Files.createDirectories(receipt.getParent());
   try(var out=Files.newOutputStream(receipt)){p.store(out,"Reversible Vader audio mixer edit");}Files.write(lib,d);
  }catch(IOException ex){throw fail(ex.toString());}
 }
 public static void restore(Path ws){
  Path receipt=ws.resolve(RECEIPT);if(!Files.exists(receipt))return;
  try{
   Properties p=new Properties();try(var in=Files.newInputStream(receipt)){p.load(in);}
   byte[] d=Files.readAllBytes(ws.resolve(LIB));int at=inspect(d);
   if(!"1".equals(p.getProperty("version"))||!StereoPatch.sha256(d).equals(p.getProperty("after"))||!Integer.toString(at).equals(p.getProperty("offset")))throw fail("Changed library or invalid rollback receipt.");
   ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN).putInt(at,ORIGINAL);
   if(!StereoPatch.sha256(d).equals(p.getProperty("before")))throw fail("Rollback checksum mismatch.");
   Files.write(ws.resolve(LIB),d);Files.delete(receipt);
  }catch(IOException ex){throw fail(ex.toString());}
 }
}
