package net.bytesly.roadcompanion;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.ads.MobileAds;

import net.bytesly.roadcompanion.util.LocaleUtils;
import net.bytesly.roadcompanion.util.MyUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AppController extends Application {

    private static AppController mInstance;

    private static AppOpenManager appOpenManager;

    public boolean shouldShowIntroActivity = true;

    public String currentLang = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        MobileAds.initialize(this);

        appOpenManager = new AppOpenManager(this);

        LocaleUtils.setPrefLangCode(this,"ka");
        LocaleUtils.setPrefCountryCode(this, MyUtils.getCountryCode("ka"));

        LocaleUtils.setLocale(new Locale(LocaleUtils.getPrefLangCode(this), LocaleUtils.getPrefCountryCode(this)));
        LocaleUtils.updateConfiguration(this, getResources().getConfiguration());

        //Write the settings into app preference file, maybe will remove this
        LocaleUtils.setPrefLangCode(this, LocaleUtils.getPrefLangCode(this));
        LocaleUtils.setPrefCountryCode(this, MyUtils.getCountryCode(LocaleUtils.getPrefLangCode(this)));

        LocaleUtils.setPrefLangCode(this,"ka");
        LocaleUtils.setPrefCountryCode(this, MyUtils.getCountryCode("ka"));

        setParkingNotificationTimes(getParkingNotificationTimes());
        setNotificationSoundStatus(getNotificationSoundStatus());
    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }


    public Integer getParkingNotificationTimes() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getInt("notif_times", 0);
    }
    public void setParkingNotificationTimes(Integer times) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt("notif_times", times);
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

    public Boolean getNotificationSoundStatus() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("notif_soundon", true);
    }
    public void setNotificationSoundStatus(Boolean status) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("notif_soundon", status);
        editor.apply();
    }
}
