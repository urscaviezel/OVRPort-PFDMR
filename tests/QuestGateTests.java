import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import moe.crx.overport.pfdmr.*;
import moe.crx.overport.patching.*;

/** Local fixture is supplied by the tester; no game files are committed or distributed. */
public class QuestGateTests {
    static int passed;
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);System.out.println("PASS "+name);passed++;}
    static void reject(Runnable fn,String name){try{fn.run();}catch(IllegalStateException expected){check(expected.getMessage().contains("PFDMR Unreal Quest fix"),name);return;}throw new AssertionError(name);}
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);Files.createDirectories(root);
        byte[] original;
        try(var z=new ZipFile(args[1])){original=z.getInputStream(z.getEntry("lib/arm64-v8a/libUE4.so")).readAllBytes();}
        var patch=PatchStore.INSTANCE.get(QuestGatePatch.ID);
        check(patch!=null&&!patch.isRecommended()&&!patch.getRequiresOverportConfig(),"registered, off by default, no runtime download needed");
        check(PatchStore.INSTANCE.recommended().stream().noneMatch(p->p.getName().equals(QuestGatePatch.ID)),"default patch list unchanged");
        var composer=(androidx.compose.runtime.Composer)java.lang.reflect.Proxy.newProxyInstance(QuestGateTests.class.getClassLoader(),new Class<?>[]{androidx.compose.runtime.Composer.class},(proxy,method,values)->method.getReturnType()==boolean.class?false:method.getReturnType()==int.class?0:null);
        check(moe.crx.overport.app.util.PatchLocalizer.INSTANCE.localizedPatch(patch,composer,0).equals(QuestGatePatch.LABEL),"GUI switch label");
        var edits=QuestGatePatch.inspect(original);
        check(edits.size()==2&&edits.get(0).offset()==0x2d10d3c&&edits.get(1).offset()==0x2c390e0,"symbol-based detection matches verified sites");
        Path ws=root.resolve("direct"),lib=ws.resolve("root/lib/arm64-v8a/libUE4.so");Files.createDirectories(lib.getParent());Files.write(lib,original);
        QuestGatePatch.apply(ws);byte[] fixed=Files.readAllBytes(lib);
        check(StereoPatch.sha256(fixed).equals("8567667276214997f702b8dbbc610ea0c429fdee4cee50fd431d6d8f0efe3eac"),"byte-identical to headset-confirmed Test 4 engine");
        QuestGatePatch.apply(ws);check(Arrays.equals(fixed,Files.readAllBytes(lib)),"repatch is idempotent");
        QuestGatePatch.restore(ws);check(Arrays.equals(original,Files.readAllBytes(lib)),"exact reversible restoration");
        check(!Files.exists(ws.resolve("root/assets/pfdmr-quest-gates.properties")),"restore removes receipt");
        QuestGatePatch.restore(ws);check(Arrays.equals(original,Files.readAllBytes(lib)),"no receipt leaves original untouched");
        Files.write(lib,fixed);QuestGatePatch.apply(ws);QuestGatePatch.restore(ws);
        check(Arrays.equals(fixed,Files.readAllBytes(lib)),"preexisting Test 4 changes are preserved on deselection");
        byte[] broken=original.clone();broken[edits.get(1).offset()]=0x12;
        Files.write(lib,broken);reject(()->QuestGatePatch.apply(ws),"unknown controller code rejected");
        check(Arrays.equals(broken,Files.readAllBytes(lib))&&!Files.exists(ws.resolve("root/assets/pfdmr-quest-gates.properties")),"no partial shader change on rejection");
        byte[] wrongArch=original.clone();wrongArch[18]=62;
        reject(()->QuestGatePatch.inspect(wrongArch),"wrong architecture rejected");
        for(int length:new int[]{0,63,4096})reject(()->QuestGatePatch.inspect(Arrays.copyOf(original,length)),"truncated ELF rejected: "+length);
        byte[] badSymbols=original.clone();ByteBuffer.wrap(badSymbols).order(ByteOrder.LITTLE_ENDIAN).putLong(40,Long.MAX_VALUE);
        reject(()->QuestGatePatch.inspect(badSymbols),"invalid symbol table range rejected");
        Files.write(lib,original);Path other=ws.resolve("root/lib/armeabi-v7a/libUE4.so");Files.createDirectories(other.getParent());Files.write(other,new byte[0]);
        reject(()->QuestGatePatch.apply(ws),"multiple participating ABIs rejected");Files.delete(other);
        QuestGatePatch.apply(ws);byte[] changed=Files.readAllBytes(lib);changed[100]^=1;Files.write(lib,changed);
        reject(()->QuestGatePatch.restore(ws),"changed library blocks rollback");check(Arrays.equals(changed,Files.readAllBytes(lib)),"failed rollback preserves library");
        var ctx=new PatcherContext("3.4.3-aa54c3f",root.resolve("cache").toFile(),"OwnedTest.apk","{filename}.QuestFix.{ext}");
        try(var in=Files.newInputStream(Path.of(args[1]))){ctx.prepare(in);}
        ctx.patch(Map.of(QuestGatePatch.ID,List.of()),(i,p)->kotlin.Unit.INSTANCE);
        Path output=root.resolve("quest-enabled.apk");try(var out=Files.newOutputStream(output)){ctx.export(out);}
        check(new com.android.apksig.ApkVerifier.Builder(output.toFile()).build().verify().isVerified(),"enabled APK signature verifies");
        try(var z=new ZipFile(output.toFile())) {
            check(Arrays.equals(fixed,z.getInputStream(z.getEntry("lib/arm64-v8a/libUE4.so")).readAllBytes()),"full pipeline exports verified engine");
            check(z.getEntry("assets/pfdmr-quest-gates.properties")!=null,"APK contains rollback receipt");
        }
        ctx.cleanup();try(var in=Files.newInputStream(output)){ctx.prepare(in);}
        ctx.patch(Map.of(),(i,p)->kotlin.Unit.INSTANCE);
        Path off=root.resolve("quest-disabled.apk");try(var out=Files.newOutputStream(off)){ctx.export(out);}
        check(new com.android.apksig.ApkVerifier.Builder(off.toFile()).build().verify().isVerified(),"disabled APK signature verifies");
        try(var z=new ZipFile(off.toFile())) {
            check(Arrays.equals(original,z.getInputStream(z.getEntry("lib/arm64-v8a/libUE4.so")).readAllBytes()),"deselecting in full pipeline restores original engine");
            check(z.getEntry("assets/pfdmr-quest-gates.properties")==null,"disabled APK has no receipt");
        }
        ctx.cleanup();System.out.println("ALL "+passed+" QUEST GATE CHECKS PASSED");
    }
}
