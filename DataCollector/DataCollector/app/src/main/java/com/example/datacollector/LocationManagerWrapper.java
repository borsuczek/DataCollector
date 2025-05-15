package com.example.datacollector;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LocationManagerWrapper implements LocationListener {

    private LocationManager locationManager;
    private SensorDataManager sensorDataManager;
    private String currentTestUUID;
    private boolean isCollectingData = false;
    private String sensorName = "location";

    public LocationManagerWrapper(Context context, SensorDataManager sensorDataManager) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.sensorDataManager = sensorDataManager;
        sensorDataManager.getSensorDataMap().put(sensorName, new StringBuilder());
    }

    public void startLocationUpdates(String testUUID) {
        currentTestUUID = testUUID;
        isCollectingData = true;
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {
            Log.e("LocationManagerWrapper", "Permission denied", e);
        }
    }

    public void stopLocationUpdates() {
        isCollectingData = false;
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {

        if (isCollectingData) {
            long locationTimeMillis = location.getTime();
            Instant instant = Instant.ofEpochMilli(locationTimeMillis);
            ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            String timestampWithMillis = zonedDateTime.format(formatter);

            String data = null;
            Double mslAltitude = null;
            Float mslAltitudeAccuracy = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mslAltitude = location.hasMslAltitude() ? location.getMslAltitudeMeters() : null;
                mslAltitudeAccuracy = location.hasMslAltitudeAccuracy() ? location.getMslAltitudeAccuracyMeters() : null;
            }
            Double altitude = location.hasAltitude() ? location.getAltitude() : null;
            Float verticalAccuracy = location.hasVerticalAccuracy() ? location.getVerticalAccuracyMeters() : null;
            Float accuracy = location.hasAccuracy() ? location.getAccuracy() : null;
            Float speed = location.hasSpeed() ? location.getSpeed() : null;
            Float speedAccuracy = location.hasSpeedAccuracy() ? location.getSpeedAccuracyMetersPerSecond() : null;
            Float bearing = location.hasBearing() ? location.getBearing() : null;
            Float bearingAccuracy = location.hasBearingAccuracy() ? location.getBearingAccuracyDegrees() : null;

            data = String.format(Locale.US, "%s;%g;%g;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s\n",
                    timestampWithMillis,
                    location.getLatitude(),
                    location.getLongitude(),
                    formatNullable(altitude),
                    formatNullable(verticalAccuracy),
                    formatNullable(accuracy),
                    formatNullable(mslAltitude),
                    formatNullable(mslAltitudeAccuracy),
                    formatNullable(speed),
                    formatNullable(speedAccuracy),
                    formatNullable(bearing),
                    formatNullable(bearingAccuracy),
                    location.getProvider(),
                    currentTestUUID
            );

            sensorDataManager.getSensorDataMap().get(sensorName).append(data);
        }
    }

    private String formatNullable(Double value) {
        return (value != null) ? String.format(Locale.US, "%g", value) : "None";
    }

    private String formatNullable(Float value) {
        return (value != null) ? String.format(Locale.US, "%g", value) : "None";
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}
