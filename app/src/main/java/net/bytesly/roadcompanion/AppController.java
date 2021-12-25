package net.bytesly.roadcompanion;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppController extends Application {

    private static AppController mInstance;

    boolean googlePlayServicesAvailable = true;

    public boolean shouldShowIntroActivity = true;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }


    public Integer getParkingNotificationInterval() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getInt("parking_notif_interval", 0);
    }
    public void setParkingNotificationInterval(Integer interval) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt("parking_notif_interval", interval);
        editor.apply();
    }

    public Set<String> getSavedParkingCodeList() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getStringSet("parking_codes", new HashSet<>());
    }
    public void setSavedParkingCodeList(Set<String> codeSet) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putStringSet("parking_codes", codeSet);
        editor.commit();
    }
}
