package com.example.jdalida.damsmart;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private LocationListener locationListener;

    private static final String HOST = "tcp://broker.mqtt-dashboard.com:1883";
    private String clientId;
    private MqttAndroidClient client;
    private MqttConnectOptions options;


    //private String USER = "rescuer";
    private String USER = "rescuee";
    private ArrayList<String> locations;


    private ImageView evaCenter;
    private CoordinatorLayout bg;
    private Button resLoc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locations = new ArrayList<>();

        bg = (CoordinatorLayout) findViewById(R.id.bg);
        evaCenter = (ImageView) findViewById(R.id.evaCenter);
        resLoc = (Button) findViewById(R.id.resLoc);

        resLoc.setTextColor(Color.WHITE);

        evaCenter.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try{
                    Intent intent = new Intent(getApplicationContext(), Evacuation.class);
                    startActivity(intent);
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

        resLoc.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                openMap();
            }
        });

        if(!isRescuee()){
            bg.setBackgroundResource(R.mipmap.alert0rescuer_01);
        }

        setUp();

        try{
            Intent i = getIntent();
            if(i != null){
                String lvl = i.getStringExtra("lvl");
                setUI(lvl);
            }
        }catch (Exception e){
            //Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
        }


    }

    private void setUp() {

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), HOST, clientId);

        options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);

        connect();
        initLocation();
    }

    private void setUI(String waterLevel){
        if(isRescuee()){
            if(waterLevel.equals("0")){
                bg.setBackgroundResource(R.mipmap.alert0_01);
                resLoc.setVisibility(View.GONE);
            }else if(waterLevel.equals("1")){
                bg.setBackgroundResource(R.mipmap.alert1_01);
                resLoc.setVisibility(View.GONE);
            }else if(waterLevel.equals("2")){
                bg.setBackgroundResource(R.mipmap.alert2_01);
                resLoc.setVisibility(View.GONE);
            }else if(waterLevel.equals("3")){
                bg.setBackgroundResource(R.mipmap.alert3_01);
                resLoc.setVisibility(View.VISIBLE);
            }
        }else{
            if(waterLevel.equals("0")){
                bg.setBackgroundResource(R.mipmap.alert0rescuer_01);
                resLoc.setVisibility(View.GONE);
            }else if(waterLevel.equals("1")){
                bg.setBackgroundResource(R.mipmap.alert1rescuer_01);
                resLoc.setVisibility(View.GONE);
            }else if(waterLevel.equals("2")){
                bg.setBackgroundResource(R.mipmap.alert2rescuer_01);
                resLoc.setVisibility(View.GONE);
            }else if(waterLevel.equals("3")){
                resLoc.setVisibility(View.GONE);
                bg.setBackgroundResource(R.mipmap.alert3rescuer_01);
                resLoc.setText("View Location of Rescuees");
                resLoc.setVisibility(View.VISIBLE);
            }
        }

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        setUp();
    }

    private void showNotification(String msg, Class c, String waterLvl) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, clientId);
        builder.setSmallIcon(R.mipmap.alert0);
        if(waterLvl.equals("1")){
            builder.setSmallIcon(R.mipmap.alert1);
        }else if(waterLvl.equals("2")){
            builder.setSmallIcon(R.mipmap.alert2);
        }else if(waterLvl.equals("3")){
            builder.setSmallIcon(R.mipmap.alert3);
        }
        builder.setContentTitle("Water Level Alert");
        builder.setContentText(msg);
        Intent i = new Intent(this, c);
        i.putExtra("lvl", waterLvl);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(c);
        stackBuilder.addNextIntent(i);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0, builder.build());
    }

    private void connect() {
        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    setSubscription("damsmart/alert/level");
                    if(isRescuee()){
                        setSubscription("damsmart/rescuer/location");
                    }else{
                        setSubscription("damsmart/rescuee/location");
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        setClientCallBack();
    }

    private void setClientCallBack() {
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_LONG).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if (topic.equals("damsmart/alert/level")) {

                    if(message.toString().equals("0")){
                        setUI("0");
                        showNotification("Water Levels are normal", MainActivity.class, "0");
                    } else if(message.toString().equals("1")) {
                        showNotification("Alert Level 1", MainActivity.class, "1");
                        setUI("1");
                    } else if (message.toString().equals("2")) {
                        showNotification("Alert Level 2", MainActivity.class, "2");
                        sendLocation();
                        setUI("2");
                    } else if (message.toString().equals("3")) {
                        showNotification("Alert Level 3", MainActivity.class, "3");
                        sendLocation();
                        setUI("3");
                    }
                    vib();
                }

                if (topic.equals("damsmart/rescuer/location") && isRescuee()) {
                    locations.add(message.toString());
                }
                if (topic.equals("damsmart/rescuee/location") && !isRescuee()) {
                    locations.add(message.toString());
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    private void openMap() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10);
            }
            return;
        }
        final ProgressDialog p = new ProgressDialog(MainActivity.this);
        p.setMessage("Loading...");
        p.setCanceledOnTouchOutside(false);
        try{
            Criteria criteria = new Criteria();
            LocationListener ll = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    p.hide();
                    String loc = location.getLatitude() + "_" + location.getLongitude();
                    locations.add(0, loc);
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    intent.putStringArrayListExtra("loc", locations);
                    startActivity(intent);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };

            locationManager.requestSingleUpdate(new Criteria(), ll, null);
            p.show();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
        }

    }

    private void vib(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            v.vibrate(500);
        }
    }

    private boolean isRescuee(){
        return USER.equals("rescuee");    }

    private void sendLocation(){
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10);
            }
            return;
        }

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            locationManager.requestSingleUpdate(new Criteria(), locationListener, null);
        }else {
            Toast.makeText(getApplicationContext(), "Please turn on GPS", Toast.LENGTH_SHORT).show();
        }
    }

    private void initLocation() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(location != null){
                    String data = location.getLatitude() + "_" + location.getLongitude();
                    String topic = "damsmart/" + USER + "/location";
                    pub(topic, data);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

//////////////////////////////////////////////////////
    private void pub(String topic, String msg){
        try{
            client.publish(topic, msg.getBytes(), 0, false);
        }catch (MqttException e){
            Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setSubscription(final String topic){
        try{
            IMqttToken subToken = client.subscribe(topic, 0);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                   // Toast.makeText(getApplicationContext(), "Successfully Subscribe to " + topic, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    String err = "Can't find rescues nearby";
                    if(!isRescuee()){
                        err = "Can't find rescuees nearby";
                    }
                    Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
                }
            });
        }catch (MqttException e){
            Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void unSubscription(final String topic){
        try{
            IMqttToken subToken = client.unsubscribe(topic);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(getApplicationContext(), "Successfully UnSubscribe to " + topic, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Toast.makeText(getApplicationContext(), "Cant UnSubscribe to " + topic, Toast.LENGTH_LONG).show();
                }
            });
        }catch (MqttException e){
            Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    private void disconnect(){
        try {
            IMqttToken token = client.disconnect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getApplicationContext(), "Unable to disconnect", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

}
