package com.example.datacollector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
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

public class SensorDataManager implements SensorEventListener {

    private SensorManager sensorManager;
    private Context context;
    private boolean isCollectingData = false;
    private String currentTestUUID;
    private Map<Integer, Pair<String, Integer>> sensorInfoMap;
    private Map<String, StringBuilder> sensorDataMap;
    private MainActivity mainActivity;
    private ServerManager serverManager;

    public SensorDataManager(Context context, ServerManager serverManager) {
        this.context = context;
        this.mainActivity = (MainActivity) context;
        this.serverManager = serverManager;
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

    public void startDataCollection(String testUUID) {
        registerSensorListeners();
        isCollectingData = true;
        currentTestUUID = testUUID;
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
            Pair<String, Integer> sensorInfo = sensorInfoMap.get(event.sensor.getType());
            if (sensorInfo != null) {
                String sensorName = sensorInfo.first;
                String data = formatSensorData(event.values, sensorInfo.second);

                long currentTimeMillis = System.currentTimeMillis();
                Instant instant = Instant.ofEpochMilli(currentTimeMillis);
                String timestampWithMillis = instant.toString();
//                long locationTimeMillis = event.timestamp;
//                Instant instant = Instant.ofEpochMilli(locationTimeMillis);
//                ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
//                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
//                String timestampWithMillis = zonedDateTime.format(formatter);
                String formattedData = String.format(Locale.US, "%s;%s;%s\n",
                        timestampWithMillis, data, currentTestUUID);

                Log.i("sensorData", formattedData);

                sensorDataMap.get(sensorName).append(formattedData);
            }
        }
    }


    private String formatSensorData(float[] values, int length) {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if(i != length-1)
                data.append(String.format(Locale.US, "%f;", values[i]));
            else
                data.append(String.format(Locale.US, "%f", values[i]));
        }
        return data.toString();
    }

    public void writeToDatabase(){
        Map<String, StringBuilder> sensorDataMapCopy = new HashMap<>();
        for (Map.Entry<String, StringBuilder> entry : sensorDataMap.entrySet()) {
            sensorDataMapCopy.put(entry.getKey(), new StringBuilder(entry.getValue()));
        }

        boolean isDescriptionNewCopy = mainActivity.getIsDescriptionNew();
        mainActivity.setIsDescriptionNew(false);
        Log.i("sss", String.valueOf(isDescriptionNewCopy));
        new Thread( () -> {
                if (serverManager.isServerAvailable()) {
                    //CountDownLatch latch = new CountDownLatch(2);
                    if(isDescriptionNewCopy)
                        serverManager.insertData(mainActivity.getDescriptionData().first.toString(), mainActivity.getDescriptionData().second.toString());
                    serverManager.insertData(mainActivity.getTestData().first.toString(), mainActivity.getTestData().second.toString());
//                    try {
//                        latch.await();
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
                    for (Map.Entry<String, StringBuilder> entry : sensorDataMapCopy.entrySet()) {

                        String sensorName = entry.getKey();
                        String formattedData = String.valueOf(sensorDataMapCopy.get(sensorName));
                        Log.i("dat", sensorName);

                        serverManager.insertData(sensorName, formattedData);

                    }
                } else {
                    mainActivity.cacheManager.cacheData(mainActivity.getTestData().first.toString(), mainActivity.getTestData().second.toString());
                    mainActivity.cacheManager.cacheData(mainActivity.getDescriptionData().first.toString(), mainActivity.getDescriptionData().second.toString());
                    for (Map.Entry<String, StringBuilder> entry : sensorDataMapCopy.entrySet()) {

                        String sensorName = entry.getKey();
                        String formattedData = String.valueOf(sensorDataMapCopy.get(sensorName));

                        Log.i("dat", formattedData);

                        mainActivity.cacheManager.cacheData(sensorName, formattedData);

                    }
                }

        }).start();

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
