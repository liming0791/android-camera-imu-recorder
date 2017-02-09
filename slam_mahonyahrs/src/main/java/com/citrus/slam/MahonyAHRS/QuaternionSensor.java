package com.citrus.slam.MahonyAHRS;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by liming on 17-2-8.
 */
public class QuaternionSensor implements SensorEventListener {

    static {
        System.loadLibrary("MahonyAHRS");
    }

    final private String TAG = "QuaternionSensor";

    private SensorManager mSensorManager;
    private Sensor mAcce;
    private Sensor mGyro;

    private QuaternionEventListener mListener;

    private float[] val = new float[6];
    private float[] q = new float[4];

    private int count = 0;

    private void updateOrentation() {
        nativeUpdateIMU(val, q);
        count++;
        if (count == 500) {
            Log.d(TAG, "val: " + val[0] + " " + val[1] + " " + val[2]);
            Log.d(TAG, "q: " + q[0] + " " + q[1] + " " + q[2] + " " + q[3]);
            count = 0;
        }
    }

    public QuaternionSensor(SensorManager sensorManager) {
        mSensorManager = sensorManager;
        mAcce = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mSensorManager.registerListener(this, mAcce, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_GAME);

    }

    public void registerListener(QuaternionEventListener listener) {
        mListener = listener;
    }

    public void unregisterListener(QuaternionEventListener listener) {
        mListener = null;
        mSensorManager.unregisterListener(this);
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            val[0] = event.values[0];
            val[1] = event.values[1];
            val[2] = event.values[2];
        }
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            val[3] = event.values[0];
            val[4] = event.values[1];
            val[5] = event.values[2];

            updateOrentation();

            if (mListener!=null) {
                mListener.onQuaternionEvent(q);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public native void nativeReset();
    public native void nativeUpdateIMU(float[] IMUval, float[] q);
}
