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
    private Object quaternionLock = new Object();

    /**
     * Constructor
     * @param sensorManager Android SensorManager {@linkplain android.hardware.SensorManager}
     */
    public QuaternionSensor(SensorManager sensorManager) {
        mSensorManager = sensorManager;
        mAcce = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mSensorManager.registerListener(this, mAcce, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_GAME);

        nativeReset();
    }

    /**
     * Callback-based method to get quaternion data, Register a listener to get quaternion data
     * @param listener {@linkplain com.citrus.slam.MahonyAHRS.QuaternionSensor}
     */
    public void registerListener(QuaternionEventListener listener) {
        mListener = listener;
    }

    /**
     * Unregister the listener which is getting quaternion data
     * @param listener {@linkplain com.citrus.slam.MahonyAHRS.QuaternionSensor}
     */
    public void unregisterListener(QuaternionEventListener listener) {
        mListener = null;
        mSensorManager.unregisterListener(this);
        mSensorManager.unregisterListener(this);
    }

    /**
     * Polling-based method to get quaternion data, get the pose data available
     * @return The quaternion representing pose at this time, 4 element float array <w, x, y, z>
     */
    public float[] getPoseData() {
        float res[] = new float[4];
        synchronized (quaternionLock) {
            res[0] = q[0];
            res[1] = q[1];
            res[2] = q[2];
            res[3] = q[3];
        }
        return res;
    }

    /**
     * Reset the state of IMU Mahony pose estimator
     */
    public void resetIMU() {
        nativeReset();
    }

    /**
     * Reset the baseline of Vision correction
     */
    public void resetVisionCorrection() {
        nativeResetVision();
    }

    /**
     * Pass a image to correct the drift of IMU pose estimator
     * @param imageArray The image array data form camera **preview** {@linkplain android.hardware.Camera}
     * @param width The width of the image data
     * @param height The height of the image data
     */
    public void visionCorrect(byte[] imageArray, int width, int height) {
        nativeUpdateVision(imageArray, width, height);
    }

    private void updateOrentation() {
        synchronized (quaternionLock) {
            nativeUpdateIMU(val, q);
        }
        count++;
        if (count == 500) {
            Log.d(TAG, "acceleration: " + val[0] + " " + val[1] + " " + val[2]);
            Log.d(TAG, "gyroscope: " + val[3] + " " + val[4] + " " + val[5]);
            Log.d(TAG, "q: " + q[0] + " " + q[1] + " " + q[2] + " " + q[3]);
            count = 0;
        }
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

    private native void nativeReset();
    private native void nativeUpdateIMU(float[] IMUval, float[] q);

    private native void nativeResetVision();
    private native void nativeUpdateVision(byte[] imageArray, int width, int height);
}
