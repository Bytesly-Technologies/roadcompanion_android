package net.bytesly.roadcompanion;

import androidx.appcompat.app.ActionBar;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends LocalizedActivity {

    WebView webViewAboutApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        webViewAboutApp = findViewById(R.id.webViewAboutApp);

        webViewAboutApp.getSettings().setAllowUniversalAccessFromFileURLs(true);

        webViewAboutApp.loadUrl("file:///android_asset/about/about.html");

        webViewAboutApp.setBackgroundColor(Color.TRANSPARENT);
    }
}