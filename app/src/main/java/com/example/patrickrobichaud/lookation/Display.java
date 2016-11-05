package com.example.patrickrobichaud.lookation;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.nearby.messages.internal.Update;

import java.util.List;

public class Display extends AppCompatActivity {
    TextView startpoint, endpoint, duration, distance, displacement, starttime, endtime;
    Button delete, showmap;
    Spinner spinner;
    DatabaseSQL LogStorage;
    List<LogEntry> LogList;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        spinner = (Spinner) findViewById(R.id.spinner);
        startpoint = (TextView) findViewById(R.id.startpoint);
        endpoint = (TextView) findViewById(R.id.endpoint);
        starttime = (TextView) findViewById(R.id.starttime);
        endtime = (TextView) findViewById(R.id.endtime);
        duration = (TextView) findViewById(R.id.duration);
        distance = (TextView) findViewById(R.id.distance);
        displacement = (TextView) findViewById(R.id.displacement);
        showmap = (Button) findViewById(R.id.showmap);
        delete = (Button) findViewById(R.id.delete);
        LogStorage = new DatabaseSQL(this);
        UpdateUI(0); // perform initial Spinner and UI population

        // listener for Delete button
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(spinner.getCount() == 0) return; // do not execute if no logs exist
                delete.setEnabled(false);
                int selectedposition = spinner.getSelectedItemPosition(); // get index of currently selected spinner item
                LogStorage.deleteLog(selectedposition); // delete log table and index from database
                UpdateUI(selectedposition); // reload Spinner items and refresh UI Textviews
                if(spinner.getCount() > 0) LogStorage.delayButtonEnable(delete, Display.this); // if items exist, launch thread to delay enabling of delete button (for reliability: to avoid bugs)

            }
        });

        // listener for Show Map button
        showmap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), ShowOnMap.class);

                // TODO find some way to pass start/end points, and an array containing all intermediate waypoints
                i.putExtra("index", spinner.getSelectedItemPosition()); // attempt at solution


                //i.putExtra("startpoint", );  // Extras would work for this purpose (as used in createlog listener on Home Activity), but I believe are far from ideal for this use case
                //i.putExtra("endpoint", );    // other possibilities, pass a List<LogEntry> (using extras or some other way), or...
                //i.putExtra("pointsarray", ); // **BEST WAY IMO** using putExtra, simply pass a Log index, and let the ShowMap class access the database directly and load/display the location points from there
                startActivity(i); // start ShowMap activity
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Refresh Textviews with data of a selected log whenever spinner item is selected
            @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // TODO instead of below statement, access ith element of List<List<LogEntry>> above using int i from onItemSelected function declaration.
                LogList = LogStorage.getEntryList(i); // get list of entries for selected log
                startpoint.setText(LogList.get(0).getLatitude().toString() + "lat, " + LogList.get(0).getLongitude().toString() + "lon");
                endpoint.setText(LogList.get(LogList.size()-1).getLatitude().toString() + "lat, " + LogList.get(LogList.size()-1).getLongitude().toString());
                starttime.setText(LogList.get(0).getDate());
                endtime.setText(LogList.get(LogList.size()-1).getDate());
                //duration.setText(Long.toString(new Date(LogList.get(LogList.size()-1).getDate()).getTime() - new Date(LogList.get(0).getDate()).getTime())); // get time of last and first, convert to Date format and substract... retrieve time, convert to string, and display
                distance.setText(String.valueOf(CalculateDistance())); // distance formula between 0 and size-1 elements
                displacement.setText(String.valueOf(CalculateDisplacement())); // summation of distance formulas between 0:1 (i-1:i), 1:2, ... , size-2:size-1 (int i = 1; i < size-1; i++)
            }
            @Override public void onNothingSelected(AdapterView<?> adapterView) { }
        });
    }
    // TODO create List<List<LogEntry>> instead of loading each List<LogEntry> when new spinner item is selected.

    // populate Spinner with logs on launch, and repopulate whenever item is deleted
    public void UpdateUI(int selecteditem) {
        List<String> LogList = LogStorage.getTableList(); // retrieve  list of log names
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, LogList); // plug list into spinner adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // set dropdown style
        spinner.setAdapter(adapter); // apply adapter to spinner
        if(selecteditem == spinner.getCount())
            selecteditem--; // decrement selected item if nonexistent item is selected (in case of deleting last element)
        spinner.setSelection(selecteditem); // set selected spinner item index
        if(spinner.getCount() == 0) { delete.setEnabled(false); startpoint.setText("--"); endpoint.setText("--"); distance.setText("--"); displacement.setText("--"); duration.setText("--"); } // if no logs exist, set all Textviews to "--"
    }

    // calculate the linear distance between start and end points.
    public double CalculateDistance() {
        return Haversine.haversine(LogList.get(0).getLatitude(), LogList.get(0).getLongitude(), LogList.get(LogList.size()-1).getLatitude(), LogList.get(LogList.size()-1).getLongitude()); // call haversine function on first and last points
        // Obsolute Distance Formula (INCORRECT): Math.sqrt(Math.pow(LogList.get(0).getLatitude() - LogList.get(LogList.size()-1).getLatitude(), 2) + Math.pow(LogList.get(0).getLongitude() - LogList.get(LogList.size()-1).getLongitude(), 2))
    }


    // calculate the total ground covered by summing distance between each pair of points
    public double CalculateDisplacement() {
        Double dist = 0.0, lat, lon, prevlat = LogList.get(0).getLatitude(), prevlon = LogList.get(0).getLongitude();

        for (int i = 1; i < LogList.size(); i++) { // loop through all points starting at 2nd
            lat = LogList.get(i).getLatitude();
            lon = LogList.get(i).getLongitude();
            dist += Haversine.haversine(prevlat, prevlon, lat, lon); // call haversine function on current and previous points
            // Obsolute Distance Formula (INCORRECT): Math.sqrt(Math.pow((lat - prevlat), 2) + Math.pow((lon - prevlon), 2));
        }
        return dist;
    }

    // Haversine distance formula class (adapted from http://rosettacode.org/wiki/Haversine_formula#Java)
    public static class Haversine {
        public static final double R = 6372.8; // In kilometers
        public static double haversine(double lat1, double lon1, double lat2, double lon2) { // main haversine function
            double dLat = Math.toRadians(lat2 - lat1); // finds latitudinal distance in radians
            double dLon = Math.toRadians(lon2 - lon1); // finds longitudinal distance in radians
            lat1 = Math.toRadians(lat1);
            lat2 = Math.toRadians(lat2);

            double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2); // core haversine calculation
            double c = 2 * Math.asin(Math.sqrt(a)); // distance in radians
            return R * c; // distance in km
        }
    }
}