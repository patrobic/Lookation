package com.example.patrickrobichaud.lookation;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

// TODO implement background services to allow location logging to persist

public class Tracking extends AppCompatActivity implements ConnectionCallbacks, LocationListener{
    Button start, stop;
    TextView startpoint, currentlocation, elapsedtime, samplinginterval, locationtime, starttime, numbersamples, lognumber;
    EditText logname;
    String mLastUpdateTime;
    Boolean run;
    Bundle extras;
    Thread TrackerThread;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation = new Location("location provider");
    Location mCurrentLocation = new Location("location provider");
    DatabaseSQL LogStorage;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        extras = getIntent().getExtras();
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        startpoint = (TextView) findViewById(R.id.startpoint);
        currentlocation = (TextView) findViewById(R.id.currentlocation);
        elapsedtime = (TextView) findViewById(R.id.elapsedtime);
        samplinginterval = (TextView) findViewById(R.id.samplinginterval);
        locationtime = (TextView) findViewById(R.id.locationtime);
        starttime = (TextView) findViewById(R.id.starttime);
        numbersamples = (TextView) findViewById(R.id.numbersamples);
        lognumber = (TextView) findViewById(R.id.lognumber);
        logname = (EditText) findViewById(R.id.logname);
        LogStorage = new DatabaseSQL(this);
        stop.setEnabled(false);
        samplinginterval.setText(Integer.toString(extras.getInt("sampleinterval")) + " milliseconds");
        mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000).setFastestInterval(1000);
        if (mGoogleApiClient == null) { mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addApi(LocationServices.API).build(); }

        // listener for Start Tracking button
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (logname.getText().toString().length() > 0 && Character.isAlphabetic(logname.getText().charAt(0))) {
                    run = true; // set boolean value to indicate logging is active
                    stop.setEnabled(true); // enable stop button
                    start.setEnabled(false); // disable stop button
                    (TrackerThread = new Thread() { public void run() { TrackerThread(0, logname.getText().toString()); } }).start(); // run new thread to periodically update and store location in database
                }
                else Toast.makeText(getApplicationContext(), "Invalid log name.", Toast.LENGTH_LONG).show(); // warn user if log name is invalid
            }
        });

        // listener for Stop Tracking button
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                run = false; // set boolean value to indicate running has stopped
                stop.setEnabled(false); // disable stop button
                LogStorage.delayButtonEnable(start, Tracking.this); // launch thread to delay enabling of start button (for reliability: to avoid bugs)
            }
        });

        onStart(); // connect to Google Location API client
    }

    protected void onStart() { mGoogleApiClient.connect(); super.onStart(); }
    protected void onStop() { mGoogleApiClient.disconnect(); super.onStop(); }
    protected void onResume() { super.onResume(); mGoogleApiClient.connect(); super.onStart(); }
    public void onConnectionSuspended (int cause) {}

    // check for location permission and start location updating process
    public void onConnected(Bundle connectionHint) {
        if (ContextCompat.checkSelfPermission(Tracking.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) { // check if permission has been granted
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
        double latitude = mCurrentLocation.getLatitude();
        double longitude = mCurrentLocation.getLongitude();
        int tablenum = LogStorage.CreateLogTable(logname);
        updateUIstatic(String.valueOf(latitude), String.valueOf(longitude), DateFormat.getTimeInstance().format(currentdate), String.valueOf(tablenum)); // set Textviews that are constant during entire log

        while(run){ // keep updating UI and logging location until boolean value is made false by stop button
            currentdate = new Date();
            //String timediff = DateFormat.getTimeInstance().format(currentdate.getTime()-startdate.getTime()); // calculate difference in time between now and start
            String timediff = Long.toString(TimeUnit.MILLISECONDS.toSeconds(currentdate.getTime()-startdate.getTime())); // displaying in raw seconds instead
            latitude = mCurrentLocation.getLatitude();
            longitude = mCurrentLocation.getLongitude();
            LogStorage.CreateEntry(new LogEntry(latitude, longitude, DateFormat.getTimeInstance().format(currentdate)), tablenum); // create new entry in appropriate table in database
            updateUIdynamic(++i, String.valueOf(latitude), String.valueOf(longitude), timediff); // refresh UI Textviews that update for each sample
            android.os.SystemClock.sleep(extras.getInt("sampleinterval")); // make updates intermittent at sample interval specified in settings
        }
    }

    // Set UI elements that change at each sample
    private void updateUIdynamic(final int numsamples, final String latitude, final String longitude, final String timediff) { // set variable data (elapsed time, currentlocation, numbersamples, locationtime)
        runOnUiThread(new Runnable() {
            public void run() {
                currentlocation.setText(latitude + "lat, " + longitude + "lon"); // display current location
                numbersamples.setText(Integer.toString(numsamples) + " samples"); // display number of samples accumulated so far
                locationtime.setText(mLastUpdateTime); // display time at which last location update was received
                elapsedtime.setText(timediff); // display time elapsed since log start
            }
        });
    }

    // set UI elements on first run only
    private void updateUIstatic(final String latitude, final String longitude, final String startdate, final String tablenum) { // set static data (startpoint, starttime, samplinginterval)
        runOnUiThread(new Runnable() {
            public void run() {
                starttime.setText(startdate); // display time at which log started
                startpoint.setText(latitude + "lat, " + longitude + "lon"); // display starting location
                samplinginterval.setText(Integer.toString(extras.getInt("sampleinterval")) + " milliseconds"); // display sampling interval from Sharedprefs
                lognumber.setText(tablenum); // display the index of this log
            }
        });
    }
}