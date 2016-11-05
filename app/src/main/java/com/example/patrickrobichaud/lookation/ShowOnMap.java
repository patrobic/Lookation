package com.example.patrickrobichaud.lookation;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.List;

public class ShowOnMap extends FragmentActivity implements OnMapReadyCallback {
    DatabaseSQL LogStorage;
    List<LogEntry> LogList;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_on_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        Intent intent = getIntent(); // receive intent from parent class Display
        Integer index = intent.getIntExtra("index", 0); // get Extra passed via intent
        LogStorage = new DatabaseSQL(this);
        LogList = LogStorage.getEntryList(index);
        mapFragment.getMapAsync(this); // get map ready and call onMapReady() to plot data
    }

    // TODO receive and parse the Log's location points
    // TODO plot the first and last points as pins/markers
    // TODO draw a line between all points sequentially [0(first)-1, 1-2, 2-3, (n-2)-(n-1), (n-1)-n(last)]

    /* Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng start = new LatLng(LogList.get(0).getLatitude(), LogList.get(0).getLongitude()); // get start position marker
        LatLng end = new LatLng(LogList.get(LogList.size()-1).getLatitude(), LogList.get(LogList.size()-1).getLongitude()); // get end position marker
        mMap.addMarker(new MarkerOptions().position(start).title("A (Start)")); // plot start position
        mMap.addMarker(new MarkerOptions().position(end).title("B (End)")); // plot end position

        LatLngBounds.Builder builder = new LatLngBounds.Builder().include(start).include(end); // create bounding box builder containing start and end points
        LatLngBounds bounds = builder.build(); // create bounding box delimited by start and end points
        // TODO builder bounding box does not account for protruding path (BUG: if path deviates significantly from linear A to B, distant portions may get cut off)
        // SOLUTION: just include all intermediate waypoints in builder, which guarantees that each path segment will ALSO be in the bounding box!

        int width = getResources().getDisplayMetrics().widthPixels; // get dimensions of device display
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.10); // padding coefficient to set screen margins around plotted objects
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding); // configure camera control
        mMap.animateCamera(cu); // fly over to specified bounding box smoothly
    }
}
