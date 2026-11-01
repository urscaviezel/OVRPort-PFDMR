package moe.crx.overport.pfdmr;
import java.io.*;import java.nio.file.*;import java.util.*;import kotlin.Unit;import moe.crx.overport.patching.*;
/** Optional app-owned render-thread configuration, separate from the tested grip pose. */
public final class ClimbStabilityPatch {
 public static final String ID="patch_pfdmr_climb_stability";
 public static final String LABEL="PFDMR compatibility: The Climb single-threaded rendering (experimental)";
 static final String FILE="root/assets/pfdmr-climb-stability.txt",PREFIX="PFDMR_CLIMB_STABILITY_V1\n";
 public static final Patch PATCH=new Patch(ID,false,false,(e,a)->{e.selectWorkspace(f->{write(f.getFile().toPath(),true);return Unit.INSTANCE;});return Unit.INSTANCE;});
 public static Map<String,List<String>> expand(Map<String,List<String>> args){Map<String,List<String>> out=new LinkedHashMap<>(args);if(args.containsKey(ID))out.putIfAbsent(CommunityProfiles.CLIMB,List.of());if(args.containsKey(RedMatterSignerPatch.ID))out.putIfAbsent(MossResolutionPatch.ID,List.of());return out;}
 public static void prepare(Path ws,boolean climb){try{Path p=ws.resolve(FILE);if(Files.exists(p)){String text=Files.readString(p);if(!text.equals(PREFIX+"enabled=true\n")&&!text.equals(PREFIX+"enabled=false\n"))throw new IllegalStateException("Unknown Climb stability configuration");}if(climb)write(ws,false);else Files.deleteIfExists(p);}catch(IOException e){throw new IllegalStateException(e);}}
 static void write(Path ws,boolean enabled){try{Path p=ws.resolve(FILE);Files.createDirectories(p.getParent());Files.writeString(p,PREFIX+"enabled="+enabled+"\n");}catch(IOException e){throw new IllegalStateException(e);}}
}
