// SPDX-License-Identifier: GPL-3.0-only
package moe.crx.overport.pfdmr;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded ARM64 ELF edits adapted from FramePort 1.4.0 analysis/elf.py (GPL-3.0-only).
 * Nothing in existing machine-code sections moves. */
public final class CommunityElf {
 final byte[] data; final ByteBuffer b; final int ph, pn, ps, sh, sn, ss;
 public CommunityElf(byte[] data) {
  this.data=data; b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
  if(data.length<64||b.getInt(0)!=0x464c457f||b.get(4)!=2||b.get(5)!=1||b.getShort(18)!=183)throw new IllegalStateException("Expected ARM64 little-endian ELF");
  ph=num(b.getLong(32));sh=num(b.getLong(40));ps=Short.toUnsignedInt(b.getShort(54));pn=Short.toUnsignedInt(b.getShort(56));ss=Short.toUnsignedInt(b.getShort(58));sn=Short.toUnsignedInt(b.getShort(60));
  if(ps!=56||ss!=64||pn==0||sn==0)throw new IllegalStateException("Unsupported ELF tables");range(ph,(long)pn*ps);range(sh,(long)sn*ss);
 }
 static int num(long n){if(n<0||n>Integer.MAX_VALUE)throw new IllegalStateException("ELF range overflow");return (int)n;}
 void range(long start,long size){if(start<0||size<0||start+size>data.length)throw new IllegalStateException("Truncated ELF");}
 int off(int s){return num(b.getLong(s+24));} int size(int s){return num(b.getLong(s+32));}
 int va(long v){for(int i=0;i<pn;i++){int p=ph+i*ps;if(b.getInt(p)==1){long a=b.getLong(p+16),n=b.getLong(p+32);if(v>=a&&v<a+n)return num(b.getLong(p+8)+v-a);}}throw new IllegalStateException("Unmapped ELF address");}
 String string(int start){range(start,1);int end=start;while(end<data.length&&data[end]!=0)end++;if(end==data.length)throw new IllegalStateException("Unterminated ELF string");return new String(data,start,end-start,StandardCharsets.UTF_8);}
 int section(int type){for(int i=0;i<sn;i++)if(b.getInt(sh+i*ss+4)==type)return sh+i*ss;throw new IllegalStateException("Missing ELF section "+type);}
 int named(String name){int ns=sh+Short.toUnsignedInt(b.getShort(62))*ss,base=off(ns);for(int i=0;i<sn;i++){int s=sh+i*ss;if(string(base+b.getInt(s)).equals(name))return s;}throw new IllegalStateException("Missing "+name);}
 public Set<String> symbols(boolean defined){int s=section(11),strings=off(sh+b.getInt(s+40)*ss);Set<String> out=new TreeSet<>();range(off(s),size(s));for(int p=off(s);p<off(s)+size(s);p+=24)if((b.getShort(p+6)!=0)==defined)out.add(string(strings+b.getInt(p)));return out;}
 public byte[] hide(Set<String> names){byte[] out=data.clone();int s=section(11),strings=off(sh+b.getInt(s+40)*ss);for(int p=off(s);p<off(s)+size(s);p+=24)if(b.getShort(p+6)!=0&&names.contains(string(strings+b.getInt(p))))out[p+4]&=15;return out;}
 public byte[] rodata(Map<String,String> edits){byte[] out=data.clone();int s=named(".rodata");range(off(s),size(s));for(var e:edits.entrySet()){byte[] old=(e.getKey()+"\0").getBytes(StandardCharsets.UTF_8),next=e.getValue().getBytes(StandardCharsets.UTF_8);if(next.length>=old.length)throw new IllegalStateException("Replacement too long");int at=-1;for(int p=off(s);p<=off(s)+size(s)-old.length;p++){boolean ok=true;for(int j=0;j<old.length;j++)if(data[p+j]!=old[j]){ok=false;break;}if(ok){if(at!=-1)throw new IllegalStateException("Ambiguous lookup "+e.getKey());at=p;}}if(at<0)throw new IllegalStateException("Missing lookup "+e.getKey());Arrays.fill(out,at,at+old.length,(byte)0);System.arraycopy(next,0,out,at,next.length);}return out;}
 public byte[] imports(Map<String,String> edits){byte[] out=data.clone();ByteBuffer w=ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);int s=section(11),base=off(sh+b.getInt(s+40)*ss),version=0;try{version=off(section(0x6fffffff));}catch(IllegalStateException ignored){}
  Set<String> found=new HashSet<>();for(int p=off(s),idx=0;p<off(s)+size(s);p+=24,idx++){String name=string(base+b.getInt(p));if(b.getShort(p+6)!=0||!edits.containsKey(name))continue;int at=base+b.getInt(p),end=at+name.length()+1;
   // Conservative string-sharing guard across all dynstr references, including versions.
   for(int q=off(s);q<off(s)+size(s);q+=24){int ref=base+b.getInt(q);if(q!=p&&ref>=at&&ref<end)throw new IllegalStateException("Shared import string");}
   for(int i=0;i<sn;i++){int v=sh+i*ss,t=b.getInt(v+4);if(t==6){for(int q=off(v);q<off(v)+size(v);q+=16){long tag=b.getLong(q);if(tag==1||tag==14||tag==15||tag==29){long ref=base+b.getLong(q+8);if(ref>=at&&ref<end)throw new IllegalStateException("Shared dynamic string");}}}else if(t==0x6ffffffe||t==0x6ffffffd){ // reject any possible reference in version records
    for(int q=off(v);q+4<=off(v)+size(v);q+=4){long ref=base+Integer.toUnsignedLong(b.getInt(q));if(ref>=at&&ref<end)throw new IllegalStateException("Shared version string");}
   }}
   byte[] next=edits.get(name).getBytes(StandardCharsets.UTF_8);if(next.length>name.length())throw new IllegalStateException("Long import name");Arrays.fill(out,at,end,(byte)0);System.arraycopy(next,0,out,at,next.length);if(version!=0)w.putShort(version+idx*2,(short)1);found.add(name);
  }if(!found.equals(edits.keySet()))throw new IllegalStateException("Missing expected imports: "+found);return out;
 }
 public List<String> needed(){int s=section(6);List<String> result=new ArrayList<>();int strings=0;for(int p=off(s);p<off(s)+size(s);p+=16)if(b.getLong(p)==5)strings=va(b.getLong(p+8));for(int p=off(s);p<off(s)+size(s);p+=16)if(b.getLong(p)==1)result.add(string(strings+num(b.getLong(p+8))));return result;}
 static long align(long v,long n){return (v+n-1)/n*n;}
 public byte[] needed(String library){if(needed().contains(library))return data.clone();List<long[]> table=new ArrayList<>();int dynamic=-1,note=-1;long end=0;for(int i=0;i<pn;i++){int p=ph+i*ps;long[] h={b.getInt(p),b.getInt(p+4),b.getLong(p+8),b.getLong(p+16),b.getLong(p+24),b.getLong(p+32),b.getLong(p+40),b.getLong(p+48)};table.add(h);if(h[0]==2)dynamic=i;if(h[0]==4)note=i;if(h[0]==1)end=Math.max(end,h[3]+h[6]);}if(dynamic<0)throw new IllegalStateException("No dynamic segment");
  long[] dp=table.get(dynamic);List<long[]> entries=new ArrayList<>();long strva=0,strsz=0;for(int p=num(dp[2]);p<dp[2]+dp[5];p+=16){long tag=b.getLong(p),value=b.getLong(p+8);if(tag==0)break;entries.add(new long[]{tag,value});if(tag==5)strva=value;if(tag==10)strsz=value;}
  int strings=va(strva);range(strings,strsz);byte[] name=(library+"\0").getBytes(StandardCharsets.UTF_8),str=Arrays.copyOfRange(data,strings,strings+num(strsz));str=Arrays.copyOf(str,str.length+name.length);System.arraycopy(name,0,str,num(strsz),name.length);
  int insert=0;while(insert<entries.size()&&entries.get(insert)[0]!=1)insert++;entries.add(insert,new long[]{1,strsz});entries.add(new long[]{0,0});entries.add(new long[]{0,0});
  boolean relocate=note<0,hasPhdr=table.stream().anyMatch(h->h[0]==6);int count=pn+(relocate?(hasPhdr?1:2):0),head=relocate?count*56:0,strOffset=num(align(head,16)),dynOffset=num(align(strOffset+str.length,16)),length=dynOffset+entries.size()*16,file=num(align(data.length,16384));long address=align(end,16384);
  byte[] out=Arrays.copyOf(data,file+length);ByteBuffer w=ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);System.arraycopy(str,0,out,file+strOffset,str.length);for(int i=0;i<entries.size();i++){long[] e=entries.get(i);if(e[0]==5)e[1]=address+strOffset;if(e[0]==10)e[1]=str.length;w.putLong(file+dynOffset+i*16,e[0]);w.putLong(file+dynOffset+i*16+8,e[1]);}
  table.set(dynamic,new long[]{2,dp[1],file+dynOffset,address+dynOffset,address+dynOffset,entries.size()*16,entries.size()*16,dp[7]});long[] load={1,6,file,address,address,length,length,16384};if(relocate){long[] hdr={6,4,file,address,address,head,head,8};if(hasPhdr){for(int i=0;i<table.size();i++)if(table.get(i)[0]==6)table.set(i,hdr);}else table.add(0,hdr);table.add(load);w.putLong(32,file);w.putShort(56,(short)count);}else table.set(note,load);
  List<long[]> loads=table.stream().filter(h->h[0]==1).sorted(Comparator.comparingLong(h->h[3])).toList();int j=0;for(int i=0;i<table.size();i++)if(table.get(i)[0]==1)table.set(i,loads.get(j++));int po=relocate?file:ph;for(int i=0;i<table.size();i++){long[] h=table.get(i);int p=po+i*56;w.putInt(p,(int)h[0]);w.putInt(p+4,(int)h[1]);for(int k=2;k<8;k++)w.putLong(p+8+(k-2)*8,h[k]);}
  for(int i=0;i<sn;i++){int s=sh+i*ss,t=b.getInt(s+4);if(t==6){w.putLong(s+16,address+dynOffset);w.putLong(s+24,file+dynOffset);w.putLong(s+32,entries.size()*16);}else if(b.getLong(s+16)==strva&&strva!=0){w.putLong(s+16,address+strOffset);w.putLong(s+24,file+strOffset);w.putLong(s+32,str.length);}}
  if(!new CommunityElf(out).needed().get(0).equals(library))throw new IllegalStateException("Dependency validation failed");return out;
 }
}
