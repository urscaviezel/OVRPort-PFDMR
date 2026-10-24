package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import com.reandroid.json.*;

/** Bounded UnityFS block editor. Untouched compressed blocks are copied byte-for-byte.
 * Only a hash-verified profile is accepted; no general Unity type-tree guesses at runtime.
 */
public final class UnityBundlePatch implements AutoCloseable {
 private final RandomAccessFile input;
 private final List<Block> blocks=new ArrayList<>();
 private final List<Node> nodes=new ArrayList<>();
 private final byte[] header;
 private final String player,engine;
 private final int version,flags;
 private long rawSize;
 private record Block(long file,long raw,int packed,int size,int flags){}
 private record Node(long offset,long size,int flags,String name){}
 private static IllegalStateException fail(String s){return new IllegalStateException("Unity bundle: "+s);}
 public static String hash(Path p)throws IOException{
  try{MessageDigest md=MessageDigest.getInstance("SHA-256");try(var in=Files.newInputStream(p)){byte[] b=new byte[131072];for(int n;(n=in.read(b))!=-1;)md.update(b,0,n);}return HexFormat.of().formatHex(md.digest());}
  catch(NoSuchAlgorithmException ex){throw new AssertionError(ex);}
 }
 private static String string(DataInput in)throws IOException{var out=new ByteArrayOutputStream();for(int i=0;i<4096;i++){int b=in.readUnsignedByte();if(b==0)return out.toString(StandardCharsets.UTF_8);out.write(b);}throw fail("Unterminated string.");}
 private static void string(DataOutput out,String s)throws IOException{out.write(s.getBytes(StandardCharsets.UTF_8));out.writeByte(0);}
 private static int bound(int n,int max){if(n<0||n>max)throw fail("Invalid block range.");return n;}
 private static long align(long n){return (n+15)&~15L;}
 public UnityBundlePatch(Path path)throws IOException{
  input=new RandomAccessFile(path.toFile(),"r");
  try{
   if(!string(input).equals("UnityFS"))throw fail("Unsupported signature.");
   version=input.readInt();player=string(input);engine=string(input);
   long size=input.readLong();int packed=bound(input.readInt(),16*1024*1024),unpacked=bound(input.readInt(),16*1024*1024);flags=input.readInt();
   if(version!=8||size!=input.length()||(flags&~0x243)!=0||(flags&0x40)==0)throw fail("Unsupported UnityFS header.");
   input.seek(align(input.getFilePointer()));byte[] bytes=new byte[packed];input.readFully(bytes);
   byte[] info=decode(bytes,unpacked,flags&63);
   if((flags&0x200)!=0)input.seek(align(input.getFilePointer()));
   long dataStart=input.getFilePointer();header=new byte[Math.toIntExact(dataStart)];input.seek(0);input.readFully(header);
   var in=new DataInputStream(new ByteArrayInputStream(info));in.skipNBytes(16);int count=bound(in.readInt(),1000000);long file=dataStart,raw=0;
   for(int i=0;i<count;i++){int n=bound(in.readInt(),16*1024*1024),c=bound(in.readInt(),16*1024*1024),f=in.readUnsignedShort();if((f&63)!=0&&(f&63)!=2&&(f&63)!=3)throw fail("Unsupported compression.");blocks.add(new Block(file,raw,c,n,f));file+=c;raw+=n;}
   if(file!=input.length())throw fail("Unexpected trailing or truncated blocks.");rawSize=raw;
   count=bound(in.readInt(),100000);for(int i=0;i<count;i++){long off=in.readLong(),n=in.readLong();int f=in.readInt();String name=string(in);if(off<0||n<0||off>raw||n>raw-off)throw fail("Invalid directory range.");nodes.add(new Node(off,n,f,name));}
   if(in.available()!=0)throw fail("Unexpected directory data.");
  }catch(Throwable ex){input.close();throw ex;}
 }
 public void close()throws IOException{input.close();}
 private byte[] packed(Block b)throws IOException{byte[] p=new byte[b.packed];input.seek(b.file);input.readFully(p);return p;}
 public static byte[] decode(byte[] src,int size,int mode){
  if(mode==0){if(src.length!=size)throw fail("Uncompressed size mismatch.");return src;}
  if(mode!=2&&mode!=3)throw fail("Unsupported compression.");
  byte[] dst=new byte[size];int s=0,d=0;
  try{
   while(s<src.length){int token=src[s++]&255,len=token>>>4;if(len==15){int n;do{n=src[s++]&255;len=Math.addExact(len,n);}while(n==255);}
    if(len>src.length-s||len>size-d)throw fail("Invalid LZ4 literals.");System.arraycopy(src,s,dst,d,len);s+=len;d+=len;if(s==src.length)break;
    int offset=(src[s++]&255)|((src[s++]&255)<<8);if(offset==0||offset>d)throw fail("Invalid LZ4 offset.");
    len=token&15;if(len==15){int n;do{n=src[s++]&255;len=Math.addExact(len,n);}while(n==255);}len=Math.addExact(len,4);
    if(len>size-d)throw fail("Invalid LZ4 match.");for(int j=0;j<len;j++)dst[d+j]=dst[d+j-offset];d+=len;
   }
   if(d!=size)throw fail("LZ4 output size mismatch.");return dst;
  }catch(IndexOutOfBoundsException|ArithmeticException ex){throw fail("Malformed LZ4.");}
 }
 private byte[] range(long offset,int size)throws IOException{
  if(offset<0||size<0||offset>rawSize||size>rawSize-offset)throw fail("Read outside bundle.");byte[] out=new byte[size];int copied=0;
  for(Block b:blocks){long lo=Math.max(offset,b.raw),hi=Math.min(offset+size,b.raw+b.size);if(hi<=lo)continue;byte[] data=decode(packed(b),b.size,b.flags&63);int n=(int)(hi-lo);System.arraycopy(data,(int)(lo-b.raw),out,(int)(lo-offset),n);copied+=n;}
  if(copied!=size)throw fail("Incomplete range.");return out;
 }
 private Node node(String name){return nodes.stream().filter(n->n.name.equals(name)).reduce((a,b)->{throw fail("Duplicate node.");}).orElseThrow(()->fail("Missing node: "+name));}
 private static byte[] editObject(byte[] old,JSONObject obj){
  if(!StereoPatch.sha256(old).equals(obj.getString("before")))throw fail("Object fingerprint mismatch.");
  var out=new ByteArrayOutputStream();int pos=0;JSONArray edits=obj.getJSONArray("edits");
  for(int i=0;i<edits.length();i++){JSONObject e=edits.getJSONObject(i);int at=e.getInt("offset"),remove=e.getInt("remove");if(at<pos||remove<0||at>old.length||remove>old.length-at)throw fail("Invalid object edit.");out.write(old,pos,at-pos);out.writeBytes(HexFormat.of().parseHex(e.getString("data")));pos=at+remove;}
  out.write(old,pos,old.length-pos);byte[] updated=out.toByteArray();if(!StereoPatch.sha256(updated).equals(obj.getString("after")))throw fail("Object result mismatch.");return updated;
 }
 /** Writes a candidate and compact rollback data, without replacing the input. */
 public void patch(JSONObject profile,Path output,Path rollback)throws IOException{
  Map<Long,byte[]> writes=new TreeMap<>(),inserts=new TreeMap<>();Map<String,ByteArrayOutputStream> appends=new LinkedHashMap<>();
  JSONArray objects=profile.getJSONArray("objects");
  for(int i=0;i<objects.length();i++){
   JSONObject o=objects.getJSONObject(i);Node n=node(o.getString("file"));if(n.size!=o.getLong("fileSize"))throw fail("Serialized file size mismatch.");
   int start=o.getInt("start"),size=o.getInt("size");byte[] updated=editObject(range(n.offset+start,size),o);
   if(size==updated.length){writes.put(n.offset+start,updated);continue;}
   var appended=appends.computeIfAbsent(n.name,k->new ByteArrayOutputStream());while((n.size+appended.size())%8!=0)appended.write(0);
   long at=n.size+appended.size();appended.writeBytes(updated);
   // SerializedFile v23 object entries: int64 pathId, uint64 relative offset, uint32 length.
   byte[] entry=range(n.offset+o.getInt("table"),20);ByteBuffer b=ByteBuffer.wrap(entry).order(ByteOrder.LITTLE_ENDIAN);
   if(b.getLong(0)!=o.getLong("id")||b.getLong(8)!=start-o.getInt("dataOffset")||b.getInt(16)!=size)throw fail("Object table mismatch.");
   b.putLong(8,at-o.getInt("dataOffset"));b.putInt(16,updated.length);writes.put(n.offset+o.getInt("table"),entry);
  }
  for(var e:appends.entrySet()){Node n=node(e.getKey());byte[] extra=e.getValue().toByteArray();inserts.put(n.offset+n.size,extra);
   byte[] size=ByteBuffer.allocate(8).putLong(n.size+extra.length).array();writes.put(n.offset+24,size);}
  Map<Integer,byte[]> changed=new TreeMap<>(),original=new TreeMap<>();List<Block> result=new ArrayList<>();
  for(int i=0;i<blocks.size();i++){
   Block b=blocks.get(i);boolean touched=writes.entrySet().stream().anyMatch(e->e.getKey()<b.raw+b.size&&e.getKey()+e.getValue().length>b.raw)||inserts.keySet().stream().anyMatch(at->at>=b.raw&&(at<b.raw+b.size||at==rawSize&&at==b.raw+b.size));
   if(!touched){result.add(b);continue;}byte[] compressed=packed(b),raw=decode(compressed,b.size,b.flags&63);original.put(i,compressed);
   for(var e:writes.entrySet()){long lo=Math.max(b.raw,e.getKey()),hi=Math.min(b.raw+b.size,e.getKey()+e.getValue().length);if(hi>lo)System.arraycopy(e.getValue(),(int)(lo-e.getKey()),raw,(int)(lo-b.raw),(int)(hi-lo));}
   var out=new ByteArrayOutputStream();int pos=0;for(var e:inserts.entrySet()){long at=e.getKey();if(at<b.raw||at>b.raw+b.size||at==b.raw+b.size&&at!=rawSize)continue;int end=(int)(at-b.raw);out.write(raw,pos,end-pos);out.writeBytes(e.getValue());pos=end;}out.write(raw,pos,raw.length-pos);byte[] updated=out.toByteArray();changed.put(i,updated);result.add(new Block(0,0,updated.length,updated.length,b.flags&~63));
  }
  var infoBytes=new ByteArrayOutputStream();var info=new DataOutputStream(infoBytes);info.write(new byte[16]);info.writeInt(result.size());
  for(Block b:result){info.writeInt(b.size);info.writeInt(b.packed);info.writeShort(b.flags);}info.writeInt(nodes.size());
  for(Node n:nodes){long shift=0;for(var e:inserts.entrySet())if(e.getKey()<=n.offset)shift+=e.getValue().length;info.writeLong(n.offset+shift);info.writeLong(n.size+(appends.containsKey(n.name)?appends.get(n.name).size():0));info.writeInt(n.flags);string(info,n.name);}
  byte[] directory=infoBytes.toByteArray();var headBytes=new ByteArrayOutputStream();var head=new DataOutputStream(headBytes);string(head,"UnityFS");head.writeInt(version);string(head,player);string(head,engine);int sizeAt=headBytes.size();head.writeLong(0);head.writeInt(directory.length);head.writeInt(directory.length);head.writeInt(flags&~63);
  while(headBytes.size()%16!=0)head.writeByte(0);head.write(directory);if((flags&0x200)!=0)while(headBytes.size()%16!=0)head.writeByte(0);
  byte[] newHeader=headBytes.toByteArray();long total=newHeader.length;for(Block b:result)total+=b.packed;ByteBuffer.wrap(newHeader).putLong(sizeAt,total);
  try(var backup=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(rollback)))){backup.writeInt(0x55425231);backup.writeInt(header.length);backup.write(header);backup.writeInt(original.size());for(var e:original.entrySet()){backup.writeInt(e.getKey());backup.writeInt(e.getValue().length);backup.write(e.getValue());}}
  try(var out=new BufferedOutputStream(Files.newOutputStream(output))){out.write(newHeader);for(int i=0;i<blocks.size();i++)out.write(changed.containsKey(i)?changed.get(i):packed(blocks.get(i)));}
 }
 public void restore(Path rollback,Path output)throws IOException{
  try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(rollback)))){
   if(in.readInt()!=0x55425231)throw fail("Invalid rollback format.");byte[] originalHeader=in.readNBytes(bound(in.readInt(),16*1024*1024));int count=bound(in.readInt(),blocks.size());Map<Integer,byte[]> originals=new HashMap<>();
   for(int i=0;i<count;i++){int index=bound(in.readInt(),blocks.size()-1),len=bound(in.readInt(),16*1024*1024);byte[] data=in.readNBytes(len);if(data.length!=len||originals.put(index,data)!=null)throw fail("Truncated/duplicate rollback block.");}
   if(in.read()!=-1)throw fail("Trailing rollback data.");
   try(var out=new BufferedOutputStream(Files.newOutputStream(output))){out.write(originalHeader);for(int i=0;i<blocks.size();i++)out.write(originals.containsKey(i)?originals.get(i):packed(blocks.get(i)));}
  }
 }
}
