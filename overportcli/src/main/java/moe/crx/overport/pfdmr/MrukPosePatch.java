package moe.crx.overport.pfdmr;
import java.util.*;
/** Known-build MRUK edit: require valid position/orientation, retain XR_SUCCESS checks. */
public final class MrukPosePatch {
 public record Profile(String before,String after,int offset) {}
 public static final Profile PINBALL=new Profile("fc2df0e83599bbd932680fae2859c06e189c8d6f18cc96babd605eed65897eb8","38ff3719a3d5abc81c9f6c5bdfd9bfce2df67895974e8bbad9c9eacb02f26f04",0x233374);
 public static final Profile ZOMBIES=new Profile("0a41fd207e149da64826bb9134ce05a3cb0d95dc2d091fd3b43e439321cc8961","aaec9e296488201bb54babaac481b2a0f2829b739bd407ac8489abf125ef2dd7",0x23faf4);
 public static byte[] apply(byte[] data,Profile p){
  String h=StereoPatch.sha256(data);if(h.equals(p.after))return data.clone();
  if(!h.equals(p.before))throw new IllegalStateException("Unsupported MRUK library.");
  byte[] out=data.clone();byte[] before=HexFormat.of().parseHex("1f0d40f2");
  if(!Arrays.equals(before,Arrays.copyOfRange(out,p.offset,p.offset+4)))throw new IllegalStateException("Unexpected MRUK instruction.");
  out[p.offset+1]=5;if(!StereoPatch.sha256(out).equals(p.after))throw new IllegalStateException("MRUK result mismatch.");return out;
 }
}
