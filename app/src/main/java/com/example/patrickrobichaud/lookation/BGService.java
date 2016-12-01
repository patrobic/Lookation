package com.example.patrickrobichaud.lookation;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BGService extends Service implements GoogleApiClient.ConnectionCallbacks, LocationListener {
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation = new Location("location provider");
    Location mCurrentLocation = new Location("location provider");
    String mLastUpdateTime;
    Thread TrackerThread;
    DatabaseSQL LogStorage;
    Boolean run;
    Bundle extras;

    @Override public void onCreate() { }

    @Override public int onStartCommand(final Intent intent, int flags, int startId) {
        extras = intent.getExtras();
        LogStorage = new DatabaseSQL(this);
        mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000).setFastestInterval(1000);
        if (mGoogleApiClient == null) { mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addApi(LocationServices.API).build(); }
        mGoogleApiClient.connect();
        (TrackerThread = new Thread() { public void run() { TrackerThread(0, extras.getString("logname")); } }).start(); // run new thread to periodically update and store location in database
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() { mGoogleApiClient.disconnect(); }

    public void onConnectionSuspended(int cause) { }

    // check for location permission and start location updating process
    public void onConnected(Bundle connectionHint) {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) { // check if permission has been granted
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient); // set the last known location
            startLocationUpdates(); // start receiving periodic location updates from services
        }
    }

    // update and store current location when new location is received
    public void onLocationChanged(Location location) {
        mCurrentLocation = location; // store fresh current location
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date()); // store time at which location was determined
    }

    // tell location services to start requesting and storing location
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this); // request location updates through FusedAPI
    }

    // thread responsible for intercepting location and storing updates into database
    protected void TrackerThread(int i, String logname) {
        Date startdate = new Date(); // set persistent start date for comparison
        Date currentdate = new Date();
        while(mCurrentLocation.getLatitude() == 0 || mCurrentLocation.getLongitude() == 0) {} // wait until lat/long are NOT null
        double latitude = mCurrentLocation.getLatitude();
        double longitude = mCurrentLocation.getLongitude();
        int tablenum = LogStorage.CreateLogTable(logname);
        updateUIstatic(String.valueOf(latitude), String.valueOf(longitude), DateFormat.getTimeInstance().format(currentdate), String.valueOf(tablenum)); // set Textviews that are constant during entire log

        while (Tracking.run) { // keep updating UI and logging location until boolean value is made false by stop button
            currentdate = new Date();
            String timediff = Long.toString(TimeUnit.MILLISECONDS.toSeconds(currentdate.getTime() - startdate.getTime())); // displaying in raw seconds instead
            latitude = mCurrentLocation.getLatitude();
            longitude = mCurrentLocation.getLongitude();
            LogStorage.CreateEntry(new LogEntry(latitude, longitude, DateFormat.getTimeInstance().format(currentdate)), tablenum); // create new entry in appropriate table in database
            updateUIdynamic(++i, String.valueOf(latitude), String.valueOf(longitude), timediff); // refresh UI Textviews that update for each sample
            android.os.SystemClock.sleep(extras.getInt("sampleinterval")); // make updates intermittent at sample interval specified in settings
        }
    }

    // Set UI elements that change at each sample
    private void updateUIdynamic(final int numsamples, final String latitude, final String longitude, final String timediff) { // set variable data (elapsed time, currentlocation, numbersamples, locationtime)
        Intent intent = new Intent("updateUIdynamic");
        intent.putExtra("currentlocation", latitude + "lat, " + longitude + "lon");
        intent.putExtra("numbersamples", Integer.toString(numsamples) + " samples");
        intent.putExtra("locationtime", mLastUpdateTime);
        intent.putExtra("elapsedtime", timediff + " seconds");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // set UI elements on first run only
    public void updateUIstatic(final String latitude, final String longitude, final String startdate, final String tablenum) { // set static data (startpoint, starttime, samplinginterval)
        Intent intent = new Intent("updateUIstatic");
        intent.putExtra("starttime", startdate);
        intent.putExtra("startpoint", latitude + "lat, " + longitude + "lon");
        intent.putExtra("samplinginterval", extras.getInt("sampleinterval"));
        intent.putExtra("lognumber", tablenum);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder { BGService getService() { return BGService.this; } }
    @Override public IBinder onBind(Intent intent) { return mBinder; }
    private final IBinder mBinder = new LocalBinder();
}