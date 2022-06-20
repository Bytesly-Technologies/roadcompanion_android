package net.bytesly.roadcompanion;

import static net.bytesly.roadcompanion.util.MyUtils.SUPPORTED_ACTIVITY_KEY;
import static net.bytesly.roadcompanion.util.MyUtils.isServiceRunning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;

import net.bytesly.roadcompanion.adapter.ParkingCodesAdapter;
import net.bytesly.roadcompanion.detectedactivity.DetectedActivityReceiver;
import net.bytesly.roadcompanion.detectedactivity.DetectedActivityService;
import net.bytesly.roadcompanion.util.MyUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends LocalizedActivity implements PurchasesUpdatedListener {

    Button buttonStartStopTracking;
    Button buttonAddCode;

    ArrayList<String> codeList;

    LinearLayout linearLayoutParkingStatus;
    TextView textViewParkingStatus;

    RecyclerView recyclerViewParkingCodes;
    ParkingCodesAdapter recyclerViewAdapter;
    ConstraintLayout noCodesAddedLayout;

    private boolean isTrackingStarted = false;

    public void setTrackingStarted(boolean trackingStarted) {
        isTrackingStarted = trackingStarted;
        linearLayoutParkingStatus.setActivated(trackingStarted);
        textViewParkingStatus.setText(trackingStarted ? getString(R.string.parking_status_enabled_text) : getString(R.string.parking_status_disabled_text));
        buttonStartStopTracking.setText(trackingStarted ? R.string.stop_button : R.string.start_button);
    }

    private BillingClient billingClient;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(SUPPORTED_ACTIVITY_KEY)) {
            String action = intent.getStringExtra(SUPPORTED_ACTIVITY_KEY);
            if (action.equals("openParkingApp")) {
                stopService(new Intent(MainActivity.this, DetectedActivityService.class));
                setTrackingStarted(false);
                DetectedActivityService.cancelAlarmElapsed();
                DetectedActivityReceiver.stopAllAdditionalReminders();
                openParkingApp();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonStartStopTracking = findViewById(R.id.buttonOpenParking);
        buttonAddCode = findViewById(R.id.buttonAddParkingCode);

        codeList = new ArrayList<>(AppController.getInstance().getSavedParkingCodeList());

        linearLayoutParkingStatus = findViewById(R.id.linearLayoutParkingStatus);
        textViewParkingStatus = findViewById(R.id.textViewParkingStatus);

        recyclerViewParkingCodes = findViewById(R.id.recyclerViewParkingCodes);
        noCodesAddedLayout = findViewById(R.id.noCodesAddedLayout);

        recyclerViewParkingCodes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new ParkingCodesAdapter(this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentCode = codeList.get(recyclerViewParkingCodes.getChildAdapterPosition((View) v.getParent()));
                deleteParkingCode(currentCode);
            }
        });
        recyclerViewParkingCodes.setAdapter(recyclerViewAdapter);

        updateCodeRecycler(codeList);

        if (isServiceRunning(this)) {
            setTrackingStarted(true);
        }

        buttonAddCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addParkingCode();
            }
        });

        buttonStartStopTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTrackingStarted) {
                    stopService(new Intent(MainActivity.this, DetectedActivityService.class));
                    setTrackingStarted(false);
                    DetectedActivityService.cancelAlarmElapsed();
                    DetectedActivityReceiver.stopAllAdditionalReminders();
                    askOpenParking();
                } else {
                    checkSubscription();
                }
            }
        });

        //billing
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases().setListener(this).build();
        //end billing
    }

    private void checkSubscription() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                BillingResult subscriptionsSupportedResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
                BillingResult subscriptionUpdateSupportedResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE);

                if (subscriptionsSupportedResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, subscriptionsSupportedResult.getDebugMessage()
                                    + MyUtils.billingResponseCodeAsString(subscriptionsSupportedResult.getResponseCode()), Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                if (subscriptionUpdateSupportedResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, subscriptionUpdateSupportedResult.getDebugMessage()
                                    + MyUtils.billingResponseCodeAsString(subscriptionUpdateSupportedResult.getResponseCode()), Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
                        @Override
                        public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> queryPurchases) {
                            if (queryPurchases != null && queryPurchases.size() > 0) {
                                handlePurchases(queryPurchases);
                            }

                            //check which items are in purchase list and which are not in purchase list
                            //if items that are found add them to purchaseFound
                            //indexOf return index of item in purchase list from 0-2 (because we have 3 items) else returns -1 if not found
                            ArrayList<Integer> purchaseFound = new ArrayList<>();
                            if (queryPurchases != null && queryPurchases.size() > 0) {
                                //check item in purchase list
                                for (Purchase p : queryPurchases) {
                                    int index = MyUtils.subscribeItemIDs.indexOf(p.getSkus().get(0));
                                    //if purchase found
                                    if (index > -1 && p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                        purchaseFound.add(index);
                                    }
                                }
                            }

                            if (purchaseFound.size() > 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Toast.makeText(PaymentGatewayActivity.this, "purchase found, go to main",Toast.LENGTH_SHORT).show();
                                    }
                                });
                                turnOnParkingInOurApp();
                            } else {
                                showPaymentScreen();
                            }
                        }
                    });
                }
                else {
                    showPaymentScreen();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {

            }
        });
    }

    private void turnOnParkingInOurApp() {
        if (isTrackingPermissionGranted()) {
            startForegroundService(new Intent(MainActivity.this, DetectedActivityService.class));
            DetectedActivityService.scheduleRepeatingElapsedNotification(getApplicationContext());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setTrackingStarted(true);
                    askOpenParking();
                }
            });

        } else {
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    MyUtils.TRACKING_PERMISSION_CODE);
        }
    }

    private void handlePurchases(List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
            final int index = MyUtils.subscribeItemIDs.indexOf(purchase.getSkus().get(0));
            //purchase found
            if (index > -1) {
                //if item is purchased
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    if (!MyUtils.verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature(), MainActivity.this)) {
                        // Invalid purchase
                        // show error to user

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, getString(R.string.error_prefix) + getString(R.string.inavlid_purchase), Toast.LENGTH_SHORT).show();
                            }
                        });

                        showPaymentScreen();
                    }
                    // else purchase is valid
                    //if item is purchased/subscribed and not Acknowledged
                    else if (!purchase.isAcknowledged()) {
                        AcknowledgePurchaseParams acknowledgePurchaseParams =
                                AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.getPurchaseToken())
                                        .build();

                        billingClient.acknowledgePurchase(acknowledgePurchaseParams,
                                new AcknowledgePurchaseResponseListener() {
                                    @Override
                                    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                                        } else {
                                            showPaymentScreen();
                                        }
                                    }
                                });

                    }
                    //else item is purchased and also acknowledged
                    else {

                    }
                }
                //if purchase is pending
                else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    MyUtils.subscribeItemIDs.get(index) + getString(R.string.purchase_pending_toast), Toast.LENGTH_SHORT).show();
                        }
                    });

                    showPaymentScreen();
                }
                //if purchase is refunded or unknown
                else if (purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                    showPaymentScreen();
                }
            }
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        //if item newly purchased
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {

            for (Purchase purchase : purchases) {
                final int index = MyUtils.subscribeItemIDs.indexOf(purchase.getSkus().get(0));
                //purchase found
                if (index > -1) {
                    //if item is purchased
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        if (MyUtils.verifyValidSignature(purchase.getOriginalJson(),
                                purchase.getSignature(), MainActivity.this) &&
                                !purchase.isAcknowledged()) {
                            AcknowledgePurchaseParams acknowledgePurchaseParams =
                                    AcknowledgePurchaseParams.newBuilder()
                                            .setPurchaseToken(purchase.getPurchaseToken())
                                            .build();

                            billingClient.acknowledgePurchase(acknowledgePurchaseParams,
                                    new AcknowledgePurchaseResponseListener() {
                                        @Override
                                        public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {

                                        }
                                    });
                        }
                    }
                }
            }
        }

    }

    private void showPaymentScreen() {
        Intent intent = new Intent(MainActivity.this, PaymentGatewayActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        boolean curSoundStatus = AppController.getInstance().getNotificationSoundStatus();
        menu.getItem(0).setTitle(curSoundStatus ? getString(R.string.menuitem_sound_on) : getString(R.string.menuitem_sound_off));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_settings_item:
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.main_menu_managesubscriptions_item:
                Intent manageSubscriptionsIntent = new Intent(MainActivity.this, ManagePaymentActivity.class);
                startActivity(manageSubscriptionsIntent);
                return true;
            case R.id.main_menu_about_item:
                Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            case R.id.main_menu_soundtoggle_item:
                boolean curSoundStatus = AppController.getInstance().getNotificationSoundStatus();
                AppController.getInstance().setNotificationSoundStatus(!curSoundStatus);
                item.setTitle(!curSoundStatus ? getString(R.string.menuitem_sound_on) : getString(R.string.menuitem_sound_off));

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void openParkingApp() {

        PackageManager pm = getPackageManager();
        if (MyUtils.isPackageInstalled(MyUtils.TB_PARKING_PACKAGE_NAME, pm)) {
            Intent intent = pm.getLaunchIntentForPackage(MyUtils.TB_PARKING_PACKAGE_NAME);
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, R.string.couldnt_open_tbilisiparking, Toast.LENGTH_SHORT).show();
            }
        } else {
            goToTbilisiParkingInstallPage();
        }

    }

    private void goToTbilisiParkingInstallPage() {
        Toast.makeText(this, R.string.please_install_tbilisiparking_toast, Toast.LENGTH_SHORT).show();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + MyUtils.TB_PARKING_PACKAGE_NAME)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + MyUtils.TB_PARKING_PACKAGE_NAME)));
        }
    }

    private void askOpenParking() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setPositiveButton(R.string.yes_btn, (dialog, which) -> {
            dialog.dismiss();
            openParkingApp();
        });

        builder.setNegativeButton(R.string.no_btn, (dialog, which) -> {
            dialog.cancel();
        });

        String alertMessage = String.format(
                getString(R.string.ask_parking_message),
                isTrackingStarted ? getString(R.string.tracking_status_started) : getString(R.string.tracking_status_stopped));

        builder.setMessage(alertMessage);
        builder.create().show();
    }

    private void deleteParkingCode(String currentCode) {
        // Delete button is clicked, handle the deletion and finish the multi select process
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setPositiveButton(R.string.yes_btn, (dialog, which) -> {
            codeList.remove(currentCode);
            AppController.getInstance().setSavedParkingCodeList(new HashSet<>(codeList));
            Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            updateCodeRecycler(codeList);
        });

        builder.setNegativeButton(R.string.no_btn, (dialog, which) -> {
            dialog.cancel();
        });
        builder.setMessage(String.format(getString(R.string.are_you_sure_to_delete_code), currentCode));
        builder.create().show();
    }

    private void addParkingCode() {
        EditText newCodeEditText = new EditText(this);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(newCodeEditText)
                .setMessage(R.string.which_number_to_check_out)
                .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create();
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newNumberStr = newCodeEditText.getText().toString();
                if (newNumberStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.invalid_code_toast, Toast.LENGTH_SHORT).show();
                } else {
                    Set<String> codeSet = new HashSet<>(AppController.getInstance().getSavedParkingCodeList());
                    codeSet.add(newNumberStr);
                    codeList = new ArrayList<>(codeSet);
                    AppController.getInstance().setSavedParkingCodeList(codeSet);
                    updateCodeRecycler(codeList);
                    Toast.makeText(MainActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }
        });
    }

    private void updateCodeRecycler(List<String> codeList) {
        recyclerViewAdapter.setCodeList(codeList);

        if (codeList.size() == 0) {
            recyclerViewParkingCodes.setVisibility(View.GONE);
            noCodesAddedLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerViewParkingCodes.setVisibility(View.VISIBLE);
            noCodesAddedLayout.setVisibility(View.GONE);
        }
    }

    private boolean isTrackingPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true;
        }

        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MyUtils.TRACKING_PERMISSION_CODE &&
                grantResults.length > 0) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startForegroundService(new Intent(this, DetectedActivityService.class));
                DetectedActivityService.scheduleRepeatingElapsedNotification(getApplicationContext());
                setTrackingStarted(true);
                askOpenParking();
            } else {

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}