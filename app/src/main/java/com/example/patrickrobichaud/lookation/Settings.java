package com.example.patrickrobichaud.lookation;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

// presents configurable options to the user and allows for their modification and saving
public class Settings extends AppCompatActivity {
    EditText sampleinterval;
    Button cleardb, savesettings;
    TextView locationpermission, locationenabled;
    SharedPreferences data;
    Boolean textchanged = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        sampleinterval = (EditText) findViewById(R.id.sampleinterval);
        cleardb = (Button) findViewById(R.id.cleardb);
        locationpermission = (TextView) findViewById(R.id.locationpermission);
        locationenabled = (TextView) findViewById(R.id.locationenabled);
        savesettings = (Button) findViewById(R.id.savesettings);
        data = getSharedPreferences("DATA", Context.MODE_PRIVATE); // load data from Sharedprefs

        // listener for Clear Database & SharedPreferences button
        cleardb.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { getApplicationContext().deleteDatabase("Location_Logs"); data.edit().clear().commit(); } });

        // listener for Save Settings button
        savesettings.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { SaveSettings(); } });

        // listener for Textedit changed
        sampleinterval.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) { }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { textchanged = true; }
        });
        UpdateUI();
    }

    // fills in UI elements
    public void UpdateUI() {
        sampleinterval.setText(Integer.toString(data.getInt("sampleinterval", 0))); // loads stored sampleinterval value into TextEdit

        boolean permission = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED);
        if(permission) locationpermission.setText("granted"); // checks and displays whether location permission is granted
        else locationpermission.setText("denied");

        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(enabled) locationenabled.setText("enabled"); // checks and displays whether location services are enabled
        else locationenabled.setText("disabled");
    }

    // checks if settings have been changed when attempting to go back
    public Boolean SaveSettings() {
        String interval = sampleinterval.getText().toString();
        if(!android.text.TextUtils.isDigitsOnly(interval) || Integer.parseInt(interval) < 10 || Integer.parseInt(interval) > 100000) { // checks if sampleinterval value is valid (numerical and within range)
            Toast.makeText(getApplicationContext(), "Invalid sampling interval.", Toast.LENGTH_LONG).show(); // warns user of invalid value
            return false;
        }
        else {
            SharedPreferences.Editor editor = data.edit();
            editor.putInt("sampleinterval", Integer.parseInt(interval)); // stores valid sampleinterval into sharedprefs
            editor.commit();
            textchanged = false;
            return true;
        }
    }

    // defines behavior of menubar back button to only engage if data has been saved
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        //if(textchanged) Toast.makeText(getApplicationContext(), "Save to Exit.", Toast.LENGTH_LONG).show();
        if(SaveSettings()) finish(); // go back only if settings have not been modified
        //else finish();
        return super.onOptionsItemSelected(item);
    }

    // defines behavior of android back button to only engage if data has been saved
    @Override public void onBackPressed() {
        //if(textchanged) Toast.makeText(getApplicationContext(), "Save to Exit.", Toast.LENGTH_LONG).show();
        if(SaveSettings()) finish(); // go back only if settings have been saved
        //else finish();
    }
}