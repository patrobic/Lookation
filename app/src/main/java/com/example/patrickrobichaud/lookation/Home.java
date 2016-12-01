package com.example.patrickrobichaud.lookation;

import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

// opens when app is first launched, exposes and acts as springboard to core functionality
public class Home extends AppCompatActivity {
    Button createlog, viewlogs, settings;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        createlog = (Button) findViewById(R.id.createlog);
        viewlogs = (Button) findViewById(R.id.viewlogs);
        settings = (Button) findViewById(R.id.settings);

        // listener for Create Log button
        createlog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if(ContextCompat.checkSelfPermission(Home.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) { // check if location permission granted
                    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ){ // check if location services enabled
                        Intent i = new Intent(v.getContext(), Tracking.class);
                        i.putExtra("sampleinterval", GetSettings());
                        startActivity(i); // launch logging activity
                    } else LocationEnabled(); // call function to prompt enable location
                } else LocationPermission(); // call function to request permission
            }
        });

        // listener for View Logs button
        viewlogs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), Display.class);
                startActivity(i);
            }
        });

        // listener for Settings button
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), Settings.class);
                startActivity(i);
            }
        });
    }

    // Loads settings from SharedPreferences
    Integer GetSettings() {
        SharedPreferences data = getSharedPreferences("DATA", Context.MODE_PRIVATE); // access sharedprefs
        if(data.getInt("sampleinterval", 0) == 0) { // check if sampleinterval has never been initialized
            SharedPreferences.Editor editor = data.edit();
            editor.putInt("sampleinterval", 1000); // set default value of 1000ms
            editor.commit();
        }
        return data.getInt("sampleinterval", 0); // return stored or newly set sampleinterval
    }

    // Requests permission to use device location from Android OS
    void LocationPermission() { ActivityCompat.requestPermissions(Home.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0); }

    // Prompt user to enable location with dialog
    void LocationEnabled() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(Home.this).addApi(LocationServices.API).build();
        googleApiClient.connect();
        LocationRequest locationRequest = LocationRequest.create();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                try {
                    status.startResolutionForResult(Home.this, 1000);
                } catch (IntentSender.SendIntentException e) { }
            }
        });
    }
}