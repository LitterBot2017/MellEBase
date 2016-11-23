package edu.cmu.melle.base.mellebase;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener {

    private SensorManager mSensorManager;
    SensorEventListener mSensorListener;

    TextView mLatitudeText;
    TextView mLongitudeText;
    TextView mRollText;
    TextView mPitchText;
    TextView mAzimuthText;

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

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        mLatitudeText = (TextView) findViewById(R.id.latitudeText);
        mLongitudeText = (TextView) findViewById(R.id.longitudeText);
        mRollText = (TextView) findViewById(R.id.rollText);
        mPitchText = (TextView) findViewById(R.id.pitchText);
        mAzimuthText = (TextView) findViewById(R.id.azimuthText);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    // The following method is required by the SensorEventListener interface;
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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

            mAzimuthText.setText(Double.toString(Math.toDegrees(values[0])));
            mRollText.setText(Float.toString(filterRoll.lowPass(roll)));
            mPitchText.setText(Float.toString(filterPitch.lowPass(pitch)));



        }
    }

    @Override
    public void onLocationChanged(Location loc) {
        mLatitudeText.setText("" + loc.getLatitude());
        mLongitudeText.setText("" + loc.getLongitude());
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
}