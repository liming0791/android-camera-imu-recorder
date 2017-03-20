package org.andresoviedo.app.model3D.controller;

import android.app.Activity;
import android.hardware.SensorManager;

import com.citrus.slam.MahonyAHRS.*;

import org.andresoviedo.app.camera.CameraManager;

/**
 * Created by liming on 17-2-6.
 */
public class SensorController implements QuaternionEventListener, CameraManager.PreviewDataListener {

    private final Activity view;

    private QuaternionSensor mQuaternionSensor;

    private float val[] = new float[6];
    private float q[] = new float[4];


    public SensorController(Activity view){
        super();
        this.view = view;

        SensorManager manager = (SensorManager)(view.getSystemService(view.SENSOR_SERVICE));
        mQuaternionSensor = new QuaternionSensor(manager);
        mQuaternionSensor.registerListener(this);
    }

    public void onStop() {
        mQuaternionSensor.unregisterListener(this);
    }

    @Override
    public void onQuaternionEvent(float[] q) {
        //Log.i("SensorController", "q: " + q[0] + " " + q[1] + " " + q[2] + " " + q[3]);
    }

    @Override
    public void onPreviewData(byte[] imageArray, int width, int height) {
        mQuaternionSensor.visionCorrect(imageArray, width, height);
    }

    public void resetVision() {
        mQuaternionSensor.resetVisionCorrection();
    }
}
