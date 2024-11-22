package com.example.datacollector;

import android.content.Context;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class CacheManager {
    private final String TAG = "CacheManager";
    private File cacheFile;
    private String cacheFileName = "data_cache.txt";
    private Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean cacheExists;
    private ServerManager serverManager;

    public CacheManager(Context context) {
        this.context = context;
        this.cacheFile = new File(this.context.getFilesDir(), cacheFileName);
        Log.i("jhghjj", "kjkjkjk");
    }

    public void checkCache(){
        cacheExists = cacheFile.exists();
        Log.i("jjj", "kjkjkjk");
        if(cacheExists) {
            Log.i("ss","dc");
            //cacheFile.delete();
            //new Thread(this::writeDataFromCache).start();
        }
    }

    public void cacheData(String tableName, String data) {
        try {
            FileWriter writer = new FileWriter(cacheFile, true);
            writer.write(tableName + "|" + data);
            writer.close();
            Log.i(TAG, "Multiline data cached successfully for table: " + tableName);
            if(!cacheExists) {
                cacheExists = true;
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to cache data: " + e.getMessage());
        }
    }

    private Map<String, StringBuilder> readDataFromCache() {
        Map<String, StringBuilder> cacheDataMap = new HashMap<>();
        if (cacheFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
                String line;
                String tableName = null;
                StringBuilder dataStringBuilder = null;
                String dataLine;

                while ((line = reader.readLine()) != null) {
                    if (line.contains("|")) {
                        String[] tableData = line.split("\\|", 2);
                        tableName = tableData[0];
                        dataLine = tableData[1] + "\n";
                        dataStringBuilder = new StringBuilder();
                        cacheDataMap.put(tableName, dataStringBuilder);
                        dataStringBuilder.append(dataLine);
                    } else if (tableName != null) {
                        dataLine = line + "\n";
                        dataStringBuilder.append(dataLine);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to read cache: " + e.getMessage());
            }
        }
        if (cacheFile.exists()) {
            cacheFile.delete();
            stopWriteCacheTask();
        }
        return cacheDataMap;
    }

    private void writeDataFromCache() {
        Log.i("llsls", "dcjcdnj");
        Map<String, StringBuilder> cacheDataMap = readDataFromCache();

        for (Map.Entry<String, StringBuilder> entry : cacheDataMap.entrySet()) {
            String tableName = entry.getKey();
            String data = entry.getValue().toString();
            Log.i("ldlld" +tableName, tableName);
            serverManager.insertData(tableName, data);
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetwork() != null;
    }

    public void writeCacheTask() {
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if(cacheExists){
//                    Log.i("cache", "cacjen");
//                    if (isNetworkAvailable()) {
//                        writeDataFromCache();
//                        Log.i("cache", "cacje");
//                    }
//                    handler.postDelayed(this, 10000);
//                }
//                else
//                    Log.i("cache", "cggacje");
//            }
//        }, 10000);
    }

    public void stopWriteCacheTask(){
        cacheExists = false;
    }

    public void setServerManager(ServerManager serverManager){
        this.serverManager = serverManager;
    }
}
