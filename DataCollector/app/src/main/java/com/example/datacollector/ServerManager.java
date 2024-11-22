package com.example.datacollector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ServerManager {
    private final String TAG = "ServerManager";
    private final String SERVER_URL = "http://192.168.1.16:8081";
    private String accessToken = null;
    private String insertUrl = "/insert_data";
    private String deleteUrl = "/delete_data";
    private String publicURL = "/public";
    private CacheManager cacheManager;
    private Context context;

    public ServerManager(Context context, CacheManager cacheManager){
        this.cacheManager = cacheManager;
        this.context = context;
        cacheManager.setServerManager(this);
    }

/*public static int insertData(String tableName, String data) {
        OkHttpClient client = new OkHttpClient();

        Request request = createRequest(tableName, data, "data", insertUrl);

        final CountDownLatch latch = new CountDownLatch(1);
        final int[] insertedId = new int[1];


        if(request == null)
            return -1;
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Insertion failed: " + e.getMessage());
                latch.countDown();
                insertedId[0] = -1;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        insertedId[0] = jsonResponse.getInt("inserted_id");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    Log.i(TAG, "Insertion successful: " + responseBody);
                } else {
                    Log.i("token", String.valueOf(response.code()));
                    if (response.code() == 401) {
                        Log.i(TAG, "Token is invalid, re-authenticating...");
                        accessToken = authenticate("test", "admin");
                        if (accessToken != null) {
                            insertedId[0] = insertData(tableName, data);
                        } else {
                            Log.e(TAG, "Authentication failed");
                            insertedId[0] = -1;
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No response body";
                        Log.e(TAG, "Insertion failed: " + response.code() + " " + response.message() + ". Error: " + errorBody);
                        insertedId[0] = -1;
                    }
                }
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return insertedId[0];
    }*/

    public void insertData(String tableName, String data) {

            OkHttpClient client = new OkHttpClient();

            if (isServerAvailable()) {
                Request request = createRequest(tableName, data, "data", insertUrl);
                if (request != null) {
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            insertionFailed("Insertion failed: " + e.getMessage(), tableName, data);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseBody = response.body().string();
                                Log.i(TAG, "Insertion successful: " + responseBody);
                            } else {
                                Log.i("token", String.valueOf(response.code()));
                                if (response.code() == 401) {
                                    Log.i(TAG, "Token is invalid, re-authenticating...");
                                    accessToken = authenticate("test", "admin");
                                    if (accessToken != null) {
                                        insertData(tableName, data);
                                    } else {
                                        insertionFailed("Authentication failed, cannot retry data insertion.", tableName, data);
                                    }
                                } else {
                                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                                    insertionFailed("Insertion failed: " + response.code() + " " + response.message() + ". Error: " + errorBody, tableName, data);
                                }
                            }
                        }

                    });
                } else
                    insertionFailed("Insertion failed, request is null", tableName, data);
            } else
                insertionFailed("Insertion failed, network not available", tableName, data);

    }
    private void insertionFailed(String message, String tableName, String data){
        Log.e(TAG, message);
        cacheManager.cacheData(tableName, data);
    }

    public void deleteData(String tableName, int id) {
        OkHttpClient client = new OkHttpClient();

        Request request = createRequest(tableName, String.valueOf(id), "id", deleteUrl);
        if(request != null) {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Deletion failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.i(TAG, "Deletion successful: " + responseBody);
                    } else {
                        Log.i("token", String.valueOf(response.code()));
                        if (response.code() == 401) {
                            Log.i(TAG, "Token is invalid, re-authenticating...");
                            accessToken = authenticate("test", "admin");
                            if (accessToken != null) {
                                deleteData(tableName, id);
                            } else {
                                Log.e(TAG, "Authentication failed");
                            }
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "No response body";
                            Log.e(TAG, "Deletion failed: " + response.code() + " " + response.message() + ". Error: " + errorBody);
                        }
                    }
                }
            });
        }
    }

    private Request createRequest(String tableName, String data, String jsonDataName, String url){
        if(data.isEmpty())
            return null;
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("table_name", tableName);
            jsonPayload.put(jsonDataName, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(jsonPayload.toString(), MediaType.get("application/json; charset=utf-8"));

        Request.Builder requestBuilder;
        if(Objects.equals(url, insertUrl)) {
            requestBuilder = new Request.Builder()
                    .url(SERVER_URL + insertUrl)
                    .post(requestBody);
        }
        else {
            requestBuilder = new Request.Builder()
                    .url(SERVER_URL + deleteUrl)
                    .delete(requestBody);
        }

        if (accessToken == null) {
            accessToken = authenticate("test", "admin");
            if (accessToken == null)
                return null;
        }
        requestBuilder.addHeader("Authorization", "Bearer " + accessToken);

        return requestBuilder.build();
    }

    private String authenticate(String username, String password) {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] token = {null};

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL + "/token")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Authentication failed: " + e.getMessage());
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.i(TAG, "Login successful: " + responseData);

                    token[0] = parseTokenFromResponse(responseData);
                } else {
                    Log.e(TAG, "Authentication failed: " + response.code() + " " + response.message());
                }
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return token[0];
    }

    private String parseTokenFromResponse(String responseData) {
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            return jsonObject.getString("access_token");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse token from response: " + e.getMessage());
            return null;
        }
    }
    public boolean isServerAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();

            if (network != null) {
                try {
                    Log.i("Connection", "First success !");
                    URL url = new URL(SERVER_URL+publicURL);
                    HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                    urlc.setConnectTimeout(5 * 1000);
                    urlc.connect();
                    if (urlc.getResponseCode() == 200) {
                        Log.i("Connection", "Success !");
                        return true;
                    } else {
                        return false;
                    }
                } catch (MalformedURLException e1) {
                    return false;
                } catch (IOException e) {
                    return false;
                }
            }
            return false;
        }
}