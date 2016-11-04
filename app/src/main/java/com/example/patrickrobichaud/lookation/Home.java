package com.example.patrickrobichaud.lookation;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;

// opens when app is first launched, exposes and acts as springboard to core functionality
public class Home extends AppCompatActivity {
    Button createlog, viewlogs, settings;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        createlog = (Button) findViewById(R.id.createlog);
        viewlogs = (Button) findViewById(R.id.viewlogs);
        settings = (Button) findViewById(R.id.settings);

        LocationPermission(); // Call permission checks

        // listener for Create Log button
        createlog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), Tracking.class);
                i.putExtra("sampleinterval", GetSettings());
                startActivity(i);
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
}
