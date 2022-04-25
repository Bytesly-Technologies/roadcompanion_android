package net.bytesly.roadcompanion.util;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;

import net.bytesly.roadcompanion.R;

public class MyUtils {
    public static final String KEY_IS_TRACKING_STARTED = "is_tracking_started";

    public static final String SUPPORTED_ACTIVITY_KEY = "supported_activity_key";

    public static final int TRACKING_PERMISSION_CODE = 10;

    public static final String TRANSITIONS_RECEIVER_ACTION = "roadcompanion_transitions_receiver_action";

    public static final int TRANSITION_PENDING_INTENT_REQUEST_CODE = 200;
    public static final int DETECTED_PENDING_INTENT_REQUEST_CODE = 100;
    public static final int REPEATINGALARM_PENDING_INTENT_REQUEST_CODE = 101;

    public static final int RELIABLE_CONFIDENCE = 40;

    public static final String DETECTED_ACTIVITY_CHANNEL_ID = "roadcompanion_detected_activity_channel_id";

    public static final int DETECTED_ACTIVITY_NOTIFICATION_ID = 10;
    public static final int REPEATING_NOTIFICATION_ID = 11;

    public static final String TB_PARKING_PACKAGE_NAME = "ge.msda.parking";

    public static final String PLAY_STORE_SUBSCRIPTION_URL = "https://play.google.com/store/account/subscriptions";
    public static final String PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";
    public static final long ADDITIONAL_REMINDERS_INTERVAL_MILLIS = 60 * 2 * 1000;


    public static String getSkuNameWithoutAppName(Context ctx, String originalName) {
        return originalName.replace(" (" + ctx.getString(R.string.app_name) + ")", "");
    }

    public static String getCountryCode(String langCode) {
        switch (langCode) {
            case "en":
                return "US";
            case "ka":
                return "GE";
            default:
                return "US";
        }
    }

    public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isServiceRunning(Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("net.bytesly.roadcompanion.detectedactivity.DetectedActivityService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
