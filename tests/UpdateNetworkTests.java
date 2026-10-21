import moe.crx.overport.app.model.AppUpdates;
import moe.crx.overport.versions.VersionManager;
public class UpdateNetworkTests {
 public static void main(String[] args)throws Exception {
  var update=AppUpdates.INSTANCE.check(VersionManager.EDITION_VERSION);
  if(update!=null && AppUpdates.INSTANCE.compareVersions(update.getTagName(),VersionManager.EDITION_VERSION)<=0)
   throw new AssertionError("Live API offered a downgrade");
  System.out.println("PASS live public GitHub API: "+(update==null?"no newer release":update.getTagName()));
 }
}
