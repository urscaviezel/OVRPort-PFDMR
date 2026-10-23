/** Read-only integration probe; runs as shell, never launches a game. */
public class RefreshJniProbe {
    private static native float request(Object context, String directory);
    public static void main(String[] args) throws Exception {
        android.os.Looper.prepareMainLooper();
        Class<?> activityThread=Class.forName("android.app.ActivityThread");
        Object thread=activityThread.getMethod("systemMain").invoke(null);
        android.content.Context context=(android.content.Context)activityThread.getMethod("getSystemContext").invoke(thread);
        int expected=android.provider.Settings.System.getInt(context.getContentResolver(),"SpatialHome_SystemFrequencyMode",0);
        System.load(args[0]+"/librefresh_jni_probe.so");
        float sent=request(context,args[0]);
        if(expected!=72&&expected!=80&&expected!=90)throw new AssertionError("Unrecognized HMD setting: "+expected);
        if(sent!=expected)throw new AssertionError("HMD setting "+expected+", wrapper sent "+sent);
        System.out.println("PASS real Android Context/Settings JNI: HMD="+expected+" Hz, forwarded="+sent+" Hz; no setting written");
    }
}
