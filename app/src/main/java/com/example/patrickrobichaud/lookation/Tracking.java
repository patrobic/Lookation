package com.example.patrickrobichaud.lookation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import java.util.Date;

public class Tracking extends AppCompatActivity {
    Button start, stop;
    TextView startpoint, currentlocation, elapsedtime, samplinginterval, locationtime, starttime, numbersamples, lognumber;
    EditText logname;
    Bundle extras;
    Intent bgservice;
    public static Boolean run;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
        stop.setEnabled(false);
        samplinginterval.setText(Integer.toString(extras.getInt("sampleinterval")) + " milliseconds");
        run = false;
        logname.setText(new Date().toString());

        // listener for Start Tracking button
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (logname.getText().toString().length() > 0){// && logname.getText().toString().matches("^[a-zA-Z0-9]*$")) {
                    run = true; // set boolean value to indicate logging is active
                    stop.setEnabled(true); // enable stop button
                    start.setEnabled(false); // disable stop button

                    bgservice = new Intent(Tracking.this, BGService.class); // create intent for background logging service
                    bgservice.putExtra("sampleinterval", extras.getInt("sampleinterval")); // send sample interval
                    bgservice.putExtra("logname", logname.getText().toString()); // send log name
                    startService(bgservice); // launch the background service
                }
                else Toast.makeText(getApplicationContext(), "Invalid log name.", Toast.LENGTH_LONG).show(); // warn user if log name is invalid
            }
        });

        // listener for Stop Tracking button
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                run = false; // set boolean value to indicate running has stopped
                stop.setEnabled(false); // disable stop button
                delayButtonEnable(start, Tracking.this); // launch thread to delay enabling of start button (for reliability: to avoid bugs)
                logname.setText(new Date().toString()); // generate new log name with current time
                stopService(bgservice);
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override public void onReceive(Context context, final Intent intent) {
                        runOnUiThread(new Runnable() { public void run() {
                        currentlocation.setText(intent.getStringExtra("currentlocation")); // display current location
                        numbersamples.setText(intent.getStringExtra("numbersamples")); // display number of samples accumulated so far
                        locationtime.setText(intent.getStringExtra("locationtime")); // display time at which last location update was received
                        elapsedtime.setText(intent.getStringExtra("elapsedtime")); // display time elapsed since log start
                        } });
                    }
                }, new IntentFilter("updateUIdynamic")
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override public void onReceive(Context context, final Intent intent) {
                        runOnUiThread(new Runnable() { public void run() {
                            starttime.setText(intent.getStringExtra("starttime")); // display time at which log started
                            startpoint.setText(intent.getStringExtra("startpoint")); // display starting location
                            samplinginterval.setText(Integer.toString(extras.getInt("sampleinterval")) + " milliseconds"); // display sampling interval from Sharedprefs
                            lognumber.setText(intent.getStringExtra("lognumber")); // display the index of this log
                        } });
                    }
                }, new IntentFilter("updateUIstatic")
        );
    }

    // function that accepts a button and its activity, and reenables the button in a thread after specified delay
    public static void delayButtonEnable(final Button button, final Activity activity) {
        Thread delay;
        (delay = new Thread() { public void run() {
            android.os.SystemClock.sleep(300);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    button.setEnabled(true);
                }
            });
        } }).start();
    }

    @Override public void onBackPressed() {
        if(!run) finish(); // go back only if settings have been saved
        else Toast.makeText(getApplicationContext(), "Stop logging first.", Toast.LENGTH_LONG).show();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if(!run) finish(); // go back only if settings have not been modified
        else Toast.makeText(getApplicationContext(), "Stop logging first.", Toast.LENGTH_LONG).show();
        return super.onOptionsItemSelected(item);
    }
}