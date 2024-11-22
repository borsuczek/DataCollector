package com.example.datacollector;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.util.Pair;

public class MainActivity extends AppCompatActivity {

    private SensorDataManager sensorDataManager;
    private LocationManagerWrapper locationManagerWrapper;
    private WifiManagerWrapper wifiManagerWrapper;
    public CacheManager cacheManager;
    private ServerManager serverManager;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private Button stopButton;
    private Button discardButton;
    private Button backButton;
    List<Button> activityButtons;
    private String descriptionUUID = "";
    private Map<String, Integer> activityIds;
    private String description = "";
    private Pair<String, String> descriptionData;
    private Pair<String, String> testData;
    private boolean isDescriptionNew = true;
    String device;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_description);

        checkPermissions();

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        device = manufacturer + " " + model;

        Log.i("model: ", manufacturer + " " + model);

        initializeActivitiesIds();
        descriptionView();

    }

    private void checkPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }
    private void initializeActivitiesIds() {
        activityIds = new HashMap<>();
        activityIds.put("In Elevator", 1);
        activityIds.put("10 Steps", 2);
        activityIds.put("Stairs", 3);
        activityIds.put("Standing", 4);
        activityIds.put("Turning 90 Degrees Left", 5);
        activityIds.put("Turning 90 Degrees Right", 6);
    }

    private void descriptionView(){
        setContentView(R.layout.activity_description);

        EditText descriptionEditText = findViewById(R.id.descriptionEditText);

        Button submitButton = (Button) findViewById(R.id.submitDescriptionButton);

        TextView dateTimeTextView = findViewById(R.id.dateTimeTextView);
        LocalDateTime DateTime = LocalDateTime.now();
        String currentDateTimeDisplay = DateTime.format(DateTimeFormatter.ofPattern("dd-M-yyyy, hh:mm"));
        dateTimeTextView.setText("Date: " + currentDateTimeDisplay);
        String currentDateTime = DateTime.format(DateTimeFormatter.ofPattern("M-dd-yyyy hh:mm:ss"));
        submitButton.setOnClickListener(view -> {
            if (!descriptionEditText.getText().toString().isEmpty()) {
                description = descriptionEditText.getText().toString();
                descriptionUUID = generateUUID();
                //to jakiś check czy już było a jak nie to na koniec
                isDescriptionNew = true;
                descriptionData = new Pair<>("test_descriptions", descriptionUUID + ";" + description + ";" + device  + ";" + currentDateTime + "\n");
                setTestsView(view);
            } else {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setTestsView(View v){
        setContentView(R.layout.activity_main);

        TextView deviceModelTextView = findViewById(R.id.deviceModelTextView);
        deviceModelTextView.setText(description);

        initializeManagers();

        activityButtons = new ArrayList<>();

        stopButton = findViewById(R.id.stopButton);
        discardButton = findViewById(R.id.discardButton);
        activityButtons.add(findViewById(R.id.inElevatorButton));
        activityButtons.add(findViewById(R.id.tenStepsButton));
        activityButtons.add(findViewById(R.id.stairsButton));
        activityButtons.add(findViewById(R.id.standingButton));
        activityButtons.add(findViewById(R.id.turningLeftButton));
        activityButtons.add(findViewById(R.id.turningRightButton));
        backButton = findViewById(R.id.backButton);


        for (Button button : activityButtons) {
            button.setOnClickListener(view -> {
                startDataCollection(button.getText().toString());
            });
        }
        backButton.setOnClickListener(view -> descriptionView());

        stopButton.setOnClickListener(view -> stopDataCollection(view, false));
        discardButton.setOnClickListener(view -> stopDataCollection(view, true));

    }

    private void hideActivityButtons() {
        for (Button button : activityButtons) {
            button.setVisibility(View.GONE);
        }
        backButton.setVisibility(View.GONE);
    }

    private void initializeManagers() {
        cacheManager = new CacheManager(this);
        serverManager = new ServerManager(this, cacheManager);
        sensorDataManager = new SensorDataManager(this, serverManager);
        locationManagerWrapper = new LocationManagerWrapper(this, sensorDataManager);
        wifiManagerWrapper = new WifiManagerWrapper(this, sensorDataManager);
        cacheManager.checkCache();
        Log.i("ddd", "oodod");
    }

    private void startDataCollection(String activity) {
        hideActivityButtons();
        TextView waitingTextView = findViewById(R.id.waitingTextView);
        waitingTextView.setVisibility(View.VISIBLE);
        waitingTextView.setText("Starting in: 3");
//testId
        String testUUID = generateUUID();
        testData = new Pair<>("tests", testUUID + ";" + activityIds.getOrDefault(activity, -1) + ";" + descriptionUUID + "\n");
        Log.i("stop", "gg");
        new CountDownTimer(3000, 1000) {
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000) +1;
                waitingTextView.setText("Starting in: " + secondsLeft);
            }

            public void onFinish() {
                waitingTextView.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                discardButton.setVisibility(View.VISIBLE);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    sensorDataManager.startDataCollection(testUUID);
                    locationManagerWrapper.startLocationUpdates(testUUID);
                    wifiManagerWrapper.startWifiScan(testUUID);
                }, 10);
            }
        }.start();


    }

    private String generateUUID(){
        return UUID.randomUUID().toString();
    }

    private void stopDataCollection(View view, Boolean discard) {
        sensorDataManager.stopDataCollection();
        locationManagerWrapper.stopLocationUpdates();
        wifiManagerWrapper.stopWifiScan();

        stopButton.setVisibility(View.GONE);
        discardButton.setVisibility(View.GONE);
        if(!discard)
            acceptTestView();
        else
            notAccept(view);
    }

    private void acceptTestView(){
        LinearLayout acceptLayout = findViewById(R.id.acceptTestLayout);
        acceptLayout.setVisibility(View.VISIBLE);
        Button acceptButton = (Button) findViewById(R.id.yesButton);
        Button noButton = (Button) findViewById(R.id.noButton);
        acceptButton.setOnClickListener(this::accept);
        noButton.setOnClickListener(this::notAccept);
    }

    private void accept(View view){
        sensorDataManager.writeToDatabase();
        sensorDataManager.clearData();
        setTestsView(view);
    }

    private void notAccept(View view){
        sensorDataManager.clearData();
        setTestsView(view);
    }

    public Pair getTestData(){
        return testData;
    }

    public Pair getDescriptionData(){
        return descriptionData;
    }

    public void setIsDescriptionNew(boolean isDescriptionNew){
        this.isDescriptionNew = isDescriptionNew;
    }

    public boolean getIsDescriptionNew(){
        return isDescriptionNew;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //sensorDataManager.registerSensorListeners();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //sensorDataManager.unregisterSensorListeners();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("permissions", "Permissions granted");
            } else {
                checkPermissions();
            }
        }
    }
}