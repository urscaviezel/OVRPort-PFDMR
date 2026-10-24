package moe.crx.pfdmr;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** Redirects this game's Meta settings navigation to the consumer DreamOS widget. */
public final class SpatialSettings {
    private static long lastRequest = -10000;
    private static int generation;
    private static final String HOME = "com.yvr.spatialhome";
    private static boolean isSettingsPackage(String name) {
        return HOME.equals(name) || "com.oculus.panelapp.settings".equals(name)
            || "com.oculus.vrshell".equals(name);
    }

    public static Intent map(Activity activity, Intent original) {
        if (original == null) return null;
        String data = original.getDataString();
        ComponentName component = original.getComponent();
        boolean settings = (data != null && data.startsWith("systemux://settings"))
            || isSettingsPackage(original.getPackage())
            || (component != null && isSettingsPackage(component.getPackageName()))
            || ("android.settings.APPLICATION_DETAILS_SETTINGS".equals(original.getAction())
                && ("package:" + activity.getPackageName()).equals(data));
        if (!settings) return original;

        // Use the system Home entry so DreamOS retains its own virtual display.
        // Allow its resume/closing animations to settle before opening the widget.
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastRequest < 3000) return null;
        lastRequest = now;
        final int request = ++generation;
        final java.lang.ref.WeakReference<Activity> owner = new java.lang.ref.WeakReference<>(activity);
        final Context context = activity.getApplicationContext();
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                Activity current = owner.get();
                if (request != generation || current == null || current.isFinishing() || current.isDestroyed()) return;
                openSettings(context, "yvrsettings://settingstab/YVR.Settings.AppSettingsView");
            }
        }, 1200);
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                Activity current = owner.get();
                if (request != generation || current == null || current.isFinishing() || current.isDestroyed()) return;
                openSettings(context, "yvrsettings://settingsview/YVR.Settings.SpatialDataPermissionSettingsView");
            }
        }, 2000);
        if (openFocusAwareMenu(activity)) return null;
        Log.w("PfdmrSettings", "Focus-aware menu unavailable; falling back to regular Home");
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return home;
    }

    private static boolean openFocusAwareMenu(Activity activity) {
        try {
            // PFDM exposes this API specifically to third-party apps (whitelisted).
            // No activity launch: the running game's task and VR session remain in place.
            Object manager = activity.getSystemService("yvr_strategy");
            if (manager == null) return false;
            Class<?> propertyType = Class.forName("android.os.DeviceStrategyProperty");
            Object property = propertyType.getConstructor().newInstance();
            Object accepted = manager.getClass()
                .getMethod("updateStrategyToFAForThirdApps", propertyType)
                .invoke(manager, property);
            Log.i("PfdmrSettings", "PFDM focus-aware menu accepted=" + accepted);
            return Boolean.TRUE.equals(accepted);
        } catch (ReflectiveOperationException | RuntimeException error) {
            Log.w("PfdmrSettings", "PFDM focus-aware menu call failed", error);
            return false;
        }
    }

    private static void openSettings(Context context, String uri) {
        Intent broadcast = new Intent("com.yvr.spatialhome.action.OPEN_WIDGET");
        broadcast.setPackage(HOME);
        broadcast.putExtra("result", "{\"packageName\":\"com.yvr.settings\",\"uri\":\"" + uri + "\"}");
        try { context.sendBroadcast(broadcast); } catch (RuntimeException error) { Log.w("PfdmrSettings", "Settings navigation unavailable", error); }
        Log.i("PfdmrSettings", "Opening PFDM settings: " + uri);
    }
}
