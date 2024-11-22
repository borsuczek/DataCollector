package com.example.datacollector;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.core.app.ActivityCompat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WifiManagerWrapper {

    private WifiManager wifiManager;
    private String currentTestUUID;
    private boolean isCollectingData = false;
    private Context context;
    private String sensorName = "wifi";
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final long SCAN_INTERVAL_MS = 5000;

    private Map<String, Pair<Integer, Integer>> previousScanResults = new HashMap<>();

    private SensorDataManager sensorDataManager;

    public WifiManagerWrapper(Context context, SensorDataManager sensorDataManager) {
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.sensorDataManager = sensorDataManager;
        this.context = context;
        sensorDataManager.getSensorDataMap().put(sensorName, new StringBuilder());
    }

    public void startWifiScan(String testUUID) {
        currentTestUUID = testUUID;
        isCollectingData = true;
        Log.i("wifi", "dd");
        scanWifiNetworks();
    }

    public void stopWifiScan() {
        isCollectingData = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void scanWifiNetworks() {
        if (wifiManager.isWifiEnabled()) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();
            Set<String> currentNetworkData = new HashSet<>();

            for (ScanResult result : results) {
                if (isCollectingData) {
                    String networkKey = result.SSID + ";" + result.BSSID;
                    int currentSignalStrength = result.level;
                    int currentFrequency = result.frequency;

                    if (!previousScanResults.containsKey(networkKey) ||
                            previousScanResults.get(networkKey).first != currentSignalStrength ||
                            previousScanResults.get(networkKey).second != currentFrequency) {

                        long currentTimeMillis = System.currentTimeMillis();
                        Instant instant = Instant.ofEpochMilli(currentTimeMillis);
                        String timestampWithMillis = instant.toString();
//                        long locationTimeMillis = result.timestamp;
//                        Instant instant = Instant.ofEpochMilli(locationTimeMillis);
//                        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
//                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
//                        String timestampWithMillis = zonedDateTime.format(formatter);

                        String data = String.format(Locale.US, "%s;%s;%s;%d;%d;%s\n",
                                timestampWithMillis,
                                result.SSID,
                                result.BSSID,
                                result.level,
                                result.frequency,
                                currentTestUUID);

                        Log.i("WifiData", data);
                        sensorDataManager.getSensorDataMap().get(sensorName).append(data);

                        previousScanResults.put(networkKey, new Pair<>(currentSignalStrength, currentFrequency));
                    }
                    currentNetworkData.add(networkKey);
                }
            }
            if (isCollectingData) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanWifiNetworks();
                    }
                }, SCAN_INTERVAL_MS);
            }
        }
    }

}
