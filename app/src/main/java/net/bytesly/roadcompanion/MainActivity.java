package net.bytesly.roadcompanion;

import static net.bytesly.roadcompanion.util.MyUtils.SUPPORTED_ACTIVITY_KEY;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;

import net.bytesly.roadcompanion.adapter.ParkingCodesAdapter;
import net.bytesly.roadcompanion.detectedactivity.DetectedActivityReceiver;
import net.bytesly.roadcompanion.detectedactivity.DetectedActivityService;
import net.bytesly.roadcompanion.util.MyUtils;
import net.bytesly.roadcompanion.util.TransitionHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button buttonOpenParking;
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
        textViewParkingStatus.setText(trackingStarted ? getString(R.string.parking_status_enabled_text): getString(R.string.parking_status_disabled_text));
        buttonOpenParking.setText(trackingStarted ? R.string.stop_button: R.string.start_button);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(SUPPORTED_ACTIVITY_KEY)) {
            String action = intent.getStringExtra(SUPPORTED_ACTIVITY_KEY);
            if(action.equals("openParkingApp")) {
                stopService(new Intent(MainActivity.this, DetectedActivityService.class));
                setTrackingStarted(false);
                openParkingApp();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonOpenParking = findViewById(R.id.buttonOpenParking);
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
                String currentCode = codeList.get(recyclerViewParkingCodes.getChildAdapterPosition((View)v.getParent()));
                deleteParkingCode(currentCode);
            }
        });
        recyclerViewParkingCodes.setAdapter(recyclerViewAdapter);

        updateCodeRecycler(codeList);

        if(isServiceRunning()) {
            setTrackingStarted(true);
        }

        buttonAddCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addParkingCode();
            }
        });

        buttonOpenParking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isTrackingStarted) {
                    stopService(new Intent(MainActivity.this, DetectedActivityService.class));
                    setTrackingStarted(false);
                }
                else {
                    if (isTrackingPermissionGranted()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(new Intent(MainActivity.this, DetectedActivityService.class));
                        }
                        else {
                            startService(new Intent(MainActivity.this, DetectedActivityService.class));
                        }
                        setTrackingStarted(true);
                    } else {
                        requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                                MyUtils.TRACKING_PERMISSION_CODE);
                        return;
                    }
                }

                askOpenParking();
            }
        });
    }


    private void openParkingApp() {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage("ge.msda.parking");
        if (intent != null) {
            startActivity(intent);
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
                if(newNumberStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.invalid_code_toast, Toast.LENGTH_SHORT).show();
                }
                else {
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

        if(codeList.size() == 0) {
            recyclerViewParkingCodes.setVisibility(View.GONE);
            noCodesAddedLayout.setVisibility(View.VISIBLE);
        }
        else {
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

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("net.bytesly.roadcompanion.detectedactivity.DetectedActivityService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MyUtils.TRACKING_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, DetectedActivityService.class));
            }
            else {
                startService(new Intent(this, DetectedActivityService.class));
            }
            setTrackingStarted(true);
            askOpenParking();
        }
    }
}