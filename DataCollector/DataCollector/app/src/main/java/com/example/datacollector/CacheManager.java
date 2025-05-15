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
    private static CacheManager instance;

    private CacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.cacheFile = new File(this.context.getFilesDir(), cacheFileName);
        Log.i(TAG, "CacheManager initialized");
    }

    public static synchronized CacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new CacheManager(context);
        }
        return instance;
    }

    public void checkCache(){
        cacheExists = cacheFile.exists();
        if(cacheExists) {
            Log.i(TAG,"Cache exists");
            //cacheFile.delete();
            new Thread(this::readDataFromCache).start();
        }
    }

    public void cacheData(String tableName, String data) {
        try {
            if(data != null) {
                FileWriter writer = new FileWriter(cacheFile, true);
                writer.write("|" + tableName + "\n" + data);
                writer.close();
                Log.i(TAG, "Data cached successfully for table: " + tableName);
                if (!cacheExists) {
                    cacheExists = true;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to cache data: " + e.getMessage());
        }
    }

    private void readDataFromCache() {
        if(ServerManager.getInstance(context).isServerAvailable()) {
            File tempFile = new File(cacheFile.getParent(), "temp_cache.txt");
            cacheFile.renameTo(tempFile);
            cacheFile = new File(this.context.getFilesDir(), cacheFileName);
            if (tempFile.exists()) {

                try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                    String line;
                    String tableName = null;
                    StringBuilder data = null;

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("|")) {
                            if (tableName != null) {
                                ServerManager.getInstance(context).insertData(tableName, data.toString());
                                data.setLength(0);
                            }
                            data = new StringBuilder();
                            tableName = line.substring(1).trim();
                        } else if (tableName != null) {
                            data.append(line).append("\n");
                        }
                    }

                    if (tableName != null) {
                        ServerManager.getInstance(context).insertData(tableName, data.toString());
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Failed to read cache: " + e.getMessage());
                }
            }
            if (tempFile.exists()) {
                tempFile.delete();
                stopWriteCacheTask();
            }
        }
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
}
