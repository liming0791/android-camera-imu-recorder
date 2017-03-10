package org.andresoviedo.app.model3D.controller;

import android.content.Context;
import android.content.pm.LabeledIntent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.util.Log;
import com.citrus.slam.MahonyAHRS.*;

import org.andresoviedo.app.model3D.MainActivity;
import org.andresoviedo.app.model3D.view.ModelRenderer;
import org.andresoviedo.app.model3D.view.ModelSurfaceView;

/**
 * Created by liming on 17-2-6.
 */
public class SensorController implements QuaternionEventListener {

    private final ModelSurfaceView view;
    private final ModelRenderer renderer;

    private QuaternionSensor mQuaternionSensor;

    private float val[] = new float[6];
    private float q[] = new float[4];


    public SensorController(ModelSurfaceView view, ModelRenderer renderer){
        super();
        this.view = view;
        this.renderer = renderer;

        SensorManager manager = (SensorManager)(view.getModelActivity().getSystemService(view.getModelActivity().SENSOR_SERVICE));
        mQuaternionSensor = new QuaternionSensor(manager);
        mQuaternionSensor.registerListener(this);
    }

    public void onStop() {
        mQuaternionSensor.unregisterListener(this);
    }

    @Override
    public void onQuaternionEvent(float[] q) {
        //Log.i("SensorController", "q: " + q[0] + " " + q[1] + " " + q[2] + " " + q[3]);
        if (this.renderer.getCamera()!=null) {
            this.renderer.getCamera().rotateCamera(q[0], q[1], q[2], q[3]);
        }
    }
}
