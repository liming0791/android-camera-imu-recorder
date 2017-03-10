package org.andresoviedo.app.camera;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private CameraManager mCameraManager;

    private Context context;

    public CameraPreview(Context context, CameraManager cameraManager) {
        super(context);

        this.context = context;
        this.mCameraManager = cameraManager;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = this.getHolder();
        mHolder.addCallback(this);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCameraManager.setSurfaceHolder(mHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        float previewRatio = (float)height / (float)width;
        if (previewRatio < 1)
            previewRatio = 1.f /previewRatio;
        Log.d("CameraPreview", "surface Size: " + height + " " + width
                + " previewRatio: " + previewRatio);
        mCameraManager.startCamera(context, previewRatio);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

}
