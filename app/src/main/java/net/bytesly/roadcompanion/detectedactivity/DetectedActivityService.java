package net.bytesly.roadcompanion.detectedactivity;

import static net.bytesly.roadcompanion.util.MyUtils.DETECTED_ACTIVITY_CHANNEL_ID;
import static net.bytesly.roadcompanion.util.MyUtils.DETECTED_ACTIVITY_NOTIFICATION_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import net.bytesly.roadcompanion.MainActivity;
import net.bytesly.roadcompanion.R;
import net.bytesly.roadcompanion.util.LocaleUtils;
import net.bytesly.roadcompanion.util.TransitionHelper;

import java.util.Locale;

public class DetectedActivityService extends Service {


    class LocalBinder extends Binder {
        DetectedActivityService serverInstance = (DetectedActivityService) DetectedActivityService.this;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        requestActivityTransitionUpdates();

        Resources localizedResources = LocaleUtils.getLocalizedResources(getApplicationContext(), Locale.forLanguageTag(LocaleUtils.getPrefLangCode(getApplicationContext())));


        createNotificationChannel(getApplicationContext());

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, DETECTED_ACTIVITY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(localizedResources.getString(R.string.app_name))
            .setContentText(localizedResources.getString(R.string.persistent_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();

        int notif_id = 123;

        startForeground(notif_id, notification);
    }

    private void createNotificationChannel(Context context) {
        Resources localizedResources = LocaleUtils.getLocalizedResources(context, Locale.forLanguageTag(LocaleUtils.getPrefLangCode(context)));
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = localizedResources.getString(R.string.reminder_notifchannel_title);
            String description = localizedResources.getString(R.string.reminder_notifchannel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(DETECTED_ACTIVITY_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setAllowBubbles(true);
            }
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void requestActivityTransitionUpdates() {
        ActivityTransitionRequest request = new ActivityTransitionRequest(TransitionHelper.getActivitiesToTrack());
        Task<Void> task = new ActivityRecognitionClient(this).requestActivityTransitionUpdates(request,
                DetectedActivityReceiver.getPendingIntent(this));

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d("ActivityUpdate", "activity update success");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("ActivityUpdate", "activity update fail");
            }
        });
    }

    private void removeActivityTransitionUpdates() {
        Task<Void> task = new ActivityRecognitionClient(this)
                .removeActivityTransitionUpdates(DetectedActivityReceiver.getPendingIntent(this));

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d("ActivityUpdate", "activity update removal success");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("ActivityUpdate", "activity update removal fail");
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeActivityTransitionUpdates();
        NotificationManagerCompat.from(this).cancel(DETECTED_ACTIVITY_NOTIFICATION_ID);
        Toast.makeText(getApplicationContext(), "DetectedActivityService destroyed", Toast.LENGTH_LONG).show();
    }
}
