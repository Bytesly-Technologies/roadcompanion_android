package net.bytesly.roadcompanion;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.bytesly.roadcompanion.util.LocaleUtils;
import net.bytesly.roadcompanion.util.MyUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AppController extends Application {

    private static AppController mInstance;

    public boolean shouldShowIntroActivity = true;

    public String currentLang = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        LocaleUtils.setPrefLangCode(this,"ka");
        LocaleUtils.setPrefCountryCode(this, MyUtils.getCountryCode("ka"));

        LocaleUtils.setLocale(new Locale(LocaleUtils.getPrefLangCode(this), LocaleUtils.getPrefCountryCode(this)));
        LocaleUtils.updateConfiguration(this, getResources().getConfiguration());

        //Write the settings into app preference file, maybe will remove this
        LocaleUtils.setPrefLangCode(this, LocaleUtils.getPrefLangCode(this));
        LocaleUtils.setPrefCountryCode(this, MyUtils.getCountryCode(LocaleUtils.getPrefLangCode(this)));

        LocaleUtils.setPrefLangCode(this,"ka");
        LocaleUtils.setPrefCountryCode(this, MyUtils.getCountryCode("ka"));
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
