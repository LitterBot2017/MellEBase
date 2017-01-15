package edu.cmu.melle.base.mellebase;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener {

    private SensorManager mSensorManager;
    SensorEventListener mSensorListener;

    double mLatitude;
    double mLongitude;
    double mAzimuth;
    double mRoll;
    double mPitch;
    int mNumSatellites;
    int mMode;
    int mCommand=0;

    LocationManager mLocationManager;
    long mLastLocationMillis;
    Location mLastLocation;
    boolean mIsGPSFix;

    ROSBridge mRosBridge;
    String mIpAddress;
    String mPortNumber;

    TextView mLatitudeText;
    TextView mLongitudeText;
    TextView mRollText;
    TextView mPitchText;
    TextView mAzimuthText;
    TextView mNumSatellitesText;
    EditText mIpAddressText;
    Button mGPSMode;
    Button mObstacleMode;
    Button mKeyboardMode;
    ImageButton mUp;
    LowPassFilter filterYaw = new LowPassFilter(0.03f);
    LowPassFilter filterPitch = new LowPassFilter(0.03f);
    LowPassFilter filterRoll = new LowPassFilter(0.03f);

    float Rot[]=null; //for gravity rotational data
    //don't use R because android uses that for other stuff
    float I[]=null; //for magnetic rotational data
    float accels[]=new float[3];
    float mags[]=new float[3];
    float[] values = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Wake lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
        mMode=1;
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        mLatitudeText = (TextView) findViewById(R.id.latitudeText);
        mLongitudeText = (TextView) findViewById(R.id.longitudeText);
        mRollText = (TextView) findViewById(R.id.rollText);
        mPitchText = (TextView) findViewById(R.id.pitchText);
        mAzimuthText = (TextView) findViewById(R.id.azimuthText);
        mNumSatellitesText = (TextView) findViewById(R.id.numSatellitesText);
        mIpAddressText = (EditText) findViewById(R.id.ipAddressText);
        mGPSMode = (Button) findViewById(R.id.gps);
        mObstacleMode = (Button) findViewById(R.id.obstacle);
        mKeyboardMode = (Button) findViewById(R.id.keyboard);

        mGPSMode.setBackgroundColor(Color.argb(255, 0, 255, 0));
        mKeyboardMode.setBackgroundColor(Color.argb(255, 255, 0, 0));
        mObstacleMode.setBackgroundColor(Color.argb(255, 255, 0, 0));

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mIpAddress = "192.168.43.225";
        mPortNumber = "9090";
        mRosBridge = new ROSBridge(mIpAddress, mPortNumber);
        mRosBridge.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    // The following method is required by the SensorEventListener interface;
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void publishToRos() {
        JSONObject json = new JSONObject();
        try {
            json.put("latitude", mLatitude);
            json.put("longitude", mLongitude);
            json.put("azimuth", mAzimuth);
            json.put("roll", mRoll);
            json.put("pitch", mPitch);
            json.put("sats", mNumSatellites);
            json.put("mode",mMode);
            json.put("command",mCommand);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mRosBridge.publishToTopic("/AndroidSensorData", json);
    }

    // The following method is required by the SensorEventListener interface;
    // Hook this event to process updates;
    public void onSensorChanged(SensorEvent event) {


        switch (event.sensor.getType())
        {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = event.values.clone();
                break;
        }

        if (mags != null && accels != null) {
            Rot = new float[9];
            I= new float[9];
            SensorManager.getRotationMatrix(Rot, I, accels, mags);
            // Correct if screen is in Landscape

            float[] outR = new float[9];
            SensorManager.remapCoordinateSystem(Rot, SensorManager.AXIS_X,SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(Rot, values);

            mags = null; //retrigger the loop when things are repopulated
            accels = null; ////retrigger the loop when things are repopulated


            float yaw = (float) (Math.toDegrees(values[0]));
            float pitch = (float) Math.toDegrees(values[1]);
            float roll = (float) Math.toDegrees(values[2]);

            mAzimuth = Math.toDegrees(values[0]);
            if(mAzimuth<0)
                mAzimuth+=360;

            mPitch = Math.toDegrees(values[1]);
            mRoll = Math.toDegrees(values[2]);

            mAzimuthText.setText(Double.toString(mAzimuth));
            mRollText.setText(Double.toString(mRoll));
            mPitchText.setText(Double.toString(mPitch));

            publishToRos();
        }

        if ((SystemClock.elapsedRealtime() - mLastLocationMillis) > 3000) {
            mNumSatellites = 0;
        }
    }

    @Override
    public void onLocationChanged(Location loc) {

        if (loc == null) {
            mNumSatellites = 0;
            return;
        }

        mLastLocationMillis = loc.getTime();
        if ((SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000) {
            mNumSatellites = 1;
        } else {
            mNumSatellites = 0;
        }

        mLatitude = loc.getLatitude();
        mLongitude = loc.getLongitude();

        mLatitudeText.setText("" + mLatitude);
        mLongitudeText.setText("" + mLongitude);
        mNumSatellitesText.setText("" + mNumSatellites);

        publishToRos();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void connect(View view) {
        if(mIpAddressText.getText().toString().equals("")){
            Toast.makeText(getApplicationContext(),"Please input a IP Address",Toast.LENGTH_SHORT).show();
           return;
        }
        mIpAddress=mIpAddressText.getText().toString();
        mPortNumber="9090";
        mRosBridge = new ROSBridge(mIpAddress, mPortNumber);
        mRosBridge.start();

        //mRosBridge.subscribeToTopic("/MellE_msg", "melle");
    }

    public void gps(View view){
        mMode=1;
        mGPSMode.setBackgroundColor(Color.argb(255, 0, 255, 0));
        mObstacleMode.setBackgroundColor(Color.argb(255, 255, 0, 0));
        mKeyboardMode.setBackgroundColor(Color.argb(255, 255, 0, 0));
    }

    public void obstacle(View view){
        mMode=2;
        mGPSMode.setBackgroundColor(Color.argb(255, 255, 0, 0));
        mObstacleMode.setBackgroundColor(Color.argb(255, 0, 255, 0));
        mKeyboardMode.setBackgroundColor(Color.argb(255, 255, 0, 0));
    }

    public void keyboard(View view){
        mMode=3;
        mGPSMode.setBackgroundColor(Color.argb(255, 255, 0, 0));
        mObstacleMode.setBackgroundColor(Color.argb(255, 255, 0, 0));
        mKeyboardMode.setBackgroundColor(Color.argb(255, 0, 255, 0));
    }

    public void up(View view){
        mCommand=1;
    }

    public void down(View view){
        mCommand=2;
    }
    public void left(View view){
        mCommand=3;
    }
    public void right(View view){
        mCommand=4;
    }
    public void strafeLeftUp(View view){
        mCommand=5;
    }
    public void strafeLeftDown(View view){
        mCommand=6;
    }
    public void strafeRightUp(View view){
        mCommand=7;
    }
    public void strafeRightDown(View view){
        mCommand=8;
    }
    public void stop(View view){
        mCommand=0;
    }

}
