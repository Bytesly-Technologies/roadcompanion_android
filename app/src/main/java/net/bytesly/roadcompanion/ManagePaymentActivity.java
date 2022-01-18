package net.bytesly.roadcompanion;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import net.bytesly.roadcompanion.util.MyUtils;
import net.bytesly.roadcompanion.util.Security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ManagePaymentActivity extends LocalizedActivity implements PurchasesUpdatedListener {

    Button buttonSubscribe3M;
    Button buttonSubscribe6M;
    Button buttonSubscribe1Y;

    LinearLayout linearLayoutCheckingPayment;
    LinearLayout linearLayoutCurrentStatus;
    LinearLayout linearLayoutErrorOccurred;
    TextView textViewErrorText;
    Button buttonTryAgain;

    TextView textViewCurrentPlanName;

    Button buttonGoToGooglePlay;

    private BillingClient billingClient;
    String foundPurchaseSkuId;
    String currentPlanName;
    String currentPurchaseToken;

    private static ArrayList<String> subscribeItemIDs = new ArrayList<String>() {{
        add("useapp_3m");
        add("useapp_6m");
        add("useapp_1y");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_payment);

        buttonSubscribe3M = findViewById(R.id.buttonSubscribe3M);
        buttonSubscribe6M = findViewById(R.id.buttonSubscribe6M);
        buttonSubscribe1Y = findViewById(R.id.buttonSubscribe1Y);

        linearLayoutCheckingPayment = findViewById(R.id.linearLayoutCheckingPayment);
        linearLayoutCurrentStatus = findViewById(R.id.linearLayoutCurrentStatus);
        linearLayoutErrorOccurred = findViewById(R.id.linearLayoutErrorOccurred);
        textViewErrorText = findViewById(R.id.textViewErrorText);
        buttonTryAgain = findViewById(R.id.buttonTryAgain);

        textViewCurrentPlanName = findViewById(R.id.textViewCurrentPlanName);
        buttonGoToGooglePlay = findViewById(R.id.buttonGoToGooglePlay);

        // Establish connection to billing client
        //check purchase status from google play store cache on every app start
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases().setListener(this).build();

        billingClient.startConnection(new BillingClientStateListener() {

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                BillingResult subscriptionsSupportedResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
                BillingResult subscriptionUpdateSupportedResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE);

                if(subscriptionsSupportedResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    showErrorLayout(subscriptionsSupportedResult.getDebugMessage() + billingResponseCodeAsString(billingResult.getResponseCode()));
                    return;
                }
                if(subscriptionUpdateSupportedResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    showErrorLayout(subscriptionUpdateSupportedResult.getDebugMessage() + billingResponseCodeAsString(billingResult.getResponseCode()));
                    return;
                }

                if(billingResult.getResponseCode()==BillingClient.BillingResponseCode.OK){
                    billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
                        @Override
                        public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> queryPurchases) {

                            if(queryPurchases!=null && queryPurchases.size()>0){
                                //check item in purchase list
                                for(Purchase p:queryPurchases){
                                    int index= subscribeItemIDs.indexOf(p.getSkus().get(0));
                                    //if purchase found
                                    if(index>-1 && p.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
                                    {
                                        foundPurchaseSkuId = p.getSkus().get(0);
                                        currentPurchaseToken = p.getPurchaseToken();
                                        break;
                                    }
                                }
                            }

                            if(foundPurchaseSkuId != null) {
                                List<String> skuList = new ArrayList<>();
                                skuList.add(foundPurchaseSkuId);
                                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                                params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);

                                billingClient.querySkuDetailsAsync(params.build(),
                                        new SkuDetailsResponseListener() {
                                            @Override
                                            public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                                             List<SkuDetails> skuDetailsList) {
                                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                    if (skuDetailsList != null && skuDetailsList.size() > 0) {

                                                        currentPlanName = MyUtils.getSkuNameWithoutAppName(ManagePaymentActivity.this, skuDetailsList.get(0).getTitle());
                                                        showPaymentScreen();
                                                    } else {
                                                        //try to add item/product id "s1" "s2" "s3" inside subscription in google play console
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(ManagePaymentActivity.this, String.format(getString(R.string.subscription_not_found), foundPurchaseSkuId), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });

                                                    }
                                                } else {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(ManagePaymentActivity.this,
                                                                    getString(R.string.error_prefix) + billingResult.getDebugMessage() + billingResponseCodeAsString(billingResult.getResponseCode()), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }
                                        });
                            }
                            else {
                                showErrorLayout(getString(R.string.no_purchasable_skus_found));
                            }
                        }
                    });
                }
                else {
                    showErrorLayout(billingResult.getDebugMessage() + billingResponseCodeAsString(billingResult.getResponseCode()));
                }
            }

            @Override
            public void onBillingServiceDisconnected() {

            }
        });



        registerListeners();
    }

    private String billingResponseCodeAsString(int code) {
        switch(code) {
            case -3: return "SERVICE_TIMEOUT";
            case -2: return "FEATURE_NOT_SUPPORTED";
            case -1: return "SERVICE_DISCONNECTED";
            case 0: return "OK";
            case 1: return "USER_CANCELED";
            case 2: return "SERVICE_UNAVAILABLE";
            case 3: return "BILLING_UNAVAILABLE";
            case 4: return "ITEM_UNAVAILABLE";
            case 5: return "DEVELOPER_ERROR";
            case 6: return "ERROR";
            case 7: return "ITEM_ALREADY_OWNED";
            case 8: return "ITEM_NOT_OWNED";
        }
        return "UNKNOWN";
    }

    private void registerListeners() {
        buttonSubscribe3M.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBillingClientReadiness("useapp_3m");
            }
        });
        buttonSubscribe6M.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBillingClientReadiness("useapp_6m");
            }
        });
        buttonSubscribe1Y.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBillingClientReadiness("useapp_1y");
            }
        });

        buttonTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ManagePaymentActivity.this.recreate();
            }
        });

        buttonGoToGooglePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url;
                if (foundPurchaseSkuId == null) {
                    // If the SKU is not specified, just open the Google Play subscriptions URL.
                    url = MyUtils.PLAY_STORE_SUBSCRIPTION_URL;
                } else {
                    // If the SKU is specified, open the deeplink for this SKU on Google Play.
                    url = String.format(MyUtils.PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL,
                            foundPurchaseSkuId, getApplicationContext().getPackageName());
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });
    }

    private void checkBillingClientReadiness(String productId) {
        if (billingClient.isReady()) {
            initiateUpdate(productId);
        }
        //else reconnect service
        else{
            billingClient = BillingClient.newBuilder(ManagePaymentActivity.this).enablePendingPurchases().setListener(ManagePaymentActivity.this).build();
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        initiateUpdate(productId);
                    } else {
                        Toast.makeText(ManagePaymentActivity.this,getString(R.string.error_prefix)+billingResult.getDebugMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onBillingServiceDisconnected() {
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(billingClient!=null){
            billingClient.endConnection();
        }
    }

    private void showPaymentScreen() {
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(subscribeItemIDs).setType(BillingClient.SkuType.SUBS);

        billingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                        for (SkuDetails skuDetail: skuDetailsList) {
                                            Log.d("SKUDETAILS", "onSkuDetailsResponse: "+ skuDetail.getSku());
                                            switch (skuDetail.getSku()) {
                                                case "useapp_3m":
                                                    buttonSubscribe3M.setText(String.format("%s (%s)", MyUtils.getSkuNameWithoutAppName(ManagePaymentActivity.this, skuDetail.getTitle()), skuDetail.getPrice()));
                                                    break;
                                                case "useapp_6m":
                                                    buttonSubscribe6M.setText(String.format("%s (%s)", MyUtils.getSkuNameWithoutAppName(ManagePaymentActivity.this, skuDetail.getTitle()), skuDetail.getPrice()));
                                                    break;
                                                case "useapp_1y":
                                                    buttonSubscribe1Y.setText(String.format("%s (%s)", MyUtils.getSkuNameWithoutAppName(ManagePaymentActivity.this, skuDetail.getTitle()), skuDetail.getPrice()));
                                                    break;
                                            }
                                        }

                                        textViewCurrentPlanName.setText(currentPlanName);

                                        buttonSubscribe3M.setVisibility(View.VISIBLE);
                                        buttonSubscribe6M.setVisibility(View.VISIBLE);
                                        buttonSubscribe1Y.setVisibility(View.VISIBLE);

                                        switch(foundPurchaseSkuId) {
                                            case "useapp_3m":
                                                buttonSubscribe3M.setVisibility(View.GONE);
                                                break;
                                            case "useapp_6m":
                                                buttonSubscribe6M.setVisibility(View.GONE);
                                                break;
                                            case "useapp_1y":
                                                buttonSubscribe1Y.setVisibility(View.GONE);
                                                break;
                                        }

                                        linearLayoutCheckingPayment.setVisibility(View.GONE);
                                        linearLayoutCurrentStatus.setVisibility(View.VISIBLE);
                                        linearLayoutErrorOccurred.setVisibility(View.GONE);
                                    }
                                }
                            });
                        } else {
                            showErrorLayout(billingResult.getDebugMessage() + billingResponseCodeAsString(billingResult.getResponseCode()));
                        }
                    }
                });
    }

    private void showErrorLayout(String debugMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                linearLayoutCheckingPayment.setVisibility(View.GONE);
                linearLayoutCurrentStatus.setVisibility(View.GONE);
                linearLayoutErrorOccurred.setVisibility(View.VISIBLE);

                textViewErrorText.setText(debugMessage);
            }
        });
    }

    private void initiateUpdate(String PRODUCT_ID) {
        List<String> skuList = new ArrayList<>();
        skuList.add(PRODUCT_ID);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);

        billingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setSubscriptionUpdateParams(BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                                            .setOldSkuPurchaseToken(currentPurchaseToken)
                                            .setReplaceSkusProrationMode(BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION).build())
                                    .setSkuDetails(skuDetailsList.get(0))
                                    .build();
                                billingClient.launchBillingFlow(ManagePaymentActivity.this, flowParams);
                            } else {
                                //try to add item/product id "s1" "s2" "s3" inside subscription in google play console
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ManagePaymentActivity.this, String.format(getString(R.string.subscription_not_found), PRODUCT_ID), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ManagePaymentActivity.this,
                                            getString(R.string.error_prefix) + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {

        //if item newly purchased
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases);
        }
        //if item already purchased then check and reflect changes
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> alreadyPurchases) {
                    if(alreadyPurchases!=null){
                        handlePurchases(alreadyPurchases);
                    }
                }
            });
        }
        //if purchase cancelled
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ManagePaymentActivity.this,getString(R.string.purchase_canceled_toast),Toast.LENGTH_SHORT).show();
                }
            });

        }
        // Handle any other error msgs
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //show error
                    Toast.makeText(ManagePaymentActivity.this,getString(R.string.error_prefix)+billingResult.getDebugMessage() + billingResponseCodeAsString(billingResult.getResponseCode()),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    void handlePurchases(List<Purchase> purchases) {
        for(Purchase purchase:purchases) {
            final int index= subscribeItemIDs.indexOf(purchase.getSkus().get(0));
            //purchase found
            if(index>-1) {
                //if item is purchased
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
                {
                    if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
                        // Invalid purchase
                        // show error to user
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ManagePaymentActivity.this, getString(R.string.error_prefix) + getString(R.string.inavlid_purchase), Toast.LENGTH_SHORT).show();
                                returnToPaymentActivity();
                            }
                        });
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
                                        if(billingResult.getResponseCode()==BillingClient.BillingResponseCode.OK){
                                            //if purchase is acknowledged
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(ManagePaymentActivity.this, R.string.subscription_will_be_updated_soon,Toast.LENGTH_SHORT).show();
                                                    ManagePaymentActivity.this.recreate();
                                                }
                                            });

                                        }
                                        else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(ManagePaymentActivity.this, getString(R.string.error_prefix) + billingResult.getDebugMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    }
                                });

                    }
                    //else item is purchased and also acknowledged
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ManagePaymentActivity.this, R.string.subscription_will_be_updated_soon,Toast.LENGTH_SHORT).show();
                                // Restart activity to show latest state
                                ManagePaymentActivity.this.recreate();
                            }
                        });

                    }
                }
                //if purchase is pending
                else if(  purchase.getPurchaseState() == Purchase.PurchaseState.PENDING)
                {
//                    Toast.makeText(ManagePaymentActivity.this,
//                            subscribeItemIDs.get(index)+" Purchase is Pending. Please complete Transaction", Toast.LENGTH_SHORT).show();

                    returnToPaymentActivity();
                }
                //if purchase is refunded or unknown
                else if( purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE)
                {
                    returnToPaymentActivity();
                }
            }

        }
    }

    private void returnToPaymentActivity() {
        Intent intent = new Intent(ManagePaymentActivity.this, PaymentGatewayActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean verifyValidSignature(String signedData, String signature) {
        try {
            // To get key go to Developer Console > Select your app > Development Tools > Services & APIs.
            String base64Key = getResources().getString(R.string.google_billing_licensekey);

            return Security.verifyPurchase(base64Key, signedData, signature);
        } catch (IOException e) {
            return false;
        }
    }
}