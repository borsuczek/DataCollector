package com.example.datacollector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewDebug;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SensorDataManager implements SensorEventListener {

    private SensorManager sensorManager;
    private Context context;
    private boolean isCollectingData = false;
    private String currentTestUUID;
    private Map<Integer, Pair<String, Integer>> sensorInfoMap;
    private Map<String, StringBuilder> sensorDataMap;
    private Pair<String, String> descriptionData = new Pair<>("","");
    private Pair<String, String> testData;
    private boolean isDescriptionNew = true;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SensorDataManager(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorInfoMap = new HashMap<>();
        sensorDataMap = new HashMap<>();
        addSensor(Sensor.TYPE_ACCELEROMETER, "accelerometer", 3);
        addSensor(Sensor.TYPE_GYROSCOPE, "gyroscope", 3);
        addSensor(Sensor.TYPE_MAGNETIC_FIELD, "magnetic_field", 3);
        addSensor(Sensor.TYPE_GRAVITY, "gravity", 3);
        addSensor(Sensor.TYPE_PROXIMITY, "proximity", 1);
        addSensor(Sensor.TYPE_PRESSURE, "pressure", 1);
        addSensor(Sensor.TYPE_LINEAR_ACCELERATION, "linear_acceleration", 3);
        addSensor(Sensor.TYPE_ROTATION_VECTOR, "rotation_vector", 4);
        registerSensorListeners();
    }

    private void addSensor(int sensorType, String sensorName, int values) {
        Log.i("addsensor", sensorName);
        if (sensorManager.getDefaultSensor(sensorType) != null) {
            sensorInfoMap.put(sensorType, new Pair<>(sensorName, values));
            sensorDataMap.put(sensorName, new StringBuilder());
        }
    }

    public void startDataCollection(String testUUID, Pair<String, String> description, Pair<String, String> test) {
        registerSensorListeners();
        isCollectingData = true;
        currentTestUUID = testUUID;
        isDescriptionNew = isDescriptionNew || !Objects.equals(description.second, descriptionData.second);
        descriptionData = description;
        testData = test;
    }

    public void stopDataCollection() {
        isCollectingData = false;
        unregisterSensorListeners();
    }

    public void registerSensorListeners() {
        for (Map.Entry<Integer, Pair<String, Integer>> entry : sensorInfoMap.entrySet()) {
            int sensorType = entry.getKey();
            Sensor sensor = sensorManager.getDefaultSensor(sensorType);

            if (sensor != null) {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
            }
        }
    }

    public void unregisterSensorListeners() {
        for (Map.Entry<Integer, Pair<String, Integer>> entry : sensorInfoMap.entrySet()) {
            int sensorType = entry.getKey();
            Sensor sensor = sensorManager.getDefaultSensor(sensorType);

            if (sensor != null) {
                sensorManager.unregisterListener(this, sensor);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isCollectingData) {
            executorService.submit(() -> {
                Pair<String, Integer> sensorInfo = sensorInfoMap.get(event.sensor.getType());
                if (sensorInfo != null) {
                    String sensorName = sensorInfo.first;
                    String data = formatSensorData(event.values, sensorInfo.second);

                    long currentTimeMillis = System.currentTimeMillis();
                    long sensorTimestampMillis = currentTimeMillis -
                            (SystemClock.elapsedRealtime() - (event.timestamp / 1_000_000L));

                    Instant instant = Instant.ofEpochMilli(sensorTimestampMillis);
                    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
                    String timestampWithMillis = zonedDateTime.format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    );
                    String formattedData = String.format(Locale.US, "%s;%s;%s\n",
                            timestampWithMillis, data, currentTestUUID);

                    sensorDataMap.get(sensorName).append(formattedData);
                }
            });
        }
    }


    private String formatSensorData(float[] values, int length) {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if(i != length-1)
                data.append(String.format(Locale.US, "%g;", values[i]));
            else
                data.append(String.format(Locale.US, "%g", values[i]));
        }
        return data.toString();
    }

    public void writeToDatabase(){
        boolean isDescriptionNewCopy = isDescriptionNew;
        isDescriptionNew = false;
        Map<String, StringBuilder> sensorDataMapCopy = new HashMap<>();
        for (Map.Entry<String, StringBuilder> entry : sensorDataMap.entrySet()) {
            sensorDataMapCopy.put(entry.getKey(), new StringBuilder(entry.getValue()));
        }
        Pair<String, String> testDataCopy = new Pair<>(testData.first, testData.second);
        Pair<String, String> descriptionDataCopy = new Pair<>(descriptionData.first, descriptionData.second);
        Log.i("SensorDataManager", "Description is new = " + isDescriptionNew);
        executorService.submit(() ->{
                if (ServerManager.getInstance(context).isServerAvailable()) {
                    if(isDescriptionNewCopy)
                        ServerManager.getInstance(context).insertData(descriptionDataCopy.first, descriptionDataCopy.second);
                    ServerManager.getInstance(context).insertData(testDataCopy.first, testDataCopy.second);

                    for (Map.Entry<String, StringBuilder> entry : sensorDataMapCopy.entrySet()) {
                        String sensorName = entry.getKey();
                        String formattedData = String.valueOf(sensorDataMapCopy.get(sensorName));
                        if(!formattedData.isEmpty())
                            ServerManager.getInstance(context).insertData(sensorName, formattedData);
                    }
                } else {
                    if(isDescriptionNewCopy)
                        CacheManager.getInstance(context).cacheData(descriptionDataCopy.first, descriptionDataCopy.second);
                    CacheManager.getInstance(context).cacheData(testDataCopy.first, testDataCopy.second);

                    for (Map.Entry<String, StringBuilder> entry : sensorDataMapCopy.entrySet()) {
                        String sensorName = entry.getKey();
                        String formattedData = String.valueOf(sensorDataMapCopy.get(sensorName));
                        if(!formattedData.isEmpty())
                            CacheManager.getInstance(context).cacheData(sensorName, formattedData);

                    }
                }
        });

    }

    public void clearData(){
        for (Map.Entry<String, StringBuilder> entry : sensorDataMap.entrySet()) {
            String sensorName = entry.getKey();
            sensorDataMap.get(sensorName).setLength(0);
        }

    }


    public Map<String, StringBuilder> getSensorDataMap() {
        return sensorDataMap;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
