package net.bytesly.roadcompanion.util;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;

import net.bytesly.roadcompanion.R;

import java.io.IOException;
import java.util.ArrayList;

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

    public static final ArrayList<String> subscribeItemIDs = new ArrayList<String>() {{
        add("useapp_3m");
        add("useapp_6m");
        add("useapp_1y");
    }};

    public static String billingResponseCodeAsString(int code) {
        switch(code) {
            case -3: return "SERVICE_TIMEOUT";
            case -2: return "FEATURE_NOT_SUPPORTED";
            case -1: return "SERVICE_DISCONNECTED";
            case 0: return "OK";
            case 1: return "USER_CANCELED";
            case 2: return "SERVICE_UNAVAILABLE";
            case 3: return "BILLING_UNAVAILABLE";
            case 4: return "ITEM_UNAVAILABLE";
            case 5: return "DEVELOPER_ERROR";
            case 6: return "ERROR";
            case 7: return "ITEM_ALREADY_OWNED";
            case 8: return "ITEM_NOT_OWNED";
        }
        return "UNKNOWN";
    }

    public static boolean verifyValidSignature(String signedData, String signature, Context ctx) {
        try {
            // To get key go to Developer Console > Select your app > Development Tools > Services & APIs.
            String base64Key = ctx.getResources().getString(R.string.google_billing_licensekey);

            return Security.verifyPurchase(base64Key, signedData, signature);
        } catch (IOException e) {
            return false;
        }
    }

}
