package net.bytesly.roadcompanion;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import net.bytesly.roadcompanion.util.MyUtils;

public class MyAppIntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_app_intro);

        if(!AppController.getInstance().shouldShowIntroActivity) {
            startMainActivity();
        }
        else {
            AppController.getInstance().shouldShowIntroActivity = false;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startMainActivity();
                }
            }, 2000);
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(MyAppIntroActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}