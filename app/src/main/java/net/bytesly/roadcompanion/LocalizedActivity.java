package net.bytesly.roadcompanion;

import static android.content.pm.PackageManager.GET_META_DATA;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.bytesly.roadcompanion.util.LocaleUtils;

public abstract class LocalizedActivity extends AppCompatActivity {

    public LocalizedActivity() {
        LocaleUtils.updateConfiguration(this);
    }

    // We only override onCreate
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppController.getInstance().currentLang = LocaleUtils.getPrefLangCode(this);
        resetTitle();
    }

    private void resetTitle() {
        try {
            int label = getPackageManager().getActivityInfo(getComponentName(), GET_META_DATA).labelRes;
            if (label != 0) {
                setTitle(label);
            }
        } catch (PackageManager.NameNotFoundException e) {

        }
    }
}
