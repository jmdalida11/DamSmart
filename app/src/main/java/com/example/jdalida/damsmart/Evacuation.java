package com.example.jdalida.damsmart;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.view.View;
import android.widget.AdapterView;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Evacuation extends AppCompatActivity implements LocationListener, OnMapReadyCallback
{
    GoogleMap mMap;
    ListView lv;
    LocationManager locationManager;
    Location myLocation;

    private ProgressDialog p;


    private ArrayList<HashMap<String, String>> mList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_parent);

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }
        p = new ProgressDialog(Evacuation.this);
        p.setMessage("Loading...");
        p.setCanceledOnTouchOutside(false);
        p.show();
//        // comment starts to remove map
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);
//         comment ends to remove map

        lv = (ListView) findViewById(R.id.list);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> e = (HashMap<String, String>)parent.getItemAtPosition(position);
                openMap(Double.parseDouble(e.get("latitude")), Double.parseDouble(e.get("longitude")));
            }
        });
        getLocation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        getLocation();
        mMap = googleMap;
    }

    protected void createMarker(double lat, double lon) {
        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    private String loadJSONFromAsset()
    {
        String json = null;
        try {
            InputStream is = getAssets().open("EvacuationCenters.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void readEvacuationList()
    {
        try
        {
            JSONObject jsonObj = new JSONObject(loadJSONFromAsset());
            JSONArray places = jsonObj.getJSONArray("EvacuationCenters");

            for (int i = 0; i < places.length(); i++)
            {
                JSONObject p = places.getJSONObject(i);
                JSONArray location = p.getJSONArray("Location");

                Location tempLocation = new Location("");
                tempLocation.setLatitude(location.getDouble(1));
                tempLocation.setLongitude(location.getDouble(0));
                float distance = myLocation.distanceTo(tempLocation) / 1000;
                DecimalFormat df = new DecimalFormat("#.#");

                HashMap<String, String> e = new HashMap<>();
                e.put("name", p.getString("Name"));
                e.put("city", p.getString("City"));
                e.put("category", p.getString("Category"));
                e.put("latitude", location.getString(1));
                e.put("longitude", location.getString(0));
                e.put("distance", df.format(distance) + " km");
                mList.add(e);
            }
        }
        catch (final JSONException e)
        {
            e.printStackTrace();
        }
    }

    protected void displayEvacuationCenters() {
        p.hide();
        ListAdapter adapter = new SimpleAdapter(
                Evacuation.this, mList,
                R.layout.list_item, new String[]{"name", "city", "category", "distance"}, new int[]{R.id.name,
                R.id.city, R.id.category, R.id.distance});
        lv.setAdapter(adapter);
    }

    private void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this);
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(Evacuation.this, "Please Enable GPS", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onLocationChanged(Location location)
    {
        myLocation = location;
        Log.d("myLog", "my latitude " + myLocation.getLatitude());
        Log.d("myLog", "my longitude " + myLocation.getLongitude());
        readEvacuationList();
//        // comment starts to remove map
//        LatLng myPosition = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
//        mMap.addMarker(new MarkerOptions().position(myPosition));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(11.0f));
//        for (int i = 0; i < mList.size(); i++)
//        {
//            mMap.addMarker(new MarkerOptions().position(new LatLng(
//                    Double.parseDouble(mList.get(i).get("latitude")),
//                    Double.parseDouble(mList.get(i).get("longitude")))));
//        }
//        // comment ends to remove map
        displayEvacuationCenters();
    }

    public void openMap(double latitude, double longitude)
    {
        Uri mapUri = Uri.parse("geo:0,0?q=" + latitude +"," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }
}
