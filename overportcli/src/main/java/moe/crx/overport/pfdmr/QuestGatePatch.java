package moe.crx.overport.pfdmr;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import kotlin.Unit;
import moe.crx.overport.patching.Patch;

/** Fix two legacy Quest gates; never force the engine's global device identity.
 * Compatibility finding: frameport's unreal_santacruz.py (see THIRD-PARTY-NOTICES).
 * Original Java implementation; addresses are resolved from the input ELF.
 */
public final class QuestGatePatch {
    public static final String ID = "patch_pfdmr_unreal_quest_gates";
    public static final String LABEL = "PFDMR Unreal: Quest startup & controllers (experimental)";
    private static final String RECEIPT = "root/assets/pfdmr-quest-gates.properties";
    private static final String PRE = "_ZN8UVRUtils31GetQuestShaderPrecompilePercentEv";
    private static final String AXIS = "_ZN27URPOCKeyMapManagerComponent14AddAxisMappingERK15FRPOCKeyMappingR16FRPOCInputMapSet";
    private static final String ACTION = "_ZN27URPOCKeyMapManagerComponent16AddActionMappingERK15FRPOCKeyMappingR16FRPOCInputMapSet";
    private static final Set<String> GATES = Set.of("_ZN15FSantaCruzUtils20IsRunningOnSantaCruzEv", "_ZN23UNTNinjaFunctionLibrary20IsRunningOnSantaCruzEv");
    private static final int ZERO = 0x1e2703e0, ONE = 0x1e2e1000, NOP = 0xd503201f;
    public static final Patch PATCH = new Patch(ID, false, false, (executor, args) -> {
        executor.selectWorkspace(file -> { apply(file.getFile().toPath()); return Unit.INSTANCE; });
        executor.selectManifestJson(manifest -> {
            moe.crx.overport.patches.LauncherEntryPatchKt.ensureLegacyVrEntry(manifest);
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    });
    private static IllegalStateException fail(String why) {
        return new IllegalStateException("PFDMR Unreal Quest fix: " + why + " Use the original APK or disable this option.");
    }
    public record Change(int offset, int before, int after) {}
    private record Symbol(long address, int size) {}

    /** A bounded ELF64/ARM64 reader for defined dynamic functions and executable file ranges. */
    private static final class Elf {
        final byte[] data; final ByteBuffer b; final Map<String, Symbol> symbols = new HashMap<>();
        final List<long[]> segments = new ArrayList<>();
        Elf(byte[] data) {
            this.data=data; b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            range(0,64);
            if(b.getInt(0)!=0x464c457f || b.get(4)!=2 || b.get(5)!=1 || b.getShort(18)!=183 || b.getShort(16)!=3)
                throw fail("Only ARM64 ELF shared libraries are supported.");
            int ph=index(b.getLong(32)), ps=Short.toUnsignedInt(b.getShort(54)), pn=Short.toUnsignedInt(b.getShort(56));
            if(ps<56 || pn==0)throw fail("Missing ELF program headers.");
            range(ph,(long)ps*pn);
            for(int i=0;i<pn;i++) {
                int p=ph+i*ps;
                if(b.getInt(p)==1 && (b.getInt(p+4)&1)!=0) {
                    long off=b.getLong(p+8), va=b.getLong(p+16), size=b.getLong(p+32);
                    range(off,size); if(va<0 || size>Long.MAX_VALUE-va)throw fail("Invalid ELF address.");
                    segments.add(new long[]{va,size,off});
                }
            }
            int sh=index(b.getLong(40)), ss=Short.toUnsignedInt(b.getShort(58)), sn=Short.toUnsignedInt(b.getShort(60));
            if(ss<64 || sn==0)throw fail("Missing ELF symbols.");
            range(sh,(long)ss*sn);
            for(int i=0;i<sn;i++) {
                int s=sh+i*ss; if(b.getInt(s+4)!=11)continue;
                int link=b.getInt(s+40);if(link<0||link>=sn)throw fail("Invalid string table.");
                int st=sh+link*ss;
                int strings=index(b.getLong(st+24)), stringSize=index(b.getLong(st+32));range(strings,stringSize);
                int off=index(b.getLong(s+24)), len=index(b.getLong(s+32)), stride=index(b.getLong(s+56));
                if(stride<24||len%stride!=0)throw fail("Invalid symbol table.");range(off,len);
                for(int p=off;p<off+len;p+=stride) {
                    int info=Byte.toUnsignedInt(b.get(p+4));
                    if(b.getShort(p+6)==0 || (info&15)!=2 || ((info>>4)!=1&&(info>>4)!=2))continue;
                    int n=b.getInt(p);if(n<0||n>=stringSize)throw fail("Invalid symbol name.");
                    int end=strings+n;while(end<strings+stringSize&&data[end]!=0)end++;
                    if(end==strings+stringSize)throw fail("Unterminated symbol name.");
                    String name=new String(data,strings+n,end-strings-n,StandardCharsets.US_ASCII);
                    if(!name.equals(PRE)&&!name.equals(AXIS)&&!name.equals(ACTION)&&!GATES.contains(name))continue;
                    Symbol value=new Symbol(b.getLong(p+8),index(b.getLong(p+16)));
                    if(value.size<=0 || value.size>0x10000)throw fail("Invalid function size.");
                    at(value.address,value.size);
                    if(symbols.putIfAbsent(name,value)!=null)throw fail("Ambiguous function symbol.");
                }
            }
        }
        int index(long v){if(v<0||v>Integer.MAX_VALUE)throw fail("Invalid ELF range.");return (int)v;}
        void range(long off,long len){if(off<0||len<0||off>data.length||len>data.length-off)throw fail("Truncated ELF.");}
        int at(long va,int len) {
            int result=-1;
            for(long[] s:segments)if(va>=s[0] && va-s[0]<=s[1] && len<=s[1]-(va-s[0])) {
                if(result!=-1)throw fail("Ambiguous executable mapping.");result=index(s[2]+va-s[0]);
            }
            if(result<0)throw fail("Function outside executable file data.");return result;
        }
        int word(long va){if((va&3)!=0)throw fail("Unaligned instruction.");return b.getInt(at(va,4));}
        Symbol required(String name){Symbol s=symbols.get(name);if(s==null)throw fail("Unsupported Unreal functions.");return s;}
    }
    private static long call(long pc,int w) { return (w&0xfc000000)==0x94000000 ? pc+((long)(w<<6>>6)<<2) : -1; }
    private static long branch(long pc,int w) { return (w&0xfc000000)==0x14000000 ? pc+((long)(w<<6>>6)<<2) : -1; }
    private static long zeroBranch(long pc,int w) { return (w&0xfff8001f)==0x36000000 ? pc+((long)(w<<13>>18)<<2) : -1; }
    private static boolean field(int w) { return (w&0xffc003ff)==0x91000260; }
    private static Set<Long> calls(Elf e,Symbol s) {
        Set<Long> out=new HashSet<>();
        for(long pc=s.address;pc+4<=s.address+s.size;pc+=4){long t=call(pc,e.word(pc));if(t>=0)out.add(t);}
        return out;
    }
    /** Return exactly two fully validated instruction edits, or reject the whole patch. */
    public static List<Change> inspect(byte[] data) {
        try {
            Elf e=new Elf(data); Set<Long> gates=new HashSet<>();
            for(String name:GATES)if(e.symbols.containsKey(name))gates.add(e.symbols.get(name).address);
            if(gates.isEmpty())throw fail("Quest detection function not found.");
            Symbol pre=e.required(PRE); List<Change> shaders=new ArrayList<>();
            for(long pc=pre.address;pc+8<=pre.address+pre.size;pc+=4) {
                if(!gates.contains(call(pc,e.word(pc))))continue;
                long target=zeroBranch(pc+4,e.word(pc+4));
                if(target<pre.address||target+12>pre.address+pre.size)continue;
                int w=e.word(target);
                if((w==ZERO||w==ONE)&&e.word(target+4)==0xa8c17bfd&&e.word(target+8)==0xd65f03c0)
                    shaders.add(new Change(e.at(target,4),w,ONE));
            }
            Set<Long> shared=calls(e,e.required(AXIS));shared.retainAll(calls(e,e.required(ACTION)));
            Map<Integer,Change> keys=new LinkedHashMap<>();
            for(long start:shared) {
                // The shared key selector is a local function without a dynamic symbol.
                // Scan only executable words and require its specific Quest/Gear-VR branch shape.
                for(int i=0;i<0x400-16;i+=4) {
                    long pc=start+i;
                    try { e.at(pc,16); } catch(IllegalStateException ex) { break; }
                    if(!gates.contains(call(pc,e.word(pc))))continue;
                    int w=e.word(pc+4), quest=e.word(pc+8), back=e.word(pc+12);
                    long epilogue=branch(pc+12,back);
                    if(!field(quest)||epilogue<start||epilogue>=start+0x400)continue;
                    // A NOP is accepted only with the matching still-present Gear-VR alternative.
                    long gear=zeroBranch(pc+4,w);
                    if(w==NOP) {
                        for(long t=pc+16;t<Math.min(pc+80,start+0x400-8);t+=4)
                            if(field(e.word(t))&&e.word(t)!=quest&&branch(t+4,e.word(t+4))==epilogue){gear=t;break;}
                    }
                    if(gear<start||gear+8>start+0x400)continue;
                    if(field(e.word(gear))&&e.word(gear)!=quest&&branch(gear+4,e.word(gear+4))==epilogue)
                        keys.put(e.at(pc+4,4),new Change(e.at(pc+4,4),w,NOP));
                }
            }
            if(shaders.size()!=1||keys.size()!=1)throw fail("Unrecognized or ambiguous Quest startup/controller code; no changes made.");
            Change shader=shaders.get(0), key=keys.values().iterator().next();
            if(shader.offset==key.offset)throw fail("Overlapping patch sites.");
            return List.of(shader,key);
        } catch(IndexOutOfBoundsException|ArithmeticException ex) {throw fail("Malformed ELF.");}
    }
    private static Path library(Path workspace,String name) {
        if(!Set.of("libUE4.so","libUnreal.so").contains(name))throw fail("Invalid patch receipt.");
        return workspace.resolve("root/lib/arm64-v8a").resolve(name);
    }
    public static void restore(Path workspace) {
        Path receipt=workspace.resolve(RECEIPT);if(!Files.exists(receipt))return;
        try {
            Properties p=new Properties();try(var in=Files.newInputStream(receipt)){p.load(in);}
            if(!"1".equals(p.getProperty("version")))throw fail("Unknown patch receipt.");
            Path lib=library(workspace,p.getProperty("library",""));byte[] data=Files.readAllBytes(lib);
            if(!StereoPatch.sha256(data).equals(p.getProperty("after")))throw fail("Patched Unreal library changed since patching.");
            ByteBuffer b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            for(int i=0;i<2;i++) {
                int off=Integer.parseInt(p.getProperty("offset"+i));
                if(off<0||off>data.length-4||(off&3)!=0)throw fail("Invalid restore offset.");
                b.putInt(off,Integer.parseUnsignedInt(p.getProperty("word"+i),16));
            }
            if(!StereoPatch.sha256(data).equals(p.getProperty("before")))throw fail("Restore checksum mismatch.");
            Files.write(lib,data);Files.delete(receipt);
        } catch(IOException|IllegalArgumentException ex){throw fail("Cannot restore Quest fix: "+ex.getMessage());}
    }
    public static void apply(Path workspace) {
        try {
            restore(workspace);
            Path libs=workspace.resolve("root/lib");List<Path> targets=new ArrayList<>();
            if(Files.isDirectory(libs))try(var files=Files.walk(libs,2)) {
                files.filter(Files::isRegularFile).filter(p->Set.of("libUE4.so","libUnreal.so").contains(p.getFileName().toString())).forEach(targets::add);
            }
            if(targets.size()!=1||!targets.get(0).getParent().equals(libs.resolve("arm64-v8a")))throw fail("Expected one ARM64 Unreal library.");
            Path lib=targets.get(0);byte[] data=Files.readAllBytes(lib);List<Change> edits=inspect(data);
            Properties p=new Properties();p.setProperty("version","1");p.setProperty("library",lib.getFileName().toString());
            p.setProperty("before",StereoPatch.sha256(data));ByteBuffer b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            for(int i=0;i<edits.size();i++){Change c=edits.get(i);p.setProperty("offset"+i,""+c.offset);p.setProperty("word"+i,Integer.toUnsignedString(c.before,16));b.putInt(c.offset,c.after);}
            p.setProperty("after",StereoPatch.sha256(data));
            Path receipt=workspace.resolve(RECEIPT);Files.createDirectories(receipt.getParent());
            try(var out=Files.newOutputStream(receipt)){p.store(out,"PFDMR Quest gates: reversible instruction edits");}
            Files.write(lib,data);
        } catch(IOException ex){throw fail(ex.toString());}
    }
}
