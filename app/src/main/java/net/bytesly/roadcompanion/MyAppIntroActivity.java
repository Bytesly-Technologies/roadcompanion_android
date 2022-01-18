package net.bytesly.roadcompanion;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MyAppIntroActivity extends LocalizedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_app_intro);

        if(!AppController.getInstance().shouldShowIntroActivity) {
            startPaymentCheckActivity();
        }
        else {
            AppController.getInstance().shouldShowIntroActivity = false;

            new Handler().postDelayed(this::startPaymentCheckActivity, 2000);
        }
    }

    private void startPaymentCheckActivity() {
        Intent intent = new Intent(MyAppIntroActivity.this, PaymentGatewayActivity.class);
        startActivity(intent);
        finish();
    }
}