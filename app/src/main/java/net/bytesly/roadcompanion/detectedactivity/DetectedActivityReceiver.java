package net.bytesly.roadcompanion.detectedactivity;

import static net.bytesly.roadcompanion.util.MyUtils.DETECTED_ACTIVITY_CHANNEL_ID;
import static net.bytesly.roadcompanion.util.MyUtils.DETECTED_PENDING_INTENT_REQUEST_CODE;
import static net.bytesly.roadcompanion.util.MyUtils.SUPPORTED_ACTIVITY_KEY;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import net.bytesly.roadcompanion.AppController;
import net.bytesly.roadcompanion.MainActivity;
import net.bytesly.roadcompanion.R;
import net.bytesly.roadcompanion.util.LocaleUtils;
import net.bytesly.roadcompanion.util.MyUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetectedActivityReceiver extends BroadcastReceiver {

    static int additionalReminderCount = 0;

    static Runnable additionalReminderRunnable;

    static Handler handler = new Handler();

    public static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, DetectedActivityReceiver.class);
        return PendingIntent.getBroadcast(context, DETECTED_PENDING_INTENT_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);

            if(result != null) {
                handleTransitionEvents(result.getTransitionEvents(), context);
            }
        }
    }

    private void handleTransitionEvents(List<ActivityTransitionEvent> transitionEvents, Context context) {
        List<ActivityTransitionEvent> filteredEvents = new ArrayList<>();

        for (ActivityTransitionEvent evt : transitionEvents) {
            if((evt.getActivityType() == DetectedActivity.IN_VEHICLE)
            && evt.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                filteredEvents.add(evt);
               }
        }

        if(filteredEvents.size() > 0) {
            showNotification(context);
            additionalReminderCount = AppController.getInstance().getParkingNotificationTimes() - 1;
            additionalReminderRunnable = new Runnable() {
                @Override
                public void run() {
                    if(MyUtils.isServiceRunning(context)) {
                        showNotification(context);
                        additionalReminderCount--;
                        if(additionalReminderCount > 0) {
                            handler.postDelayed(additionalReminderRunnable, MyUtils.ADDITIONAL_REMINDERS_INTERVAL_MILLIS);
                        }
                    }
                }
            };

            if(additionalReminderCount > 0) {
                handler.postDelayed(additionalReminderRunnable, MyUtils.ADDITIONAL_REMINDERS_INTERVAL_MILLIS);
            }
        }
    }


    private void showNotification(Context context) {
        createNotificationChannel(context);

        Resources localizedResources = LocaleUtils.getLocalizedResources(context, Locale.forLanguageTag(LocaleUtils.getPrefLangCode(context)));

        Intent intent = new Intent(context, MainActivity.class).putExtra(SUPPORTED_ACTIVITY_KEY, "openParkingApp");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DETECTED_ACTIVITY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(
                        localizedResources.getString(R.string.reminder_notification_title))
                .setContentText(localizedResources.getString(R.string.reminder_notification_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(false)
                .setContentIntent(pendingIntent)
                .setSound(null)
                .setSilent(false)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(MyUtils.DETECTED_ACTIVITY_NOTIFICATION_ID, builder.build());

        if(AppController.getInstance().getNotificationSoundStatus()) {
            Uri NOTIFICATION_SOUND = Uri.parse(
                    ContentResolver.SCHEME_ANDROID_RESOURCE
                            + File.pathSeparator + File.separator + File.separator
                            + context.getPackageName()
                            + File.separator
                            + R.raw.siren
            );

            RingtoneManager.getRingtone(context, NOTIFICATION_SOUND).play();
        }
    }

    public static void stopAllAdditionalReminders() {
        handler.removeCallbacks(additionalReminderRunnable);
    }

    private void createNotificationChannel(Context context) {
        Resources localizedResources = LocaleUtils.getLocalizedResources(context, Locale.forLanguageTag(LocaleUtils.getPrefLangCode(context)));

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = localizedResources.getString(R.string.reminder_notifchannel_title);
            String description = localizedResources.getString(R.string.reminder_notifchannel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(DETECTED_ACTIVITY_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setAllowBubbles(true);
            }
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setBypassDnd(true);
            channel.setSound(null,null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
