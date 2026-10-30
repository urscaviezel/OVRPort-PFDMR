import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import kotlin.*;
import kotlin.coroutines.*;
import moe.crx.overport.app.model.MainViewModel;
import moe.crx.overport.patching.PatcherContext;
import androidx.compose.runtime.MutableState;

/** Pause real cleanup after archive closure, as an outgoing animation reads UI data. */
public class CancelLifecycleTests {
 static int passed;
 static void check(boolean b,String label){if(!b)throw new AssertionError(label);System.out.println("PASS "+label);passed++;}
 static class PausedDirectory extends File {
  final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  final boolean fail;
  PausedDirectory(File f,boolean fail){super(f.getAbsolutePath());this.fail=fail;}
  @Override public File[] listFiles(){
   entered.countDown();try{if(!release.await(15,TimeUnit.SECONDS))throw new AssertionError("cleanup not released");}catch(InterruptedException e){throw new RuntimeException(e);}
   if(fail)throw new IllegalStateException("Simulated cleanup failure");return super.listFiles();
  }
 }
 @SuppressWarnings("unchecked") static void attach(MainViewModel vm,PatcherContext ctx)throws Exception{
  var f=MainViewModel.class.getDeclaredField("patcherState$delegate");f.setAccessible(true);((MutableState<PatcherContext>)f.get(vm)).setValue(ctx);
 }
 static CompletableFuture<Unit> cancel(MainViewModel vm){
  var done=new CompletableFuture<Unit>();Continuation<Unit> continuation=new Continuation<>(){
   public CoroutineContext getContext(){return EmptyCoroutineContext.INSTANCE;}
   public void resumeWith(Object value){try{ResultKt.throwOnFailure(value);done.complete(Unit.INSTANCE);}catch(Throwable ex){done.completeExceptionally(ex);}}
  };
  try{Object result=vm.cancel(continuation);if(result!=kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED())done.complete(Unit.INSTANCE);}catch(Throwable ex){done.completeExceptionally(ex);}return done;
 }
 static PatcherContext load(Path run,String filename,Path apk)throws Exception{
  var c=new PatcherContext("test",run.toFile(),filename,"{filename}.{ext}");try(var in=Files.newInputStream(apk)){c.prepare(in);}return c;
 }
 static void empty(MainViewModel vm){
  check(!vm.isApkLoaded(),"UI no longer has loaded APK");
  check(vm.currentAppName()==null&&vm.currentAppPackage()==null&&vm.currentAppVersion()==null&&vm.currentAppIcon()==null,"outgoing UI reads are safe after archive closure");
  check(vm.patchedName().equals("output.apk"),"filename read no longer touches disposed context");
 }
 public static void main(String[] args)throws Exception{
  Path run=Path.of(args[0]);Files.createDirectories(run);var vm=new MainViewModel(run.toFile(),(bytes,w,h)->bytes);
  for(boolean failure:new boolean[]{false,true}){
   var ctx=load(run,"First.apk",Path.of(args[1]));attach(vm,ctx);check(vm.isApkLoaded()&&vm.currentAppPackage()!=null,"APK loaded before cancel");
   var original=ctx.getWorkingDir();var blocked=new PausedDirectory(original,failure);var f=PatcherContext.class.getDeclaredField("workingDir");f.setAccessible(true);f.set(ctx,blocked);
   var done=cancel(vm);
   try{
    check(blocked.entered.await(10,TimeUnit.SECONDS),"cleanup paused after archive close");
    check(!ctx.isApkLoaded(),"underlying archive closed");empty(vm);cancel(vm).get(5,TimeUnit.SECONDS);check(vm.getWorking(),"duplicate Cancel does not enable new load during cleanup");
   }finally{blocked.release.countDown();}
   if(failure){try{done.get(15,TimeUnit.SECONDS);throw new AssertionError("missing cleanup error");}catch(ExecutionException ex){check(ex.getCause().getMessage().equals("Simulated cleanup failure"),"cleanup failure propagated for UI error dialog");}}
   else done.get(15,TimeUnit.SECONDS);
   empty(vm);check(!vm.getWorking()&&vm.getCurrentProgress()==null&&vm.getCurrentProgressFloat()==null,"idle state restored");
   f.set(ctx,original);ctx.cleanup();
   var next=load(run,"Other.apk",Path.of(args[2]));attach(vm,next);check(vm.isApkLoaded()&&vm.currentAppPackage().equals(next.appPackage()),"another APK can be loaded after cancel");
   cancel(vm).get(15,TimeUnit.SECONDS);cancel(vm).get(15,TimeUnit.SECONDS);check(!vm.isApkLoaded()&&!vm.getWorking(),"repeated Cancel on empty state is safe");
  }
  System.out.println("ALL "+passed+" CANCEL LIFECYCLE CHECKS PASSED");
 }
}
